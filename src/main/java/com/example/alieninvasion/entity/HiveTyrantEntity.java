package com.example.alieninvasion.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Тиран Роя (Босс пришельцев): Грозный мини-босс вторжения.
// Создан на основе Вардена, преследует игрока, издает звуковые атаки. Имеет 300 HP.
public class HiveTyrantEntity extends Warden implements IAlienUnit {
    public HiveTyrantEntity(EntityType<? extends Warden> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.COMMANDER; }

    public static AttributeSupplier.Builder createAttributes() {
        return Warden.createAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)        // boss tank
                .add(Attributes.ATTACK_DAMAGE, 25.0D)      // still brutal, below a vanilla Warden's 30
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 50.0D);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }

        // The Warden brain only attacks things that disturb it. Force the boss to
        // constantly enrage at and chase the nearest valid player.
        if (this.tickCount % 40 == 0) {
            Player nearest = this.level().getNearestPlayer(this, 50.0D);
            if (nearest != null && !nearest.isCreative() && !nearest.isSpectator() && this.canTargetEntity(nearest)) {
                this.increaseAngerAt(nearest);
                this.setAttackTarget(nearest);
            } else if (this.getTarget() == null || !this.getTarget().isAlive()) {
                // HOSTILE TO ALL: with no player around, the boss culls any non-alien.
                LivingEntity prey = null;
                double best = Double.MAX_VALUE;
                for (net.minecraft.world.entity.Mob mob : this.level().getEntitiesOfClass(
                        net.minecraft.world.entity.Mob.class, this.getBoundingBox().inflate(30.0D),
                        e -> e != this && !AlienUtils.isAlliedTo(this, e) && this.canTargetEntity(e))) {
                    double d = this.distanceToSqr(mob);
                    if (d < best) {
                        best = d;
                        prey = mob;
                    }
                }
                if (prey != null) {
                    this.setAttackTarget(prey);
                }
            }
        }

        boolean hasTarget = this.getTarget() != null && this.getTarget().isAlive();

        // COMMANDER AURA: periodically empower every nearby alien with Strength +
        // Speed so the swarm hits harder around the boss.
        if (hasTarget && this.tickCount % 100 == 0) {
            for (LivingEntity ally : this.level().getEntitiesOfClass(LivingEntity.class,
                    this.getBoundingBox().inflate(16.0D), e -> e != this && AlienUtils.isAlliedTo(this, e))) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 140, 0, false, false));
                ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 140, 0, false, false));
            }
        }

        // SUMMON SWARM: call in a single chicken every ~15s while fighting (toned down).
        if (hasTarget && this.tickCount % 300 == 0) {
            for (int i = 0; i < 1; i++) {
                AlienChickenEntity chick = com.example.alieninvasion.registry.EntityRegistry.ALIEN_CHICKEN
                        .create(this.level());
                if (chick != null) {
                    chick.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 3.0, this.getY(),
                            this.getZ() + (this.random.nextDouble() - 0.5) * 3.0, this.random.nextFloat() * 360F, 0);
                    chick.setTarget(this.getTarget());
                    this.level().addFreshEntity(chick);
                }
            }
        }

        // ENRAGE: below half health the tyrant permanently boosts its own speed and
        // damage (re-applied so it never lapses).
        if (this.getHealth() < this.getMaxHealth() * 0.5F && this.tickCount % 40 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, false, false));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            AlienUtils.spawnGoreParticles(this, amount);
            if (source.getEntity() instanceof LivingEntity attacker && !AlienUtils.isAlliedTo(this, attacker)) {
                this.increaseAngerAt(attacker);
                this.setAttackTarget(attacker);
            }
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
