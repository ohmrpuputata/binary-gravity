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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    // Базовое HP королевы (масштабируется по числу игроков). Поднято с 300 — финальный
    // босс должен быть стеной, а не падать за минуту.
    public static final double BASE_HEALTH = 600.0D;

    private int tickTimer = 0;
    private boolean scaledForPlayers = false;
    private boolean enraged = false;
    private int phase = 1;
    // Кулдауны приёмов (как у Макса — у каждого приёма свой таймер, бой динамичный).
    private int cdSummon, cdMeteor, cdSlam, cdNova, cdGrasp, cdCharge, cdAura;
    // Кинематографичное появление: маяк ставит её высоко в небо, и она медленно
    // спускается на луче света, неуязвимая, пока не коснётся земли.
    private boolean descending = false;
    private int descentTicks = 0;

    public SwarmMotherEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
    }

    /** Включает режим нисхождения с орбиты (вызывается Маяком Роя при призыве). */
    public void beginDescent() {
        this.descending = true;
        this.descentTicks = 0;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Descending", this.descending);
        tag.putInt("DescentTicks", this.descentTicks);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.descending = tag.getBoolean("Descending");
        this.descentTicks = tag.getInt("DescentTicks");
        if (this.descending) {
            this.setNoGravity(true);
            this.setInvulnerable(true);
        }
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.SUPREME; }

    public static AttributeSupplier.Builder createAttributes() {
        // Финальный босс: толстый, бронированный, бьёт больно. Урон 22 -> 30 -> 38 по
        // фазам, плюс броня и стойкость к нокбеку — теперь это стена, а не мешок с HP.
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.ATTACK_DAMAGE, 22.0D)
                .add(Attributes.ARMOR, 12.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
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
                double newMax = BASE_HEALTH * (1.0D + 0.6D * (players - 1));
                this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
                this.setHealth((float) newMax);
            }
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());

        if (this.descending) {
            tickDescent();
            return;
        }

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
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.40D);
        }

        if (this.getTarget() != null && !this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            double dist = this.distanceTo(target);
            // Кулдауны быстрее с ростом фазы — финал должен душить.
            float cdMul = currentPhase == 3 ? 0.6f : currentPhase == 2 ? 0.8f : 1.0f;
            if (cdSummon > 0) cdSummon--;
            if (cdMeteor > 0) cdMeteor--;
            if (cdSlam > 0) cdSlam--;
            if (cdNova > 0) cdNova--;
            if (cdGrasp > 0) cdGrasp--;
            if (cdCharge > 0) cdCharge--;
            if (cdAura > 0) cdAura--;

            // Поле заражения — постоянная аура слабости вокруг королевы.
            if (cdAura <= 0) { infestAura(); cdAura = 40; }

            if (currentPhase == 1) {
                // Фаза 1: непрерывные подкрепления + рывок, чтобы достать беглеца.
                if (cdSummon <= 0) { spawnMinions(target); cdSummon = (int) (190 * cdMul); }
                if (dist > 6.0 && cdCharge <= 0) { chargeDash(target); cdCharge = (int) (90 * cdMul); }
                if (dist < 7.0 && cdNova <= 0) { acidNova(); cdNova = (int) (170 * cdMul); }
            } else if (currentPhase == 2) {
                // Фаза 2: орбитальная бомбардировка + телекинез + слэм вблизи.
                if (cdMeteor <= 0) { launchMeteorBarrage(target); cdMeteor = (int) (150 * cdMul); }
                if (dist > 5.0 && cdGrasp <= 0) { voidGrasp(); cdGrasp = (int) (150 * cdMul); }
                if (dist < 7.0 && cdSlam <= 0) { groundSlam(); cdSlam = (int) (140 * cdMul); }
                if (cdSummon <= 0) { spawnMinions(target); cdSummon = (int) (300 * cdMul); }
            } else {
                // Фаза 3 (ярость): всё и сразу, очень быстро.
                if (cdSlam <= 0) { groundSlam(); cdSlam = (int) (110 * cdMul); }
                if (cdNova <= 0) { acidNova(); cdNova = (int) (120 * cdMul); }
                if (dist > 4.0 && cdGrasp <= 0) { voidGrasp(); cdGrasp = (int) (120 * cdMul); }
                if (cdMeteor <= 0) { launchMeteorBarrage(target); cdMeteor = (int) (180 * cdMul); }
                if (tickTimer % 70 == 0) { telekineticShockwave(target); }
            }
        }
    }

    private void tickDescent() {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        this.descentTicks++;
        // Медленное грозное падение; навигация и гравитация отключены.
        this.setDeltaMovement(0.0D, -0.06D, 0.0D);
        this.hurtMarked = true;
        this.getNavigation().stop();

        // Светящаяся тройная спираль вокруг неё + столб портальных частиц.
        double angle = this.descentTicks * 0.35D;
        for (int arm = 0; arm < 3; arm++) {
            double a = angle + arm * (Math.PI * 2.0D / 3.0D);
            sl.sendParticles(ParticleTypes.END_ROD,
                    this.getX() + Math.cos(a) * 2.2D, this.getY() + 1.2D, this.getZ() + Math.sin(a) * 2.2D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(), this.getZ(), 8, 0.8D, 1.4D, 0.8D, 0.25D);
        if (this.descentTicks % 20 == 0) {
            sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 3.0F, 0.6F);
        }

        // Приземление (или страховка от зависания через 30 секунд).
        if (this.onGround() || this.descentTicks > 600) {
            finishDescent(sl);
        }
    }

    private void finishDescent(ServerLevel sl) {
        this.descending = false;
        this.setNoGravity(false);
        this.setInvulnerable(false);
        // Ударная волна приземления: гром, кольцо пыли, игроков отбрасывает.
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 4.0F, 0.4F);
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 4.0F, 0.5F);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 0.5D, this.getZ(), 2, 0.5D, 0.5D, 0.5D, 0.0D);
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16.0D;
            sl.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + Math.cos(a) * 3.0D, this.getY() + 0.2D, this.getZ() + Math.sin(a) * 3.0D,
                    1, 0.0D, 0.0D, 0.0D, 0.12D);
        }
        AABB area = this.getBoundingBox().inflate(10.0D);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area,
                en -> en != this && en.isAlive() && en instanceof Player)) {
            Vec3 push = e.position().subtract(this.position()).normalize().scale(1.4D);
            e.setDeltaMovement(push.x, 0.6D, push.z);
            e.hurtMarked = true;
        }
        AlienUtils.broadcastTitle(sl,
                Component.literal("§5МАТЬ РОЯ"),
                Component.literal("§dБой за Землю начинается"));
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
        // Урон растёт по фазам (22 -> 30 -> 38): бой обязан становиться страшнее,
        // а не только менять цвет частиц.
        var attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(newPhase == 3 ? 38.0D : newPhase == 2 ? 30.0D : 22.0D);
        }
        // Каждый переход фазы сопровождается волной подкрепления.
        spawnMinions(this.getTarget());
        if (newPhase == 2) {
            this.bossEvent.setName(Component.literal("Мать Роя — Бомбардировка"));
            AlienUtils.broadcastTitle(sl,
                    Component.literal("§6Мать Роя зовёт огонь с орбиты"),
                    Component.literal("§eНе стойте на месте!"));
        } else if (newPhase == 3) {
            this.bossEvent.setName(Component.literal("Мать Роя — Ярость"));
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            // ЯРОСТЬ: разовый лечащий всплеск + ускорение, чтобы финальная фаза была пиком,
            // а не агонией. Лечит на 20% макс. HP и навсегда ускоряется.
            if (!this.enraged) {
                this.enraged = true;
                this.heal((float) (this.getMaxHealth() * 0.20D));
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.40D);
                sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(),
                        40, 1.0D, 1.2D, 1.0D, 0.4D);
            }
            AlienUtils.broadcastTitle(sl,
                    Component.literal("§4ЯРОСТЬ РОЯ"),
                    Component.literal("§cОна сражается за своё потомство"));
        }
    }

    private void spawnMinions(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
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

    /** Орбитальный залп: три метеора накрывают площадь вокруг цели. */
    private void launchMeteorBarrage(LivingEntity target) {
        ServerLevel sl = (ServerLevel) this.level();
        sl.playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 2.5F, 0.45F);
        for (int i = 0; i < 3; i++) {
            double ox = target.getX() + (this.random.nextDouble() - 0.5D) * 12.0D;
            double oz = target.getZ() + (this.random.nextDouble() - 0.5D) * 12.0D;
            double oy = target.getY() + 30.0D + i * 2.0D;
            com.example.alieninvasion.entity.MeteorEntity meteor = EntityRegistry.METEOR.create(sl);
            if (meteor != null) {
                meteor.setPos(ox, oy, oz);
                meteor.setDeltaMovement((target.getX() - ox) * 0.04D, -1.0D, (target.getZ() - oz) * 0.04D);
                sl.addFreshEntity(meteor);
            }
        }
    }

    /** Рывок: королева бросается на цель, чтобы догнать беглеца. */
    private void chargeDash(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel sl)) return;
        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.x * 1.5D, 0.42D, dir.z * 1.5D);
        this.hurtMarked = true;
        this.getLookControl().setLookAt(target, 60.0F, 60.0F);
        sl.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.2F, 0.7F);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.0D, this.getZ(), 14, 0.5D, 0.5D, 0.5D, 0.05D);
    }

    /** Удар по земле: купол урона и подброс всех вокруг. */
    private void groundSlam() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.6F, 0.6F);
        sl.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.6F);
        double radius = 6.5D;
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.8F;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                en -> en != this && en.isAlive() && en instanceof Player)) {
            if (this.distanceToSqr(e) <= radius * radius) {
                e.hurt(this.damageSources().mobAttack(this), dmg);
                Vec3 push = e.position().subtract(this.position()).normalize().scale(0.8D);
                e.setDeltaMovement(push.x, 1.1D, push.z);
                e.hurtMarked = true;
            }
        }
        for (int i = 0; i < 44; i++) {
            double a = i * Math.PI / 22.0D;
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    this.getX() + Math.cos(a) * radius, this.getY() + 0.2D, this.getZ() + Math.sin(a) * radius,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Телекинетический захват: притягивает игроков из 16 блоков к королеве. */
    private void voidGrasp() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 2.2F, 0.6F);
        double radius = 16.0D;
        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            Vec3 pull = this.position().subtract(p.position()).normalize().scale(2.4D);
            p.setDeltaMovement(pull.x, 0.35D, pull.z);
            p.hurtMarked = true;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
            sl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0D, p.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.6D);
        }
    }

    /** Кислотная новая: взрыв биомассы вокруг — урон + яд + иссушение. */
    private void acidNova() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.4F, 1.5F);
        sl.sendParticles(ParticleTypes.SCULK_CHARGE_POP, this.getX(), this.getY() + 1.0D, this.getZ(), 70, 4.0D, 1.5D, 4.0D, 0.1D);
        double radius = 7.0D;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                en -> en != this && en.isAlive() && en instanceof Player)) {
            if (this.distanceToSqr(e) <= radius * radius) {
                e.hurt(this.damageSources().mobAttack(this), 9.0F);
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1));
            }
        }
    }

    /** Поле заражения: лёгкая слабость на всех игроков рядом (постоянная аура). */
    private void infestAura() {
        if (!(this.level() instanceof ServerLevel sl)) return;
        for (Player p : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(12.0D),
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false));
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
            InvasionManager.get(sl).triggerVictory(sl, this.blockPosition());
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.5D, 0.5D, 0.5D, 0.1D);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(), 60, 1.2D, 1.5D, 1.2D, 0.15D);
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(), 50, 1.0D, 1.2D, 1.0D, 0.3D);
            sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.5F);
            sl.playSound(null, this.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 2.0F, 0.8F);
            // Кольцо визуальных молний вокруг туши — смерть королевы видно издалека.
            for (int i = 0; i < 5; i++) {
                net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                if (bolt != null) {
                    double a = i * (Math.PI * 2.0D / 5.0D);
                    bolt.moveTo(this.getX() + Math.cos(a) * 6.0D, this.getY(), this.getZ() + Math.sin(a) * 6.0D);
                    bolt.setVisualOnly(true);
                    sl.addFreshEntity(bolt);
                }
            }
            // Гарантированные трофеи: тёмная материя, ядра улья и шаблон нибирия —
            // победитель уносит то, ради чего другим приходится грабить материнский корабль.
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.DARK_MATTER_SHARD, 3));
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.HIVE_CORE, 2));
            this.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                    com.example.alieninvasion.registry.ItemRegistry.NIBIRIUM_SMITHING_TEMPLATE, 1));
        }
    }
}
