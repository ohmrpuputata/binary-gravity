package com.example.alieninvasion.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Pre-contaminates chunks when they load so that the world's infection level
 * always matches the current invasion day, regardless of when the area was explored.
 *
 * Surface layer (top 4 blocks): probabilistic, seeded per-block so the same
 * chunk always looks identical at the same day.
 *
 * Underground ores (Y_MIN..63): converted 100% per ContaminationRules.oreConversionFor(),
 * which applies day-gated conversion thresholds (day 2, 3, 4).
 */
public final class WorldContaminationManager {
    private WorldContaminationManager() {}

    private static final int SURFACE_PER_TICK = 8;
    private static final int ORE_PER_TICK     = 2;

    private static final Queue<ChunkPos> SURFACE_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Queue<ChunkPos> ORE_QUEUE     = new ConcurrentLinkedQueue<>();

    /**
     * Target contamination fraction (0..1) for a given invasion day.
     * Matches user's stated schedule: day 6=70%, day 7=85%, day 8=100%.
     * SurvivalManager.getDay() is 0-based, so "Day 6" = getDay()=5.
     */
    public static float getTarget(int day) {
        return Math.min(1.0f, Math.max(0f, (day * 15f - 5f) / 100f));
    }

    /** Called by ServerChunkEvents.CHUNK_LOAD for Overworld chunks. */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int day = SurvivalManager.getDay(level);
        if (day < 1) return;
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cp = chunk.getPos();
        if (data.getSurfaceDay(cp) < day) SURFACE_QUEUE.offer(cp);
        if (day >= 2 && data.getOreDay(cp) < day) ORE_QUEUE.offer(cp);
    }

    /**
     * Called when the invasion day increments. Re-queues all chunks within
     * server view distance of every online player so currently loaded areas
     * update to the new contamination level.
     */
    public static void onDayChange(ServerLevel level, int newDay) {
        int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 12);
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                    SURFACE_QUEUE.offer(cp);
                    if (newDay >= 2) ORE_QUEUE.offer(cp);
                }
            }
        }
    }

    /** Process queued chunks. Call once per world tick from END_WORLD_TICK. */
    public static void tickQueues(ServerLevel level) {
        int day = SurvivalManager.getDay(level);
        if (day < 1) return;

        int s = 0;
        while (s++ < SURFACE_PER_TICK && !SURFACE_QUEUE.isEmpty()) {
            ChunkPos pos = SURFACE_QUEUE.poll();
            if (pos == null) break;
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) applySurface(level, chunk, day);
        }

        if (day >= 2) {
            int o = 0;
            while (o++ < ORE_PER_TICK && !ORE_QUEUE.isEmpty()) {
                ChunkPos pos = ORE_QUEUE.poll();
                if (pos == null) break;
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) applyOres(level, chunk, day);
            }
        }
    }

    private static void applySurface(ServerLevel level, LevelChunk chunk, int day) {
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cpos = chunk.getPos();
        if (data.getSurfaceDay(cpos) >= day) return;

        float target = getTarget(day);
        if (target <= 0f) { data.setSurfaceDay(cpos, day); return; }

        long chunkSeed = cpos.toLong() * 0x9E3779B97F4A7C15L;

        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                int x = cpos.getMinBlockX() + bx;
                int z = cpos.getMinBlockZ() + bz;

                for (int dy = 0; dy >= -3; dy--) {
                    int y = surfaceY + dy;
                    if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) continue;
                    BlockPos bp = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(bp);

                    if (ContaminationRules.isContaminated(state)) continue;
                    BlockState converted = ContaminationRules.contaminatedStateFor(state);
                    if (converted == null) continue;

                    // Deterministic per-block hash (Murmur3-like finaliser, no heap allocation)
                    long h = (chunkSeed ^ bp.asLong()) * 0x6C62272E07BB0142L;
                    h ^= (h >>> 33);
                    h *= 0xFF51AFD7ED558CCDL;
                    h ^= (h >>> 33);
                    float p = ((h >>> 1) & 0x7FFFFFFFL) / (float) 0x7FFFFFFFL;

                    if (p < target) {
                        level.setBlock(bp, converted, Block.UPDATE_ALL);
                    }
                }
            }
        }

        data.setSurfaceDay(cpos, day);
    }

    private static void applyOres(ServerLevel level, LevelChunk chunk, int day) {
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cpos = chunk.getPos();
        if (data.getOreDay(cpos) >= day) return;

        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                int x = cpos.getMinBlockX() + bx;
                int z = cpos.getMinBlockZ() + bz;
                for (int y = level.getMinBuildHeight(); y < 64; y++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(bp);
                    BlockState converted = ContaminationRules.oreConversionFor(state, day);
                    if (converted != null) {
                        level.setBlock(bp, converted, Block.UPDATE_ALL);
                    }
                }
            }
        }

        data.setOreDay(cpos, day);
    }
}
