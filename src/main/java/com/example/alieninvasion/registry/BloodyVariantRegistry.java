package com.example.alieninvasion.registry;

import com.example.alieninvasion.block.BloodyBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class BloodyVariantRegistry {
    private static final Map<Block, Block> BY_SOURCE = new IdentityHashMap<>();
    private static final Map<Block, Block> CLEAN_BY_INFESTED = new IdentityHashMap<>();
    private static final Map<Block, Block> INFESTED_BY_BLOODY = new IdentityHashMap<>();
    private static final Map<Block, Block> BLOODY_BY_COMBINED = new IdentityHashMap<>();
    private static final List<Block> ALL = new ArrayList<>();
    private static final List<Block> CUTOUT = new ArrayList<>();
    private static final List<Block> TRANSLUCENT = new ArrayList<>();
    private static boolean registered;

    private BloodyVariantRegistry() {
    }

    public static boolean registerAll() {
        if (registered) {
            return true;
        }
        registered = true;

        addExisting(Blocks.STONE, ModBlocks.BLOODY_STONE);
        addExisting(Blocks.DIRT, ModBlocks.BLOODY_DIRT);
        addExisting(Blocks.OAK_PLANKS, ModBlocks.BLOODY_PLANKS);
        addExisting(Blocks.STONE_BRICKS, ModBlocks.BLOODY_STONE_BRICKS);
        addExisting(ModBlocks.INFESTED_STONE, ModBlocks.BLOODY_INFESTED);
        addExisting(ModBlocks.INFESTED_DIRT, ModBlocks.BLOODY_INFESTED_DIRT);
        CLEAN_BY_INFESTED.put(ModBlocks.INFESTED_STONE, ModBlocks.BLOODY_STONE);
        CLEAN_BY_INFESTED.put(ModBlocks.INFESTED_DIRT, ModBlocks.BLOODY_DIRT);
        INFESTED_BY_BLOODY.put(ModBlocks.BLOODY_STONE, ModBlocks.BLOODY_INFESTED);
        INFESTED_BY_BLOODY.put(ModBlocks.BLOODY_DIRT, ModBlocks.BLOODY_INFESTED_DIRT);
        BLOODY_BY_COMBINED.put(ModBlocks.BLOODY_INFESTED, ModBlocks.BLOODY_STONE);
        BLOODY_BY_COMBINED.put(ModBlocks.BLOODY_INFESTED_DIRT, ModBlocks.BLOODY_DIRT);
        addCombined("planks", ModBlocks.INFESTED_PLANKS, wood(1.8F));
        addCombined("stone_bricks", ModBlocks.INFESTED_STONE_BRICKS, stone(1.6F, SoundType.STONE));
        Collections.addAll(ALL,
                ModBlocks.BLOODY_PLANK_STAIRS, ModBlocks.BLOODY_STONE_STAIRS,
                ModBlocks.BLOODY_PLANK_SLAB, ModBlocks.BLOODY_STONE_SLAB,
                ModBlocks.BLOODY_PLANK_FENCE);

        pairPlain("diamond_ore", Blocks.DIAMOND_ORE, ModBlocks.INFESTED_DIAMOND_ORE,
                stone(4.5F, SoundType.STONE), stone(4.5F, SoundType.DEEPSLATE));
        alias(Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DIAMOND_ORE);
        registerPair("cosmic_crystal_ore", ModBlocks.COSMIC_CRYSTAL_ORE, ModBlocks.INFESTED_COSMIC_CRYSTAL_ORE,
                () -> new BloodyBlocks.BloodyCosmicCrystalPlain(stone(3.5F, SoundType.AMETHYST).lightLevel(s -> 4),
                        ModBlocks.COSMIC_CRYSTAL_ORE::defaultBlockState),
                () -> new BloodyBlocks.BloodyCosmicCrystalPlain(stone(3.5F, SoundType.AMETHYST).lightLevel(s -> 4),
                        ModBlocks.INFESTED_COSMIC_CRYSTAL_ORE::defaultBlockState));
        pairPlain("redstone_ore", Blocks.REDSTONE_ORE, ModBlocks.INFESTED_REDSTONE_ORE,
                stone(4.5F, SoundType.STONE).lightLevel(s -> 7),
                stone(4.5F, SoundType.DEEPSLATE).lightLevel(s -> 7));
        alias(Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.REDSTONE_ORE);
        pairPlain("deepslate", Blocks.DEEPSLATE, ModBlocks.INFESTED_DEEPSLATE,
                stone(2.2F, SoundType.DEEPSLATE), stone(2.2F, SoundType.DEEPSLATE));
        pairPlain("sand", Blocks.SAND, ModBlocks.INFESTED_SAND,
                ground(0.5F, SoundType.SAND), ground(0.5F, SoundType.SAND));
        pairPlain("gravel", Blocks.GRAVEL, ModBlocks.INFESTED_GRAVEL,
                ground(0.6F, SoundType.GRAVEL), ground(0.6F, SoundType.GRAVEL));
        pairPlain("clay", Blocks.CLAY, ModBlocks.INFESTED_CLAY,
                ground(0.7F, SoundType.GRAVEL), ground(0.7F, SoundType.GRAVEL));
        pairPlain("netherrack", Blocks.NETHERRACK, ModBlocks.INFESTED_NETHERRACK,
                ground(0.6F, SoundType.NETHERRACK), ground(0.6F, SoundType.NETHERRACK));
        pairPlain("grass", Blocks.GRASS_BLOCK, ModBlocks.INFESTED_GRASS,
                ground(0.6F, SoundType.GRASS), ground(0.6F, SoundType.GRASS));
        pairPillar("log", Blocks.OAK_LOG, ModBlocks.INFESTED_LOG,
                wood(2.0F), wood(2.0F));
        pairPlain("leaves", Blocks.OAK_LEAVES, ModBlocks.INFESTED_LEAVES,
                ground(0.2F, SoundType.GRASS).noOcclusion(),
                ground(0.2F, SoundType.GRASS).noOcclusion());
        pairPlain("sandstone", Blocks.SANDSTONE, ModBlocks.INFESTED_SANDSTONE,
                stone(0.9F, SoundType.STONE), stone(0.9F, SoundType.STONE));
        pairPlain("terracotta", Blocks.TERRACOTTA, ModBlocks.INFESTED_TERRACOTTA,
                stone(1.25F, SoundType.STONE), stone(1.25F, SoundType.STONE));
        pairPlain("snow", Blocks.SNOW_BLOCK, ModBlocks.INFESTED_SNOW,
                ground(0.3F, SoundType.SNOW), ground(0.3F, SoundType.SNOW));
        pairPlain("ice", Blocks.ICE, ModBlocks.INFESTED_ICE,
                ground(0.6F, SoundType.GLASS).friction(0.98F).noOcclusion(),
                ground(0.6F, SoundType.GLASS).friction(0.98F).noOcclusion());
        pairPlain("wool", Blocks.WHITE_WOOL, ModBlocks.INFESTED_WOOL,
                ground(0.6F, SoundType.WOOL), ground(0.6F, SoundType.WOOL));
        pairPlain("glass", Blocks.GLASS, ModBlocks.INFESTED_GLASS,
                ground(0.3F, SoundType.GLASS).noOcclusion(),
                ground(0.3F, SoundType.GLASS).noOcclusion());
        pairDoor("door", Blocks.OAK_DOOR, ModBlocks.INFESTED_DOOR);
        pairTrapDoor("trapdoor", Blocks.OAK_TRAPDOOR, ModBlocks.INFESTED_TRAPDOOR);
        pairPlain("crafting_table", Blocks.CRAFTING_TABLE, ModBlocks.INFESTED_CRAFTING_TABLE,
                wood(2.5F), wood(2.5F));
        pairPlain("barrel", Blocks.BARREL, ModBlocks.INFESTED_BARREL,
                wood(2.5F), wood(2.5F));
        pairPlain("cartography_table", Blocks.CARTOGRAPHY_TABLE, ModBlocks.INFESTED_CARTOGRAPHY_TABLE,
                wood(2.5F), wood(2.5F));
        pairPlain("smithing_table", Blocks.SMITHING_TABLE, ModBlocks.INFESTED_SMITHING_TABLE,
                wood(2.5F), wood(2.5F));
        pairPlain("fletching_table", Blocks.FLETCHING_TABLE, ModBlocks.INFESTED_FLETCHING_TABLE,
                wood(2.5F), wood(2.5F));
        pairPlain("loom", Blocks.LOOM, ModBlocks.INFESTED_LOOM,
                wood(2.5F), wood(2.5F));
        pairPlain("stonecutter", Blocks.STONECUTTER, ModBlocks.INFESTED_STONECUTTER,
                stone(3.5F, SoundType.STONE).noOcclusion(),
                stone(3.5F, SoundType.STONE).noOcclusion());
        pairPlain("grindstone", Blocks.GRINDSTONE, ModBlocks.INFESTED_GRINDSTONE,
                stone(3.5F, SoundType.STONE).noOcclusion(),
                stone(3.5F, SoundType.STONE).noOcclusion());

        markCutout("leaves", "glass", "door", "trapdoor");
        markTranslucent("ice");
        return true;
    }

    public static Block forSource(Block source) {
        return BY_SOURCE.get(source);
    }

    public static Block cleanForInfested(Block infested) {
        return CLEAN_BY_INFESTED.get(infested);
    }

    public static Block infestedForBloody(Block bloody) {
        return INFESTED_BY_BLOODY.get(bloody);
    }

    public static Block cleanForBloodyInfested(Block combined) {
        return BLOODY_BY_COMBINED.get(combined);
    }

    public static Block[] allBloodyBlocks() {
        return ALL.toArray(Block[]::new);
    }

    public static Block[] cutoutBlocks() {
        return CUTOUT.toArray(Block[]::new);
    }

    public static Block[] translucentBlocks() {
        return TRANSLUCENT.toArray(Block[]::new);
    }

    private static void pairPlain(String suffix, Block clean, Block infested,
            BlockBehaviour.Properties cleanProperties, BlockBehaviour.Properties infestedProperties) {
        registerPair(suffix, clean, infested,
                () -> new BloodyBlocks.Plain(cleanProperties, clean::defaultBlockState),
                () -> new BloodyBlocks.Plain(infestedProperties, infested::defaultBlockState));
    }

    private static void pairPillar(String suffix, Block clean, Block infested,
            BlockBehaviour.Properties cleanProperties, BlockBehaviour.Properties infestedProperties) {
        registerPair(suffix, clean, infested,
                () -> new BloodyBlocks.Pillar(cleanProperties, clean::defaultBlockState),
                () -> new BloodyBlocks.Pillar(infestedProperties, infested::defaultBlockState));
    }

    private static void pairDoor(String suffix, Block clean, Block infested) {
        registerPair(suffix, clean, infested,
                () -> new BloodyBlocks.Door(wood(2.2F).noOcclusion().pushReaction(PushReaction.DESTROY),
                        clean::defaultBlockState),
                () -> new BloodyBlocks.Door(wood(2.2F).noOcclusion().pushReaction(PushReaction.DESTROY),
                        infested::defaultBlockState));
    }

    private static void pairTrapDoor(String suffix, Block clean, Block infested) {
        registerPair(suffix, clean, infested,
                () -> new BloodyBlocks.TrapDoor(wood(2.2F).noOcclusion(), clean::defaultBlockState),
                () -> new BloodyBlocks.TrapDoor(wood(2.2F).noOcclusion(), infested::defaultBlockState));
    }

    private static void registerPair(String suffix, Block clean, Block infested,
            Supplier<Block> cleanFactory, Supplier<Block> infestedFactory) {
        Block bloody = ModBlocks.registerBlock("bloody_" + suffix, cleanFactory.get());
        Block combined = ModBlocks.registerBlock("bloody_infested_" + suffix, infestedFactory.get());
        BY_SOURCE.put(clean, bloody);
        BY_SOURCE.put(infested, combined);
        CLEAN_BY_INFESTED.put(infested, bloody);
        INFESTED_BY_BLOODY.put(bloody, combined);
        BLOODY_BY_COMBINED.put(combined, bloody);
        ALL.add(bloody);
        ALL.add(combined);
    }

    private static void addCombined(String suffix, Block infested, BlockBehaviour.Properties properties) {
        Block combined = ModBlocks.registerBlock("bloody_infested_" + suffix,
                new BloodyBlocks.Plain(properties, infested::defaultBlockState));
        BY_SOURCE.put(infested, combined);
        Block bloody = BY_SOURCE.get(suffix.equals("planks") ? Blocks.OAK_PLANKS : Blocks.STONE_BRICKS);
        CLEAN_BY_INFESTED.put(infested, bloody);
        INFESTED_BY_BLOODY.put(bloody, combined);
        BLOODY_BY_COMBINED.put(combined, bloody);
        ALL.add(combined);
    }

    private static void addExisting(Block source, Block bloody) {
        BY_SOURCE.put(source, bloody);
        ALL.add(bloody);
    }

    private static void alias(Block source, Block canonicalSource) {
        BY_SOURCE.put(source, BY_SOURCE.get(canonicalSource));
    }

    private static void markCutout(String... suffixes) {
        for (String suffix : suffixes) {
            addRenderPair(CUTOUT, suffix);
        }
    }

    private static void markTranslucent(String... suffixes) {
        for (String suffix : suffixes) {
            addRenderPair(TRANSLUCENT, suffix);
        }
    }

    private static void addRenderPair(List<Block> target, String suffix) {
        Block clean = BuiltInLookup.block("bloody_" + suffix);
        Block combined = BuiltInLookup.block("bloody_infested_" + suffix);
        if (clean != null) target.add(clean);
        if (combined != null) target.add(combined);
    }

    private static BlockBehaviour.Properties stone(float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(strength).sound(sound);
    }

    private static BlockBehaviour.Properties ground(float strength, SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(strength).sound(sound);
    }

    private static BlockBehaviour.Properties wood(float strength) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(strength).sound(SoundType.WOOD);
    }

    private static final class BuiltInLookup {
        private static Block block(String path) {
            return net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("alien-invasion", path));
        }
    }
}
