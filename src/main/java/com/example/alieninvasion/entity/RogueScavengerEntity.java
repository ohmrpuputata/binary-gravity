package com.example.alieninvasion.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * РЕДКИЙ выживший — человек-NPC, НЕ из роя. Появляется ОДЕТЫМ (меч + инструменты +
 * лёгкая броня), бродит по пещерам и ДОБЫВАЕТ руду и дерево, переключаясь на нужный
 * ИНСТРУМЕНТ (кирка/топор), а не голыми руками; подбирает и надевает лучшее снаряжение.
 * ПРОТИВ ВСЕХ: бьёт игроков и пришельцев/монстров (но не своих). Теряя бой —
 * тактически отступает, вне боя «ест»/отдыхает. Случайный ник + скин, в чат молчит.
 */
public class RogueScavengerEntity extends Zombie {
    private static final String[] NICKS = {
            "DiggerJoe", "xX_Miner_Xx", "Prospector", "Vova_", "lost_guy", "OreHunter",
            "Sneaky_", "Kostya777", "WandererZ", "pickaxe_pro", "Hermit", "Slavik_",
            "deepdark_", "Nomad", "rud0kop", "Stalker_", "Anton_", "caveDweller"
    };
    private static final EntityDataAccessor<Integer> SKIN =
            SynchedEntityData.defineId(RogueScavengerEntity.class, EntityDataSerializers.INT);

    // Носимые инструменты — выживший достаёт нужный при добыче, а в бою держит меч.
    private final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
    private final ItemStack axe = new ItemStack(Items.IRON_AXE);
    private int restCooldown;

    public RogueScavengerEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true); // подбирает и надевает лучшее снаряжение
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SKIN, 0);
    }

    public int getSkin() {
        return this.entityData.get(SKIN);
    }

    /** Кирка для руды/камня, топор для дерева — выживший НЕ ломает блоки голыми руками. */
    public ItemStack toolFor(BlockState state) {
        return state.is(BlockTags.MINEABLE_WITH_AXE) ? this.axe : this.pickaxe;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.TacticalRetreatGoal(this, 1.3D));
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienLeapGoal(this, 0.5F));
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1D, false));
        // Вне боя — добывает руду И дерево нужным инструментом (x-ray скан).
        this.goalSelector.addGoal(4, new com.example.alieninvasion.ai.GatherResourcesGoal(this, 10, 14));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        // ПРОТИВ ВСЕХ: игроки + пришельцы/монстры (кроме себе подобных).
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, true));
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Monster.class, 10, true, false, e -> !(e instanceof RogueScavengerEntity)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        // ОДЕТ И ВООРУЖЁН (не голый), причём в СЛУЧАЙНЫЙ нормальный комплект — выжившие
        // выглядят по-разному (кожа/кольчуга/железо/золото/алмаз) + меч под тир. Инструменты носит.
        Item[][] kits = {
                {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.STONE_SWORD},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS, Items.IRON_SWORD},
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD},
                {Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_SWORD},
                {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.DIAMOND_SWORD},
        };
        Item[] kit = kits[this.random.nextInt(kits.length)];
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(kit[4]));
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(kit[0]));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(kit[1]));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(kit[2]));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(kit[3]));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            this.setDropChance(slot, 0.10F); // редко роняет — не ферма лута
        }
        this.setCustomName(net.minecraft.network.chat.Component.literal(NICKS[this.random.nextInt(NICKS.length)]));
        this.setCustomNameVisible(true);
        this.entityData.set(SKIN, this.random.nextInt(4));
        this.setPersistenceRequired();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
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
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)        // меч добавит ещё — как у игрока
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }
}
