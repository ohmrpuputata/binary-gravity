package com.example.alieninvasion.entity;

import com.example.alieninvasion.ai.TrollFleeGoal;
import com.example.alieninvasion.ai.TrollDeliverLootGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// Тролль-Пришелец: Быстрый и хрупкий воришка.
// Не сражается напрямую, а подбегает, крадет предмет у игрока и убегает прятать его.
public class AlienTrollEntity extends Monster implements IAlienUnit {
    // ALIEN VOICE: quiet, pitched vanilla sounds remixed into something wrong -
    // rarer and softer than the originals so the swarm unnerves instead of annoys.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 280;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public float getVoicePitch() {
        return 1.8F + this.random.nextFloat() * 0.1F;
    }

    private ItemStack stolenItem = ItemStack.EMPTY;
    private int carryTicks;

    public AlienTrollEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.TRICKSTER; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)         // glass cannon: one hit kills it
                .add(Attributes.MOVEMENT_SPEED, 0.5D)     // very fast
                .add(Attributes.ATTACK_DAMAGE, 1.0D)      // barely hits - it steals
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TrollFleeGoal(this, 1.7D));               // flee when carrying and threat is near
        this.goalSelector.addGoal(2, new TrollDeliverLootGoal(this, 1.5D));         // deliver to stash when carrying and safe
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.4D));      // dart in to steal
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Only hunts players (the source of loot), and only while empty-handed.
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true,
                (java.util.function.Predicate<LivingEntity>) entity -> this.stolenItem.isEmpty()));
    }

    public ItemStack getStolenItem() {
        return this.stolenItem;
    }

    public void setStolenItem(ItemStack stolenItem) {
        this.stolenItem = stolenItem;
    }

    public boolean hasLoot() {
        return !this.stolenItem.isEmpty();
    }

    // Steal instead of fighting: on a successful hit, lift a random stack from the
    // player's inventory and immediately disengage.
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = super.doHurtTarget(target);
        if (flag && !this.level().isClientSide && target instanceof Player player && this.stolenItem.isEmpty()) {
            List<Integer> filled = new ArrayList<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (!player.getInventory().getItem(i).isEmpty()) {
                    filled.add(i);
                }
            }
            if (!filled.isEmpty()) {
                int slot = filled.get(this.random.nextInt(filled.size()));
                this.stolenItem = player.getInventory().removeItemNoUpdate(slot);
                this.carryTicks = 0;
                this.setTarget(null);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FOX_AGGRO,
                        this.getSoundSource(), 1.0F, 1.4F);
                player.displayClientMessage(net.minecraft.network.chat.Component
                        .literal("§c[Троль] украл: §f" + this.stolenItem.getHoverName().getString()), true);
            }
        }
        return flag;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Stashing is now handled by TrollDeliverLootGoal, no drop on the ground logic needed here
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && !this.level().isClientSide) {
            AlienUtils.spawnGoreParticles(this, amount);
        }
        return result;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && hasLoot()) {
            this.spawnAtLocation(this.stolenItem); // drop what it stole
            this.stolenItem = ItemStack.EMPTY;
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!this.stolenItem.isEmpty()) {
            tag.put("StolenItem", this.stolenItem.save(this.registryAccess()));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("StolenItem")) {
            this.stolenItem = ItemStack.parse(this.registryAccess(), tag.getCompound("StolenItem"))
                    .orElse(ItemStack.EMPTY);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
