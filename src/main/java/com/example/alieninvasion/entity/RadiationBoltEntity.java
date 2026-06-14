package com.example.alieninvasion.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

public class RadiationBoltEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<Boolean> BIG =
            SynchedEntityData.defineId(RadiationBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private static final EntityDataAccessor<Boolean> GREEN_RAY =
            SynchedEntityData.defineId(RadiationBoltEntity.class, EntityDataSerializers.BOOLEAN);

    private static final DustParticleOptions DUST_BIG   =
            new DustParticleOptions(new Vector3f(1.0F, 0.80F, 0.0F), 2.0F);
    private static final DustParticleOptions DUST_SMALL =
            new DustParticleOptions(new Vector3f(1.0F, 0.80F, 0.0F), 0.9F);

    private static final DustParticleOptions DUST_GREEN_BIG   =
            new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.0F), 2.0F);
    private static final DustParticleOptions DUST_GREEN_SMALL =
            new DustParticleOptions(new Vector3f(0.0F, 1.0F, 0.0F), 0.9F);

    public RadiationBoltEntity(EntityType<? extends RadiationBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public RadiationBoltEntity(Level level, LivingEntity shooter, boolean big) {
        super(com.example.alieninvasion.registry.EntityRegistry.RADIATION_BOLT, shooter, level);
        this.setNoGravity(true);
        this.entityData.set(BIG, big);
        this.entityData.set(GREEN_RAY, false);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BIG, true);
        builder.define(GREEN_RAY, false);
    }

    public boolean isBig() {
        return this.entityData.get(BIG);
    }

    public boolean isGreenRay() {
        return this.entityData.get(GREEN_RAY);
    }

    public void setGreenRay(boolean greenRay) {
        this.entityData.set(GREEN_RAY, greenRay);
    }

    @Override
    protected Item getDefaultItem() {
        return isGreenRay() ? com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_INGOT : com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            DustParticleOptions dust = isGreenRay()
                    ? (isBig() ? DUST_GREEN_BIG : DUST_GREEN_SMALL)
                    : (isBig() ? DUST_BIG : DUST_SMALL);
            int count = isBig() ? 5 : 2;
            sl.sendParticles(dust, this.getX(), this.getY(), this.getZ(), count, 0.04, 0.04, 0.04, 0);
        }
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity victim) {
            LivingEntity shooter = (LivingEntity) this.getOwner();
            float damage = isBig() ? 20.0F : 4.0F;
            if (isGreenRay()) {
                damage = isBig() ? 30.0F : 7.0F;
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
            }
            victim.hurt(this.damageSources().thrown(this, shooter), damage);
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHit(HitResult result) {
        if (isGreenRay()) {
            if (result.getType() == HitResult.Type.ENTITY) {
                super.onHit(result);
                this.discard();
            }
        } else {
            super.onHit(result);
            if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
                DustParticleOptions dust = isBig() ? DUST_BIG : DUST_SMALL;
                int burst = isBig() ? 25 : 10;
                sl.sendParticles(dust, this.getX(), this.getY(), this.getZ(), burst, 0.25, 0.25, 0.25, 0.02);
                sl.playSound(null, this.blockPosition(),
                        SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                        isBig() ? 0.8F : 0.4F, isBig() ? 1.6F : 2.0F);
                this.discard();
            }
        }
    }
}
