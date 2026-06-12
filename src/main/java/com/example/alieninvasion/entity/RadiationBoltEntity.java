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

    private static final DustParticleOptions DUST_BIG   =
            new DustParticleOptions(new Vector3f(1.0F, 0.80F, 0.0F), 2.0F);
    private static final DustParticleOptions DUST_SMALL =
            new DustParticleOptions(new Vector3f(1.0F, 0.80F, 0.0F), 0.9F);

    public RadiationBoltEntity(EntityType<? extends RadiationBoltEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public RadiationBoltEntity(Level level, LivingEntity shooter, boolean big) {
        super(com.example.alieninvasion.registry.EntityRegistry.RADIATION_BOLT, shooter, level);
        this.setNoGravity(true);
        this.entityData.set(BIG, big);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BIG, true);
    }

    public boolean isBig() {
        return this.entityData.get(BIG);
    }

    @Override
    protected Item getDefaultItem() {
        return com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            DustParticleOptions dust = isBig() ? DUST_BIG : DUST_SMALL;
            int count = isBig() ? 5 : 2;
            sl.sendParticles(dust, this.getX(), this.getY(), this.getZ(), count, 0.04, 0.04, 0.04, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity victim) {
            LivingEntity shooter = (LivingEntity) this.getOwner();
            // Малый болт 4.0: при 6.0 burst-режим бластера (10 болтов / 1.25с) давал
            // ~48 DPS и обесценивал всё остальное оружие мода.
            float damage = isBig() ? 20.0F : 4.0F;
            victim.hurt(this.damageSources().thrown(this, shooter), damage);
        }
    }

    @Override
    protected void onHit(HitResult result) {
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
