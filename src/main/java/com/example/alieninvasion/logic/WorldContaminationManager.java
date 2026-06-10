package com.example.alieninvasion.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class WorldContaminationManager {
    private WorldContaminationManager() {}

    private static final int SURFACE_PER_TICK = 32;
    private static final int ORE_PER_TICK     = 4;

    // LinkedHashSet: O(1) dedup on add, FIFO iteration order, single-threaded (main thread only)
    private static final LinkedHashSet<ChunkPos> SURFACE_QUEUE = new LinkedHashSet<>();
    private static final LinkedHashSet<ChunkPos> ORE_QUEUE     = new LinkedHashSet<>();

    public static float getTarget(int day) {
        return Math.min(1.0f, Math.max(0f, (day * 15f - 5f) / 100f));
    }

    /** Called by ServerChunkEvents.CHUNK_LOAD. Surface applied immediately; ores queued. */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int day = SurvivalManager.getDay(level);
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cp = chunk.getPos();

        if (day >= 1 && data.getSurfaceDay(cp) < day) {
            applySurface(level, chunk, day, data);
        }
        if (day >= 2 && data.getOreDay(cp) < day) {
            ORE_QUEUE.add(cp);
        }
    }

    /**
     * Called when the invasion day increments. Re-queues loaded chunks so already-visible
     * areas update to the new contamination level. Ore is only re-queued when crossing
     * conversion thresholds (days 2, 3, 4) — not on every day change.
     */
    public static void onDayChange(ServerLevel level, int prevDay, int newDay) {
        int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 12);
        boolean oreNeeded = needsOreUpdate(prevDay, newDay);
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                    if (level.getChunkSource().getChunkNow(cp.x, cp.z) != null) {
                        SURFACE_QUEUE.add(cp);
                        if (oreNeeded) ORE_QUEUE.add(cp);
                    }
                }
            }
        }
    }

    /** Process queued chunks. Call once per world tick from END_WORLD_TICK. */
    public static void tickQueues(ServerLevel level) {
        int day = SurvivalManager.getDay(level);
        if (day < 1) return;

        ChunkContaminationData data = ChunkContaminationData.get(level);

        int s = 0;
        Iterator<ChunkPos> sit = SURFACE_QUEUE.iterator();
        while (s++ < SURFACE_PER_TICK && sit.hasNext()) {
            ChunkPos pos = sit.next(); sit.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) applySurface(level, chunk, day, data);
        }

        if (day >= 2) {
            int o = 0;
            Iterator<ChunkPos> oit = ORE_QUEUE.iterator();
            while (o++ < ORE_PER_TICK && oit.hasNext()) {
                ChunkPos pos = oit.next(); oit.remove();
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) applyOres(level, chunk, day, data);
            }
        }
    }

    private static void applySurface(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        if (data.getSurfaceDay(cpos) >= day) return;

        float target = getTarget(day);
        if (target <= 0f) { data.setSurfaceDay(cpos, day); return; }

        long chunkSeed = cpos.toLong() * 0x9E3779B97F4A7C15L;

        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;

                int x = cpos.getMinBlockX() + bx;
                int z = cpos.getMinBlockZ() + bz;
                BlockPos bp = new BlockPos(x, y, z);
                BlockState state = chunk.getBlockState(bp);

                if (ContaminationRules.isContaminated(state)) continue;
                BlockState converted = ContaminationRules.contaminatedStateFor(state);
                if (converted == null) continue;

                long h = (chunkSeed ^ bp.asLong()) * 0x6C62272E07BB0142L;
                h ^= (h >>> 33); h *= 0xFF51AFD7ED558CCDL; h ^= (h >>> 33);
                float p = ((h >>> 1) & 0x7FFFFFFFL) / (float) 0x7FFFFFFFL;

                if (p < target) {
                    // flag 34 = UPDATE_CLIENTS(2) | UPDATE_SUPPRESS_LIGHT(32): no neighbor cascade, no lighting
                    level.setBlock(bp, converted, 34);
                }
            }
        }

        data.setSurfaceDay(cpos, day);
    }

    private static void applyOres(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        if (data.getOreDay(cpos) >= day) return;

        int x0 = cpos.getMinBlockX();
        int z0 = cpos.getMinBlockZ();
        int yMin = level.getMinBuildHeight();

        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                int x = x0 + bx;
                int z = z0 + bz;
                for (int y = yMin; y < 64; y++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(bp);
                    BlockState converted = ContaminationRules.oreConversionFor(state, day);
                    if (converted != null) {
                        level.setBlock(bp, converted, 34);
                    }
                }
            }
        }

        data.setOreDay(cpos, day);
    }

    private static boolean needsOreUpdate(int prevDay, int newDay) {
        return (prevDay < 2 && newDay >= 2)
            || (prevDay < 3 && newDay >= 3)
            || (prevDay < 4 && newDay >= 4);
    }
}
