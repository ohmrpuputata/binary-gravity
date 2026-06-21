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
import net.minecraft.world.entity.player.Player;
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

    private static final EntityDataAccessor<Integer> AMMO_TYPE =
            SynchedEntityData.defineId(RadiationBoltEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> BLASTER_TIER =
            SynchedEntityData.defineId(RadiationBoltEntity.class, EntityDataSerializers.INT);

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
        builder.define(AMMO_TYPE, 0);
        builder.define(BLASTER_TIER, 1);
    }

    public boolean isBig() {
        if (this.entityData == null) {
            return false;
        }
        return this.entityData.get(BIG);
    }

    public boolean isGreenRay() {
        if (this.entityData == null) {
            return false;
        }
        return this.entityData.get(GREEN_RAY);
    }

    public void setGreenRay(boolean greenRay) {
        if (this.entityData != null) {
            this.entityData.set(GREEN_RAY, greenRay);
        }
    }

    public int getAmmoType() {
        if (this.entityData == null) {
            return 0;
        }
        return this.entityData.get(AMMO_TYPE);
    }

    public void setAmmoType(int type) {
        if (this.entityData != null) {
            this.entityData.set(AMMO_TYPE, type);
        }
    }

    public int getBlasterTier() {
        if (this.entityData == null) {
            return 1;
        }
        return this.entityData.get(BLASTER_TIER);
    }

    public void setBlasterTier(int tier) {
        if (this.entityData != null) {
            this.entityData.set(BLASTER_TIER, tier);
        }
    }

    @Override
    protected Item getDefaultItem() {
        if (this.entityData == null) {
            return com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL;
        }
        return isGreenRay() ? com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_INGOT : com.example.alieninvasion.registry.ItemRegistry.RADIATION_CRYSTAL;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            int tier = getBlasterTier();
            int ammo = getAmmoType();
            
            // Base particles (Platinum or Palladium)
            DustParticleOptions baseDust = ammo == 0 
                ? new DustParticleOptions(new org.joml.Vector3f(1.0F, 1.0F, 1.0F), 1.2F) // White
                : new DustParticleOptions(new org.joml.Vector3f(0.5F, 0.5F, 0.5F), 1.2F); // Grey
            
            for (int i = 0; i < 3; i++) {
                this.level().addParticle(baseDust, 
                    this.getRandomX(0.15D), this.getRandomY(), this.getRandomZ(0.15D), 
                    0.0D, 0.0D, 0.0D);
            }
            
            // Tier 3 additional particles (Yellow and Green)
            if (tier == 3) {
                DustParticleOptions yellowDust = new DustParticleOptions(new org.joml.Vector3f(1.0F, 0.9F, 0.0F), 1.0F);
                DustParticleOptions greenDust = new DustParticleOptions(new org.joml.Vector3f(0.0F, 1.0F, 0.0F), 1.0F);
                for (int i = 0; i < 2; i++) {
                    this.level().addParticle(yellowDust, 
                        this.getRandomX(0.15D), this.getRandomY(), this.getRandomZ(0.15D), 
                        0.0D, 0.0D, 0.0D);
                    this.level().addParticle(greenDust, 
                        this.getRandomX(0.15D), this.getRandomY(), this.getRandomZ(0.15D), 
                        0.0D, 0.0D, 0.0D);
                }
            }
        } else {
            // Server side particle trail for fallback or non-blaster entities (like original behavior)
            if (isGreenRay() || getBlasterTier() == 3) {
                if (isGreenRay() && getBlasterTier() != 3 && this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(isBig() ? DUST_GREEN_BIG : DUST_GREEN_SMALL, this.getX(), this.getY(), this.getZ(), isBig() ? 5 : 2, 0.04, 0.04, 0.04, 0);
                }
            } else {
                if (this.level() instanceof ServerLevel sl && this.tickCount % 2 == 0) {
                    int ammo = getAmmoType();
                    DustParticleOptions dust = ammo == 0 
                        ? new DustParticleOptions(new org.joml.Vector3f(1.0F, 1.0F, 1.0F), 1.0F)
                        : new DustParticleOptions(new org.joml.Vector3f(0.5F, 0.5F, 0.5F), 1.0F);
                    sl.sendParticles(dust, this.getX(), this.getY(), this.getZ(), 2, 0.04, 0.04, 0.04, 0);
                }
            }
        }
        if (!this.level().isClientSide && this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide && result.getEntity() instanceof LivingEntity victim) {
            LivingEntity shooter = (LivingEntity) this.getOwner();
            int tier = getBlasterTier();
            int ammo = getAmmoType();
            
            float damage;
            if (isGreenRay() && tier != 3) {
                damage = isBig() ? 30.0F : 7.0F;
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
            } else if (tier == 1) {
                damage = ammo == 0 ? 4.5F : 6.0F;
            } else if (tier == 2 || tier == 3) {
                damage = ammo == 0 ? 6.0F : 9.0F;
            } else {
                damage = isBig() ? 20.0F : 4.0F;
            }
            
            victim.hurt(this.damageSources().thrown(this, shooter), damage);

            // Platinum small knockback (ammo type 0, tier 1 or 2)
            if (ammo == 0 && (tier == 1 || tier == 2)) {
                net.minecraft.world.phys.Vec3 look = this.getDeltaMovement().normalize();
                victim.push(look.x * 0.25D, 0.1D, look.z * 0.25D);
            }

            // Blaster III (tier 3) effects
            if (tier == 3) {
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.GLOWING, 200, 0, false, false));
                victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(
                                com.example.alieninvasion.registry.ModEffects.IRRADIATION),
                        160, 0));
                if (victim instanceof Player playerVictim) {
                    com.example.alieninvasion.logic.RadiationManager.addDose(playerVictim, 15.0F);
                }
            }
        }
        super.onHitEntity(result);
    }

    @Override
    protected void onHit(HitResult result) {
        if (isGreenRay() || getBlasterTier() == 3) {
            if (result.getType() == HitResult.Type.ENTITY) {
                super.onHit(result);
                this.discard();
            }
        } else {
            super.onHit(result);
            if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
                int ammo = getAmmoType();
                DustParticleOptions dust = ammo == 0 
                    ? new DustParticleOptions(new org.joml.Vector3f(1.0F, 1.0F, 1.0F), 1.5F)
                    : new DustParticleOptions(new org.joml.Vector3f(0.5F, 0.5F, 0.5F), 1.5F);
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
