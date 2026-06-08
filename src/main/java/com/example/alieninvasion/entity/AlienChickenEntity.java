package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
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

// Курица-Пришелец: Ослабленный надоедливый дальний боец.
// Стреляет яйцами, которые наносят почти нулевой урон. Служит отвлекающим маневром.
public class AlienChickenEntity extends Monster implements RangedAttackMob {
    public AlienChickenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)        // very low HP - dies fast
                .add(Attributes.MOVEMENT_SPEED, 0.35D)   // skittish
                .add(Attributes.ATTACK_DAMAGE, 0.0D)     // melee does nothing - it's a ranged pest
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Ranged egg pelting; keeps its distance instead of rushing in.
        this.goalSelector.addGoal(1, new RangedAttackGoal(this, 1.0D, 30, 50, 12.0F));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.FollowLeaderGoal(this,
                com.example.alieninvasion.entity.AlienBruteEntity.class, 1.2D)); // swarm behind brutes
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 24.0D)); // hive mind
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
        // Not eggs - a tracking bolt that "marks" whatever it hits for the swarm.
        MarkBoltEntity bolt = new MarkBoltEntity(this.level(), this);
        double dx = target.getX() - this.getX();
        double dy = target.getEyeY() - bolt.getY();
        double dz = target.getZ() - this.getZ();
        bolt.shoot(dx, dy, dz, 1.4F, 3.0F);
        this.playSound(SoundEvents.BLAZE_SHOOT, 1.0F,
                1.2F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(bolt);
    }

    // Alien chickens, like vanilla chickens, ignore fall damage (they flap).
    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
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

    // Harmless death puff (no more kamikaze damage burst).
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNEEZE,
                    this.getX(), this.getY() + 0.3, this.getZ(), 12, 0.3, 0.2, 0.3, 0.05);
        }
        super.die(source);
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
