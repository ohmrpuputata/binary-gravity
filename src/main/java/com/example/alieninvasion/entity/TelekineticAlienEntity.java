package com.example.alieninvasion.entity;

import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;

// Телекинетик (Пришелец-Экстрасенс): Мобильный и опасный боец.
// Создан на основе Эндермена. Использует телепортацию для обхода игрока с фланга.
public class TelekineticAlienEntity extends EnderMan {
    public TelekineticAlienEntity(EntityType<? extends EnderMan> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        // Goal priorities
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.TacticalTeleportGoal(this)); // FLANK!
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                net.minecraft.world.entity.player.Player.class, 8.0F));

        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 32.0D)); // HIVE MIND
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, true));
        // HOSTILE TO ALL: hunts any non-alien living thing.
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(this, e)));
    }

    @Override
    public boolean isSensitiveToWater() {
        return false; // No fear of water
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN))
            return false; // Double check

        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            AlienUtils.spawnGoreParticles(this, amount);
            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && !AlienUtils.isAlliedTo(this, attacker)) {
                this.setTarget(attacker);
                // BLINK: phase out of melee range when struck (enderman gene).
                if (this.random.nextFloat() < 0.5f) {
                    this.teleport();
                }
            }
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getTarget() != null && !this.level().isClientSide) {
            net.minecraft.world.entity.LivingEntity target = this.getTarget();

            // Gentle constant telekinetic pull toward the alien.
            if (this.tickCount % 10 == 0 && this.distanceToSqr(target) < 256.0D) { // 16 blocks
                net.minecraft.world.phys.Vec3 pull = this.position().subtract(target.position()).normalize().scale(0.2);
                target.push(pull.x, pull.y + 0.1, pull.z);
                target.hurtMarked = true;
            }

            // TELEKINETIC SLAM: every ~10s rip the target into the air, then let
            // gravity (and fall damage) finish the job.
            if (this.tickCount % 200 == 0 && this.distanceToSqr(target) < 144.0D
                    && this.getSensing().hasLineOfSight(target)) {
                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.LEVITATION, 30, 2, false, false));
                this.level().playSound(null, this.blockPosition(),
                        net.minecraft.sounds.SoundEvents.ENDERMAN_STARE, this.getSoundSource(), 1.0F, 0.7F);
            }
        }
    }
}
