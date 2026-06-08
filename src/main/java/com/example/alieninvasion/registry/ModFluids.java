package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.fluid.ToxicWaterFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;

public class ModFluids {
    public static final FlowingFluid TOXIC_WATER_STILL = Registry.register(
            BuiltInRegistries.FLUID,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "toxic_water"),
            new ToxicWaterFluid.Source());

    public static final FlowingFluid TOXIC_WATER_FLOWING = Registry.register(
            BuiltInRegistries.FLUID,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "flowing_toxic_water"),
            new ToxicWaterFluid.Flowing());

    // Infected water - corrupted look, but harmless and water-like physics.
    public static final FlowingFluid INFECTED_WATER_STILL = Registry.register(
            BuiltInRegistries.FLUID,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infected_water"),
            new com.example.alieninvasion.fluid.InfectedWaterFluid.Source());

    public static final FlowingFluid INFECTED_WATER_FLOWING = Registry.register(
            BuiltInRegistries.FLUID,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "flowing_infected_water"),
            new com.example.alieninvasion.fluid.InfectedWaterFluid.Flowing());

    public static void registerFluids() {
        AlienInvasionMod.LOGGER.info("Registering toxic fluids for " + AlienInvasionMod.MODID);
    }
}
