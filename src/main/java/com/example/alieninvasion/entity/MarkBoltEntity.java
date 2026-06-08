package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

// Fired by the Alien Chicken: a harmless bolt that "tags" whoever it hits with the
// Marked effect (so the swarm hunts them) - no real damage, just a tracker.
public class MarkBoltEntity extends ThrowableItemProjectile {
    public MarkBoltEntity(EntityType<? extends MarkBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public MarkBoltEntity(Level level, LivingEntity shooter) {
        super(com.example.alieninvasion.registry.EntityRegistry.MARK_BOLT, shooter, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return com.example.alieninvasion.registry.ItemRegistry.PLASMA_BOLT_ITEM;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity victim) {
            LivingEntity shooter = (LivingEntity) this.getOwner();
            if (shooter == null || !AlienUtils.isAlliedTo(shooter, victim)) {
                victim.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.MARKED), 600, 0, false, true));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY(), this.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
            }
            this.discard();
        }
    }
}
