package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

// Thrown gadget: on impact it releases an anti-gravity burst, flinging every
// nearby creature skyward and afflicting them with the Anti-Gravity effect.
public class GravityGrenadeEntity extends ThrowableItemProjectile {
    public GravityGrenadeEntity(EntityType<? extends GravityGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GravityGrenadeEntity(Level level, LivingEntity shooter) {
        super(EntityRegistry.GRAVITY_GRENADE, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemRegistry.GRAVITY_GRENADE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            detonate();
            this.discard();
        }
    }

    private void detonate() {
        ServerLevel sl = (ServerLevel) this.level();
        Entity owner = this.getOwner();
        for (LivingEntity victim : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(5.0D), e -> e != owner)) {
            victim.addEffect(new MobEffectInstance(
                    BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.ANTI_GRAVITY), 120, 0, false, true));
            victim.push(0.0D, 0.85D, 0.0D);
            victim.hurtMarked = true;
        }
        sl.sendParticles(ParticleTypes.GLOW, this.getX(), this.getY(), this.getZ(), 60, 1.0, 1.0, 1.0, 0.3);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 40, 1.0, 1.0, 1.0, 0.2);
        sl.playSound(null, this.blockPosition(), SoundEvents.SHULKER_BULLET_HIT, SoundSource.PLAYERS, 1.5F, 0.8F);
    }
}
