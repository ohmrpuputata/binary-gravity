package com.example.alieninvasion.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Био-Гарпун (Bio-Grappling Hook):
 * Органический инструмент пришельцев. Притягивает игрока к блокам (позволяя быстро карабкаться по пещерам)
 * или притягивает мелких пришельцев/мобов к игроку для быстрого удара мечом.
 */
public class BioGrapplingHookItem extends Item {
    public BioGrapplingHookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        double range = 24.0D;
        Vec3 targetVec = eyePos.add(lookVec.scale(range));

        Entity hitEntity = null;
        double currentDist = range;

        // Ищем сущностей по линии взгляда
        for (Entity entity : level.getEntities(player, player.getBoundingBox().expandTowards(lookVec.scale(range)).inflate(1.0D))) {
            if (entity instanceof LivingEntity && entity.isAlive() && entity != player) {
                var box = entity.getBoundingBox().inflate(0.5D);
                var clip = box.clip(eyePos, targetVec);
                if (clip.isPresent()) {
                    double d = eyePos.distanceTo(clip.get());
                    if (d < currentDist) {
                        hitEntity = entity;
                        currentDist = d;
                    }
                }
            }
        }

        if (hitEntity != null) {
            // Притягиваем моба к игроку
            if (!level.isClientSide) {
                Vec3 pullDir = player.position().subtract(hitEntity.position()).normalize().scale(1.2D);
                hitEntity.setDeltaMovement(pullDir.x, 0.5D, pullDir.z);
                hitEntity.hurtMarked = true;
                
                // Спавним частицы слизи/нити
                ServerLevel sl = (ServerLevel) level;
                for (int i = 0; i < (int)(currentDist * 2); i++) {
                    Vec3 p = eyePos.add(lookVec.scale(i * 0.5D));
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.5F);
            player.getCooldowns().addCooldown(this, 15);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // Если не попали в моба, делаем рейкаст блоков
        BlockHitResult blockHitResult = level.clip(new net.minecraft.world.level.ClipContext(
                eyePos, targetVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));

        if (blockHitResult.getType() == HitResult.Type.BLOCK) {
            Vec3 hitPos = blockHitResult.getLocation();
            double dist = eyePos.distanceTo(hitPos);
            
            if (!level.isClientSide) {
                // Притягиваем игрока к блоку
                Vec3 pushDir = hitPos.subtract(player.position()).normalize().scale(1.6D);
                player.setDeltaMovement(pushDir.x, Math.max(0.5D, pushDir.y * 0.8D), pushDir.z);
                player.hurtMarked = true;

                ServerLevel sl = (ServerLevel) level;
                for (int i = 0; i < (int)(dist * 2); i++) {
                    Vec3 p = eyePos.add(lookVec.scale(i * 0.5D));
                    sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            level.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.2F);
            player.getCooldowns().addCooldown(this, 20);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        return InteractionResultHolder.pass(stack);
    }
}
