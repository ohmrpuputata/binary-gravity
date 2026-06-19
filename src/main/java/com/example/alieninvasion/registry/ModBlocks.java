package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.AlienBeaconBlockEntity;
import com.example.alieninvasion.block.AlienHiveBlock;
import com.example.alieninvasion.block.AlienResidueBlock;
import com.example.alieninvasion.block.AlienStashBlock;
import com.example.alieninvasion.block.AlienStashBlockEntity;
import com.example.alieninvasion.block.BlackMarketTerminalBlock;
import com.example.alieninvasion.block.BloodPoolBlock;
import com.example.alieninvasion.block.DarkMatterOreBlock;
import com.example.alieninvasion.block.CosmicCrystalOreBlock;
import com.example.alieninvasion.block.PureRadiationBlock;
import com.example.alieninvasion.block.DeadInfestedCropBlock;
import com.example.alieninvasion.block.InfestedBlock;
import com.example.alieninvasion.block.InfestedGrassBlock;
import com.example.alieninvasion.block.InfestedLeavesBlock;
import com.example.alieninvasion.block.InfestedLogBlock;
import com.example.alieninvasion.block.PurifierBlock;
import com.example.alieninvasion.block.ToxicWaterBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {
    public static final Block TOXIC_WATER = registerBlockNoItem("toxic_water",
            new ToxicWaterBlock(ModFluids.TOXIC_WATER_STILL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).replaceable().noCollission()
                            .strength(100.0F).noLootTable().liquid().pushReaction(PushReaction.DESTROY)));

    public static final Block BLOOD_POOL = registerBlockNoItem("blood_pool",
            new BloodPoolBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .noCollission().instabreak().replaceable().randomTicks().noLootTable().noOcclusion()
                    .sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.DESTROY)));

    // Стойкая кровавая декаль поверх ЛЮБОГО блока (не исчезает сама; ПКМ/вода смывают).
    // Кладётся splatter'ом на блоки, которые нельзя превратить в кровавый куб
    // (листва, стекло, модовые/нестандартные блоки) — чтобы кровь была на ВСём.
    public static final Block BLOOD_LAYER = registerBlockNoItem("blood_layer",
            new com.example.alieninvasion.block.BloodLayerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .noCollission().instabreak().noLootTable().noOcclusion().replaceable().randomTicks()
                    .sound(SoundType.WET_GRASS).pushReaction(PushReaction.DESTROY)));

    // Ядовитое облако: проходимый полупрозрачный газ-блок. Голова в нём = «ядовитый
    // воздух» — без герметичной маски травит, в маске тратит запас воздуха. Им
    // заполняют заражённые данжи/зоны.
    public static final Block TOXIC_GAS = registerBlock("toxic_gas",
            new com.example.alieninvasion.block.ToxicGasBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                    .noCollission().instabreak().noOcclusion().replaceable().noLootTable()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)));

    public static final Block INFECTED_WATER = registerBlockNoItem("infected_water",
            new com.example.alieninvasion.block.InfectedWaterBlock(ModFluids.INFECTED_WATER_STILL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).replaceable().noCollission()
                            .strength(100.0F).noLootTable().liquid().pushReaction(PushReaction.DESTROY)));

    public static final Block ALIEN_RESIDUE = registerBlock("alien_residue",
            new AlienResidueBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).randomTicks()
                            .sound(SoundType.SLIME_BLOCK)));

    public static final Block PURE_RADIATION_BLOCK = registerBlock("pure_radiation_block",
            new PureRadiationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(3.0F).lightLevel(s -> 12).sound(SoundType.AMETHYST)));

    // --- New Day-3 ore blocks (converted from lapis/gold by contamination spread) ---
    public static final Block COSMIC_CRYSTAL_ORE = registerBlock("cosmic_crystal_ore",
            new CosmicCrystalOreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 4)));

    public static final Block INFESTED_COSMIC_CRYSTAL_ORE = registerBlock("infested_cosmic_crystal_ore",
            new CosmicCrystalOreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(s -> 4)));

    // --- Day-infected ores ---
    public static final Block PLATINUM_ORE = registerBlock("platinum_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final Block PALLADIUM_ORE = registerBlock("palladium_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final Block PALLADIUM_BLOCK = registerBlock("palladium_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN).strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final Block PLATINUM_BLOCK = registerBlock("platinum_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(5.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final Block ALIEN_FLESH = registerBlock("alien_flesh",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final Block INFESTED_DIAMOND_ORE = registerBlock("infested_diamond_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final Block INFESTED_REDSTONE_ORE = registerBlock("infested_redstone_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops().lightLevel(s -> 7).sound(SoundType.DEEPSLATE)));

    public static final Block ALIEN_HIVE = registerBlock("alien_hive",
            new AlienHiveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).randomTicks()
                    .sound(SoundType.NETHER_WART).lightLevel(state -> 10)));

    public static final Block INFESTED_STONE = registerBlock("infested_stone",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(1.5F)
                    .randomTicks().sound(SoundType.STONE)));

    public static final Block INFESTED_DEEPSLATE = registerBlock("infested_deepslate",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(2.2F)
                    .randomTicks().sound(SoundType.DEEPSLATE)));

    public static final Block INFESTED_DIRT = registerBlock("infested_dirt",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.55F)
                    .randomTicks().sound(SoundType.GRAVEL)));

    public static final Block INFESTED_SAND = registerBlock("infested_sand",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F)
                    .randomTicks().sound(SoundType.SAND)));

    public static final Block INFESTED_GRAVEL = registerBlock("infested_gravel",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.6F)
                    .randomTicks().sound(SoundType.GRAVEL)));

    public static final Block INFESTED_CLAY = registerBlock("infested_clay",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).strength(0.7F)
                    .randomTicks().sound(SoundType.GRAVEL)));

    public static final Block INFESTED_NETHERRACK = registerBlock("infested_netherrack",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(0.6F)
                    .randomTicks().sound(SoundType.NETHERRACK)));

    public static final Block INFESTED_GRASS = registerBlock("infested_grass",
            new InfestedGrassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F)
                    .sound(SoundType.GRASS).randomTicks()));

    public static final Block INFESTED_LOG = registerBlock("infested_log",
            new InfestedLogBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0F).randomTicks().sound(SoundType.WOOD)));

    public static final Block INFESTED_PLANKS = registerBlock("infested_planks",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.8F)
                    .randomTicks().sound(SoundType.WOOD)));

    public static final Block INFESTED_LEAVES = registerBlock("infested_leaves",
            new InfestedLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.2F)
                    .randomTicks().sound(SoundType.GRASS).noOcclusion()));

    public static final Block INFESTED_SANDSTONE = registerBlock("infested_sandstone",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.9F)
                    .randomTicks().sound(SoundType.STONE)));

    public static final Block INFESTED_TERRACOTTA = registerBlock("infested_terracotta",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.25F)
                    .randomTicks().sound(SoundType.STONE)));

    public static final Block INFESTED_SNOW = registerBlock("infested_snow",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.3F)
                    .randomTicks().sound(SoundType.SNOW)));

    public static final Block INFESTED_ICE = registerBlock("infested_ice",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.6F)
                    .friction(0.98F).randomTicks().sound(SoundType.GLASS).noOcclusion()));

    public static final Block INFESTED_STONE_BRICKS = registerBlock("infested_stone_bricks",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).strength(1.6F)
                    .randomTicks().sound(SoundType.STONE)));

    public static final Block INFESTED_WOOL = registerBlock("infested_wool",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6F)
                    .randomTicks().sound(SoundType.WOOL)));

    public static final Block INFESTED_GLASS = registerBlock("infested_glass",
            new InfestedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3F)
                    .randomTicks().sound(SoundType.GLASS).noOcclusion()));

    public static final Block INFESTED_DOOR = registerBlock("infested_door",
            new net.minecraft.world.level.block.DoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.2F)
                            .sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.DESTROY)));

    public static final Block INFESTED_TRAPDOOR = registerBlock("infested_trapdoor",
            new net.minecraft.world.level.block.TrapDoorBlock(net.minecraft.world.level.block.state.properties.BlockSetType.OAK,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.2F)
                            .sound(SoundType.WOOD).noOcclusion()));

    // --- Bloodstained blocks: shape-preserving gore. Right-click wipes them
    // clean; touching water washes them instantly (see BloodyBlocks). ---
    private static BlockBehaviour.Properties bloodyProps(float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(strength).sound(sound);
    }

    public static final Block BLOODY_PLANKS = registerBlock("bloody_planks",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(2.0F, SoundType.WOOD),
                    () -> net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState()));

    public static final Block BLOODY_STONE = registerBlock("bloody_stone",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(1.5F, SoundType.STONE),
                    () -> net.minecraft.world.level.block.Blocks.STONE.defaultBlockState()));

    public static final Block BLOODY_DIRT = registerBlock("bloody_dirt",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(0.5F, SoundType.GRAVEL),
                    () -> net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState()));

    public static final Block BLOODY_STONE_BRICKS = registerBlock("bloody_stone_bricks",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(1.6F, SoundType.STONE),
                    () -> net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState()));

    public static final Block BLOODY_PLANK_STAIRS = registerBlock("bloody_plank_stairs",
            new com.example.alieninvasion.block.BloodyBlocks.Stairs(
                    net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState(),
                    bloodyProps(2.0F, SoundType.WOOD),
                    () -> net.minecraft.world.level.block.Blocks.OAK_STAIRS.defaultBlockState()));

    public static final Block BLOODY_STONE_STAIRS = registerBlock("bloody_stone_stairs",
            new com.example.alieninvasion.block.BloodyBlocks.Stairs(
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                    bloodyProps(1.5F, SoundType.STONE),
                    () -> net.minecraft.world.level.block.Blocks.STONE_STAIRS.defaultBlockState()));

    public static final Block BLOODY_PLANK_SLAB = registerBlock("bloody_plank_slab",
            new com.example.alieninvasion.block.BloodyBlocks.Slab(bloodyProps(2.0F, SoundType.WOOD),
                    () -> net.minecraft.world.level.block.Blocks.OAK_SLAB.defaultBlockState()));

    public static final Block BLOODY_STONE_SLAB = registerBlock("bloody_stone_slab",
            new com.example.alieninvasion.block.BloodyBlocks.Slab(bloodyProps(1.5F, SoundType.STONE),
                    () -> net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState()));

    public static final Block BLOODY_PLANK_FENCE = registerBlock("bloody_plank_fence",
            new com.example.alieninvasion.block.BloodyBlocks.Fence(bloodyProps(2.0F, SoundType.WOOD),
                    () -> net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState()));

    // Кровь на ЗАРАЖЁННЫХ блоках — отдельная текстура (заражённая основа + кровь),
    // чтобы не выглядело как чужеродный обычный камень. Точный оригинал хранит BE.
    public static final Block BLOODY_INFESTED = registerBlock("bloody_infested",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(1.5F, SoundType.STONE),
                    () -> INFESTED_STONE.defaultBlockState()));
    public static final Block BLOODY_INFESTED_DIRT = registerBlock("bloody_infested_dirt",
            new com.example.alieninvasion.block.BloodyBlocks.Plain(bloodyProps(0.6F, SoundType.GRAVEL),
                    () -> INFESTED_DIRT.defaultBlockState()));

    public static final Block INFESTED_CRAFTING_TABLE = registerBlock("infested_crafting_table",
            new net.minecraft.world.level.block.CraftingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)) {});

    // Заражённые станции деревни: выглядят как ванильные, но поглощены биомассой
    // (декоративные руины — не функциональны). Заражение переводит ванильные
    // станции в эти блоки, очиститель возвращает обратно (см. ContaminationRules).
    public static final Block INFESTED_BARREL = registerBlock("infested_barrel",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block INFESTED_CARTOGRAPHY_TABLE = registerBlock("infested_cartography_table",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block INFESTED_SMITHING_TABLE = registerBlock("infested_smithing_table",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block INFESTED_FLETCHING_TABLE = registerBlock("infested_fletching_table",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block INFESTED_LOOM = registerBlock("infested_loom",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F).sound(SoundType.WOOD)));
    public static final Block INFESTED_STONECUTTER = registerBlock("infested_stonecutter",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F).sound(SoundType.STONE).noOcclusion()));
    public static final Block INFESTED_GRINDSTONE = registerBlock("infested_grindstone",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F).sound(SoundType.STONE).noOcclusion()));

    private static final boolean BLOODY_VARIANTS_REGISTERED = BloodyVariantRegistry.registerAll();

    // (Блок «Сердце заражения» удалён по просьбе — был чисто декоративным.)

    public static final Block RADIO_TRANSMITTER = registerBlock("radio_transmitter",
            new com.example.alieninvasion.block.RadioTransmitterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(2.5F).requiresCorrectToolForDrops()
                    .sound(SoundType.METAL).lightLevel(state -> 3)));

    public static final Block ALIEN_TENDRILS = registerBlock("alien_tendrils",
            new com.example.alieninvasion.block.AlienTendrilsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).noCollission().instabreak().noLootTable()
                    .sound(SoundType.GRASS).lightLevel(state -> 5).noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final Block PURIFIER = registerBlock("purifier",
            new PurifierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 12)));

    public static final Block ALIEN_STASH = registerBlock("alien_stash",
            new AlienStashBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F)
                    .sound(SoundType.WOOD)));

    public static final Block ALIEN_BEACON = registerBlock("alien_beacon",
            new com.example.alieninvasion.block.AlienBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5F).sound(SoundType.METAL)));

    public static final Block SWARM_BEACON = registerBlock("swarm_beacon",
            new com.example.alieninvasion.block.SwarmBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F).sound(SoundType.METAL).lightLevel(state -> 8)));

    public static final Block DARK_MATTER_ORE = registerBlock("dark_matter_ore",
            new DarkMatterOreBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 15)));

    public static final Block PLASMA_TURRET = registerBlock("plasma_turret",
            new com.example.alieninvasion.block.PlasmaTurretBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final Block BLACK_MARKET_TERMINAL = registerBlock("black_market_terminal",
            new BlackMarketTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 6)));

    public static final Block PURIFIER_STATION = registerBlock("purifier_station",
            new com.example.alieninvasion.block.PurifierStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 8)));

    public static final Block ORE_WASHER = registerBlock("ore_washer",
            new com.example.alieninvasion.block.OreWasherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final Block BLUEPRINT_TABLE = registerBlock("blueprint_table",
            new com.example.alieninvasion.block.BlueprintTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    public static final Block WARNING_LAMP = registerBlock("warning_lamp",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 12)));

    public static final Block CRACKED_ALIEN_PIPE = registerBlock("cracked_alien_pipe",
            new net.minecraft.world.level.block.RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5F).sound(SoundType.METAL).noOcclusion()));

    public static final Block TOXIC_BARREL = registerBlock("toxic_barrel",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0F).sound(SoundType.METAL)));

    public static final Block BROKEN_LAB_CRATE = registerBlock("broken_lab_crate",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.4F).sound(SoundType.WOOD)));

    public static final Block CONTAMINATED_BONES = registerBlock("contaminated_bones",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.7F).sound(SoundType.BONE_BLOCK)));

    public static final BlockEntityType<AlienStashBlockEntity> ALIEN_STASH_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_stash"),
            BlockEntityType.Builder.of(AlienStashBlockEntity::new, ALIEN_STASH).build(null)
    );

    // Хранит точный исходный блок под кровью (любой ваниль/мод/заражённый блок),
    // чтобы при смывании вернулся именно он. Один тип на все кровавые блоки.
    public static final BlockEntityType<com.example.alieninvasion.block.BloodyBlockEntity> BLOODY_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "bloody_block"),
            BlockEntityType.Builder.of(com.example.alieninvasion.block.BloodyBlockEntity::new,
                    BloodyVariantRegistry.allBloodyBlocks()).build(null)
    );

    public static final BlockEntityType<AlienBeaconBlockEntity> ALIEN_BEACON_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_beacon"),
            BlockEntityType.Builder.of(AlienBeaconBlockEntity::new, ALIEN_BEACON).build(null)
    );

    public static final BlockEntityType<com.example.alieninvasion.block.PlasmaTurretBlockEntity> PLASMA_TURRET_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_turret"),
            BlockEntityType.Builder.of(com.example.alieninvasion.block.PlasmaTurretBlockEntity::new, PLASMA_TURRET).build(null)
    );

    // --- Финальный акт: портал в мир Роя и реактор охотника ---
    public static final Block ALIEN_PORTAL = registerBlockNoItem("alien_portal",
            new com.example.alieninvasion.block.AlienPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).noLootTable()
                    .noCollission().lightLevel(s -> 11).sound(SoundType.GLASS)
                    .pushReaction(PushReaction.BLOCK)));

    public static final Block PLANET_REACTOR = registerBlock("planet_reactor",
            new com.example.alieninvasion.block.PlanetReactorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE).strength(25.0F, 1200.0F)
                    .requiresCorrectToolForDrops().lightLevel(s -> 9).sound(SoundType.NETHERITE_BLOCK)));

    public static final BlockEntityType<com.example.alieninvasion.block.PlanetReactorBlockEntity> PLANET_REACTOR_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "planet_reactor"),
            BlockEntityType.Builder.of(com.example.alieninvasion.block.PlanetReactorBlockEntity::new, PLANET_REACTOR).build(null)
    );

    public static final Block PLATINUM_ANVIL = registerBlock("platinum_anvil",
            new com.example.alieninvasion.block.PlatinumAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()));

    public static final net.minecraft.world.inventory.MenuType<com.example.alieninvasion.block.PlatinumAnvilMenu> PLATINUM_ANVIL_MENU = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_anvil"),
            new net.minecraft.world.inventory.MenuType<>(com.example.alieninvasion.block.PlatinumAnvilMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static final net.minecraft.world.inventory.MenuType<com.example.alieninvasion.block.MaskMenu> MASK_MENU = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "mask"),
            new net.minecraft.world.inventory.MenuType<>(com.example.alieninvasion.block.MaskMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static final net.minecraft.world.item.crafting.RecipeType<com.example.alieninvasion.block.PlatinumAnvilRecipe> PLATINUM_ANVIL_RECIPE_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_anvil"),
            new net.minecraft.world.item.crafting.RecipeType<>() {
                @Override
                public String toString() {
                    return "platinum_anvil";
                }
            }
    );

    public static final net.minecraft.world.item.crafting.RecipeSerializer<com.example.alieninvasion.block.PlatinumAnvilRecipe> PLATINUM_ANVIL_RECIPE_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_anvil"),
            new com.example.alieninvasion.block.PlatinumAnvilRecipe.Serializer()
    );

    public static final Block PALLADIUM_ANVIL = registerBlock("palladium_anvil",
            new com.example.alieninvasion.block.PalladiumAnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1200.0F)
                    .sound(SoundType.ANVIL)
                    .requiresCorrectToolForDrops()));

    public static final net.minecraft.world.inventory.MenuType<com.example.alieninvasion.block.PalladiumAnvilMenu> PALLADIUM_ANVIL_MENU = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "palladium_anvil"),
            new net.minecraft.world.inventory.MenuType<>(com.example.alieninvasion.block.PalladiumAnvilMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)
    );

    public static final net.minecraft.world.item.crafting.RecipeType<com.example.alieninvasion.block.PalladiumAnvilRecipe> PALLADIUM_ANVIL_RECIPE_TYPE = Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "palladium_anvil"),
            new net.minecraft.world.item.crafting.RecipeType<>() {
                @Override
                public String toString() {
                    return "palladium_anvil";
                }
            }
    );

    public static final net.minecraft.world.item.crafting.RecipeSerializer<com.example.alieninvasion.block.PalladiumAnvilRecipe> PALLADIUM_ANVIL_RECIPE_SERIALIZER = Registry.register(
            BuiltInRegistries.RECIPE_SERIALIZER,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "palladium_anvil"),
            new com.example.alieninvasion.block.PalladiumAnvilRecipe.Serializer()
    );

    static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name), block);
    }

    public static Block bloodyVariantFor(Block source) {
        return BloodyVariantRegistry.forSource(source);
    }

    public static Block cleanBloodyVariantForInfested(Block infested) {
        return BloodyVariantRegistry.cleanForInfested(infested);
    }

    private static Block registerBlockNoItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name),
                new BlockItem(block, new Item.Properties()));
    }

    public static void registerBlocks() {
        AlienInvasionMod.LOGGER.info("Registering ModBlocks for " + AlienInvasionMod.MODID);
    }
}
