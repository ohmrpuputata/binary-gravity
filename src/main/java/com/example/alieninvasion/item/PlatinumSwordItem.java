package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class PlatinumSwordItem extends SwordItem {
    public PlatinumSwordItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        player.getCooldowns().addCooldown(this, 120); // 6 seconds cooldown

        // Play sweep sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2F, 1.0F);

        if (!level.isClientSide && level instanceof ServerLevel sl) {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 playerPos = player.position();
            Vec3 swipePos = player.getEyePosition(1.0F).add(look.scale(1.2));

            // Spawn sweep visual particles
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    swipePos.x, swipePos.y, swipePos.z,
                    1, 0.1, 0.1, 0.1, 0.0);

            // Find entities in front of the player
            AABB box = player.getBoundingBox().inflate(4.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());

            int hits = 0;
            for (LivingEntity target : targets) {
                Vec3 toTarget = new Vec3(target.getX() - playerPos.x, target.getY() - playerPos.y, target.getZ() - playerPos.z).normalize();
                double dot = look.dot(toTarget);

                // Cone check (~120 degrees) and distance check (within 4.5 blocks)
                if (dot > 0.5 && player.distanceTo(target) <= 4.5) {
                    // Deal damage
                    target.hurt(level.damageSources().playerAttack(player), 8.0F);

                    // Drain max health (reduce by 2.0 per hit)
                    target.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_DRAIN), 300, 0));

                    hits++;
                }
            }

            // Grant health boost to the player
            if (hits > 0) {
                int currentAmp = -1;
                if (player.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_BOOST))) {
                    var effect = player.getEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_BOOST));
                    if (effect != null) {
                        currentAmp = effect.getAmplifier();
                    }
                }
                int newAmp = Math.min(9, currentAmp + hits); // Cap at level 10 (+20 Max Health)
                player.addEffect(new MobEffectInstance(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PLATINUM_HEALTH_BOOST), 300, newAmp, false, true));

                // Heal the player for the stolen health amount
                player.heal(hits * 2.0F);

                // Play level up sound for healing feedback
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
            }
        }

        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
