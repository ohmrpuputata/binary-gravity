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

        // TWO-ROOM SHELTER: main hall + storage annex, properly lived-in.
        StructureUtil.fillBox(level, c.offset(-7, -1, -4), c.offset(7, 4, 4), wall, false);
        StructureUtil.fillBox(level, c.offset(-6, 0, -3), c.offset(2, 3, 3), air, false);   // main hall
        StructureUtil.fillBox(level, c.offset(4, 0, -3), c.offset(6, 3, 3), air, false);    // storage annex
        StructureUtil.set(level, c.offset(3, 1, 0), air);                                   // doorway
        StructureUtil.set(level, c.offset(3, 2, 0), air);

        // Lighting.
        StructureUtil.set(level, c.offset(-5, 3, -2), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(0, 3, 2), ModBlocks.WARNING_LAMP.defaultBlockState());
        StructureUtil.set(level, c.offset(5, 3, 0), ModBlocks.WARNING_LAMP.defaultBlockState());

        // Living corner: two bunks, furnace + crafting, a little carrot patch.
        StructureUtil.set(level, c.offset(-6, 0, -3), Blocks.RED_BED.defaultBlockState());
        StructureUtil.set(level, c.offset(-6, 0, 3), Blocks.WHITE_BED.defaultBlockState());
        StructureUtil.set(level, c.offset(-4, 0, 3), Blocks.CRAFTING_TABLE.defaultBlockState());
        StructureUtil.set(level, c.offset(-3, 0, 3), Blocks.FURNACE.defaultBlockState());
        StructureUtil.set(level, c.offset(-2, 0, 3), Blocks.SMOKER.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, -1, 2), Blocks.FARMLAND.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, 0, 2), Blocks.CARROTS.defaultBlockState());
        StructureUtil.set(level, c.offset(-1, -1, 3), Blocks.WATER.defaultBlockState());

        // Storage annex: barrels, crates and the good chest.
        StructureUtil.set(level, c.offset(6, 0, -3), Blocks.BARREL.defaultBlockState());
        StructureUtil.set(level, c.offset(6, 1, -3), Blocks.BARREL.defaultBlockState());
        StructureUtil.set(level, c.offset(6, 0, 3), ModBlocks.BROKEN_LAB_CRATE.defaultBlockState());
        StructureUtil.set(level, c.offset(5, 0, 3), ModBlocks.TOXIC_BARREL.defaultBlockState());
        StructureUtil.placeLootChest(level, c.offset(6, 0, 0), rng, ModFeatures.ABANDONED_LAB_LOOT);
        StructureUtil.placeLootChest(level, c.offset(4, 0, -3), rng, ModFeatures.CAVE_DUNGEON_LOOT);

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

        // THE TRADER: each bunker shelters a DIFFERENT survivor - a medic, a
        // farmer or an engineer - with their own face, name and barter list.
        Villager trader = EntityType.VILLAGER.create(level.getLevel());
        if (trader != null) {
            trader.moveTo(c.getX() + 0.5D, c.getY(), c.getZ() + 0.5D, rng.nextFloat() * 360.0F, 0.0F);
            int profile = rng.nextInt(3);
            VillagerProfession prof = profile == 0 ? VillagerProfession.CLERIC
                    : profile == 1 ? VillagerProfession.FARMER : VillagerProfession.TOOLSMITH;
            String name = profile == 0 ? "§aВыживший медик"
                    : profile == 1 ? "§aВыживший фермер" : "§aВыживший инженер";
            trader.setVillagerData(trader.getVillagerData().setProfession(prof).setLevel(5));
            trader.setCustomName(net.minecraft.network.chat.Component.literal(name));
            trader.setCustomNameVisible(true);
            trader.setPersistenceRequired();
            trader.addTag("BunkerTrader");
            if (profile == 0) { // МЕДИК: лекарства за кристаллы и реагенты
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL, 2),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.RAD_PILLS, 2), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 3),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.WEAK_ANTIDOTE, 2), 10, 8, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.INFESTED_FLESH, 6),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.BIO_SERUM, 1), 8, 8, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 2),
                        new ItemStack(Items.GOLDEN_APPLE, 1), 6, 10, 0.05F));
            } else if (profile == 1) { // ФЕРМЕР: чистая еда и вода
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 2),
                        new ItemStack(Items.BREAD, 8), 16, 2, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 6),
                        new ItemStack(Items.GOLDEN_CARROT, 4), 12, 4, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_INGOT, 3),
                        new ItemStack(Items.MILK_BUCKET), 8, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.ALIEN_SKIN, 3),
                        new ItemStack(Items.COOKED_BEEF, 5), 14, 3, 0.05F));
            } else { // ИНЖЕНЕР: компоненты и снаряжение
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PLATINUM_INGOT, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.ALIEN_BATTERY, 1), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_INGOT, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.PLASMA_CELL, 3), 10, 6, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.COSMIC_SHARD, 5),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.GEIGER_COUNTER, 1), 4, 12, 0.05F));
                trader.getOffers().add(new MerchantOffer(
                        new ItemCost(com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL, 4),
                        new ItemStack(com.example.alieninvasion.registry.ItemRegistry.RAD_PILLS, 1), 8, 6, 0.05F));
            }
            level.addFreshEntity(trader);
        }
        return true;
    }
}
