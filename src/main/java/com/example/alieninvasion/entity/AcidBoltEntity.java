package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

// Acid glob lobbed by the Acid Spitter: splashes a small corrosive AoE that hurts
// and poisons non-aliens. Area denial, no permanent griefing.
public class AcidBoltEntity extends ThrowableItemProjectile {
    public AcidBoltEntity(EntityType<? extends AcidBoltEntity> type, Level level) {
        super(type, level);
    }

    public AcidBoltEntity(Level level, LivingEntity shooter) {
        super(com.example.alieninvasion.registry.EntityRegistry.ACID_BOLT, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return com.example.alieninvasion.registry.ItemRegistry.PLASMA_BOLT_ITEM;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        LivingEntity shooter = this.getOwner() instanceof LivingEntity le ? le : null;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNEEZE, this.getX(), this.getY(), this.getZ(), 24, 1.2, 0.4, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(), this.getZ(), 16, 1.0, 0.3, 1.0, 0.02);
            AABB area = this.getBoundingBox().inflate(2.5D);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area,
                    e -> e.isAlive() && !AlienUtils.isAlliedTo(null, e) && e != shooter)) {
                e.hurt(this.damageSources().indirectMagic(this, shooter), 3.0F);
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0, false, true));
                if (e instanceof net.minecraft.world.entity.player.Player p) {
                    com.example.alieninvasion.logic.RadiationManager.addDose(p, 3.0F);
                }
            }
            sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_BLOCK_BREAK,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.8F, 0.8F);
        }
        this.discard();
    }
}
