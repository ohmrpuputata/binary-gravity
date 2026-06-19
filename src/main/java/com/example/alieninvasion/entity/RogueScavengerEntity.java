package com.example.alieninvasion.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * РЕДКИЙ выживший — игрок-NPC, не из роя. Бродит по пещерам, добывает руду «сквозь
 * стены» (x-ray), подбирает и НАДЕВАЕТ лучшее снаряжение (как ваниль-зомби с
 * подбором), отдыхает/«ест» (реген, когда нет боя). НЕЙТРАЛЕН, пока игрок не подойдёт
 * близко в зоне видимости — тогда «оценивает» и нападает; теряя бой — тактически
 * отступает (как реальный игрок). Слабее клона: обычные HP/урон, не весь арсенал.
 * В чат ничего не пишет — только случайный ник над головой и случайный скин.
 */
public class RogueScavengerEntity extends Zombie {
    private static final String[] NICKS = {
            "DiggerJoe", "xX_Miner_Xx", "Prospector", "Vova_", "lost_guy", "OreHunter",
            "Sneaky_", "Kostya777", "WandererZ", "pickaxe_pro", "Hermit", "Slavik_",
            "deepdark_", "Nomad", "rud0kop", "Stalker_", "Anton_", "caveDweller"
    };
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(RogueScavengerEntity.class, EntityDataSerializers.INT);

    private int restCooldown;

    public RogueScavengerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true); // подбирает и надевает лучшее снаряжение, как зомби
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        // Теряя бой — отступает (как живой игрок), а не лезет на верную смерть.
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.TacticalRetreatGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienLeapGoal(this, 0.5F));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1D, false));
        // Без боя — копает ближайшую руду «сквозь стены» (x-ray).
        this.goalSelector.addGoal(4, new com.example.alieninvasion.ai.MineOreGoal(this, 8, 16));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        // НЕЙТРАЛЕН издалека: «замечает» игрока и нападает только вблизи (≈9 блоков) и
        // по линии взгляда — словно сначала оценил обстановку, потом решил атаковать.
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false, this::noticesPlayer));
    }

    private boolean noticesPlayer(net.minecraft.world.entity.LivingEntity e) {
        return this.distanceToSqr(e) < 81.0D && this.hasLineOfSight(e); // ~9 блоков + видит
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.getCustomName() == null) {
            this.setCustomName(net.minecraft.network.chat.Component.literal(NICKS[this.random.nextInt(NICKS.length)]));
            this.setCustomNameVisible(true);
            this.entityData.set(SKIN, this.random.nextInt(4));
            this.setPersistenceRequired(); // редкая сущность — не деспавнить
        }
        if (restCooldown > 0) {
            restCooldown--;
        }
        // «Отдыхает/ест» вне боя — медленно лечится, как игрок на привале.
        if (this.getTarget() == null && restCooldown == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal(1.0F);
            restCooldown = 40;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Skin", getSkin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(SKIN, tag.getInt("Skin"));
    }

    @Override
    public boolean canBreakDoors() {
        return true;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false; // живой человек, не нежить — на солнце не горит
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    // Тихий: никаких зомби-стонов; человеческие звуки боли/смерти.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.PLAYER_DEATH;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)        // как у игрока
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)      // слабый без оружия (с мечом — сильнее)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }
}
