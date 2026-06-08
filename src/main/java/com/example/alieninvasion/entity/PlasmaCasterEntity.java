package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Плазменщик (Пришелец-Артиллерист): Дальнобойный стрелок.
// Держит дистанцию и стреляет прожигающими насквозь плазменными лучами.
public class PlasmaCasterEntity extends Monster implements RangedAttackMob {
    public PlasmaCasterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Real artillery kiting: when a foe closes the gap, back-pedal to re-open
        // firing range instead of standing still and getting meleed.
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(
                this, Player.class, 7.0F, 1.0D, 1.35D));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 30, 50, 18.0F));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 32.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(this, e)));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide) {
            return;
        }
        Vec3 from = this.getEyePosition();
        Vec3 to = target.getEyePosition();
        if (this.level() instanceof ServerLevel sl) {
            int steps = (int) Math.max(1, from.distanceTo(to));
            Vec3 dir = to.subtract(from);
            for (int i = 0; i < steps; i++) {
                Vec3 p = from.add(dir.scale((double) i / steps));
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        if (this.getSensing().hasLineOfSight(target)) {
            target.hurt(this.damageSources().mobAttack(this), 4.0F);
            target.setRemainingFireTicks(60);
        }
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
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
    public boolean fireImmune() {
        return true; // it's made of plasma
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
