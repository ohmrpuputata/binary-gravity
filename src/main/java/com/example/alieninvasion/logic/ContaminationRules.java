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

    public static boolean isProtectedBlock(BlockState state) {
        if (isOreOrValuable(state)) return true;
        return state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
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
                || state.is(ModBlocks.COSMIC_ORE) || state.is(ModBlocks.URANIUM_ORE)
                || state.is(ModBlocks.DEEPSLATE_URANIUM_ORE) || state.is(ModBlocks.XENOCRYSTAL_ORE)
                || state.is(ModBlocks.BIO_VEIN_ORE) || state.is(ModBlocks.PLASMA_ORE)
                || state.is(ModBlocks.IRIDIUM_ORE) || state.is(ModBlocks.DARK_MATTER_ORE)
                || state.is(Blocks.COAL_BLOCK) || state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.REDSTONE_BLOCK) || state.is(Blocks.LAPIS_BLOCK)
                || state.is(Blocks.EMERALD_BLOCK) || state.is(Blocks.DIAMOND_BLOCK)
                || state.is(Blocks.NETHERITE_BLOCK) || state.is(ModBlocks.COSMIC_BLOCK);
    }

    public static boolean canContaminate(LevelAccessor level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) return false;
        if (level.getBlockEntity(pos) != null) return false;
        if (isProtectedBlock(state)) return false;
        return contaminatedStateFor(state) != null;
    }

    public static BlockState contaminatedStateFor(BlockState state) {
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.MUD)) {
            return ModBlocks.INFESTED_DIRT.defaultBlockState();
        }
        if (state.is(BlockTags.SAND)) return ModBlocks.INFESTED_SAND.defaultBlockState();
        if (state.is(Blocks.GRAVEL)) return ModBlocks.INFESTED_GRAVEL.defaultBlockState();
        if (state.is(Blocks.CLAY)) return ModBlocks.INFESTED_CLAY.defaultBlockState();
        if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.ANDESITE)
                || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CALCITE)) {
            return ModBlocks.INFESTED_STONE.defaultBlockState();
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
        if (day >= 4) {
            if (state.is(BlockTags.GOLD_ORES)) return ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState();
            if (state.is(BlockTags.DIAMOND_ORES)) return ModBlocks.INFESTED_DIAMOND_ORE.defaultBlockState();
            if (state.is(BlockTags.LAPIS_ORES)) return ModBlocks.INFESTED_LAPIS_ORE.defaultBlockState();
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
        if (state.is(ModBlocks.INFESTED_LOG)) return Blocks.OAK_LOG.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_PLANKS)) return Blocks.OAK_PLANKS.defaultBlockState();
        if (state.is(ModBlocks.INFESTED_LEAVES) || state.is(ModBlocks.DEAD_INFESTED_CROP)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.is(ModBlocks.TOXIC_WATER)) return Blocks.WATER.defaultBlockState();
        return null;
    }

    public static boolean isContaminated(BlockState state) {
        return cleanStateFor(state) != null || state.is(ModBlocks.ALIEN_HIVE) || state.is(ModBlocks.ALIEN_STASH);
    }
}
