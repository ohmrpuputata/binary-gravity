package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class ContaminationRules {
    private ContaminationRules() {
    }

    private static final java.util.Set<net.minecraft.world.level.block.Block> CONCRETE = java.util.Set.of(
            Blocks.WHITE_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.MAGENTA_CONCRETE, Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.PURPLE_CONCRETE, Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE, Blocks.GREEN_CONCRETE, Blocks.RED_CONCRETE, Blocks.BLACK_CONCRETE);

    private static final java.util.Set<net.minecraft.world.level.block.Block> CONCRETE_POWDER = java.util.Set.of(
            Blocks.WHITE_CONCRETE_POWDER, Blocks.ORANGE_CONCRETE_POWDER, Blocks.MAGENTA_CONCRETE_POWDER,
            Blocks.LIGHT_BLUE_CONCRETE_POWDER, Blocks.YELLOW_CONCRETE_POWDER, Blocks.LIME_CONCRETE_POWDER,
            Blocks.PINK_CONCRETE_POWDER, Blocks.GRAY_CONCRETE_POWDER, Blocks.LIGHT_GRAY_CONCRETE_POWDER,
            Blocks.CYAN_CONCRETE_POWDER, Blocks.PURPLE_CONCRETE_POWDER, Blocks.BLUE_CONCRETE_POWDER,
            Blocks.BROWN_CONCRETE_POWDER, Blocks.GREEN_CONCRETE_POWDER, Blocks.RED_CONCRETE_POWDER,
            Blocks.BLACK_CONCRETE_POWDER);

    /** Small surface vegetation that withers when the ground under it rots. */
    public static boolean isSurfaceVegetation(BlockState state) {
        return state.is(BlockTags.FLOWERS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(BlockTags.SAPLINGS);
    }

    public static boolean isProtectedBlock(BlockState state) {
        if (isOreOrValuable(state)) return true;
        // NOTE: the crafting table is NOT protected anymore - it rots into the
        // infested crafting table, which still works as a crafting table.
        return state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.ENCHANTING_TABLE) || state.is(Blocks.BREWING_STAND)
                || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.GRINDSTONE) || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.CARTOGRAPHY_TABLE) || state.is(Blocks.FLETCHING_TABLE)
                || state.is(Blocks.LOOM) || state.is(Blocks.COMPOSTER) || state.is(Blocks.BEACON)
                || state.is(Blocks.HOPPER) || state.is(Blocks.DISPENSER) || state.is(Blocks.DROPPER)
                || state.is(Blocks.CRAFTER);
    }

    public static boolean isOreOrValuable(BlockState state) {
        return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.DIAMOND_ORES)
                || state.is(Blocks.COAL_BLOCK) || state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.REDSTONE_BLOCK) || state.is(Blocks.LAPIS_BLOCK)
                || state.is(Blocks.EMERALD_BLOCK) || state.is(Blocks.DIAMOND_BLOCK)
                || state.is(Blocks.NETHERITE_BLOCK);
    }

    public static boolean canContaminate(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (isProtectedBlock(state)) return false;
        // Purifier-claimed chunks are reclaimed territory: no spread of any kind.
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && ChunkContaminationData.get(serverLevel).isProtectedChunk(new net.minecraft.world.level.ChunkPos(pos))) {
            return false;
        }
        return contaminatedStateFor(state) != null;
    }

    public static BlockState contaminatedStateFor(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MYCELIUM) || state.is(Blocks.MOSS_BLOCK)) {
            return ModBlocks.INFESTED_GRASS.defaultBlockState();
        }
        if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MUD)) {
            return ModBlocks.INFESTED_DIRT.defaultBlockState();
        }
        if (state.is(Blocks.SANDSTONE) || state.is(Blocks.SMOOTH_SANDSTONE) || state.is(Blocks.CUT_SANDSTONE)
                || state.is(Blocks.CHISELED_SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
                || state.is(Blocks.SMOOTH_RED_SANDSTONE) || state.is(Blocks.CUT_RED_SANDSTONE)) {
            return ModBlocks.INFESTED_SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.TERRACOTTA) || state.is(Blocks.WHITE_TERRACOTTA) || state.is(Blocks.ORANGE_TERRACOTTA)
                || state.is(Blocks.YELLOW_TERRACOTTA) || state.is(Blocks.BROWN_TERRACOTTA)
                || state.is(Blocks.RED_TERRACOTTA) || state.is(Blocks.LIGHT_GRAY_TERRACOTTA)) {
            return ModBlocks.INFESTED_TERRACOTTA.defaultBlockState();
        }
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return ModBlocks.INFESTED_SNOW.defaultBlockState();
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.FROSTED_ICE)) {
            return ModBlocks.INFESTED_ICE.defaultBlockState();
        }
        if (state.is(BlockTags.SAND)) return ModBlocks.INFESTED_SAND.defaultBlockState();
        if (state.is(Blocks.GRAVEL)) return ModBlocks.INFESTED_GRAVEL.defaultBlockState();
        if (state.is(Blocks.CLAY)) return ModBlocks.INFESTED_CLAY.defaultBlockState();
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE) || state.is(Blocks.SMOOTH_STONE)
                || state.is(Blocks.POLISHED_ANDESITE) || state.is(Blocks.POLISHED_DIORITE)
                || state.is(Blocks.POLISHED_GRANITE) || state.is(Blocks.DRIPSTONE_BLOCK)) {
            return ModBlocks.INFESTED_STONE.defaultBlockState();
        }
        if (CONCRETE.contains(state.getBlock())) return ModBlocks.INFESTED_TERRACOTTA.defaultBlockState();
        if (CONCRETE_POWDER.contains(state.getBlock())) return ModBlocks.INFESTED_SAND.defaultBlockState();
        if (state.is(Blocks.RED_MUSHROOM_BLOCK) || state.is(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.is(Blocks.CACTUS)) {
            return ModBlocks.ALIEN_FLESH.defaultBlockState();
        }
        if (state.is(Blocks.MUSHROOM_STEM)) return ModBlocks.INFESTED_LOG.defaultBlockState();
        if (state.is(Blocks.SUGAR_CANE) || state.is(Blocks.BAMBOO)) {
            return ModBlocks.DEAD_INFESTED_CROP.defaultBlockState();
        }
        if (state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE)) {
            return ModBlocks.INFESTED_DEEPSLATE.defaultBlockState();
        }
        if (state.is(Blocks.NETHERRACK)) return ModBlocks.INFESTED_NETHERRACK.defaultBlockState();
        if (state.is(BlockTags.OVERWORLD_NATURAL_LOGS) || state.is(BlockTags.LOGS_THAT_BURN)) {
            return ModBlocks.INFESTED_LOG.defaultBlockState();
        }
        if (state.is(BlockTags.PLANKS)) return ModBlocks.INFESTED_PLANKS.defaultBlockState();
        if (state.is(BlockTags.LEAVES)) return ModBlocks.INFESTED_LEAVES.defaultBlockState();
        // Built structures rot wholesale: doors and trapdoors get true infested
        // counterparts (orientation preserved via copyProperties); carved shapes
        // (stairs/slabs/fences) are swallowed into formless infested mass.
        if (state.is(Blocks.CRAFTING_TABLE)) return ModBlocks.INFESTED_CRAFTING_TABLE.defaultBlockState();
        if (state.is(BlockTags.WOODEN_DOORS)) return ModBlocks.INFESTED_DOOR.defaultBlockState();
        if (state.is(BlockTags.WOODEN_TRAPDOORS)) return ModBlocks.INFESTED_TRAPDOOR.defaultBlockState();
        if (state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES)
                || state.is(Blocks.BOOKSHELF)) {
            return ModBlocks.INFESTED_PLANKS.defaultBlockState();
        }
        if (state.is(Blocks.STONE_BRICKS) || state.is(Blocks.MOSSY_STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS) || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.BRICKS) || state.is(Blocks.MOSSY_COBBLESTONE)) {
            return ModBlocks.INFESTED_STONE_BRICKS.defaultBlockState();
        }
        if (state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS) || state.is(BlockTags.WALLS)) {
            return ModBlocks.INFESTED_STONE.defaultBlockState(); // stone shapes (wooden matched above)
        }
        if (state.is(BlockTags.WOOL)) return ModBlocks.INFESTED_WOOL.defaultBlockState();
        // IMPERMEABLE = glass + all 16 stained glasses; panes (incl. stained) by class.
        if (state.is(BlockTags.IMPERMEABLE) || state.is(Blocks.GLASS_PANE)
                || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock) {
            return ModBlocks.INFESTED_GLASS.defaultBlockState();
        }
        if (state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)) {
            return ModBlocks.INFESTED_DIRT.defaultBlockState();
        }
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.CARVED_PUMPKIN) || state.is(Blocks.MELON)
                || state.is(Blocks.HAY_BLOCK)) {
            return ModBlocks.ALIEN_FLESH.defaultBlockState();
        }
        if (state.is(BlockTags.CROPS) || state.is(Blocks.WHEAT) || state.is(Blocks.CARROTS)
                || state.is(Blocks.POTATOES) || state.is(Blocks.BEETROOTS)) {
            return ModBlocks.DEAD_INFESTED_CROP.defaultBlockState();
        }
        return null;
    }

    /**
     * Day-gated ore corruption. As the infestation spreads it transmutes the ores it
     * reaches (mirrors the "world infects from the inside" patch): day 2 turns coal ->
     * platinum, copper -> palladium, iron -> alien flesh; day 4 turns gold into pure
     * radiation and the gem ores into their infected variants. Returns null if the
     * block isn't a convertible ore yet.
     */
    public static BlockState oreConversionFor(BlockState state, int day) {
        if (day >= 2) {
            if (state.is(BlockTags.COAL_ORES)) return ModBlocks.PLATINUM_ORE.defaultBlockState();
            if (state.is(BlockTags.COPPER_ORES)) return ModBlocks.PALLADIUM_ORE.defaultBlockState();
            if (state.is(BlockTags.IRON_ORES)) return ModBlocks.ALIEN_FLESH.defaultBlockState();
        }
        if (day >= 3) {
            if (state.is(BlockTags.LAPIS_ORES)) return ModBlocks.COSMIC_CRYSTAL_ORE.defaultBlockState();
            if (state.is(BlockTags.GOLD_ORES)) return ModBlocks.PURE_RADIATION_CRYSTAL_ORE.defaultBlockState();
        }
        if (day >= 4) {
            if (state.is(BlockTags.DIAMOND_ORES)) return ModBlocks.INFESTED_DIAMOND_ORE.defaultBlockState();
            if (state.is(BlockTags.REDSTONE_ORES)) return ModBlocks.INFESTED_REDSTONE_ORE.defaultBlockState();
        }
        return null;
    }

    public static BlockState cleanStateFor(BlockState state) {
        if (state.is(ModBlocks.ALIEN_RESIDUE) || state.is(ModBlocks.INFESTED_DIRT)
                || state.is(ModBlocks.INFESTED_GRASS)) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (state.is(ModBlocks.INFESTED_STONE)) return Blocks.STONE.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_DEEPSLATE)) return Blocks.DEEPSLATE.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_SAND)) return Blocks.SAND.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_GRAVEL)) return Blocks.GRAVEL.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_CLAY)) return Blocks.CLAY.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_NETHERRACK)) return Blocks.NETHERRACK.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_SANDSTONE)) return Blocks.SANDSTONE.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_TERRACOTTA)) return Blocks.TERRACOTTA.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_SNOW)) return Blocks.SNOW_BLOCK.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_ICE)) return Blocks.ICE.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_LOG)) return Blocks.OAK_LOG.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_PLANKS)) return Blocks.OAK_PLANKS.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_CRAFTING_TABLE)) return Blocks.CRAFTING_TABLE.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_DOOR)) return Blocks.OAK_DOOR.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_TRAPDOOR)) return Blocks.OAK_TRAPDOOR.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_STONE_BRICKS)) return Blocks.STONE_BRICKS.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_WOOL)) return Blocks.WHITE_WOOL.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_GLASS)) return Blocks.GLASS.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_LEAVES) || state.is(ModBlocks.DEAD_INFESTED_CROP)
                || state.is(ModBlocks.ALIEN_TENDRILS)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.is(ModBlocks.TOXIC_WATER) || state.is(ModBlocks.INFECTED_WATER)) {
            return Blocks.WATER.defaultBlockState();
        }
        return null;
    }

    public static boolean isContaminated(BlockState state) {
        return cleanStateFor(state) != null || state.is(ModBlocks.ALIEN_HIVE) || state.is(ModBlocks.ALIEN_STASH);
    }

    /**
     * Copies every shared blockstate property (facing, axis, door half/hinge/open,
     * stairs shape...) from the original block onto its converted counterpart, so
     * doors stay doors and logs keep their orientation through the conversion.
     */
    public static BlockState copyProperties(BlockState from, BlockState to) {
        for (net.minecraft.world.level.block.state.properties.Property<?> property : from.getProperties()) {
            if (to.hasProperty(property)) {
                to = copyProperty(from, to, property);
            }
        }
        return to;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to,
            net.minecraft.world.level.block.state.properties.Property<T> property) {
        return to.setValue(property, from.getValue(property));
    }
}
