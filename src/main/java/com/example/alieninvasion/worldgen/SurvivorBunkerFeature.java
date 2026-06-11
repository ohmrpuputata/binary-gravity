package com.example.alieninvasion.worldgen;

import com.example.alieninvasion.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * SURVIVOR BUNKER: a rare buried shelter — the only friendly place left. A lone
 * trader barters clean food, water and meds for alien materials. Marked on the
 * surface by a lamp post over the hatch; a ladder shaft drops into the shelter.
 * The bunker's chunk is flagged for protection by the trader's presence (see
 * ModEvents), so the corruption never eats your one safe haven.
 */
public class SurvivorBunkerFeature extends Feature<NoneFeatureConfiguration> {
    public SurvivorBunkerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rng = ctx.random();
        BlockPos o = ctx.origin();
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, o.getX(), o.getZ());
        if (surfaceY <= level.getMinBuildHeight() + 16) return false;
        BlockPos hatch = new BlockPos(o.getX(), surfaceY, o.getZ());
        if (!StructureUtil.getBlockState(level, hatch.below()).getFluidState().isEmpty()) return false;

        BlockPos c = hatch.below(9); // bunker floor center
        BlockState wall = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState air = Blocks.CAVE_AIR.defaultBlockState();

        // Shell 11x5x9, hollow interior.
        StructureUtil.fillBox(level, c.offset(-5, -1, -4), c.offset(5, 4, 4), wall, false);
        StructureUtil.fillBox(level, c.offset(-4, 0, -3), c.offset(4, 3, 3), air, false);

        // Lighting + dressing: lamps, crates, a workbench corner.
        StructureUtil.set(level, c.offset(-4, 3, -3), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(4, 3, 3), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(0, 3, 0), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(-4, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        StructureUtil.set(level, c.offset(-3, 0, 3), Blocks.FURNACE.defaultBlockState());
        StructureUtil.set(level, c.offset(4, 0, -3), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, c.offset(3, 0, -3), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.placeLootChest(level, c.offset(4, 0, 0), rng, ModFeatures.ABANDONED_LAB_LOOT);

        // Ladder shaft from the surface hatch down into the bunker.
        for (int y = surfaceY; y > c.getY(); y--) {
            BlockPos p = new BlockPos(hatch.getX(), y, hatch.getZ());
            StructureUtil.set(level, p, Blocks.CAVE_AIR.defaultBlockState());
            StructureUtil.set(level, p.north(), wall);
            StructureUtil.set(level, p.south(), wall);
            StructureUtil.set(level, p.east(), wall);
            StructureUtil.set(level, p.west(), wall);
            StructureUtil.set(level, p.north(), wall);
            StructureUtil.set(level, new BlockPos(hatch.getX(), y, hatch.getZ()),
                    Blocks.LADDER.defaultBlockState());
        }
        // Surface marker: a tall radio mast with a beacon lamp - visible from afar,
        // and the bunker broadcasts on the Radio Transmitter as a survivor signal.
        for (int i = 1; i <= 5; i++) {
            StructureUtil.set(level, hatch.above(i), Blocks.IRON_BARS.defaultBlockState());
        }
        StructureUtil.set(level, hatch.above(6), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, hatch.east().above(), ModBlocks.WARNING_LAMP.defaultBlockState());
        com.example.alieninvasion.logic.StructureLocationsData.get(level.getLevel()).add("bunker", hatch);

        // THE TRADER: clean supplies for alien salvage.
        Villager trader = EntityType.VILLAGER.create(level.getLevel());
        if (trader != null) {
            trader.moveTo(c.getX() + 0.5D, c.getY(), c.getZ() + 0.5D, rng.nextFloat() * 360.0F, 0.0F);
            trader.setVillagerData(trader.getVillagerData()
                    .setProfession(VillagerProfession.CLERIC).setLevel(5));
            trader.setCustomName(net.minecraft.network.chat.Component.literal("§aВыживший-торговец"));
            trader.setCustomNameVisible(true);
            trader.setPersistenceRequired();
            trader.addTag("BunkerTrader");
            trader.getOffers().add(new MerchantOffer(
                    new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 3),
                    new ItemStack(Items.BREAD, 6), 16, 2, 0.05F));
            trader.getOffers().add(new MerchantOffer(
                    new ItemCost(com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 8),
                    new ItemStack(Items.GOLDEN_CARROT, 4), 12, 4, 0.05F));
            trader.getOffers().add(new MerchantOffer(
                    new ItemCost(com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL, 2),
                    new ItemStack(com.example.alieninvasion.registry.ItemRegistry.RAD_PILLS, 2), 10, 6, 0.05F));
            trader.getOffers().add(new MerchantOffer(
                    new ItemCost(com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 4),
                    new ItemStack(com.example.alieninvasion.registry.ItemRegistry.WEAK_ANTIDOTE, 2), 10, 8, 0.05F));
            trader.getOffers().add(new MerchantOffer(
                    new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_INGOT, 5),
                    new ItemStack(Items.MILK_BUCKET), 8, 6, 0.05F));
            level.addFreshEntity(trader);
        }
        return true;
    }
}
