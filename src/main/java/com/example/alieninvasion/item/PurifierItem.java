package com.example.alieninvasion.item;

import com.example.alieninvasion.logic.ContaminationRules;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PurifierItem extends Item {
    public PurifierItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            Vec3 eyePosition = player.getEyePosition(1.0F);
            Vec3 lookVector = player.getViewVector(1.0F);
            double range = 6.0D;

            BlockPos center = player.blockPosition();
            for (int dx = -5; dx <= 5; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -5; dz <= 5; dz++) {
                        BlockPos targetPos = center.offset(dx, dy, dz);
                        Vec3 targetVec = new Vec3(targetPos.getX() + 0.5D - eyePosition.x, targetPos.getY() + 0.5D - eyePosition.y, targetPos.getZ() + 0.5D - eyePosition.z);
                        double dist = targetVec.length();
                        if (dist <= range) {
                            double dot = targetVec.normalize().dot(lookVector);
                            if (dot > 0.7D) {
                                BlockState state = level.getBlockState(targetPos);
                                BlockState clean = ContaminationRules.cleanStateFor(state);
                                if (clean != null) {
                                    level.setBlockAndUpdate(targetPos, clean);
                                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D, 3, 0.1, 0.1, 0.1, 0.0);
                                }
                            }
                        }
                    }
                }
            }

            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0D))) {
                if (target != player) {
                    Vec3 targetVec = target.position().subtract(eyePosition);
                    if (targetVec.length() <= range && targetVec.normalize().dot(lookVector) > 0.7D) {
                        if (target.hasEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION))) {
                            target.removeEffect(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
                            serverLevel.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.0D, target.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
                        }
                    }
                }
            }

            for (int i = 0; i < 15; i++) {
                double speed = 0.2D + level.random.nextDouble() * 0.3D;
                double rx = lookVector.x * speed + (level.random.nextDouble() - 0.5D) * 0.1D;
                double ry = lookVector.y * speed + (level.random.nextDouble() - 0.5D) * 0.1D;
                double rz = lookVector.z * speed + (level.random.nextDouble() - 0.5D) * 0.1D;
                serverLevel.sendParticles(ParticleTypes.SPLASH, eyePosition.x + lookVector.x * 0.5D, eyePosition.y + lookVector.y * 0.5D, eyePosition.z + lookVector.z * 0.5D, 0, rx, ry, rz, 1.0D);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 1.5F);

            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
