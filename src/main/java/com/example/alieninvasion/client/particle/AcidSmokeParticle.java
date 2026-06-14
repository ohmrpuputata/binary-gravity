package com.example.alieninvasion.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SmokeParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

public class AcidSmokeParticle extends SmokeParticle {
    private final float startAlpha;

    protected AcidSmokeParticle(
            ClientLevel level,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            SpriteSet sprites) {
        super(level, x, y, z, velocityX, velocityY, velocityZ,
                1.8F + level.random.nextFloat() * 0.9F, sprites);

        float shade = 0.88F + level.random.nextFloat() * 0.18F;
        setColor(0.30F * shade, 0.82F * shade, 0.075F * shade);
        startAlpha = 0.58F + level.random.nextFloat() * 0.18F;
        setAlpha(0.0F);
        lifetime = 34 + level.random.nextInt(30);
        quadSize *= 0.72F + level.random.nextFloat() * 0.30F;
        friction = 0.975F;
        hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }

        float progress = age / (float) lifetime;
        float fadeIn = Mth.clamp(progress / 0.12F, 0.0F, 1.0F);
        float fadeOut = 1.0F - Mth.clamp((progress - 0.58F) / 0.42F, 0.0F, 1.0F);
        setAlpha(startAlpha * fadeIn * fadeOut);

        // The cloud yellows slightly as the droplets react with the surface.
        rCol = Mth.lerp(progress, rCol, 0.43F);
        gCol = Mth.lerp(progress, gCol, 0.72F);
        bCol = Mth.lerp(progress, bCol, 0.055F);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(
                SimpleParticleType type,
                ClientLevel level,
                double x,
                double y,
                double z,
                double velocityX,
                double velocityY,
                double velocityZ) {
            return new AcidSmokeParticle(level, x, y, z, velocityX, velocityY, velocityZ, sprites);
        }
    }
}
