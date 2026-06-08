package com.example.alieninvasion.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

// НЛО (Пришелец-Истребитель): Летающий боевой корабль пришельцев.
// Variants: 0 = Scout (small/fast), 1 = Destroyer (big/tanky), 2 = Carrier (huge,
// drops squads; also the evacuation ship at the finale).
public class UfoEntity extends Ghast {
    public static final int SCOUT = 0, DESTROYER = 1, CARRIER = 2;
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(UfoEntity.class, EntityDataSerializers.INT);

    private int abductionCooldown;
    private int abductionTicks;
    private int evacTicks;
    private int empTicks;

    public void setEmpTicks(int ticks) {
        this.empTicks = ticks;
    }

    public UfoEntity(EntityType<? extends Ghast> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0D)
                .add(Attributes.FLYING_SPEED, 0.8F)
                .add(Attributes.MOVEMENT_SPEED, 0.4F)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, SCOUT);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, variant);
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double max = variant == DESTROYER ? 110.0D : variant == CARRIER ? 150.0D : 50.0D;
            hp.setBaseValue(max);
            this.setHealth((float) max);
        }
        var kb = this.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(variant == SCOUT ? 0.0D : 0.7D);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.BombingRunGoal(this));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.RandomFlyGoal(this));
        this.goalSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                net.minecraft.world.entity.player.Player.class, 8.0F));

        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, true));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.animal.Animal.class, false));
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.npc.AbstractVillager.class, false));
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(this, e)));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.getTags().contains("EmpActive")) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.5D, 0.8D));
            if (this.level() instanceof ServerLevel sl && this.tickCount % 5 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.5, 0.5, 0.5, 0.05);
            }
            this.hurtMarked = true;
            if (this.empTicks <= 0) {
                this.empTicks = 160;
            }
            this.empTicks--;
            if (this.empTicks <= 0) {
                this.removeTag("EmpActive");
            }
            return;
        }
        // ----- DROPSHIP: descend, spawn a squad in a beam of light, and fly away. -----
        if (this.getTags().contains("Dropship")) {
            this.setTarget(null);
            int phase = this.getTags().contains("ds_descending") ? 1 :
                        this.getTags().contains("ds_dropping") ? 2 :
                        this.getTags().contains("ds_ascending") ? 3 : 0;
            
            ServerLevel sl = (ServerLevel) this.level();
            int groundY = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, this.getBlockX(), this.getBlockZ());
            int targetY = groundY + 7;

            if (phase == 0) {
                // Initialize
                this.addTag("ds_descending");
                phase = 1;
            }

            if (phase == 1) {
                // Descend quickly
                double diff = targetY - this.getY();
                if (diff < -0.5D) {
                    this.setDeltaMovement(0.0D, -0.6D, 0.0D);
                } else {
                    this.removeTag("ds_descending");
                    this.addTag("ds_dropping");
                    this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                    this.abductionTicks = 0; // use abductionTicks as a timer
                }
                this.hurtMarked = true;
            } else if (phase == 2) {
                // Hover and drop mobs
                this.setDeltaMovement(0.0D, 0.0D, 0.0D);
                this.hurtMarked = true;
                this.abductionTicks++;

                // Beam of light particles
                for (double py = groundY; py < this.getY(); py += 0.5D) {
                    sl.sendParticles(ParticleTypes.GLOW, this.getX() + (this.random.nextDouble() - 0.5D) * 3.0D, py, this.getZ() + (this.random.nextDouble() - 0.5D) * 3.0D, 2, 0.1, 0.1, 0.1, 0.0);
                    sl.sendParticles(ParticleTypes.END_ROD, this.getX() + (this.random.nextDouble() - 0.5D) * 1.5D, py, this.getZ() + (this.random.nextDouble() - 0.5D) * 1.5D, 1, 0.05, 0.05, 0.05, 0.0);
                }

                // Sound effect
                if (this.abductionTicks == 1) {
                    sl.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.HOSTILE, 3.0F, 0.8F);
                }

                // Spawn the mobs at tick 15
                if (this.abductionTicks == 15) {
                    spawnSquadMobs(sl);
                }

                if (this.abductionTicks >= 40) {
                    this.removeTag("ds_dropping");
                    this.addTag("ds_ascending");
                }
            } else if (phase == 3) {
                // Ascend quickly
                this.setDeltaMovement(0.0D, 0.8D, 0.0D);
                this.hurtMarked = true;
                if (this.getY() > groundY + 40 || this.getY() > sl.getMaxBuildHeight() - 4) {
                    sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 20, 1.0, 0.5, 1.0, 0.1);
                    this.discard();
                }
            }
            return;
        }

        ServerLevel sl = (ServerLevel) this.level();

        // ----- EVACUATION SHIP (finale): ignore players, beam up retreating aliens. -----
        if (this.getTags().contains("EvacShip")) {
            this.setTarget(null);
            this.evacTicks++;
            for (Mob a : sl.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(12.0D, 26.0D, 12.0D),
                    e -> e != this && e.getTags().contains("Retreating") && AlienUtils.isAlliedTo(null, e))) {
                Vec3 to = this.position().subtract(a.position());
                a.setDeltaMovement(to.x * 0.06D, 0.4D, to.z * 0.06D);
                a.hurtMarked = true;
                for (double py = a.getY(); py < this.getY(); py += 1.0D) {
                    sl.sendParticles(ParticleTypes.GLOW, this.getX(), py, this.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                }
                if (a.distanceToSqr(this) < 12.0D) {
                    sl.sendParticles(ParticleTypes.PORTAL, a.getX(), a.getY(), a.getZ(), 12, 0.3, 0.3, 0.3, 0.2);
                    a.discard();
                }
            }
            // hover, then ascend and vanish into the sky.
            if (this.evacTicks < 160) {
                this.setDeltaMovement(this.getDeltaMovement().x * 0.7D, 0.06D, this.getDeltaMovement().z * 0.7D);
            } else {
                this.setDeltaMovement(0.0D, 0.7D, 0.0D);
                if (this.evacTicks > 220 || this.getY() > sl.getMaxBuildHeight() - 4) {
                    sl.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 20, 1.0, 0.5, 1.0, 0.1);
                    this.discard();
                }
            }
            this.hurtMarked = true;
            return;
        }

        boolean carrier = this.getVariant() == CARRIER;
        // Dropship logic: carriers drop squads far more often.
        int dropPeriod = carrier ? 160 : 400;
        float dropChance = carrier ? 0.6f : 0.2f;
        if (this.tickCount % dropPeriod == 0 && this.random.nextFloat() < dropChance) {
            int n = carrier ? 1 + this.random.nextInt(2) : 1;
            for (int i = 0; i < n; i++) {
                com.example.alieninvasion.entity.AlienGruntEntity grunt = com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT
                        .create(this.level());
                if (grunt != null) {
                    grunt.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 3.0, this.getY() - 2,
                            this.getZ() + (this.random.nextDouble() - 0.5) * 3.0, this.getYRot(), 0);
                    this.level().addFreshEntity(grunt);
                }
            }
        }

        // DRILL DROP: breach an underground player.
        net.minecraft.world.entity.LivingEntity tgt = this.getTarget();
        if (tgt != null && tgt.isAlive() && this.tickCount % 300 == 0 && this.random.nextFloat() < 0.5f
                && tgt.getY() < this.getY() - 8) {
            com.example.alieninvasion.entity.DrillEntity drill = com.example.alieninvasion.registry.EntityRegistry.DRILL
                    .create(this.level());
            if (drill != null) {
                drill.setPos(this.getX(), this.getY() - 2, this.getZ());
                drill.setTargetY(tgt.getBlockY());
                this.level().addFreshEntity(drill);
            }
        }

        // ABDUCTION BEAM (unchanged): yanks up a target standing directly under the ship.
        if (this.abductionCooldown > 0) {
            this.abductionCooldown--;
        } else {
            net.minecraft.world.entity.LivingEntity target = this.getTarget();
            if (target != null && target.isAlive()) {
                double dx = target.getX() - this.getX();
                double dz = target.getZ() - this.getZ();
                boolean directlyUnder = (dx * dx + dz * dz) < 9.0D;
                double vGap = this.getY() - target.getY();
                if (directlyUnder && vGap > 2.0D && vGap < 24.0D) {
                    target.setDeltaMovement(target.getDeltaMovement().x, 0.18D, target.getDeltaMovement().z);
                    target.hurtMarked = true;
                    this.abductionTicks++;
                    for (double py = target.getY(); py < this.getY() - 1.0D; py += 1.0D) {
                        sl.sendParticles(ParticleTypes.GLOW, this.getX(), py, this.getZ(), 2, 0.3, 0.1, 0.3, 0.01);
                        sl.sendParticles(ParticleTypes.PORTAL, this.getX(), py, this.getZ(), 1, 0.3, 0.1, 0.3, 0.01);
                    }
                    boolean reached = target.getY() >= this.getY() - 2.5D;
                    if (reached || this.abductionTicks > 80) {
                        if (reached && target instanceof Player player) {
                            player.hurt(this.damageSources().magic(), 6.0F);
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[НЛО] похитило вас!"), true);
                            double tx = this.getX() + (this.random.nextDouble() - 0.5D) * 16.0D;
                            double tz = this.getZ() + (this.random.nextDouble() - 0.5D) * 16.0D;
                            int ty = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) tx, (int) tz);
                            player.teleportTo(tx, ty + 1, tz);
                        } else if (reached) {
                            sl.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY(), target.getZ(), 5, 0.1, 0.1, 0.1, 0.1);
                            target.discard();
                        }
                        this.abductionTicks = 0;
                        this.abductionCooldown = 300;
                        this.setTarget(null);
                    }
                } else {
                    this.abductionTicks = 0;
                }
            } else {
                this.abductionTicks = 0;
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide && amount > 0) {
            AlienUtils.spawnGoreParticles(this, amount);
            if (source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker && !AlienUtils.isAlliedTo(this, attacker)) {
                this.setTarget(attacker);
                Vec3 toAttacker = attacker.position().subtract(this.position());
                double sign = this.random.nextBoolean() ? 1.0D : -1.0D;
                Vec3 strafe = new Vec3(-toAttacker.z, 0.0D, toAttacker.x).normalize().scale(0.8D * sign);
                this.setDeltaMovement(this.getDeltaMovement().add(strafe.x, 0.4D, strafe.z));
                this.hurtMarked = true;
            }
        }
        return result;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return null; }
    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() { return null; }
    @Override
    protected float getSoundVolume() { return 0.0F; }

    private void spawnSquadMobs(ServerLevel level) {
        int grunts = 0, brutes = 0, trolls = 0, shamans = 0, stalkers = 0, casters = 0, telekinetics = 0, difficulty = 1;
        for (String tag : this.getTags()) {
            if (tag.startsWith("grunts:")) grunts = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("brutes:")) brutes = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("trolls:")) trolls = Integer.parseInt(tag.substring(7));
            else if (tag.startsWith("shamans:")) shamans = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("stalkers:")) stalkers = Integer.parseInt(tag.substring(9));
            else if (tag.startsWith("casters:")) casters = Integer.parseInt(tag.substring(8));
            else if (tag.startsWith("teles:")) telekinetics = Integer.parseInt(tag.substring(6));
            else if (tag.startsWith("diff:")) difficulty = Integer.parseInt(tag.substring(5));
        }

        BlockPos spawnPos = this.blockPosition().below(2);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_GRUNT, grunts, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_BRUTE, brutes, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_TROLL, trolls, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.HIVE_SHAMAN, shamans, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.ALIEN_STALKER, stalkers, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.PLASMA_CASTER, casters, difficulty);
        spawnDropshipMob(level, spawnPos, com.example.alieninvasion.registry.EntityRegistry.TELEKINETIC_ALIEN, telekinetics, difficulty);
    }

    private void spawnDropshipMob(ServerLevel level, BlockPos pos, EntityType<?> type, int count, int difficulty) {
        for (int i = 0; i < count; i++) {
            Mob mob = (Mob) type.create(level);
            if (mob != null) {
                mob.moveTo(pos.getX() + this.random.nextDouble() * 2.0D - 1.0D, pos.getY(),
                        pos.getZ() + this.random.nextDouble() * 2.0D - 1.0D, this.random.nextFloat() * 360F, 0);
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                com.example.alieninvasion.logic.AlienEvolution.evolve(mob, difficulty);
                mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 100, 0));
                level.addFreshEntity(mob);
            }
        }
    }
}
