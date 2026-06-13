package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.effect.InfectionEffect;
import com.example.alieninvasion.effect.RadiationEffect;
import com.example.alieninvasion.effect.PsychicPressureEffect;
import com.example.alieninvasion.effect.AntiGravityEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public class ModEffects {
    public static final MobEffect INFECTION = new InfectionEffect();
    public static final MobEffect RADIATION = new RadiationEffect();
    public static final MobEffect PSYCHIC_PRESSURE = new PsychicPressureEffect();
    public static final MobEffect ANTI_GRAVITY = new AntiGravityEffect();
    public static final MobEffect MARKED = new com.example.alieninvasion.effect.MarkedEffect();
    public static final MobEffect STRONG_RADIATION = new com.example.alieninvasion.effect.StrongRadiationEffect();
    public static final MobEffect IRRADIATION = new com.example.alieninvasion.effect.IrradiationEffect();
    public static final MobEffect RADIATION_CLEANSE = new com.example.alieninvasion.effect.RadiationCleanseEffect();
    public static final MobEffect INFECTION_CLEANSE = new com.example.alieninvasion.effect.InfectionCleanseEffect();
    public static final MobEffect PLATINUM_HEALTH_BOOST = new com.example.alieninvasion.effect.PlatinumHealthBoostEffect();
    public static final MobEffect PLATINUM_HEALTH_DRAIN = new com.example.alieninvasion.effect.PlatinumHealthDrainEffect();

    public static void registerEffects() {
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "strong_radiation"), STRONG_RADIATION);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "irradiation"), IRRADIATION);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "radiation_cleanse"), RADIATION_CLEANSE);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infection_cleanse"), INFECTION_CLEANSE);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_health_boost"), PLATINUM_HEALTH_BOOST);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "platinum_health_drain"), PLATINUM_HEALTH_DRAIN);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "infection"), INFECTION);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "radiation"), RADIATION);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "psychic_pressure"), PSYCHIC_PRESSURE);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "anti_gravity"), ANTI_GRAVITY);
        Registry.register(BuiltInRegistries.MOB_EFFECT, ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "marked"), MARKED);
    }
}
