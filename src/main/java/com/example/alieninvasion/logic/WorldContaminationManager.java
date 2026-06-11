package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * Global day-driven world apocalypse. Every chunk, on load and on day change, is
 * brought up to the current day's level of ruin: infested ground (3 deep), dead
 * purple tree canopies, impact craters with toxic pools, scattered remains, and
 * day-gated ore corruption across the full world height.
 *
 * Design rules that keep it correct and cheap:
 *  - CHUNK_LOAD only ENQUEUES work; blocks are written later from END_WORLD_TICK
 *    (writing during chunk promotion can cascade-load neighbours and stall).
 *  - All writes use SET_FLAGS (no neighbour shape updates), so converting the
 *    edge of a chunk never touches a chunk that isn't loaded.
 *  - Column selection is a deterministic hash of (chunk, column): as the day
 *    target grows the same columns stay converted and new ones join in, so the
 *    corruption only ever spreads, never flickers.
 *  - Per-chunk progress persists in ChunkContaminationData ("surface day"
 *    watermark), so chunks are never reprocessed for a day they already had,
 *    and craters are carved exactly once.
 */
public final class WorldContaminationManager {
    private WorldContaminationManager() {}

    /** UPDATE_CLIENTS(2) | UPDATE_KNOWN_SHAPE(16) | UPDATE_SUPPRESS_DROPS(32). */
    private static final int SET_FLAGS = 2 | 16 | 32;

    private static final int MAX_SURFACE_CHUNKS_PER_TICK = 16;
    private static final int MAX_ORE_CHUNKS_PER_TICK = 2;
    /** Hard cap on block writes per tick so a day-change wave never freezes the server. */
    private static final int WRITE_BUDGET_PER_TICK = 4096;

    /** How deep below the surface the infestation reaches. */
    private static final int SURFACE_DEPTH = 3;
    /** Max crater attempts rolled per chunk (each has its own activation day). */
    private static final int CRATER_SLOTS = 3;

    // LinkedHashSet: O(1) dedup on add, FIFO iteration order, single-threaded (main thread only)
    private static final LinkedHashSet<ChunkPos> SURFACE_QUEUE = new LinkedHashSet<>();
    private static final LinkedHashSet<ChunkPos> ORE_QUEUE     = new LinkedHashSet<>();

    private static int lastKnownDay = -1;
    private static int writesThisTick;

    /** Fraction of the surface eaten by the corruption on day N: the whole world by day 5. */
    public static float getTarget(int day) {
        if (day <= 0) return 0f;
        switch (day) {
            case 1:  return 0.15f;
            case 2:  return 0.35f;
            case 3:  return 0.60f;
            case 4:  return 0.85f;
            default: return 1.0f;
        }
    }

    /** Called by ServerChunkEvents.CHUNK_LOAD. Only enqueues — never writes blocks. */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int day = SurvivalManager.getDay(level);
        if (day < 1) return;
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cp = chunk.getPos();
        if (data.getSurfaceDay(cp) < day) SURFACE_QUEUE.add(cp);
        if (day >= 2 && data.getOreDay(cp) < day) ORE_QUEUE.add(cp);
    }

    /** Clears all session state. Called on world unload so queues never leak into another world. */
    public static void onWorldUnload() {
        SURFACE_QUEUE.clear();
        ORE_QUEUE.clear();
        lastKnownDay = -1;
    }

    /** Call once per world tick from END_WORLD_TICK (Overworld only). */
    public static void tick(ServerLevel level) {
        int day = SurvivalManager.getDay(level);
        if (day != lastKnownDay) {
            onDayChange(level, lastKnownDay, day);
            lastKnownDay = day;
        }
        if (day < 1) return;

        writesThisTick = 0;
        ChunkContaminationData data = ChunkContaminationData.get(level);

        int processed = 0;
        Iterator<ChunkPos> sit = SURFACE_QUEUE.iterator();
        while (processed < MAX_SURFACE_CHUNKS_PER_TICK && writesThisTick < WRITE_BUDGET_PER_TICK && sit.hasNext()) {
            ChunkPos pos = sit.next();
            sit.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) {
                applySurface(level, chunk, day, data);
                processed++;
            }
        }

        if (day >= 2) {
            processed = 0;
            Iterator<ChunkPos> oit = ORE_QUEUE.iterator();
            while (processed < MAX_ORE_CHUNKS_PER_TICK && writesThisTick < WRITE_BUDGET_PER_TICK && oit.hasNext()) {
                ChunkPos pos = oit.next();
                oit.remove();
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) {
                    applyOres(level, chunk, day, data);
                    processed++;
                }
            }
        }
    }

    /**
     * Re-queues loaded chunks around players so already-visible areas catch up to the
     * new day immediately (this is what makes "/invasion set 5" visibly rot the world
     * around you within seconds). Ore re-queues only when crossing conversion thresholds.
     */
    private static void onDayChange(ServerLevel level, int prevDay, int newDay) {
        if (newDay < 1) return;
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

    private static void applySurface(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        int prevDay = data.getSurfaceDay(cpos);
        if (prevDay >= day) return;

        float target = getTarget(day);
        long chunkSeed = mix(cpos.toLong() * 0x9E3779B97F4A7C15L);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        if (target > 0f) {
            for (int bx = 0; bx < 16; bx++) {
                for (int bz = 0; bz < 16; bz++) {
                    int x = cpos.getMinBlockX() + bx;
                    int z = cpos.getMinBlockZ() + bz;

                    // One deterministic roll per column: the same columns stay infected
                    // as the target grows, so the corruption only ever expands.
                    long h = mix(chunkSeed ^ BlockPos.asLong(x, 0, z));
                    float p = ((h >>> 1) & 0x7FFFFFFFL) / (float) 0x7FFFFFFFL;
                    if (p >= target) continue;

                    int groundY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                    if (groundY < minY || groundY >= maxY) continue;

                    // Ground: top SURFACE_DEPTH blocks, so slopes, cliffs and shallow
                    // digs read as infected too — not just a one-block paint job.
                    boolean groundConverted = false;
                    for (int dy = 0; dy < SURFACE_DEPTH; dy++) {
                        int y = groundY - dy;
                        if (y < minY) break;
                        cursor.set(x, y, z);
                        BlockState state = chunk.getBlockState(cursor);
                        if (ContaminationRules.isContaminated(state)) {
                            groundConverted = true;
                            continue;
                        }
                        BlockState converted = ContaminationRules.contaminatedStateFor(state);
                        if (converted == null) {
                            if (dy == 0) break; // water / snow / unconvertible top: skip column
                            continue;
                        }
                        if (chunk.getBlockEntity(cursor) != null) continue;
                        converted = copyAxis(state, converted);
                        place(level, new BlockPos(x, y, z), converted);
                        groundConverted = true;
                    }

                    // Tree canopy: MOTION_BLOCKING includes leaves, NO_LEAVES does not —
                    // everything between the two heightmaps is the tree. Whole trees turn
                    // at once (same column roll), which reads far better than noise.
                    int canopyY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    if (canopyY > groundY && canopyY < maxY) {
                        int stopY = Math.max(groundY + 1, canopyY - 16);
                        for (int y = canopyY; y >= stopY; y--) {
                            cursor.set(x, y, z);
                            BlockState state = chunk.getBlockState(cursor);
                            if (state.isAir() || ContaminationRules.isContaminated(state)) continue;
                            BlockState converted = null;
                            if (state.is(BlockTags.LEAVES)) {
                                converted = ModBlocks.INFESTED_LEAVES.defaultBlockState();
                            } else if (state.is(BlockTags.LOGS)) {
                                converted = copyAxis(state, ModBlocks.INFESTED_LOG.defaultBlockState());
                            }
                            if (converted != null && chunk.getBlockEntity(cursor) == null) {
                                place(level, new BlockPos(x, y, z), converted);
                            }
                        }
                    }

                    // Scattered remains: rare contaminated bones on the dead ground.
                    if (groundConverted && day >= 2 && ((h >>> 40) & 0x3FL) == 0L && groundY + 1 < maxY) {
                        cursor.set(x, groundY + 1, z);
                        if (chunk.getBlockState(cursor).isAir()) {
                            place(level, new BlockPos(x, groundY + 1, z),
                                    ModBlocks.CONTAMINATED_BONES.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Impact craters — the "holes" punched into the world by the orbital bombardment.
        carveCraters(level, chunk, day, prevDay, chunkSeed);

        data.setSurfaceDay(cpos, day);
    }

    /**
     * Deterministic crater slots per chunk. Each slot has a fixed position, radius and
     * activation day; a slot is carved exactly once — when the chunk's surface watermark
     * first crosses its activation day. Filling a crater back in is permanent: it is
     * never re-carved, and slots near player block entities (chests etc.) are skipped.
     */
    private static void carveCraters(ServerLevel level, LevelChunk chunk, int day, int prevDay, long chunkSeed) {
        if (day < 2) return;
        ChunkPos cpos = chunk.getPos();
        int minY = level.getMinBuildHeight();

        for (int slot = 0; slot < CRATER_SLOTS; slot++) {
            long h = mix(chunkSeed ^ (0xC2B2AE3D27D4EB4FL * (slot + 1)));
            if (((h >>> 4) & 0xFFL) >= 100L) continue;            // ~39% of slots exist
            int activationDay = 2 + (int) ((h >>> 12) % 5L);       // strikes land on days 2..6
            if (activationDay > day || activationDay <= prevDay) continue;

            int radius = 3 + (int) ((h >>> 20) % 3L);              // 3..5
            // Keep the whole bowl inside this chunk so carving never touches a neighbour.
            int span = Math.max(1, 14 - 2 * radius);
            int cx = radius + 1 + (int) ((h >>> 28) % span);
            int cz = radius + 1 + (int) ((h >>> 36) % span);
            boolean toxic = ((h >>> 44) & 3L) != 0L && day >= 3;   // most craters pool toxic water

            int wx = cpos.getMinBlockX() + cx;
            int wz = cpos.getMinBlockZ() + cz;
            int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            if (surfaceY <= minY + radius + 1 || surfaceY >= level.getMaxBuildHeight() - 8) continue;

            BlockPos center = new BlockPos(wx, surfaceY, wz);
            if (!chunk.getBlockState(center).getFluidState().isEmpty()) continue; // no underwater craters

            // Never carve through player storage/workstations.
            boolean blocked = false;
            for (BlockPos bePos : chunk.getBlockEntitiesPos()) {
                if (Math.abs(bePos.getX() - wx) <= radius + 1 && Math.abs(bePos.getZ() - wz) <= radius + 1
                        && bePos.getY() >= surfaceY - radius - 2 && bePos.getY() <= surfaceY + 6) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) continue;

            carveBowl(level, chunk, wx, surfaceY, wz, radius, toxic);
        }
    }

    private static void carveBowl(ServerLevel level, LevelChunk chunk, int wx, int surfaceY, int wz,
                                  int radius, boolean toxic) {
        int minY = level.getMinBuildHeight();
        int floorY = Math.max(minY + 1, surfaceY - radius);
        int clearTop = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 6);
        int r2 = radius * radius;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r2) continue;
                int x = wx + dx;
                int z = wz + dz;

                // Parabolic bowl: full depth at the centre, fading to nothing at the rim.
                int dig = (int) Math.round(radius * (1.0 - (double) d2 / r2));
                if (dig <= 0) {
                    // Scorched rim: convert the top block ringing the hole.
                    int rimY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
                    if (rimY <= minY || rimY >= level.getMaxBuildHeight()) continue;
                    cursor.set(x, rimY, z);
                    BlockState rimState = chunk.getBlockState(cursor);
                    BlockState rimConverted = ContaminationRules.contaminatedStateFor(rimState);
                    if (rimConverted != null && chunk.getBlockEntity(cursor) == null) {
                        place(level, new BlockPos(x, rimY, z), copyAxis(rimState, rimConverted));
                    }
                    continue;
                }

                int bottom = Math.max(floorY, surfaceY - dig);
                for (int y = clearTop; y > bottom; y--) {
                    cursor.set(x, y, z);
                    BlockState state = chunk.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(level, cursor) < 0.0F) break; // bedrock: stop this column
                    if (chunk.getBlockEntity(cursor) != null) continue;
                    place(level, new BlockPos(x, y, z), air);
                }

                // Line the bowl floor with corruption.
                cursor.set(x, bottom, z);
                BlockState floorState = chunk.getBlockState(cursor);
                if (!floorState.isAir() && floorState.getFluidState().isEmpty()
                        && floorState.getDestroySpeed(level, cursor) >= 0.0F
                        && chunk.getBlockEntity(cursor) == null) {
                    BlockState lining = ContaminationRules.contaminatedStateFor(floorState);
                    place(level, new BlockPos(x, bottom, z),
                            lining != null ? lining : ModBlocks.INFESTED_STONE.defaultBlockState());
                }

                // Toxic pool in the deepest part of the bowl (walls contain it).
                if (toxic && dig >= radius - 1 && d2 <= Math.max(1, (radius - 2) * (radius - 2))) {
                    cursor.set(x, bottom + 1, z);
                    if (chunk.getBlockState(cursor).isAir()) {
                        place(level, new BlockPos(x, bottom + 1, z), ModBlocks.TOXIC_WATER.defaultBlockState());
                    }
                }
            }
        }
    }

    /** Full-height ore corruption with cheap section skipping (empty / no-ore sections). */
    private static void applyOres(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        if (data.getOreDay(cpos) >= day) return;

        int x0 = cpos.getMinBlockX();
        int z0 = cpos.getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;
            // Palette-level pre-check: skip sections that can't contain convertible ores.
            if (!section.maybeHas(s -> ContaminationRules.oreConversionFor(s, day) != null)) continue;

            int yBase = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(si));
            for (int ly = 0; ly < 16; ly++) {
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        BlockState converted = ContaminationRules.oreConversionFor(state, day);
                        if (converted != null) {
                            place(level, new BlockPos(x0 + lx, yBase + ly, z0 + lz), converted);
                        }
                    }
                }
            }
        }

        data.setOreDay(cpos, day);
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, SET_FLAGS);
        writesThisTick++;
    }

    private static BlockState copyAxis(BlockState from, BlockState to) {
        if (to.hasProperty(RotatedPillarBlock.AXIS) && from.hasProperty(RotatedPillarBlock.AXIS)) {
            return to.setValue(RotatedPillarBlock.AXIS, from.getValue(RotatedPillarBlock.AXIS));
        }
        return to;
    }

    private static boolean needsOreUpdate(int prevDay, int newDay) {
        return (prevDay < 2 && newDay >= 2)
            || (prevDay < 3 && newDay >= 3)
            || (prevDay < 4 && newDay >= 4);
    }

    private static long mix(long h) {
        h ^= (h >>> 33); h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33); h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h;
    }
}
