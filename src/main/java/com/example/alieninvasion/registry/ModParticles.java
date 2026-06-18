package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModParticles {
    public static final SimpleParticleType ACID_SMOKE = FabricParticleTypes.simple();
    /** Капля крови (гравитация + физика) — реалистичные брызги вместо обломков редстоуна. */
    public static final SimpleParticleType BLOOD = FabricParticleTypes.simple();
    /** Заражённая/чужая кровь — фиолетовый ихор (пришельцы, заражённые существа). */
    public static final SimpleParticleType BLOOD_PURPLE = FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void registerParticles() {
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "acid_smoke"),
                ACID_SMOKE);
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "blood"),
                BLOOD);
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "blood_purple"),
                BLOOD_PURPLE);
    }
}
