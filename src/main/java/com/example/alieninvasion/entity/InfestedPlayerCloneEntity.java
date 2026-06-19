package com.example.alieninvasion.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * CORPSE-RUNNER: when a player dies in the apocalypse, the swarm grows a clone
 * from the body. The clone HOOVERS UP the entire death drop into its gut,
 * equips the best armor and weapon from it, and then hunts — its former owner
 * above everyone else. Kill it to get everything back; until then your own
 * gear walks the wasteland wearing your face.
 *
 * "Smart" toolkit: tactical retreat when hurt, leaps gaps, digs through walls,
 * bridges and towers to reach you, eats golden apples from the stash when low.
 */
public class InfestedPlayerCloneEntity extends Zombie implements IAlienUnit {
    // Случайные ники — клон выглядит как ОЧЕРЕДНОЙ погибший игрок, а не как твоя копия.
    private static final String[] CLONE_NICKS = {
            "Steve_2009", "xX_Reaper_Xx", "Dimon4ik", "ProGamer228", "Kreed_", "Herobrine",
            "MrFox", "Player_1337", "Survivor", "Shadow__", "Nub_Slayer", "Cooldwarf",
            "Ivan_", "Kelev", "Dead_Andrey", "grisha_top", "Notch_fan", "Alex22"
    };
    private UUID ownerId;
    private String ownerName = "";
    private final NonNullList<ItemStack> stash = NonNullList.create();
    private int eatCooldown;

    public InfestedPlayerCloneEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.INFECTED; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.TacticalRetreatGoal(this, 1.25D));
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienLeapGoal(this, 0.5F));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.BlockBreakGoal(this,
                state -> !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                        && !state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                        && !state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
                        && !state.is(net.minecraft.world.level.block.Blocks.BARRIER), 30));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.BridgeToTargetGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.PillarUpGoal(this));
        this.goalSelector.addGoal(4, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 12.0F));

        this.targetSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        // The OWNER is prey #1 — the clone remembers who it grew from.
        this.targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, Player.class, 10, true, false,
                p -> ownerId != null && p.getUUID().equals(ownerId)));
        this.targetSelector.addGoal(2, new com.example.alieninvasion.ai.XrayTargetGoal<>(this, Player.class, false));
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                this, net.minecraft.world.entity.npc.AbstractVillager.class, false));
    }

    /** Bind to the dead player: name, face, grudge. */
    public void bindOwner(Player player) {
        this.ownerId = player.getUUID();
        this.ownerName = player.getName().getString();
        // Случайный ник, как у обычного погибшего игрока (а не имя жертвы красным).
        String nick = CLONE_NICKS[this.random.nextInt(CLONE_NICKS.length)];
        this.setCustomName(net.minecraft.network.chat.Component.literal(nick));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public boolean isOwner(UUID id) {
        return ownerId != null && ownerId.equals(id);
    }

    /** Swallow every dropped item around the corpse into the stash, then gear up. */
    public void absorbDeathLoot(ServerLevel level, net.minecraft.world.phys.Vec3 at) {
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                new AABB(at.x - 5, at.y - 3, at.z - 5, at.x + 5, at.y + 4, at.z + 5))) {
            stash.add(item.getItem().copy());
            item.discard();
        }
        equipBest();
    }

    /** Wear the best armor and weapon found in the stash. */
    private void equipBest() {
        for (int i = 0; i < stash.size(); i++) {
            ItemStack s = stash.get(i);
            if (s.getItem() instanceof ArmorItem armor) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                ItemStack current = this.getItemBySlot(slot);
                boolean better = current.isEmpty()
                        || (current.getItem() instanceof ArmorItem cur && armor.getDefense() > cur.getDefense());
                if (better) {
                    if (!current.isEmpty()) stash.add(current);
                    this.setItemSlot(slot, s.copyAndClear());
                    this.setDropChance(slot, 2.0F);
                }
            } else if ((s.getItem() instanceof SwordItem || s.getItem() instanceof AxeItem)
                    && this.getMainHandItem().isEmpty()) {
                this.setItemSlot(EquipmentSlot.MAINHAND, s.copyAndClear());
                this.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
            } else if (s.getItem() instanceof ShieldItem && this.getOffhandItem().isEmpty()) {
                this.setItemSlot(EquipmentSlot.OFFHAND, s.copyAndClear());
                this.setDropChance(EquipmentSlot.OFFHAND, 2.0F);
            }
        }
        stash.removeIf(ItemStack::isEmpty);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (eatCooldown > 0) eatCooldown--;
        // SMART: low on health, it eats golden apples from YOUR loot.
        if (eatCooldown == 0 && this.getHealth() < this.getMaxHealth() * 0.5F) {
            for (int i = 0; i < stash.size(); i++) {
                ItemStack s = stash.get(i);
                if (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                    s.shrink(1);
                    this.heal(8.0F);
                    this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.REGENERATION, 100, 1));
                    this.level().playSound(null, this.blockPosition(),
                            net.minecraft.sounds.SoundEvents.PLAYER_BURP,
                            net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.8F);
                    eatCooldown = 200;
                    break;
                }
            }
            stash.removeIf(ItemStack::isEmpty);
        }
    }

    /** Everything it swallowed comes back when it dies. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        for (ItemStack s : stash) {
            if (!s.isEmpty()) this.spawnAtLocation(s);
        }
        stash.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerId != null) tag.putUUID("CloneOwner", ownerId);
        tag.putString("CloneOwnerName", ownerName);
        ListTag list = new ListTag();
        for (ItemStack s : stash) {
            if (!s.isEmpty()) list.add(s.save(this.registryAccess()));
        }
        tag.put("CloneStash", list);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("CloneOwner")) ownerId = tag.getUUID("CloneOwner");
        ownerName = tag.getString("CloneOwnerName");
        stash.clear();
        ListTag list = tag.getList("CloneStash", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack.parse(this.registryAccess(), list.getCompound(i)).ifPresent(stash::add);
        }
    }

    @Override
    public boolean canBreakDoors() { return true; }

    @Override
    protected boolean isSunBurnTick() { return false; }

    @Override
    public boolean isBaby() { return false; }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }

    // Silent stalker: no idle groans at all; player-like pain sounds, pitched down.
    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return net.minecraft.sounds.SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float getVoicePitch() { return 0.75F; }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }

    /** Old API kept for dungeon spawns: copy visible gear only. */
    public void copyFromPlayer(Player player) {
        bindOwner(player);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                this.setItemSlot(slot, stack.copy());
                this.setDropChance(slot, 2.0F);
            }
        }
    }
}
