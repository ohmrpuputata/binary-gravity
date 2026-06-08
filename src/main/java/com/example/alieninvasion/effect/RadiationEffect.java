package com.example.alieninvasion.effect;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Radiation effect — multi-stage harmful effect from cosmic ore and alien tech.
 *
 * Stage 0 (amplifier 0): Light radiation
 *   - Reduce max health by 2 (1 heart)
 *   - Slow chip damage every 3 seconds
 *   - Faint yellow-green particle aura
 *   - Occasional Geiger-counter click sounds
 *
 * Stage 1+ (amplifier >= 1): Heavy radiation
 *   - Reduce max health by 2 per level
 *   - Faster chip damage (every 1.5s) + higher damage
 *   - Dense glowing particle cloud around the victim
 *   - Nausea effect
 *   - Mining fatigue
 *   - Screen distortion (handled in HUD overlay)
 */
public class RadiationEffect extends MobEffect {
    public RadiationEffect() {
        super(MobEffectCategory.HARMFUL, 0xB8E600); // bright radioactive yellow-green
        // Reduce maximum health by 2.0 (1 heart) per level of effect
        this.addAttributeModifier(Attributes.MAX_HEALTH,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "radiation_max_health"),
            -2.0D, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Level level = entity.level();
        if (!level.isClientSide && level instanceof ServerLevel sl) {
            // --- Chip damage ---
            // Stage 0: 1 dmg every 3s (60 ticks)
            // Stage 1+: (1 + amplifier) dmg every 1.5s (30 ticks)
            int damageInterval = amplifier >= 1 ? 30 : 60;
            if (entity.tickCount % damageInterval == 0) {
                entity.hurt(entity.damageSources().magic(), 1.0F + amplifier);
            }

            // --- Particle effects ---
            double w = entity.getBbWidth() * 0.6;
            double h = entity.getBbHeight();

            // Stage 0: faint radioactive particles every 8 ticks
            if (entity.tickCount % 8 == 0) {
                // Yellow-green "dust" particles around the entity
                sl.sendParticles(ParticleTypes.FALLING_SPORE_BLOSSOM,
                        entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        1 + amplifier, w, h * 0.5, w, 0.01);

                // Glow-like sparks
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        entity.getX(), entity.getY() + h * 0.3, entity.getZ(),
                        1 + amplifier * 2, w * 0.8, h * 0.4, w * 0.8, 0.02);
            }

            // Stage 1+: denser particles + additional effects
            if (amplifier >= 1 && entity.tickCount % 5 == 0) {
                // Dense glowing cloud
                sl.sendParticles(ParticleTypes.END_ROD,
                        entity.getX(), entity.getY() + h * 0.5, entity.getZ(),
                        2 + amplifier, w, h * 0.6, w, 0.03);

                // Dripping radiation "fallout"
                sl.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                        entity.getX(), entity.getY() + h, entity.getZ(),
                        1, w * 0.5, 0.1, w * 0.5, 0.0);
            }

            // --- Geiger counter ticking ---
            // Runs every tick, so the click rate ramps up sharply with the radiation
            // tier. Routed through PLAYERS so the victim's own dosimeter is audible.
            int clickChance = amplifier >= 2 ? 4 : amplifier >= 1 ? 7 : 16;
            if (level.random.nextInt(clickChance) == 0) {
                float pitch = 1.5F + level.random.nextFloat() * 0.9F;
                float volume = 0.22F + amplifier * 0.08F;
                sl.playSound(null, entity.blockPosition(),
                        SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS,
                        volume, pitch);
            }
            // Severe radiation: a periodic warning whine over the clicks.
            if (amplifier >= 2 && entity.tickCount % 45 == 0) {
                sl.playSound(null, entity.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(),
                        SoundSource.PLAYERS, 0.5F, 0.55F);
            }

            // --- Nausea at stage 1+ ---
            if (amplifier >= 1 && entity.tickCount % 60 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0, false, false));
            }

            // --- Mining fatigue at stage 2+ ---
            if (amplifier >= 2 && entity.tickCount % 40 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, amplifier - 1, false, false));
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
