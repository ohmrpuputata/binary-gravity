package com.example.alieninvasion.entity;

import com.example.alieninvasion.ai.BlockBreakGoal;
import com.example.alieninvasion.ai.AlienAttackGoal;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

// Прорыватель Пещер (Пришелец-Разрушитель): Тяжелый шагоход.
// Быстро прокапывается сквозь камень, грязь и постройки, чтобы выкурить игрока из пещер.
public class AlienBreacherEntity extends Monster implements IAlienUnit {
    public AlienBreacherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.ENGINEER; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)         // tanky structure breacher
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        
        // FAST TUNNELING: breaks stone/dirt extremely quickly
        this.goalSelector.addGoal(1, new BlockBreakGoal(this,
                state -> !state.is(Blocks.BEDROCK)
                        && !state.is(Blocks.END_PORTAL_FRAME)
                        && !state.is(Blocks.END_PORTAL)
                        && !state.is(Blocks.COMMAND_BLOCK),
                15)); // 15 ticks per block = 0.75 seconds
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.BridgeToTargetGoal(this, 1.1D));

        this.goalSelector.addGoal(2, new AlienAttackGoal(this, 1.2D));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 36.0D));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(this, e)));
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
