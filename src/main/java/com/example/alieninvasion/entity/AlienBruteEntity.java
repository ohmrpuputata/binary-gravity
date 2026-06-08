package com.example.alieninvasion.entity;

import com.example.alieninvasion.ai.BlockBreakGoal;
import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Брут (Пришелец-Громила): Тяжелый бронированный монстр-танк роя.
 * Имеет огромный запас здоровья (150 HP), ломает блоки на своем пути и наносит огромный урон.
 */
public class AlienBruteEntity extends IronGolem {
        public AlienBruteEntity(EntityType<? extends IronGolem> type, Level level) {
                super(type, level);
        }

        public static AttributeSupplier.Builder createAttributes() {
                return Monster.createMonsterAttributes()
                                .add(Attributes.MAX_HEALTH, 150.0D) // tank
                                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                                .add(Attributes.ATTACK_DAMAGE, 12.0D) // heavy hitter, no longer a guaranteed one-shot
                                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                                .add(Attributes.FOLLOW_RANGE, 64.0D);
        }

        @Override
        protected void registerGoals() {
                // Do NOT call super.registerGoals to avoid Iron Golem friendly fire

                // TACTICS
                this.goalSelector.addGoal(0, new com.example.alieninvasion.ai.TacticalRetreatGoal(this, 1.0D));

                // SIEGE BREAKER MODE: the Brute chews through almost anything to reach
                // a target it can't path to.
                this.goalSelector.addGoal(1,
                                new BlockBreakGoal(this,
                                                state -> !state.is(Blocks.BEDROCK)
                                                                && !state.is(Blocks.END_PORTAL_FRAME)
                                                                && !state.is(Blocks.END_PORTAL)
                                                                && !state.is(Blocks.COMMAND_BLOCK),
                                                40));
                this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.BridgeToTargetGoal(this, 0.95D));
                // CLIMB: tower up to a target perched out of reach.
                this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.PillarUpGoal(this));
                // CHARGE: sprint across mid-range and slam the target.
                this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.ChargeAttackGoal(this, 1.6D, 12.0F));
                this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.0D));
                this.goalSelector.addGoal(3,
                                new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
                this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                                net.minecraft.world.entity.player.Player.class, 8.0F));

                // TARGETING
                this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.BloodScentGoal(this, 128.0D));
                this.targetSelector.addGoal(2, new com.example.alieninvasion.ai.XrayTargetGoal<>(this,
                                net.minecraft.world.entity.player.Player.class, false)); // WALLHACK
                this.targetSelector.addGoal(3,
                                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                                                net.minecraft.world.entity.animal.Animal.class, false));
                this.targetSelector.addGoal(4,
                                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                                                net.minecraft.world.entity.npc.AbstractVillager.class, false));
                this.targetSelector.addGoal(5,
                                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                                                net.minecraft.world.entity.animal.IronGolem.class, true));
                // HOSTILE TO ALL: everything that isn't an alien is prey.
                this.targetSelector.addGoal(6,
                                new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                                                net.minecraft.world.entity.Mob.class, 10, true, false,
                                                e -> !AlienUtils.isAlliedTo(this, e)));
        }

        @Override
        public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
                boolean result = super.hurt(source, amount);
                if (result && !this.level().isClientSide) {
                        AlienUtils.spawnGoreParticles(this, amount);
                        if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && !AlienUtils.isAlliedTo(this, attacker)) {
                                this.setTarget(attacker);
                        }
                }
                return result;
        }

        @Override
        public void aiStep() {
                super.aiStep();

                // Smash Attack Logic: a shockwave that knocks back and damages nearby
                // foes. It is a NONE-interaction explosion (visual + sound only) so the
                // Brute no longer craters the player's world every few seconds.
                if (!this.level().isClientSide && this.getTarget() != null && this.tickCount % 200 == 0) {
                        if (this.distanceToSqr(this.getTarget()) < 25.0D) {
                                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F,
                                                Level.ExplosionInteraction.NONE);

                                // Knockback + moderate damage to nearby non-allies.
                                java.util.List<net.minecraft.world.entity.LivingEntity> nearby = this.level()
                                                .getEntitiesOfClass(
                                                                net.minecraft.world.entity.LivingEntity.class,
                                                                this.getBoundingBox().inflate(5.0D),
                                                                e -> !AlienUtils.isAlliedTo(this, e));

                                for (net.minecraft.world.entity.LivingEntity entity : nearby) {
                                        double dx = entity.getX() - this.getX();
                                        double dz = entity.getZ() - this.getZ();
                                        entity.knockback(1.5F, -dx, -dz);
                                        entity.hurt(this.damageSources().mobAttack(this), 6.0F);
                                }
                        }
                }
        }

        @Override
        public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
                return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
        }
}
