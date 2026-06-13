package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Nibirium Sword — the most powerful melee weapon.
 * <p>
 * Normal attacks: applies Irradiation effect (blocks healing).
 * Alt attack (right-click): radioactive sweep that drains max health,
 * knocks back enemies, and applies poison + slowness + weakness.
 */
public class NibiriumSwordItem extends SwordItem {
    public NibiriumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, 140); // 7 seconds cooldown

        // Play menacing sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.8F, 1.5F);

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 playerPos = player.position();

            // Spawn radioactive sweep particles in a cone
            for (int d = 1; d <= 6; d++) {
                Vec3 waveCenter = player.getEyePosition(1.0F).add(look.scale(d));
                double radius = 1.2 + d * 0.2;

                // Green radioactive particles
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        waveCenter.x, waveCenter.y, waveCenter.z,
                        6, radius * 0.4, radius * 0.4, radius * 0.4, 0.02);
                sl.sendParticles(ParticleTypes.SCULK_SOUL,
                        waveCenter.x, waveCenter.y, waveCenter.z,
                        3, radius * 0.3, radius * 0.3, radius * 0.3, 0.01);
            }

            // Sweep particles
            Vec3 swipePos = player.getEyePosition(1.0F).add(look.scale(1.5));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    swipePos.x, swipePos.y, swipePos.z,
                    2, 0.2, 0.2, 0.2, 0.0);

            // Find entities in front of the player in a wide cone
            AABB box = player.getBoundingBox().inflate(5.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());

            int hits = 0;
            for (LivingEntity target : targets) {
                Vec3 toTarget = new Vec3(
                        target.getX() - playerPos.x,
                        target.getY() - playerPos.y,
                        target.getZ() - playerPos.z).normalize();
                double dot = look.dot(toTarget);

                // Cone check (~120 degrees) and distance check (within 5.5 blocks)
                if (dot > 0.4 && player.distanceTo(target) <= 5.5) {
                    // Deal heavy damage
                    target.hurt(level.damageSources().playerAttack(player), 10.0F);

                    // Drain max health (same effect as Platinum sword)
                    target.addEffect(new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_DRAIN),
                            400, 0)); // 20 seconds

                    // Poison II for 8 seconds
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 160, 1));

                    // Slowness II for 8 seconds
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));

                    // Weakness II for 8 seconds
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));

                    // Irradiation for 10 seconds (blocks healing)
                    target.addEffect(new MobEffectInstance(
                            BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION),
                            200, 0));

                    // Strong knockback away from the player
                    Vec3 knockbackDir = new Vec3(
                            target.getX() - player.getX(), 0.0,
                            target.getZ() - player.getZ());
                    if (knockbackDir.lengthSqr() > 0.001) {
                        knockbackDir = knockbackDir.normalize().scale(1.2);
                        target.setDeltaMovement(target.getDeltaMovement().add(
                                knockbackDir.x, 0.4, knockbackDir.z));
                        target.hurtMarked = true;
                    }

                    hits++;
                }
            }

            // Steal health for the player
            if (hits > 0) {
                // Health boost for player
                int currentAmp = -1;
                var holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_BOOST);
                if (player.hasEffect(holder)) {
                    var effect = player.getEffect(holder);
                    if (effect != null) {
                        currentAmp = effect.getAmplifier();
                    }
                }
                int newAmp = Math.min(9, currentAmp + hits);
                player.addEffect(new MobEffectInstance(holder, 400, newAmp, false, true));

                // Heal the player
                player.heal(hits * 3.0F);

                // Victory sound
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
