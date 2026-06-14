package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AstralPrismGunItem extends GravityGunItem {

    public AstralPrismGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getTags().contains("EmpActive")) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 0.5F);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] ЭМП-буря отключила призма-пушку!"), true);
            return InteractionResultHolder.fail(stack);
        }

        boolean creative = player.getAbilities().instabuild;
        int charge = getCharge(stack);

        if (charge <= 0 && !creative) {
            if (!level.isClientSide) {
                level.playSound(null, player.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.7F, 1.3F);
            }
            return InteractionResultHolder.fail(stack);
        }

        // Sneaking triggers the slow charge attack
        if (player.isShiftKeyDown()) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }

        // Normal attack: anti-gravity for 160 ticks (8 seconds) + slight knockback
        if (!level.isClientSide) {
            Vec3 eyePosition = player.getEyePosition(1.0F);
            Vec3 lookVector = player.getViewVector(1.0F);
            double maxRange = 35.0D;
            Vec3 targetVec = eyePosition.add(lookVector.scale(maxRange));

            Entity hitEntity = null;
            for (Entity entity : level.getEntities(player, player.getBoundingBox().expandTowards(lookVector.scale(maxRange)).inflate(1.5D))) {
                if (entity instanceof LivingEntity && entity != player) {
                    var boundingBox = entity.getBoundingBox().inflate(0.5D);
                    var optional = boundingBox.clip(eyePosition, targetVec);
                    if (optional.isPresent()) {
                        double dist = eyePosition.distanceToSqr(optional.get());
                        if (dist < maxRange * maxRange) {
                            hitEntity = entity;
                            maxRange = Math.sqrt(dist);
                        }
                    }
                }
            }

            ServerLevel serverLevel = (ServerLevel) level;
            double steps = maxRange * 2.0D;
            for (int i = 0; i < steps; i++) {
                Vec3 p = eyePosition.add(lookVector.scale(i * 0.5D));
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.GLOW, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0);
            }

            if (hitEntity instanceof LivingEntity target) {
                target.addEffect(new MobEffectInstance(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.ANTI_GRAVITY),
                        160, 0, false, false
                ));
                
                // Slight knockback
                Vec3 dir = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
                if (dir.lengthSqr() > 0.001D) {
                    dir = dir.normalize().scale(0.8D);
                    target.setDeltaMovement(target.getDeltaMovement().add(dir.x, 0.25D, dir.z));
                    target.hurtMarked = true;
                }

                level.playSound(null, target.blockPosition(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.5F, 1.0F);
                serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1.0D, target.getZ(), 20, 0.4, 0.4, 0.4, 0.1);
            }

            level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.7F);

            if (!creative) {
                setCharge(stack, charge - 1);
                player.getCooldowns().addCooldown(this, 10);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        int elapsed = getUseDuration(stack, entity) - count;

        if (elapsed % 10 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.8F, 0.5F + ((float) elapsed / 60.0F) * 1.0F);
        }

        if (level instanceof ServerLevel sl) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double dist = 1.2D + level.random.nextDouble() * 1.5D;
            double px = entity.getX() + Math.cos(angle) * dist;
            double py = entity.getY() + 0.1D + level.random.nextDouble() * 2.0D;
            double pz = entity.getZ() + Math.sin(angle) * dist;
            
            Vec3 vel = entity.getEyePosition().subtract(px, py, pz).normalize().scale(0.2D);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.PORTAL, px, py, pz, 1, vel.x, vel.y, vel.z, 0.1D);
            if (elapsed >= 60) {
                // Fully charged spark indicator
                sl.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getEyeY() + 0.3D, entity.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        int elapsed = getUseDuration(stack, entity) - timeLeft;

        if (elapsed < 60) {
            // Did not charge long enough
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.5F);
            return;
        }

        if (entity instanceof Player player) {
            boolean creative = player.getAbilities().instabuild;

            if (!level.isClientSide) {
                Vec3 start = player.getEyePosition();
                Vec3 look = player.getViewVector(1.0F).normalize();
                Vec3 end = start.add(look.scale(40.0D));

                ServerLevel sl = (ServerLevel) level;

                // Play epic sounds
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0F, 0.8F);
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5F, 1.2F);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5F, 1.5F);

                // Show sonic boom along the ray
                for (int i = 0; i < 40; i += 2) {
                    Vec3 pt = start.add(look.scale(i));
                    sl.sendParticles(ParticleTypes.SONIC_BOOM, pt.x, pt.y, pt.z, 1, 0.0, 0.0, 0.0, 0);
                    sl.sendParticles(ParticleTypes.GLOW, pt.x, pt.y, pt.z, 15, 0.4, 0.4, 0.4, 0.2);
                }

                // Push and damage entities in 40-block long cylinder (5-block radius)
                double length = 40.0D;
                double radius = 5.0D;

                java.util.List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(length));
                for (Entity t : targets) {
                    if (t instanceof LivingEntity target && target != player) {
                        Vec3 targetPos = target.position();
                        Vec3 rel = targetPos.subtract(start);
                        double projection = rel.dot(look);

                        if (projection >= 0.0D && projection <= length) {
                            Vec3 projectedPoint = start.add(look.scale(projection));
                            double dist = targetPos.distanceTo(projectedPoint);

                            if (dist <= radius) {
                                // Knockback very very very far away!
                                Vec3 diff = targetPos.subtract(player.position());
                                Vec3 kb = new Vec3(diff.x, 0.0D, diff.z);
                                if (kb.lengthSqr() > 0.001D) {
                                    kb = kb.normalize().scale(3.5D).add(0.0D, 1.2D, 0.0D);
                                } else {
                                    kb = new Vec3(0.0D, 1.2D, 0.0D);
                                }
                                target.setDeltaMovement(kb);
                                target.hurtMarked = true;
                                target.hurt(level.damageSources().magic(), 15.0F);

                                sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 0.5D, target.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                            }
                        }
                    }
                }

                if (!creative) {
                    setCharge(stack, 0); // Consumes all charges
                    player.getCooldowns().addCooldown(this, 300); // 15 seconds cooldown
                }
            }
        }
    }
}
