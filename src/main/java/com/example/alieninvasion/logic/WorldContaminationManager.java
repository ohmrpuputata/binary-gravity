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
 * brought up to the current day's level of ruin:
 *  - infested ground (3 deep) and dead purple tree canopies;
 *  - gradually infected water bodies (ponds, rivers, shores);
 *  - impact craters with toxic pools (some radioactive);
 *  - narrow bore holes left by orbital drills, lined with corruption and
 *    glowing radiation crystals at the bottom;
 *  - corrupted cave walls/floors with glowing tendrils (caves lag the surface
 *    by one day);
 *  - day-gated ore corruption across the full world height;
 *  - scattered remains (bones) and alien tendrils on the surface.
 *
 * Design rules that keep it correct and cheap:
 *  - CHUNK_LOAD only ENQUEUES work; blocks are written later from END_WORLD_TICK
 *    (writing during chunk promotion can cascade-load neighbours and stall).
 *  - All writes use SET_FLAGS (no neighbour shape updates), so converting the
 *    edge of a chunk never touches a chunk that isn't loaded.
 *  - Column/block selection is a deterministic hash of position: as the day
 *    target grows the same spots stay converted and new ones join in, so the
 *    corruption only ever spreads, never flickers.
 *  - Per-chunk progress persists in ChunkContaminationData (surface/underground
 *    day watermarks), so chunks are never reprocessed for a day they already
 *    had, and craters/bore holes are carved exactly once.
 */
public final class WorldContaminationManager {
    private WorldContaminationManager() {}

    /** UPDATE_CLIENTS(2) | UPDATE_KNOWN_SHAPE(16) | UPDATE_SUPPRESS_DROPS(32). */
    private static final int SET_FLAGS = 2 | 16 | 32;

    private static final int MAX_SURFACE_CHUNKS_PER_TICK = 16;
    private static final int MAX_UNDERGROUND_CHUNKS_PER_TICK = 2;
    /** Hard cap on block writes per tick so a day-change wave never freezes the server. */
    private static final int WRITE_BUDGET_PER_TICK = 4096;

    /** How deep below the surface the infestation reaches. */
    private static final int SURFACE_DEPTH = 3;
    /** Max crater attempts rolled per chunk (each has its own activation day). */
    private static final int CRATER_SLOTS = 3;
    /** Max drill bore hole attempts rolled per chunk. */
    private static final int DRILL_SLOTS = 2;

    // LinkedHashSet: O(1) dedup on add, FIFO iteration order, single-threaded (main thread only)
    private static final LinkedHashSet<ChunkPos> SURFACE_QUEUE     = new LinkedHashSet<>();
    private static final LinkedHashSet<ChunkPos> UNDERGROUND_QUEUE = new LinkedHashSet<>();
    private static final LinkedHashSet<ChunkPos> PURIFY_QUEUE      = new LinkedHashSet<>();

    private static int lastKnownDay = -1;
    private static int writesThisTick;

    /** Fraction of the surface eaten by the corruption on day N: the whole world by day 5. */
    public static float getTarget(int day) {
        if (day <= 0) return 0f;
        switch (day) {
            case 1:  return 0.05f;
            case 2:  return 0.12f;
            case 3:  return 0.25f;
            case 4:  return 0.40f;
            case 5:  return 0.55f;
            case 6:  return 0.70f;
            case 7:  return 0.85f;
            default: return 1.0f;
        }
    }

    /** Fraction of water gone bad on day N: starts day 4, lags the land slightly. */
    private static float getWaterTarget(int day) {
        if (day < 4) return 0f;
        return getTarget(day - 1);
    }

    /** Caves rot two days behind the surface — the corruption sinks in from above. */
    private static float getCaveTarget(int day) {
        return getTarget(day - 2);
    }

    /** Called by ServerChunkEvents.CHUNK_LOAD. Only enqueues — never writes blocks. */
    public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        int day = SurvivalManager.getDay(level);
        if (day < 1) return;
        ChunkContaminationData data = ChunkContaminationData.get(level);
        ChunkPos cp = chunk.getPos();
        if (data.isProtectedChunk(cp)) return; // reclaimed/inert territory stays clean
        if (data.getSurfaceDay(cp) < day) SURFACE_QUEUE.add(cp);
        if (day >= 2 && data.getOreDay(cp) < day) UNDERGROUND_QUEUE.add(cp);
    }

    /** Clears all session state. Called on world unload so queues never leak into another world. */
    public static void onWorldUnload() {
        SURFACE_QUEUE.clear();
        UNDERGROUND_QUEUE.clear();
        PURIFY_QUEUE.clear();
        lastKnownDay = -1;
    }

    /**
     * A Purifier claimed this chunk: mark it reclaimed (all contamination systems
     * skip it from now on) and queue a full scrub of everything already infested.
     */
    public static void onPurifierPlaced(ServerLevel level, net.minecraft.core.BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkContaminationData data = ChunkContaminationData.get(level);
        data.setPurified(cp, true);
        SURFACE_QUEUE.remove(cp);
        UNDERGROUND_QUEUE.remove(cp);
        PURIFY_QUEUE.add(cp);
    }

    /**
     * The Purifier is gone: the chunk loses its protection, its watermarks reset,
     * and the corruption immediately starts reclaiming it. Defend your purifiers.
     */
    public static void onPurifierRemoved(ServerLevel level, net.minecraft.core.BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        ChunkContaminationData data = ChunkContaminationData.get(level);
        data.setPurified(cp, false);
        data.setSurfaceDay(cp, -1);
        data.setOreDay(cp, -1);
        PURIFY_QUEUE.remove(cp);
        if (SurvivalManager.getDay(level) >= 1) {
            SURFACE_QUEUE.add(cp);
            UNDERGROUND_QUEUE.add(cp);
        }
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

        // Player-triggered reclamation first: one chunk per tick, fully scrubbed.
        if (!PURIFY_QUEUE.isEmpty()) {
            Iterator<ChunkPos> pit = PURIFY_QUEUE.iterator();
            ChunkPos pos = pit.next();
            pit.remove();
            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk != null) applyPurify(level, chunk);
        }

        // Ambient spore fog drifting over heavily infested ground.
        if (day >= 2 && level.getGameTime() % 8L == 0L) {
            spawnSporeFog(level, day);
        }

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
            Iterator<ChunkPos> oit = UNDERGROUND_QUEUE.iterator();
            while (processed < MAX_UNDERGROUND_CHUNKS_PER_TICK && writesThisTick < WRITE_BUDGET_PER_TICK && oit.hasNext()) {
                ChunkPos pos = oit.next();
                oit.remove();
                LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
                if (chunk != null) {
                    applyUnderground(level, chunk, day, data);
                    processed++;
                }
            }
        }
    }

    /**
     * Re-queues loaded chunks around players so already-visible areas catch up to the
     * new day immediately (this is what makes "/invasion set 5" visibly rot the world
     * around you within seconds).
     */
    private static void onDayChange(ServerLevel level, int prevDay, int newDay) {
        if (newDay < 1) return;
        ChunkContaminationData data = ChunkContaminationData.get(level);
        int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 12);
        boolean undergroundNeeded = newDay > prevDay && prevDay < 9; // caves/ores keep growing until day 8
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    ChunkPos cp = new ChunkPos(center.x + dx, center.z + dz);
                    if (data.isProtectedChunk(cp)) continue;
                    if (level.getChunkSource().getChunkNow(cp.x, cp.z) != null) {
                        SURFACE_QUEUE.add(cp);
                        if (undergroundNeeded) UNDERGROUND_QUEUE.add(cp);
                    }
                }
            }
        }
    }

    private static void applySurface(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        if (data.isProtectedChunk(cpos)) return;
        int prevDay = data.getSurfaceDay(cpos);
        if (prevDay >= day) return;

        float target = getTarget(day);
        float waterTarget = getWaterTarget(day);
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

                    // Water columns: ponds, rivers and shores go bad from day 3 and by
                    // day 5 EVERY water surface is infected. Shallow water (<=6 deep)
                    // converts whole; deep water gets a 3-block corrupted film (the
                    // clean depths below are the only respite left).
                    cursor.set(x, groundY, z);
                    BlockState topState = chunk.getBlockState(cursor);
                    if (!topState.getFluidState().isEmpty()) {
                        if (waterTarget > 0f && p < waterTarget && topState.is(Blocks.WATER)) {
                            int depth = 0;
                            while (depth < 6 && groundY - depth > minY
                                    && chunk.getBlockState(cursor.set(x, groundY - depth, z)).is(Blocks.WATER)) {
                                depth++;
                            }
                            boolean deep = depth == 6
                                    && chunk.getBlockState(cursor.set(x, groundY - 6, z)).is(Blocks.WATER);
                            int convert = deep ? 3 : depth;
                            for (int i = 0; i < convert; i++) {
                                place(level, new BlockPos(x, groundY - i, z),
                                        ModBlocks.INFECTED_WATER.defaultBlockState());
                            }
                        }
                        continue;
                    }

                    // Ground: top SURFACE_DEPTH blocks, so slopes, cliffs and shallow
                    // digs read as infected too — not just a one-block paint job.
                    // Wooden structures (village houses) rot DEEP: the corruption eats
                    // whole walls top-to-bottom, not just the roofline.
                    int depthLimit = (topState.is(BlockTags.PLANKS) || topState.is(BlockTags.LOGS)
                            || topState.is(BlockTags.WOOL)) ? 8 : SURFACE_DEPTH;
                    boolean groundConverted = false;
                    for (int dy = 0; dy < depthLimit; dy++) {
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
                            if (dy == 0) break; // snow layer / unconvertible top: skip column
                            continue;
                        }
                        if (chunk.getBlockEntity(cursor) != null) continue;
                        converted = ContaminationRules.copyProperties(state, converted);
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
                                converted = ContaminationRules.copyProperties(state, ModBlocks.INFESTED_LOG.defaultBlockState());
                            }
                            if (converted != null && chunk.getBlockEntity(cursor) == null) {
                                place(level, new BlockPos(x, y, z), converted);
                            }
                        }
                    }

                    if (groundConverted && groundY + 1 < maxY) {
                        // Vegetation withers on rotten ground: grass, flowers and
                        // saplings twist into tendrils, dry husks, or crumble away.
                        cursor.set(x, groundY + 1, z);
                        BlockState plant = chunk.getBlockState(cursor);
                        if (ContaminationRules.isSurfaceVegetation(plant)) {
                            long vb = (h >>> 50) & 3L;
                            BlockState withered = vb == 0L ? ModBlocks.ALIEN_TENDRILS.defaultBlockState()
                                    : (vb == 1L ? ModBlocks.DEAD_INFESTED_CROP.defaultBlockState()
                                                : Blocks.AIR.defaultBlockState());
                            place(level, new BlockPos(x, groundY + 1, z), withered);
                            // Upper half of a double plant: clear it so nothing floats.
                            if (groundY + 2 < maxY) {
                                cursor.set(x, groundY + 2, z);
                                if (ContaminationRules.isSurfaceVegetation(chunk.getBlockState(cursor))) {
                                    place(level, new BlockPos(x, groundY + 2, z), Blocks.AIR.defaultBlockState());
                                }
                            }
                        }
                        // Scattered remains: rare contaminated bones on the dead ground.
                        if (day >= 3 && ((h >>> 40) & 0x3FL) == 0L) {
                            cursor.set(x, groundY + 1, z);
                            if (chunk.getBlockState(cursor).isAir()) {
                                place(level, new BlockPos(x, groundY + 1, z),
                                        ModBlocks.CONTAMINATED_BONES.defaultBlockState());
                            }
                        }
                        // Glowing alien tendrils sprouting from the corruption.
                        if (day >= 3 && ((h >>> 46) & 0x1FL) == 0L) {
                            cursor.set(x, groundY + 1, z);
                            if (chunk.getBlockState(cursor).isAir()) {
                                place(level, new BlockPos(x, groundY + 1, z),
                                        ModBlocks.ALIEN_TENDRILS.defaultBlockState());
                            }
                        }
                    }
                }
            }
        }

        // Impact craters and drill bore holes — the "holes" punched into the world.
        carveCraters(level, chunk, day, prevDay, chunkSeed);
        carveDrillShafts(level, chunk, day, prevDay, chunkSeed);

        // INFECTION HEART: from day 3 roughly a third of chunks grow a pulsing
        // organ on the surface. Destroy it -> the chunk goes inert (existing rot
        // stays, growth stops). It spawns exactly once: a broken heart flags the
        // chunk inert, and inert chunks never run this pass again.
        if (day >= 4 && prevDay < 4) {
            long hh = mix(chunkSeed ^ 0xD1B54A32D192ED03L);
            if (((hh >>> 8) & 0xFFL) < 90L) { // ~35% of chunks
                int hx = cpos.getMinBlockX() + 2 + (int) ((hh >>> 16) % 12L);
                int hz = cpos.getMinBlockZ() + 2 + (int) ((hh >>> 24) % 12L);
                int hy = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx & 15, hz & 15) + 1;
                if (hy > minY && hy < maxY
                        && chunk.getBlockState(cursor.set(hx, hy, hz)).isAir()
                        && chunk.getBlockState(cursor.set(hx, hy - 1, hz)).getFluidState().isEmpty()
                        && !chunk.getBlockState(cursor.set(hx, hy - 1, hz)).isAir()) {
                    place(level, new BlockPos(hx, hy, hz), ModBlocks.ALIEN_HEART.defaultBlockState());
                }
            }
        }

        data.setSurfaceDay(cpos, day);
    }

    /**
     * Deterministic crater slots per chunk. Each slot has a fixed position, radius and
     * activation day; a slot is carved exactly once — when the chunk's surface watermark
     * first crosses its activation day. Filling a crater back in is permanent: it is
     * never re-carved, and slots near player block entities (chests etc.) are skipped.
     */
    private static void carveCraters(ServerLevel level, LevelChunk chunk, int day, int prevDay, long chunkSeed) {
        if (day < 3) return;
        ChunkPos cpos = chunk.getPos();
        int minY = level.getMinBuildHeight();

        for (int slot = 0; slot < CRATER_SLOTS; slot++) {
            long h = mix(chunkSeed ^ (0xC2B2AE3D27D4EB4FL * (slot + 1)));
            if (((h >>> 4) & 0xFFL) >= 100L) continue;            // ~39% of slots exist
            int activationDay = 3 + (int) ((h >>> 12) % 5L);       // strikes land on days 3..7
            if (activationDay > day || activationDay <= prevDay) continue;

            int radius = 3 + (int) ((h >>> 20) % 3L);              // 3..5
            // Keep the whole bowl inside this chunk so carving never touches a neighbour.
            int span = Math.max(1, 14 - 2 * radius);
            int cx = radius + 1 + (int) ((h >>> 28) % span);
            int cz = radius + 1 + (int) ((h >>> 36) % span);
            boolean toxic = ((h >>> 44) & 3L) != 0L && day >= 3;   // most craters pool toxic water
            boolean radioactive = day >= 4 && ((h >>> 47) & 7L) == 0L; // ~12% become radiation hot spots

            int wx = cpos.getMinBlockX() + cx;
            int wz = cpos.getMinBlockZ() + cz;
            int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            if (surfaceY <= minY + radius + 1 || surfaceY >= level.getMaxBuildHeight() - 8) continue;

            BlockPos center = new BlockPos(wx, surfaceY, wz);
            if (!chunk.getBlockState(center).getFluidState().isEmpty()) continue; // no underwater craters

            if (hasBlockEntityNear(chunk, wx, wz, radius + 1, surfaceY - radius - 2, surfaceY + 6)) continue;

            carveBowl(level, chunk, wx, surfaceY, wz, radius, toxic && !radioactive, radioactive);
        }
    }

    private static void carveBowl(ServerLevel level, LevelChunk chunk, int wx, int surfaceY, int wz,
                                  int radius, boolean toxic, boolean radioactive) {
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
                        place(level, new BlockPos(x, rimY, z), ContaminationRules.copyProperties(rimState, rimConverted));
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

        // Radiation hot spot: a glowing crystal heart instead of a pool. Geiger
        // counters click, RadiationFieldManager handles the dose.
        if (radioactive) {
            place(level, new BlockPos(wx, floorY, wz), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
            cursor.set(wx, floorY + 1, wz);
            if (chunk.getBlockState(cursor).isAir()) {
                place(level, new BlockPos(wx, floorY + 1, wz),
                        ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState());
            }
        }
    }

    /**
     * Narrow vertical bore holes left behind by the orbital drills (day 3+): a rough
     * shaft 14-31 blocks deep, walls lined with corruption, with either a toxic puddle
     * or a glowing radiation-crystal pocket at the bottom. Same one-shot watermark
     * logic as craters.
     */
    private static void carveDrillShafts(ServerLevel level, LevelChunk chunk, int day, int prevDay, long chunkSeed) {
        if (day < 4) return;
        ChunkPos cpos = chunk.getPos();
        int minY = level.getMinBuildHeight();

        for (int slot = 0; slot < DRILL_SLOTS; slot++) {
            long h = mix(chunkSeed ^ (0xA0761D6478BD642FL * (slot + 1)));
            if (((h >>> 4) & 0xFFL) >= 56L) continue;             // ~22% of slots exist
            int activationDay = 4 + (int) ((h >>> 12) % 4L);       // drills hit on days 4..7
            if (activationDay > day || activationDay <= prevDay) continue;

            int radius = 1 + (int) ((h >>> 20) % 2L);              // 1..2
            int margin = radius + 2;
            int span = Math.max(1, 15 - 2 * margin);
            int cx = margin + (int) ((h >>> 28) % span);
            int cz = margin + (int) ((h >>> 36) % span);

            int wx = cpos.getMinBlockX() + cx;
            int wz = cpos.getMinBlockZ() + cz;
            int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz);
            if (surfaceY <= minY + 22 || surfaceY >= level.getMaxBuildHeight() - 8) continue;
            if (!chunk.getBlockState(new BlockPos(wx, surfaceY, wz)).getFluidState().isEmpty()) continue;

            int depth = 14 + (int) ((h >>> 44) % 18L);             // 14..31 blocks deep
            int bottomY = Math.max(minY + 6, surfaceY - depth);
            if (hasBlockEntityNear(chunk, wx, wz, radius + 1, bottomY - 2, surfaceY + 6)) continue;

            boolean radioactive = day >= 4 && ((h >>> 52) & 1L) == 0L; // half the shafts glow
            carveShaft(level, chunk, wx, surfaceY, wz, radius, bottomY, radioactive);
        }
    }

    private static void carveShaft(ServerLevel level, LevelChunk chunk, int wx, int surfaceY, int wz,
                                   int radius, int bottomY, boolean radioactive) {
        int r2 = radius * radius;
        int outer2 = (radius + 1) * (radius + 1);
        int clearTop = Math.min(level.getMaxBuildHeight() - 1, surfaceY + 3);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int dx = -(radius + 1); dx <= radius + 1; dx++) {
            for (int dz = -(radius + 1); dz <= radius + 1; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > outer2) continue;
                int x = wx + dx;
                int z = wz + dz;

                if (d2 <= r2) {
                    // The bore itself: punched clean from above the surface to the bottom.
                    for (int y = clearTop; y >= bottomY; y--) {
                        cursor.set(x, y, z);
                        BlockState state = chunk.getBlockState(cursor);
                        if (state.isAir()) continue;
                        if (state.getDestroySpeed(level, cursor) < 0.0F) break; // bedrock
                        if (chunk.getBlockEntity(cursor) != null) continue;
                        place(level, new BlockPos(x, y, z), air);
                    }
                    // Floor of the bore.
                    cursor.set(x, bottomY - 1, z);
                    BlockState floorState = chunk.getBlockState(cursor);
                    if (!floorState.isAir() && floorState.getFluidState().isEmpty()
                            && floorState.getDestroySpeed(level, cursor) >= 0.0F
                            && chunk.getBlockEntity(cursor) == null) {
                        BlockState lining = ContaminationRules.contaminatedStateFor(floorState);
                        place(level, new BlockPos(x, bottomY - 1, z),
                                lining != null ? lining : ModBlocks.INFESTED_STONE.defaultBlockState());
                    }
                } else {
                    // The casing: corrupt the ring of blocks around the hole, full depth.
                    for (int y = surfaceY; y >= bottomY; y--) {
                        cursor.set(x, y, z);
                        BlockState state = chunk.getBlockState(cursor);
                        if (state.isAir() || ContaminationRules.isContaminated(state)) continue;
                        BlockState converted = ContaminationRules.contaminatedStateFor(state);
                        if (converted != null && chunk.getBlockEntity(cursor) == null) {
                            place(level, new BlockPos(x, y, z), ContaminationRules.copyProperties(state, converted));
                        }
                    }
                }
            }
        }

        // What the drill left behind at the bottom.
        if (radioactive) {
            place(level, new BlockPos(wx, bottomY - 1, wz), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState());
            cursor.set(wx, bottomY, wz);
            if (chunk.getBlockState(cursor).isAir()) {
                place(level, new BlockPos(wx, bottomY, wz),
                        ModBlocks.RADIATION_CRYSTAL_CLUSTER.defaultBlockState());
            }
        } else {
            cursor.set(wx, bottomY, wz);
            if (chunk.getBlockState(cursor).isAir()) {
                place(level, new BlockPos(wx, bottomY, wz), ModBlocks.TOXIC_WATER.defaultBlockState());
            }
        }
    }

    /**
     * Underground pass: full-height ore corruption + cave-surface corruption.
     * Sections are pre-filtered by palette (maybeHas) so solid stone sections and
     * pure-air sections are skipped wholesale; only sections that actually contain
     * cave surfaces or convertible ores get scanned.
     */
    private static void applyUnderground(ServerLevel level, LevelChunk chunk, int day, ChunkContaminationData data) {
        ChunkPos cpos = chunk.getPos();
        if (data.isProtectedChunk(cpos)) return;
        if (data.getOreDay(cpos) >= day) return;

        float caveTarget = getCaveTarget(day);
        int x0 = cpos.getMinBlockX();
        int z0 = cpos.getMinBlockZ();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        long chunkSeed = mix(cpos.toLong() * 0x9E3779B97F4A7C15L);
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // Cached surface heights: the cave pass must stay below the surface band,
        // which the surface pass already owns.
        int[] surface = new int[256];
        for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
                surface[(bx << 4) | bz] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
            }
        }

        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;

            boolean oreInteresting = section.maybeHas(s -> ContaminationRules.oreConversionFor(s, day) != null);
            boolean caveInteresting = caveTarget > 0f
                    && section.maybeHas(BlockState::isAir)
                    && section.maybeHas(s -> ContaminationRules.contaminatedStateFor(s) != null);
            if (!oreInteresting && !caveInteresting) continue;

            int yBase = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(si));
            for (int ly = 0; ly < 16; ly++) {
                int wy = yBase + ly;
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState state = section.getBlockState(lx, ly, lz);

                        BlockState ore = ContaminationRules.oreConversionFor(state, day);
                        if (ore != null) {
                            place(level, new BlockPos(x0 + lx, wy, z0 + lz), ore);
                            continue;
                        }

                        if (!caveInteresting || state.isAir()) continue;
                        // Stay below the surface band — the surface pass owns the top.
                        if (wy >= surface[(lx << 4) | lz] - SURFACE_DEPTH) continue;

                        int x = x0 + lx;
                        int z = z0 + lz;
                        // Cheap deterministic roll FIRST, neighbour checks only for winners.
                        long bh = mix(chunkSeed ^ BlockPos.asLong(x, wy, z));
                        float bp = ((bh >>> 1) & 0x7FFFFFFFL) / (float) 0x7FFFFFFFL;
                        if (bp >= caveTarget) continue;

                        // Underground pools: the exposed surface of cave water rots too.
                        if (state.is(Blocks.WATER)) {
                            if (wy + 1 < maxY && chunk.getBlockState(cursor.set(x, wy + 1, z)).isAir()) {
                                place(level, new BlockPos(x, wy, z), ModBlocks.INFECTED_WATER.defaultBlockState());
                            }
                            continue;
                        }

                        BlockState converted = ContaminationRules.contaminatedStateFor(state);
                        if (converted == null) continue;

                        // Only blocks that actually face a cave (any air neighbour).
                        boolean airAbove = wy + 1 < maxY
                                && chunk.getBlockState(cursor.set(x, wy + 1, z)).isAir();
                        boolean exposed = airAbove
                                || (wy - 1 > minY && chunk.getBlockState(cursor.set(x, wy - 1, z)).isAir())
                                || (lx > 0  && chunk.getBlockState(cursor.set(x - 1, wy, z)).isAir())
                                || (lx < 15 && chunk.getBlockState(cursor.set(x + 1, wy, z)).isAir())
                                || (lz > 0  && chunk.getBlockState(cursor.set(x, wy, z - 1)).isAir())
                                || (lz < 15 && chunk.getBlockState(cursor.set(x, wy, z + 1)).isAir());
                        if (!exposed) continue;
                        if (chunk.getBlockEntity(cursor.set(x, wy, z)) != null) continue;

                        place(level, new BlockPos(x, wy, z), ContaminationRules.copyProperties(state, converted));

                        // Glowing tendrils on some cave floors — alien caves light
                        // themselves in eerie violet.
                        if (airAbove && ((bh >>> 40) & 0x1FL) == 0L) {
                            place(level, new BlockPos(x, wy + 1, z), ModBlocks.ALIEN_TENDRILS.defaultBlockState());
                        }
                    }
                }
            }
        }

        data.setOreDay(cpos, day);
    }

    /**
     * Full scrub of a Purifier-claimed chunk: every infested block (surface, walls,
     * caves, water, tendrils, bones) reverts to its clean form in one sweep, with a
     * cleansing particle column so the reclamation reads loud and clear.
     */
    private static void applyPurify(ServerLevel level, LevelChunk chunk) {
        ChunkPos cpos = chunk.getPos();
        int x0 = cpos.getMinBlockX();
        int z0 = cpos.getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

        for (int si = 0; si < sections.length; si++) {
            LevelChunkSection section = sections[si];
            if (section == null || section.hasOnlyAir()) continue;
            if (!section.maybeHas(s -> ContaminationRules.cleanStateFor(s) != null)) continue;

            int yBase = SectionPos.sectionToBlockCoord(level.getSectionYFromSectionIndex(si));
            for (int ly = 0; ly < 16; ly++) {
                for (int lx = 0; lx < 16; lx++) {
                    for (int lz = 0; lz < 16; lz++) {
                        BlockState state = section.getBlockState(lx, ly, lz);
                        BlockState clean = ContaminationRules.cleanStateFor(state);
                        if (clean != null) {
                            // Doors/trapdoors/logs keep their orientation through the cure.
                            place(level, new BlockPos(x0 + lx, yBase + ly, z0 + lz),
                                    ContaminationRules.copyProperties(state, clean));
                        }
                    }
                }
            }
        }

        // Mark fully treated so nothing re-queues it while the purifier stands.
        ChunkContaminationData data = ChunkContaminationData.get(level);
        int day = SurvivalManager.getDay(level);
        data.setSurfaceDay(cpos, Math.max(day, data.getSurfaceDay(cpos)));
        data.setOreDay(cpos, Math.max(day, data.getOreDay(cpos)));

        BlockPos center = new BlockPos(x0 + 8, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + 8, z0 + 8), z0 + 8);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                center.getX(), center.getY() + 1.0D, center.getZ(), 80, 6.0D, 4.0D, 6.0D, 0.02D);
        level.playSound(null, center, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.2F, 1.4F);
    }

    /**
     * Spore fog: slow drifting motes over infested ground around every player —
     * the air itself reads as sick where the corruption has taken hold. Density
     * scales with the day.
     */
    private static void spawnSporeFog(ServerLevel level, int day) {
        int samples = Math.min(6 + day * 2, 16);
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < samples; i++) {
                int x = player.getBlockX() + level.random.nextInt(49) - 24;
                int z = player.getBlockZ() + level.random.nextInt(49) - 24;
                if (!level.isLoaded(new BlockPos(x, player.getBlockY(), z))) continue;
                int airY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                BlockPos ground = new BlockPos(x, airY - 1, z);
                BlockState gs = level.getBlockState(ground);
                if (!ContaminationRules.isContaminated(gs)) continue;
                double py = airY + 0.4D + level.random.nextDouble() * 2.2D;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR,
                        x + 0.5D, py, z + 0.5D, 2, 0.9D, 0.5D, 0.9D, 0.0D);
                if (level.random.nextInt(5) == 0) {
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.WARPED_SPORE,
                            x + 0.5D, py, z + 0.5D, 3, 0.8D, 0.4D, 0.8D, 0.0D);
                }
            }
        }
    }

    private static boolean hasBlockEntityNear(LevelChunk chunk, int wx, int wz, int hRange, int yMin, int yMax) {
        for (BlockPos bePos : chunk.getBlockEntitiesPos()) {
            if (Math.abs(bePos.getX() - wx) <= hRange && Math.abs(bePos.getZ() - wz) <= hRange
                    && bePos.getY() >= yMin && bePos.getY() <= yMax) {
                return true;
            }
        }
        return false;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, SET_FLAGS);
        writesThisTick++;
    }

    private static long mix(long h) {
        h ^= (h >>> 33); h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33); h *= 0xC4CEB9FE1A85EC53L;
        h ^= (h >>> 33);
        return h;
    }
}
