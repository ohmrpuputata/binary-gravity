package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.AlienBeaconBlockEntity;
import com.example.alieninvasion.block.AlienHiveBlock;
import com.example.alieninvasion.block.AlienResidueBlock;
import com.example.alieninvasion.block.AlienStashBlock;
import com.example.alieninvasion.block.AlienStashBlockEntity;
import com.example.alieninvasion.block.BlackMarketTerminalBlock;
import com.example.alieninvasion.block.BloodPoolBlock;
import com.example.alieninvasion.block.CosmicCrystalBlock;
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

    // Flat blood-pool decal dropped by bleeding entities (see the bleeding mechanic
    // in ModEvents). No item; fades on its own.
    public static final Block BLOOD_POOL = registerBlockNoItem("blood_pool",
            new BloodPoolBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .noCollission().instabreak().replaceable().randomTicks().noLootTable().noOcclusion()
                    .sound(SoundType.SLIME_BLOCK).pushReaction(PushReaction.DESTROY)));

    // Infected water (harmless, water-like; corrupted look). No item - use the bucket.
    public static final Block INFECTED_WATER = registerBlockNoItem("infected_water",
            new com.example.alieninvasion.block.InfectedWaterBlock(ModFluids.INFECTED_WATER_STILL,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).replaceable().noCollission()
                            .strength(100.0F).noLootTable().liquid().pushReaction(PushReaction.DESTROY)));

    public static final Block ALIEN_RESIDUE = registerBlock("alien_residue",
            new AlienResidueBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).randomTicks()
                            .sound(SoundType.SLIME_BLOCK)));

    // Pure Radiation Block (day-4 cave infection / structure hazard). Glows; the
    // RadiationFieldManager makes it lethal up close. Drops a radiation crystal.
    public static final Block PURE_RADIATION_BLOCK = registerBlock("pure_radiation_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(3.0F).lightLevel(s -> 12).sound(SoundType.AMETHYST)));

    // --- Day-infected ores (converted from vanilla ores as the corruption spreads) ---
    public static final Block PLATINUM_ORE = registerBlock("platinum_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    public static final Block PALLADIUM_ORE = registerBlock("palladium_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));
    // Alien flesh (from infected iron). Soft; cut it for alien skin.
    public static final Block ALIEN_FLESH = registerBlock("alien_flesh",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6F)
                    .sound(SoundType.SLIME_BLOCK)));
    public static final Block INFESTED_DIAMOND_ORE = registerBlock("infested_diamond_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));
    public static final Block INFESTED_LAPIS_ORE = registerBlock("infested_lapis_ore",
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

    public static final Block DEAD_INFESTED_CROP = registerBlock("dead_infested_crop",
            new DeadInfestedCropBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollission()
                    .strength(0.1F).sound(SoundType.CROP).noOcclusion()));

    public static final Block PURIFIER = registerBlock("purifier",
            new PurifierBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 12)));

    public static final Block ALIEN_STASH = registerBlock("alien_stash",
            new AlienStashBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5F)
                    .sound(SoundType.WOOD)));

    public static final Block COSMIC_ORE = registerBlock("cosmic_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final Block URANIUM_ORE = registerBlock("uranium_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(3.3F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(state -> 2)));

    public static final Block DEEPSLATE_URANIUM_ORE = registerBlock("deepslate_uranium_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(4.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).lightLevel(state -> 2)));

    public static final Block XENOCRYSTAL_ORE = registerBlock("xenocrystal_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.8F)
                    .requiresCorrectToolForDrops().sound(SoundType.AMETHYST).lightLevel(state -> 5)));

    public static final Block BIO_VEIN_ORE = registerBlock("bio_vein_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.4F)
                    .requiresCorrectToolForDrops().sound(SoundType.NETHER_WART)));

    public static final Block PLASMA_ORE = registerBlock("plasma_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE).lightLevel(state -> 7)));

    public static final Block IRIDIUM_ORE = registerBlock("iridium_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE)));

    public static final Block DARK_MATTER_ORE = registerBlock("dark_matter_ore",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.SCULK).lightLevel(state -> 3)));

    public static final Block ALIEN_BEACON = registerBlock("alien_beacon",
            new com.example.alieninvasion.block.AlienBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED)
                    .strength(1.5F).sound(SoundType.METAL)));

    public static final Block SWARM_BEACON = registerBlock("swarm_beacon",
            new com.example.alieninvasion.block.SwarmBeaconBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(3.0F).sound(SoundType.METAL).lightLevel(state -> 8)));

    public static final Block COSMIC_BLOCK = registerBlock("cosmic_block",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(5.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 6)));

    public static final Block COSMIC_CRYSTAL = registerBlock("cosmic_crystal",
            new CosmicCrystalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 15)));

    public static final Block PLASMA_TURRET = registerBlock("plasma_turret",
            new com.example.alieninvasion.block.PlasmaTurretBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(3.5F).requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final Block BLACK_MARKET_TERMINAL = registerBlock("black_market_terminal",
            new BlackMarketTerminalBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 6)));

    public static final Block PURIFIER_STATION = registerBlock("purifier_station",
            new com.example.alieninvasion.block.PurifierStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> 8)));

    public static final Block ORE_WASHER = registerBlock("ore_washer",
            new com.example.alieninvasion.block.OreWasherBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final Block ALIEN_RECYCLER = registerBlock("alien_recycler",
            new com.example.alieninvasion.block.AlienRecyclerBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(3.5F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL)));

    public static final Block BLUEPRINT_TABLE = registerBlock("blueprint_table",
            new com.example.alieninvasion.block.BlueprintTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD)));

    public static final Block WARNING_LAMP = registerBlock("warning_lamp",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.0F)
                    .sound(SoundType.GLASS).lightLevel(state -> 12)));

    public static final Block CRACKED_ALIEN_PIPE = registerBlock("cracked_alien_pipe",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.5F).sound(SoundType.METAL)));

    public static final Block TOXIC_BARREL = registerBlock("toxic_barrel",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0F).sound(SoundType.METAL)));

    public static final Block BROKEN_LAB_CRATE = registerBlock("broken_lab_crate",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.4F).sound(SoundType.WOOD)));

    public static final Block RADIATION_CRYSTAL_CLUSTER = registerBlock("radiation_crystal_cluster",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.2F)
                    .sound(SoundType.AMETHYST).lightLevel(state -> 11).noOcclusion()));

    public static final Block CONTAMINATED_BONES = registerBlock("contaminated_bones",
            new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.7F).sound(SoundType.BONE_BLOCK)));

    public static final BlockEntityType<AlienStashBlockEntity> ALIEN_STASH_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "alien_stash"),
            BlockEntityType.Builder.of(AlienStashBlockEntity::new, ALIEN_STASH).build(null)
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

    private static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, name), block);
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
