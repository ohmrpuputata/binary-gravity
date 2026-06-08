package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Blink Core: an escape/mobility tool. Right-click to teleport ~9 blocks in the
 * direction you're looking (stopping short of walls), gaining a brief Resistance
 * and Speed burst. Reusable on a short cooldown - real relief when the swarm
 * corners you, and great for repositioning in co-op fights.
 */
public class BlinkCoreItem extends Item {
    public BlinkCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!level.isClientSide) {
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            Vec3 end = start.add(look.scale(9.0D));
            HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            Vec3 dest = hit.getLocation();
            if (hit.getType() != HitResult.Type.MISS) {
                dest = dest.subtract(look.scale(1.2D)); // back off the surface
            }
            double feetY = dest.y - player.getEyeHeight();

            ServerLevel sl = (ServerLevel) level;
            sl.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.3, 0.6, 0.3, 0.4);
            player.teleportTo(dest.x, feetY, dest.z);
            player.fallDistance = 0.0F;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, true));
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.3, 0.6, 0.3, 0.4);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.3F);
        }

        player.getCooldowns().addCooldown(this, 60); // 3s
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
