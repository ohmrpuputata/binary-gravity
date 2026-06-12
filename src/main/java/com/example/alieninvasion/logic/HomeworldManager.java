package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Родной мир Роя (alien-invasion:homeworld) — плоская заражённая пустошь,
 * куда после победы ведёт портал. При первом входе строится арена финала:
 * ГЛАВНЫЙ УЛЕЙ (огромный органический купол с сердцем) и обратный портал.
 * Реактор охотника можно установить только рядом с этим ульем.
 */
public class HomeworldManager extends SavedData {
    private static final String DATA_NAME = "alien_invasion_homeworld";

    public static final ResourceKey<Level> HOMEWORLD = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("alien-invasion", "homeworld"));

    /** Центр главного улья (XZ); реактор должен стоять в этом радиусе от него. */
    public static final BlockPos HIVE_CENTER = new BlockPos(0, 0, 0);
    public static final double REACTOR_PLACEMENT_RADIUS = 40.0D;
    /** Внутри этого радиуса от улья ландшафт остаётся ровной ареной финала. */
    private static final int PROTECTED_FLAT_RADIUS = 56;
    private static final int DECORATE_CHUNKS_PER_TICK = 3;

    /** ГИГАНТСКИЙ ГОРОД РОЯ: неоновый мегаполис к востоку от главного улья. */
    public static final BlockPos CITY_CENTER = new BlockPos(272, 0, 0);
    public static final int CITY_RADIUS = 88;

    private boolean arenaBuilt = false;
    /** Чанки, уже получившие рельеф и декорации (см. decorateChunk). */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet decoratedChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /**
     * Очередь чанков на декорирование: {ключ чанка, gameTime готовности}.
     * Декорируем с задержкой ~2 сек после загрузки: данные сущностей чанка
     * подгружаются асинхронно, и мобы, добавленные в тот же тик, что и
     * загрузка, молча терялись (так пропадал Тиран со шпиля).
     */
    private final java.util.ArrayDeque<long[]> pendingChunks = new java.util.ArrayDeque<>();
    private static final int DECORATE_DELAY_TICKS = 40;

    public static HomeworldManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(HomeworldManager::new, HomeworldManager::load, null), DATA_NAME);
    }

    public static HomeworldManager load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        HomeworldManager data = new HomeworldManager();
        data.arenaBuilt = tag.getBoolean("ArenaBuilt");
        for (long l : tag.getLongArray("DecoratedChunks")) {
            data.decoratedChunks.add(l);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("ArenaBuilt", arenaBuilt);
        tag.putLongArray("DecoratedChunks", decoratedChunks.toLongArray());
        return tag;
    }

    // ------------------------------------------------------------------
    // ЖИВОЙ ЛАНДШАФТ: плоская заготовка генератора превращается в чужой мир
    // при первой загрузке каждого чанка — дюны, шпили улья, мясные курганы,
    // токсичные озёрца, кристаллы, гнёзда и руины труб.
    // ------------------------------------------------------------------

    public static void onChunkLoad(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk) {
        HomeworldManager data = get(level);
        long key = chunk.getPos().toLong();
        if (!data.decoratedChunks.contains(key)) {
            data.pendingChunks.add(new long[] { key, level.getGameTime() + DECORATE_DELAY_TICKS });
        }
    }

    public static void tickWorld(ServerLevel level) {
        HomeworldManager data = get(level);
        int budget = DECORATE_CHUNKS_PER_TICK;
        while (budget-- > 0 && !data.pendingChunks.isEmpty()) {
            long[] entry = data.pendingChunks.peek();
            if (level.getGameTime() < entry[1]) {
                break; // очередь в порядке загрузки — дальше все ещё «дозревают»
            }
            data.pendingChunks.poll();
            long key = entry[0];
            if (data.decoratedChunks.contains(key)) {
                continue;
            }
            net.minecraft.world.level.ChunkPos cp = new net.minecraft.world.level.ChunkPos(key);
            if (!level.hasChunk(cp.x, cp.z)) {
                continue; // выгрузился до обработки — вернётся со следующей загрузкой
            }
            decorateChunk(level, cp);
            data.decoratedChunks.add(key);
            data.setDirty();
        }
    }

    /** Плавные дюны: непрерывная функция мировых координат — без швов на чанках. */
    private static int duneHeight(int wx, int wz) {
        double n = Math.sin(wx * 0.045D) * 1.7D
                + Math.cos(wz * 0.06D) * 1.4D
                + Math.sin((wx + wz) * 0.021D) * 1.5D;
        return Math.max(0, (int) Math.round(n));
    }

    private static boolean inArena(int wx, int wz) {
        long dx = wx - HIVE_CENTER.getX();
        long dz = wz - HIVE_CENTER.getZ();
        return dx * dx + dz * dz < (long) PROTECTED_FLAT_RADIUS * PROTECTED_FLAT_RADIUS;
    }

    private static boolean inCity(int wx, int wz) {
        long dx = wx - CITY_CENTER.getX();
        long dz = wz - CITY_CENTER.getZ();
        return dx * dx + dz * dz <= (long) CITY_RADIUS * CITY_RADIUS;
    }

    private static void decorateChunk(ServerLevel level, net.minecraft.world.level.ChunkPos cp) {
        RandomSource r = RandomSource.create(level.getSeed() ^ (cp.toLong() * 0x9E3779B97F4A7C15L));

        // ГОРОД: чанки городской зоны застраиваются мегаполисом вместо пустоши.
        long ccx = cp.getMiddleBlockX() - CITY_CENTER.getX();
        long ccz = cp.getMiddleBlockZ() - CITY_CENTER.getZ();
        if (ccx * ccx + ccz * ccz <= (long) (CITY_RADIUS + 16) * (CITY_RADIUS + 16)) {
            buildCityChunk(level, cp, r);
            return;
        }

        // 1) Рельеф: мягкие дюны (только насыпаем, ничего не копаем).
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cp.getMinBlockX() + lx;
                int wz = cp.getMinBlockZ() + lz;
                if (inArena(wx, wz)) {
                    continue;
                }
                int add = duneHeight(wx, wz);
                if (add <= 0) {
                    continue;
                }
                int y0 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
                for (int i = 0; i < add; i++) {
                    Block b = (i == add - 1) ? ModBlocks.INFESTED_GRASS : ModBlocks.INFESTED_DIRT;
                    level.setBlock(new BlockPos(wx, y0 + i, wz), b.defaultBlockState(), 2);
                }
            }
        }

        // 2) Фичи: 2-5 штук на чанк.
        int features = 2 + r.nextInt(4);
        for (int i = 0; i < features; i++) {
            int wx = cp.getMinBlockX() + r.nextInt(16);
            int wz = cp.getMinBlockZ() + r.nextInt(16);
            if (inArena(wx, wz)) {
                continue;
            }
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
            BlockPos ground = new BlockPos(wx, y, wz);
            switch (r.nextInt(8)) {
                case 0 -> hiveSpire(level, r, ground, 4 + r.nextInt(7));
                case 1 -> fleshMound(level, r, ground, 2 + r.nextInt(3));
                case 2 -> tendrilGarden(level, r, ground);
                case 3 -> toxicPool(level, r, ground);
                case 4 -> crystalCluster(level, r, ground);
                case 5 -> wormNest(level, r, ground);
                case 6 -> pipeRuin(level, r, ground);
                case 7 -> monolith(level, r, ground);
            }
        }

        // 3) Редкий мега-шпиль с сердцем — местная достопримечательность.
        if (r.nextInt(45) == 0) {
            int wx = cp.getMinBlockX() + 8;
            int wz = cp.getMinBlockZ() + 8;
            if (!inArena(wx, wz)) {
                megaSpire(level, r, new BlockPos(wx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz), wz));
            }
        }

        // 4) Местная фауна: одинокие обитатели пустоши.
        if (r.nextInt(4) == 0) {
            int wx = cp.getMinBlockX() + r.nextInt(16);
            int wz = cp.getMinBlockZ() + r.nextInt(16);
            if (!inArena(wx, wz)) {
                float roll = r.nextFloat();
                Mob mob = (roll < 0.45F ? EntityRegistry.ALIEN_GRUNT
                        : roll < 0.75F ? EntityRegistry.ALIEN_CHICKEN
                        : EntityRegistry.ALIEN_RAPTOR).create(level);
                if (mob != null) {
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, wx, wz);
                    mob.moveTo(wx + 0.5D, y, wz + 0.5D, r.nextFloat() * 360.0F, 0.0F);
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()),
                            MobSpawnType.NATURAL, null);
                    level.addFreshEntity(mob);
                }
            }
        }
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }

    /** Кривой органический шпиль улья со светящейся верхушкой. */
    private static void hiveSpire(ServerLevel level, RandomSource r, BlockPos base, int height) {
        int x = base.getX(), z = base.getZ();
        for (int i = 0; i < height; i++) {
            if (r.nextInt(4) == 0) { // шпиль «ведёт» в сторону
                x += r.nextInt(3) - 1;
                z += r.nextInt(3) - 1;
            }
            Block b = r.nextFloat() < 0.55F ? ModBlocks.INFESTED_STONE : ModBlocks.ALIEN_FLESH;
            set(level, new BlockPos(x, base.getY() + i, z), b);
        }
        set(level, new BlockPos(x, base.getY() + height, z), ModBlocks.ALIEN_HIVE);
    }

    /** Мясной курган с лужей крови на макушке. */
    private static void fleshMound(ServerLevel level, RandomSource r, BlockPos base, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius) {
                    continue;
                }
                int h = (int) Math.round((radius - d) * (0.8D + r.nextDouble() * 0.4D));
                for (int y = 0; y < h; y++) {
                    set(level, base.offset(dx, y, dz), ModBlocks.ALIEN_FLESH);
                }
            }
        }
        set(level, base.above((int) (radius * 0.8D)), ModBlocks.BLOOD_POOL);
    }

    /** Заросли щупалец. */
    private static void tendrilGarden(ServerLevel level, RandomSource r, BlockPos base) {
        for (int i = 0; i < 9 + r.nextInt(6); i++) {
            BlockPos p = base.offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            BlockPos at = new BlockPos(p.getX(), y, p.getZ());
            if (level.getBlockState(at).isAir()) {
                set(level, at, r.nextInt(5) == 0 ? ModBlocks.BLOOD_POOL : ModBlocks.ALIEN_TENDRILS);
            }
        }
    }

    /** Токсичное озерцо с каймой из слизи. */
    private static void toxicPool(ServerLevel level, RandomSource r, BlockPos base) {
        int radius = 1 + r.nextInt(2);
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        base.getX() + dx, base.getZ() + dz) - 1;
                BlockPos at = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
                if (d <= radius) {
                    set(level, at, ModBlocks.TOXIC_WATER);
                } else if (d <= radius + 1 && r.nextInt(3) != 0) {
                    set(level, at, ModBlocks.ALIEN_RESIDUE);
                }
            }
        }
    }

    /** Светящаяся друза радиационных кристаллов (опасная красота). */
    private static void crystalCluster(ServerLevel level, RandomSource r, BlockPos base) {
        for (int i = 0; i < 3 + r.nextInt(4); i++) {
            BlockPos p = base.offset(r.nextInt(5) - 2, 0, r.nextInt(5) - 2);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            set(level, new BlockPos(p.getX(), y, p.getZ()),
                    r.nextInt(6) == 0 ? ModBlocks.DARK_MATTER_ORE : ModBlocks.PURE_RADIATION_BLOCK);
        }
    }

    /** Кладка яиц: мясное гнездо, из которого выползают черви. */
    private static void wormNest(ServerLevel level, RandomSource r, BlockPos base) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                set(level, base.offset(dx, 0, dz), ModBlocks.ALIEN_FLESH);
            }
        }
        set(level, base.offset(r.nextInt(2), 1, r.nextInt(2)), ModBlocks.ALIEN_HIVE);
        for (int i = 0; i < 1 + r.nextInt(2); i++) {
            Mob worm = EntityRegistry.INFESTED_WORM.create(level);
            if (worm != null) {
                worm.moveTo(base.getX() + r.nextDouble() * 3.0D - 1.0D, base.getY() + 2,
                        base.getZ() + r.nextDouble() * 3.0D - 1.0D, r.nextFloat() * 360.0F, 0.0F);
                level.addFreshEntity(worm);
            }
        }
    }

    /** Руины древних труб — торчащие из земли обломки инфраструктуры роя. */
    private static void pipeRuin(ServerLevel level, RandomSource r, BlockPos base) {
        var axis = r.nextBoolean()
                ? net.minecraft.core.Direction.Axis.X : net.minecraft.core.Direction.Axis.Z;
        int len = 3 + r.nextInt(4);
        int lift = r.nextInt(2);
        for (int i = 0; i < len; i++) {
            BlockPos p = axis == net.minecraft.core.Direction.Axis.X
                    ? base.offset(i, lift, 0) : base.offset(0, lift, i);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            level.setBlock(new BlockPos(p.getX(), y + lift, p.getZ()),
                    ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, axis), 2);
        }
    }

    /** Обелиск из заражённого камня с кристаллом-навершием. */
    private static void monolith(ServerLevel level, RandomSource r, BlockPos base) {
        int height = 3 + r.nextInt(4);
        for (int y = 0; y < height; y++) {
            set(level, base.above(y), ModBlocks.INFESTED_STONE_BRICKS);
        }
        set(level, base.above(height), ModBlocks.DARK_MATTER_ORE);
    }

    // ------------------------------------------------------------------
    // ГИГАНТСКИЙ ГОРОД РОЯ. Детерминированная застройка по чанкам: кольцевая
    // стена, дорожная сетка с неоновыми полосами, кварталы с башнями, куполами,
    // заводами, посадочными площадками — и центральный шпиль с Тираном Улья.
    // ------------------------------------------------------------------

    private static void buildCityChunk(ServerLevel level, net.minecraft.world.level.ChunkPos cp, RandomSource r) {
        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                cp.getMiddleBlockX(), cp.getMiddleBlockZ());

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cp.getMinBlockX() + lx;
                int wz = cp.getMinBlockZ() + lz;
                if (!inCity(wx, wz)) {
                    continue;
                }
                long dx = wx - CITY_CENTER.getX();
                long dz = wz - CITY_CENTER.getZ();
                double d = Math.sqrt(dx * dx + dz * dz);

                // КРЕПОСТНАЯ СТЕНА с воротами по осям и сигнальными лампами.
                if (d >= CITY_RADIUS - 3) {
                    boolean gate = Math.abs(wz - CITY_CENTER.getZ()) < 4
                            || Math.abs(wx - CITY_CENTER.getX()) < 4;
                    if (!gate) {
                        for (int y = 0; y < 9; y++) {
                            set(level, new BlockPos(wx, groundY + y, wz), ModBlocks.INFESTED_STONE_BRICKS);
                        }
                        if ((wx + wz) % 7 == 0) {
                            set(level, new BlockPos(wx, groundY + 9, wz), ModBlocks.WARNING_LAMP);
                        }
                    } else {
                        set(level, new BlockPos(wx, groundY - 1, wz), ModBlocks.INFESTED_STONE);
                    }
                    continue;
                }

                // ДОРОЖНАЯ СЕТКА (полосы 3 шириной по мировой сетке 16) с неоном.
                boolean road = (wx & 15) < 3 || (wz & 15) < 3;
                if (road) {
                    boolean neon = ((wx & 15) == 1 && wz % 6 == 0) || ((wz & 15) == 1 && wx % 6 == 0);
                    set(level, new BlockPos(wx, groundY - 1, wz),
                            neon ? ModBlocks.PURE_RADIATION_BLOCK : ModBlocks.INFESTED_STONE);
                } else {
                    // Двор квартала — тёмный камень вместо травы.
                    set(level, new BlockPos(wx, groundY - 1, wz), ModBlocks.INFESTED_STONE);
                }
            }
        }

        // ЗАСТРОЙКА КВАРТАЛА: одно сооружение на чанк, целиком внутри стен.
        long mdx = cp.getMiddleBlockX() - CITY_CENTER.getX();
        long mdz = cp.getMiddleBlockZ() - CITY_CENTER.getZ();
        double centerDist = Math.sqrt(mdx * mdx + mdz * mdz);
        if (centerDist < CITY_RADIUS - 18) {
            BlockPos plot = new BlockPos(cp.getMinBlockX() + 9, groundY, cp.getMinBlockZ() + 9);
            if (cp.x == CITY_CENTER.getX() >> 4 && cp.z == CITY_CENTER.getZ() >> 4) {
                centralSpire(level, r, plot);
            } else {
                cityBuilding(level, r, plot);
            }
        }

        // Воздушный трафик: летающие тарелки над кварталами.
        if (centerDist < CITY_RADIUS && r.nextInt(7) == 0) {
            Mob ufo = EntityRegistry.UFO.create(level);
            if (ufo != null) {
                ufo.moveTo(cp.getMiddleBlockX() + 0.5D, groundY + 26 + r.nextInt(14),
                        cp.getMiddleBlockZ() + 0.5D, r.nextFloat() * 360.0F, 0.0F);
                ufo.setPersistenceRequired();
                level.addFreshEntity(ufo);
            }
        }
    }

    private static void cityBuilding(ServerLevel level, RandomSource r, BlockPos base) {
        int roll = r.nextInt(100);
        if (roll < 40) {
            cityTower(level, r, base);
        } else if (roll < 55) {
            cityDome(level, r, base);
        } else if (roll < 70) {
            landingPad(level, r, base);
        } else if (roll < 82) {
            tankFarm(level, r, base);
        } else if (roll < 92) {
            factory(level, r, base);
        } else {
            plaza(level, r, base);
        }
    }

    /** Башня: кирпичный каркас, вертикальные неоновые витражи, кристалл на крыше. */
    private static void cityTower(ServerLevel level, RandomSource r, BlockPos base) {
        int half = 3 + r.nextInt(2);              // 7 или 9 в поперечнике
        int height = 14 + r.nextInt(25);          // 14..38
        for (int y = 0; y <= height; y++) {
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    boolean shell = Math.abs(dx) == half || Math.abs(dz) == half;
                    BlockPos p = base.offset(dx, y, dz);
                    if (y == height) {
                        set(level, p, ModBlocks.INFESTED_STONE_BRICKS); // крыша
                    } else if (shell) {
                        boolean doorway = dz == half && Math.abs(dx) <= 1 && y >= 1 && y <= 2;
                        if (doorway) {
                            level.removeBlock(p, false);
                        } else {
                            boolean strip = (dx == 0 || dz == 0) && y % 5 != 0;
                            set(level, p, strip ? ModBlocks.INFESTED_GLASS : ModBlocks.INFESTED_STONE_BRICKS);
                        }
                    } else if (y % 5 == 0 && y > 0) {
                        set(level, p, ModBlocks.INFESTED_STONE); // межэтажные перекрытия
                    } else if (y > 0) {
                        level.removeBlock(p, false); // полый интерьер
                    }
                }
            }
        }
        // Крыша: лампы по углам и кристалл-антенна в центре.
        for (int sx : new int[] { -half, half }) {
            for (int sz : new int[] { -half, half }) {
                set(level, base.offset(sx, height + 1, sz), ModBlocks.WARNING_LAMP);
            }
        }
        set(level, base.above(height + 1), ModBlocks.DARK_MATTER_ORE);
        // Лут и охрана на первом этаже.
        if (r.nextBoolean()) {
            set(level, base.offset(half - 1, 1, -half + 1), ModBlocks.ALIEN_STASH);
        }
        if (r.nextInt(3) != 0) {
            set(level, base.offset(-half + 1, 1, half - 1), ModBlocks.BROKEN_LAB_CRATE);
        }
        spawnCityGuards(level, r, base, 1 + r.nextInt(2));
    }

    /** Купол-лаборатория: стеклянная полусфера со светящимся ядром. */
    private static void cityDome(ServerLevel level, RandomSource r, BlockPos base) {
        int radius = 5;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= radius; dy++) {
                    double dist = Math.sqrt(dx * dx + dz * dz + dy * dy);
                    if (dist <= radius && dist > radius - 1.3D) {
                        boolean doorway = dz == radius - 1 && Math.abs(dx) <= 1 && dy <= 2;
                        if (!doorway) {
                            set(level, base.offset(dx, dy, dz),
                                    dy == 0 ? ModBlocks.INFESTED_STONE_BRICKS : ModBlocks.INFESTED_GLASS);
                        }
                    }
                }
            }
        }
        for (int y = 0; y < 2; y++) {
            set(level, base.above(y), ModBlocks.PURE_RADIATION_BLOCK);
        }
        set(level, base.above(2), ModBlocks.DARK_MATTER_ORE);
        if (r.nextBoolean()) {
            set(level, base.offset(2, 0, -2), ModBlocks.ALIEN_STASH);
        }
        spawnCityGuards(level, r, base, 1);
    }

    /** Посадочная площадка на опорах, с лампами и припаркованной тарелкой. */
    private static void landingPad(ServerLevel level, RandomSource r, BlockPos base) {
        int half = 5, lift = 6;
        for (int sx : new int[] { -half + 1, half - 1 }) {
            for (int sz : new int[] { -half + 1, half - 1 }) {
                for (int y = 0; y < lift; y++) {
                    set(level, base.offset(sx, y, sz), ModBlocks.INFESTED_STONE_BRICKS);
                }
            }
        }
        for (int dx = -half; dx <= half; dx++) {
            for (int dz = -half; dz <= half; dz++) {
                set(level, base.offset(dx, lift, dz), ModBlocks.INFESTED_STONE);
            }
        }
        for (int sx : new int[] { -half, half }) {
            for (int sz : new int[] { -half, half }) {
                set(level, base.offset(sx, lift + 1, sz), ModBlocks.WARNING_LAMP);
            }
        }
        if (r.nextInt(3) != 0) {
            Mob ufo = EntityRegistry.UFO.create(level);
            if (ufo != null) {
                ufo.moveTo(base.getX() + 0.5D, base.getY() + lift + 2, base.getZ() + 0.5D,
                        r.nextFloat() * 360.0F, 0.0F);
                ufo.setPersistenceRequired();
                level.addFreshEntity(ufo);
            }
        }
    }

    /** Топливный склад: колонны токсичных бочек, связанные трубами. */
    private static void tankFarm(ServerLevel level, RandomSource r, BlockPos base) {
        for (int i = 0; i < 3; i++) {
            BlockPos at = base.offset(r.nextInt(9) - 4, 0, r.nextInt(9) - 4);
            int h = 3 + r.nextInt(3);
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    for (int y = 0; y < h; y++) {
                        set(level, at.offset(dx, y, dz), ModBlocks.TOXIC_BARREL);
                    }
                }
            }
            set(level, at.offset(0, h, 0), ModBlocks.WARNING_LAMP);
        }
        pipeRuin(level, r, base);
        spawnCityGuards(level, r, base, 1);
    }

    /** Завод: цех с дымовыми трубами, ящиками и охраной. */
    private static void factory(ServerLevel level, RandomSource r, BlockPos base) {
        int hx = 5, hz = 4, height = 6;
        for (int dx = -hx; dx <= hx; dx++) {
            for (int dz = -hz; dz <= hz; dz++) {
                boolean shell = Math.abs(dx) == hx || Math.abs(dz) == hz;
                for (int y = 0; y <= height; y++) {
                    BlockPos p = base.offset(dx, y, dz);
                    if (y == height) {
                        set(level, p, ModBlocks.INFESTED_STONE);
                    } else if (shell) {
                        boolean doorway = dz == hz && Math.abs(dx) <= 1 && y >= 1 && y <= 2;
                        if (doorway) {
                            level.removeBlock(p, false);
                        } else {
                            set(level, p, y == 4 ? ModBlocks.INFESTED_GLASS : ModBlocks.INFESTED_STONE_BRICKS);
                        }
                    } else if (y == 0) {
                        set(level, p, ModBlocks.ALIEN_RESIDUE);
                    } else {
                        level.removeBlock(p, false);
                    }
                }
            }
        }
        // Дымовые трубы на крыше.
        for (int i = 0; i < 2; i++) {
            BlockPos chim = base.offset(-hx + 2 + i * 5, height + 1, -hz + 2);
            for (int y = 0; y < 4; y++) {
                level.setBlock(chim.above(y), ModBlocks.CRACKED_ALIEN_PIPE.defaultBlockState(), 2);
            }
        }
        set(level, base.offset(hx - 1, 1, hz - 1), ModBlocks.ALIEN_STASH);
        set(level, base.offset(-hx + 1, 1, -hz + 1), ModBlocks.BROKEN_LAB_CRATE);
        set(level, base.offset(-hx + 2, 1, -hz + 1), ModBlocks.BROKEN_LAB_CRATE);
        spawnCityGuards(level, r, base, 2);
    }

    /** Площадь: монолит, кристаллы и щупальца — место собраний роя. */
    private static void plaza(ServerLevel level, RandomSource r, BlockPos base) {
        monolith(level, r, base);
        for (int sx : new int[] { -4, 4 }) {
            for (int sz : new int[] { -4, 4 }) {
                set(level, base.offset(sx, 0, sz), ModBlocks.PURE_RADIATION_BLOCK);
            }
        }
        tendrilGarden(level, r, base);
        spawnCityGuards(level, r, base, 1 + r.nextInt(2));
    }

    /** ЦЕНТРАЛЬНЫЙ ШПИЛЬ: 60 блоков неона и стекла, наверху — Тиран Улья и трофеи. */
    private static void centralSpire(ServerLevel level, RandomSource r, BlockPos base) {
        int radius = 5, height = 60;
        for (int y = 0; y < height; y++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dz * dz);
                    if (d > radius || d <= radius - 1.4D) {
                        continue; // полый цилиндр
                    }
                    // Вход с южной стороны: проём 3x3 у земли.
                    if (y >= 1 && y <= 3 && dz > 0 && Math.abs(dx) <= 1) {
                        continue;
                    }
                    boolean strip = ((dx + dz + 100) % 3 == 0) && y % 9 != 0;
                    set(level, base.offset(dx, y, dz),
                            strip ? ModBlocks.INFESTED_GLASS : ModBlocks.INFESTED_STONE_BRICKS);
                }
            }
            if (y % 9 == 0 && y > 0) {
                set(level, base.offset(radius, y, 0), ModBlocks.WARNING_LAMP);
                set(level, base.offset(-radius, y, 0), ModBlocks.WARNING_LAMP);
                set(level, base.offset(0, y, radius), ModBlocks.WARNING_LAMP);
                set(level, base.offset(0, y, -radius), ModBlocks.WARNING_LAMP);
            }
        }
        // Шахта с лестницей по северной внутренней стене — путь на трон.
        for (int y = 1; y <= height + 1; y++) {
            level.setBlock(base.offset(0, y, -(radius - 2)),
                    net.minecraft.world.level.block.Blocks.LADDER.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.LadderBlock.FACING,
                                    net.minecraft.core.Direction.SOUTH), 2);
        }
        // Тронная площадка на вершине (с люком над лестницей и парапетом).
        for (int dx = -radius - 2; dx <= radius + 2; dx++) {
            for (int dz = -radius - 2; dz <= radius + 2; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 2) {
                    continue;
                }
                if (dx == 0 && dz == -(radius - 2)) {
                    continue; // люк: лестница выходит сквозь площадку
                }
                set(level, base.offset(dx, height, dz), ModBlocks.INFESTED_STONE);
                if (d > radius + 1) {
                    set(level, base.offset(dx, height + 1, dz), ModBlocks.INFESTED_STONE_BRICKS); // парапет
                }
            }
        }
        for (int sx : new int[] { -radius, radius }) {
            for (int sz : new int[] { -radius, radius }) {
                for (int y = 1; y <= 3; y++) {
                    set(level, base.offset(sx, height + y, sz), ModBlocks.DARK_MATTER_ORE);
                }
            }
        }
        set(level, base.offset(0, height + 1, 0), ModBlocks.ALIEN_FLESH);
        set(level, base.offset(0, height + 2, 0), ModBlocks.ALIEN_HEART);
        set(level, base.offset(2, height + 1, 2), ModBlocks.ALIEN_STASH);
        set(level, base.offset(-2, height + 1, -2), ModBlocks.ALIEN_STASH);
        // Хозяин города.
        Mob tyrant = EntityRegistry.HIVE_TYRANT.create(level);
        if (tyrant != null) {
            tyrant.moveTo(base.getX() + 0.5D, base.getY() + height + 1, base.getZ() + 3.5D,
                    r.nextFloat() * 360.0F, 0.0F);
            tyrant.setPersistenceRequired();
            level.addFreshEntity(tyrant);
        }
    }

    private static void spawnCityGuards(ServerLevel level, RandomSource r, BlockPos base, int count) {
        for (int i = 0; i < count; i++) {
            float roll = r.nextFloat();
            Mob guard = (roll < 0.5F ? EntityRegistry.ALIEN_GRUNT
                    : roll < 0.8F ? EntityRegistry.PLASMA_CASTER
                    : EntityRegistry.ALIEN_STALKER).create(level);
            if (guard != null) {
                guard.moveTo(base.getX() + r.nextInt(9) - 4 + 0.5D, base.getY() + 1,
                        base.getZ() + r.nextInt(9) - 4 + 0.5D, r.nextFloat() * 360.0F, 0.0F);
                guard.setPersistenceRequired();
                level.addFreshEntity(guard);
            }
        }
    }

    /** Мега-шпиль: 16-24 блока высоты, сердце на вершине, стражи у подножия. */
    private static void megaSpire(ServerLevel level, RandomSource r, BlockPos base) {
        int height = 16 + r.nextInt(9);
        int x = base.getX(), z = base.getZ();
        for (int i = 0; i < height; i++) {
            int radius = i < 3 ? 2 : (i < height / 2 ? 1 : 0);
            if (r.nextInt(5) == 0) {
                x += r.nextInt(3) - 1;
                z += r.nextInt(3) - 1;
            }
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = r.nextFloat() < 0.4F ? ModBlocks.ALIEN_HIVE
                            : r.nextFloat() < 0.5F ? ModBlocks.ALIEN_FLESH : ModBlocks.INFESTED_STONE;
                    set(level, new BlockPos(x + dx, base.getY() + i, z + dz), b);
                }
            }
        }
        set(level, new BlockPos(x, base.getY() + height, z), ModBlocks.ALIEN_HEART);
        for (int i = 0; i < 2; i++) {
            Mob guard = EntityRegistry.ALIEN_GRUNT.create(level);
            if (guard != null) {
                guard.moveTo(base.getX() + r.nextInt(7) - 3, base.getY() + 1,
                        base.getZ() + r.nextInt(7) - 3, r.nextFloat() * 360.0F, 0.0F);
                guard.setPersistenceRequired();
                level.addFreshEntity(guard);
            }
        }
    }

    /** Строит главный улей и обратный портал, если их ещё нет. Вызывать перед телепортом игрока. */
    public static void ensureArena(ServerLevel hw) {
        HomeworldManager data = get(hw);
        if (data.arenaBuilt) {
            return;
        }
        data.arenaBuilt = true;
        data.setDirty();

        RandomSource r = hw.random;
        int surface = hw.getHeight(Heightmap.Types.MOTION_BLOCKING, HIVE_CENTER.getX(), HIVE_CENTER.getZ());

        // ГЛАВНЫЙ УЛЕЙ: полый органический купол R=18, H=14, с проёмами по сторонам света.
        int radius = 18;
        int height = 14;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > radius) {
                    continue;
                }
                int domeY = (int) Math.round(height * Math.cos((d / radius) * (Math.PI / 2.0D)));
                boolean entrance = (Math.abs(x) < 2 || Math.abs(z) < 2) && d > radius - 4;
                for (int y = 0; y <= domeY; y++) {
                    boolean shell = y >= domeY - 1 || d >= radius - 1.5D;
                    if (!shell) {
                        continue;
                    }
                    if (entrance && y <= 4) {
                        continue; // проёмы-входы у земли
                    }
                    if (r.nextFloat() < 0.92F) {
                        float roll = r.nextFloat();
                        var block = roll < 0.35F ? ModBlocks.ALIEN_HIVE
                                : roll < 0.65F ? ModBlocks.INFESTED_STONE
                                : roll < 0.85F ? ModBlocks.ALIEN_FLESH
                                : ModBlocks.ALIEN_RESIDUE;
                        hw.setBlockAndUpdate(new BlockPos(x, surface + y, z), block.defaultBlockState());
                    }
                }
            }
        }
        // Сердце улья на колонне в центре купола.
        for (int y = 0; y < height - 4; y++) {
            hw.setBlockAndUpdate(new BlockPos(0, surface + y, 0), ModBlocks.ALIEN_FLESH.defaultBlockState());
        }
        hw.setBlockAndUpdate(new BlockPos(0, surface + height - 4, 0), ModBlocks.ALIEN_HEART.defaultBlockState());
        // Щупальца и лужи вокруг улья.
        for (int i = 0; i < 60; i++) {
            BlockPos p = new BlockPos(r.nextInt(61) - 30, 0, r.nextInt(61) - 30);
            int y = hw.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            BlockPos ground = new BlockPos(p.getX(), y, p.getZ());
            if (hw.getBlockState(ground).isAir()) {
                hw.setBlockAndUpdate(ground, r.nextBoolean()
                        ? ModBlocks.ALIEN_TENDRILS.defaultBlockState()
                        : ModBlocks.BLOOD_POOL.defaultBlockState());
            }
        }

        // Обратный портал — к югу от улья, лицом на купол.
        int portalY = hw.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 30);
        buildPortalFrame(hw, new BlockPos(-2, portalY, 30));

        // Стражи улья.
        for (int i = 0; i < 8; i++) {
            Mob guard = (i % 3 == 0 ? EntityRegistry.ALIEN_RAPTOR : EntityRegistry.ALIEN_GRUNT).create(hw);
            if (guard != null) {
                double a = r.nextDouble() * Math.PI * 2.0D;
                double gx = Math.cos(a) * (radius + 4);
                double gz = Math.sin(a) * (radius + 4);
                int gy = hw.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) gx, (int) gz);
                guard.moveTo(gx, gy, gz, r.nextFloat() * 360.0F, 0.0F);
                guard.finalizeSpawn(hw, hw.getCurrentDifficultyAt(guard.blockPosition()), MobSpawnType.EVENT, null);
                guard.setPersistenceRequired();
                hw.addFreshEntity(guard);
            }
        }
    }

    /**
     * Рамка портала 4x5 вдоль оси X: обсидиан с плачущими углами, внутри 2x3
     * портальных блока. base — нижний левый блок рамки.
     */
    public static void buildPortalFrame(ServerLevel level, BlockPos base) {
        // Площадка под порталом, чтобы он не висел в воздухе.
        for (int dx = -1; dx <= 4; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setBlockAndUpdate(base.offset(dx, -1, dz), Blocks.OBSIDIAN.defaultBlockState());
            }
        }
        for (int dx = 0; dx < 4; dx++) {
            for (int dy = 0; dy < 5; dy++) {
                boolean frame = dx == 0 || dx == 3 || dy == 0 || dy == 4;
                boolean corner = (dx == 0 || dx == 3) && (dy == 0 || dy == 4);
                BlockPos p = base.offset(dx, dy, 0);
                level.setBlockAndUpdate(p, corner ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                        : frame ? Blocks.OBSIDIAN.defaultBlockState()
                        : ModBlocks.ALIEN_PORTAL.defaultBlockState());
            }
        }
    }
}
