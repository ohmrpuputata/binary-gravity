package com.example.alieninvasion.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * Капля крови: маленький тёмно-красный дроп с гравитацией и физикой — летит по
 * дуге от раны и оседает на поверхности. Заменяет «обломки редстоун-блока»,
 * которые раньше изображали кровь и выглядели ненатурально.
 */
public class BloodDropParticle extends TextureSheetParticle {

    protected BloodDropParticle(ClientLevel level, double x, double y, double z,
            double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.gravity = 0.65F;
        this.friction = 0.84F;
        this.hasPhysics = true;
        this.lifetime = 14 + this.random.nextInt(20);
        this.quadSize *= 0.55F + this.random.nextFloat() * 0.55F;
        // текстура уже красная; варьируем только яркость, чтобы капли не были одинаковыми
        float shade = 0.70F + this.random.nextFloat() * 0.30F;
        this.setColor(shade, shade, shade);
        this.pickSprite(sprites);
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
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double vx, double vy, double vz) {
            return new BloodDropParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
