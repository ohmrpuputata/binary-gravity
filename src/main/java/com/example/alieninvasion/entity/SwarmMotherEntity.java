package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.world.InvasionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Мать Роя (Swarm Mother):
 * Финальный босс мода с тремя уникальными фазами:
 * Фаза 1: Призыв волн миньонов пришельцев.
 * Фаза 2: Орбитальная бомбардировка метеорами.
 * Фаза 3: Разъяренный ближний бой и шоковые волны.
 * Победа над ней завершает игру.
 */
public class SwarmMotherEntity extends Monster implements IAlienUnit {
    // ALIEN VOICE: quiet, pitched vanilla sounds remixed into something wrong -
    // rarer and softer than the originals so the swarm unnerves instead of annoys.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.WARDEN_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.WARDEN_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 280;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    @Override
    public float getVoicePitch() {
        return 0.8F + this.random.nextFloat() * 0.1F;
    }

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.literal("Мать Роя"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_12
    ).setPlayBossMusic(true).setDarkenScreen(true);

    private int tickTimer = 0;
    private boolean scaledForPlayers = false;
    private int phase = 1;

    public SwarmMotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.SUPREME; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        // Without goals she just stood there and never acquired a target, so her
        // phase abilities (which require getTarget()) never fired. Now she hunts,
        // chases and melees the player - and the phases come alive.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 24.0F));

        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        // MULTIPLAYER: scale the boss's health to the number of players online so a
        // co-op team faces a fair final fight (the boss bar is already shared).
        if (!this.scaledForPlayers) {
            this.scaledForPlayers = true;
            int players = (this.getServer() != null)
                    ? Math.max(1, this.getServer().getPlayerList().getPlayers().size()) : 1;
            if (players > 1) {
                double newMax = 300.0D * (1.0D + 0.6D * (players - 1));
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                this.setHealth((float) newMax);
            }
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        tickTimer++;

        // Phase is driven by health. Crossing a threshold fires a one-time transition
        // (roar + shock burst + boss-bar rename); the ambient aura recolours per phase
        // so the fight visibly escalates.
        float hpPercent = this.getHealth() / this.getMaxHealth();
        int currentPhase = hpPercent >= 0.66f ? 1 : (hpPercent >= 0.33f ? 2 : 3);
        if (currentPhase != this.phase) {
            this.phase = currentPhase;
            onPhaseTransition(currentPhase);
        }
        if (tickTimer % 4 == 0) {
            emitAura(currentPhase);
        }
        if (currentPhase == 3) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        }

        if (this.getTarget() != null && !this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            // Фаза 1 (100%–66%): призыв миньонов каждые 15 сек
            if (currentPhase == 1) {
                if (tickTimer % 300 == 0) {
                    spawnMinions(target);
                }
            }
            // Фаза 2 (66%–33%): орбитальный удар метеорами каждые 6 сек
            else if (currentPhase == 2) {
                if (tickTimer % 120 == 0) {
                    launchMeteorStrike(target);
                }
            }
            // Фаза 3 (<33%): телекинетические шоковые волны каждые 4 сек
            else {
                if (tickTimer % 80 == 0) {
                    telekineticShockwave(target);
                }
            }
        }
    }

    private void emitAura(int currentPhase) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        double x = this.getX(), y = this.getY() + 1.4D, z = this.getZ();
        if (currentPhase == 1) {
            sl.sendParticles(ParticleTypes.PORTAL, x, y, z, 6, 0.8D, 1.0D, 0.8D, 0.4D);
        } else if (currentPhase == 2) {
            sl.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.8D, 1.0D, 0.8D, 0.02D);
        } else {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 8, 0.9D, 1.1D, 0.9D, 0.02D);
        }
    }

    private void onPhaseTransition(int newPhase) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 3.0F, 0.7F);
        sl.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 2.0F, 0.6F);
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.5D, this.getZ(), 8, 1.0D, 1.0D, 1.0D, 0.0D);
        // The new phase "slams in" - shove nearby players back so the shift reads.
        AABB area = this.getBoundingBox().inflate(6.0D);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area, en -> en != this && en.isAlive() && en instanceof Player)) {
            Vec3 push = e.position().subtract(this.position()).normalize().scale(1.2D);
            e.setDeltaMovement(push.x, 0.5D, push.z);
            e.hurtMarked = true;
        }
        if (newPhase == 2) {
            this.bossEvent.setName(Component.literal("Мать Роя — Бомбардировка"));
        } else if (newPhase == 3) {
            this.bossEvent.setName(Component.literal("Мать Роя — Ярость"));
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        }
    }

    private void spawnMinions(LivingEntity target) {
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.8F);
        
        for (int i = 0; i < 3; i++) {
            BlockPos spawnPos = this.blockPosition().offset(this.random.nextInt(7) - 3, 0, this.random.nextInt(7) - 3);
            net.minecraft.world.entity.Mob minion;
            
            float r = this.random.nextFloat();
            if (r < 0.4F) {
                minion = EntityRegistry.ALIEN_GRUNT.create(sl);
            } else if (r < 0.7F) {
                minion = EntityRegistry.INFESTED_ZOMBIE.create(sl);
            } else {
                minion = EntityRegistry.INFESTED_CREEPER.create(sl);
            }

            if (minion != null) {
                minion.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
                minion.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
                minion.setTarget(target);
                sl.addFreshEntity(minion);
                sl.sendParticles(ParticleTypes.PORTAL, minion.getX(), minion.getY() + 1.0D, minion.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
            }
        }
    }

    private void launchMeteorStrike(LivingEntity target) {
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 2.0F, 0.5F);

        double ox = target.getX() + (this.random.nextDouble() - 0.5D) * 10.0D;
        double oz = target.getZ() + (this.random.nextDouble() - 0.5D) * 10.0D;
        double oy = target.getY() + 30.0D;

        com.example.alieninvasion.entity.MeteorEntity meteor = EntityRegistry.METEOR.create(sl);
        if (meteor != null) {
            meteor.setPos(ox, oy, oz);
            meteor.setDeltaMovement((target.getX() - ox) * 0.04D, -1.0D, (target.getZ() - oz) * 0.04D);
            sl.addFreshEntity(meteor);
        }
    }

    private void telekineticShockwave(LivingEntity target) {
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.8F);
        sl.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 1.0D, this.getZ(), 1, 0, 0, 0, 0);

        double radius = 8.0D;
        AABB area = this.getBoundingBox().inflate(radius);
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && e.isAlive())) {
            double distSq = this.distanceToSqr(entity);
            if (distSq <= radius * radius) {
                Vec3 push = entity.position().subtract(this.position()).normalize().scale(1.5D);
                entity.setDeltaMovement(push.x, 0.6D, push.z);
                entity.hurtMarked = true;
                entity.hurt(this.damageSources().mobAttack(this), 10.0F);
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            ServerLevel sl = (ServerLevel) this.level();
            InvasionManager.get(sl).triggerVictory(sl);
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.1D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 60, 1.2D, 1.5D, 1.2D, 0.15D);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(), 50, 1.0D, 1.2D, 1.0D, 0.3D);
            sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
            sl.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.8F);
        }
    }
}
