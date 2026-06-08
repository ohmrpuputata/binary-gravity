package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class PlasmaBoltEntity extends ThrowableItemProjectile {
    public PlasmaBoltEntity(EntityType<? extends PlasmaBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public PlasmaBoltEntity(Level level, LivingEntity shooter) {
        super(com.example.alieninvasion.registry.EntityRegistry.PLASMA_BOLT, shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        // A dedicated glowing plasma round, so the shot reads as a bolt - not a
        // spinning blaster icon flying through the air.
        return com.example.alieninvasion.registry.ItemRegistry.PLASMA_BOLT_ITEM;
    }

    @Override
    protected void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        if (!this.level().isClientSide && entityHitResult.getEntity() instanceof LivingEntity victim) {
            LivingEntity shooter = (LivingEntity) this.getOwner();
            boolean shooterIsAlien = shooter != null && com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, shooter);
            boolean victimIsAlien = com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, victim);
            // Only skip friendly fire BETWEEN aliens. A player's bolt always hits the
            // swarm (with bonus damage) - that was the bug: it used to spare aliens.
            if (victim != shooter && !(shooterIsAlien && victimIsAlien)) {
                victim.hurt(this.damageSources().thrown(this, shooter), victimIsAlien ? 16.0F : 11.0F);
                victim.setRemainingFireTicks(80); // ignites target
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
                sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.8F);
            }
            this.discard();
        }
    }
}
