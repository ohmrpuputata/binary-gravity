package com.example.alieninvasion.registry;

import com.example.alieninvasion.AlienInvasionMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModParticles {
    public static final SimpleParticleType ACID_SMOKE = FabricParticleTypes.simple();

    private ModParticles() {
    }

    public static void registerParticles() {
        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "acid_smoke"),
                ACID_SMOKE);
    }
}
