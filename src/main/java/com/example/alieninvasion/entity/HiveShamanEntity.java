package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Шаман Роя (Пришелец-Поддержка): Поддерживающий юнит.
// Лечит и усиливает союзных пришельцев поблизости (Регенерация + Сила), делая орду опаснее.
public class HiveShamanEntity extends Monster {
    public HiveShamanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 10.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 32.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(this, e)));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount % 60 == 0) {
            boolean buffed = false;
            for (Mob ally : this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(12.0D),
                    e -> e != this && AlienUtils.isAlliedTo(this, e))) {
                ally.heal(3.0F);
                ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, false));
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, false, false));
                buffed = true;
            }
            this.heal(2.0F);
            if (buffed && this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.5D, this.getZ(),
                        8, 1.5, 0.8, 1.5, 0.0);
                sl.sendParticles(ParticleTypes.WITCH, this.getX(), this.getY() + 1.0D, this.getZ(),
                        12, 1.0, 0.6, 1.0, 0.05);
                this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.3F);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            AlienUtils.spawnGoreParticles(this, amount);
            if (source.getEntity() instanceof LivingEntity attacker && !AlienUtils.isAlliedTo(this, attacker)) {
                this.setTarget(attacker);
            }
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
