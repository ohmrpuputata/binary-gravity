package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
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

public class PalladiumSwordItem extends SwordItem {
    public PalladiumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, 100); // 5 seconds cooldown

        // Play toxic sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.2F, 0.8F);

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 startPos = player.getEyePosition(1.0F);

            // Travel forward to damage/knockback entities
            for (int d = 1; d <= 7; d++) {
                Vec3 waveCenter = startPos.add(look.scale(d));
                double radius = 1.5 + d * 0.15; // Cone expanding

                // Spawn toxic wave particles
                sl.sendParticles(ParticleTypes.SNEEZE,
                        waveCenter.x, waveCenter.y, waveCenter.z,
                        8, radius * 0.4, radius * 0.4, radius * 0.4, 0.05);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        waveCenter.x, waveCenter.y, waveCenter.z,
                        2, radius * 0.4, radius * 0.4, radius * 0.4, 0.0);

                // Find entities in the wave radius
                AABB box = new AABB(waveCenter.x - radius, waveCenter.y - radius, waveCenter.z - radius,
                                    waveCenter.x + radius, waveCenter.y + radius, waveCenter.z + radius);
                List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive());

                for (LivingEntity target : targets) {
                    // Poison wave magic damage
                    target.hurt(level.damageSources().indirectMagic(player, player), 5.0F);

                    // Heavily poisons: Poison II (amplifier 1) for 6 seconds (120 ticks)
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));

                    // Strong knockback away from the player
                    Vec3 knockbackDir = new Vec3(target.getX() - player.getX(), 0.0, target.getZ() - player.getZ());
                    if (knockbackDir.lengthSqr() > 0.001) {
                        knockbackDir = knockbackDir.normalize().scale(0.85);
                        target.setDeltaMovement(target.getDeltaMovement().add(knockbackDir.x, 0.35, knockbackDir.z));
                        target.hurtMarked = true;
                    }
                }
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
