package com.example.alieninvasion.entity;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;

/**
 * Грант (Пехотинец пришельцев): Базовый боец роя.
 * Днем может спавниться как рабочий (Scavenger), собирающий ресурсы, а ночью - как агрессивный воин.
 */
public class AlienGruntEntity extends Zombie {
    private boolean scavenger = false;

    public AlienGruntEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        // Smart inventory: grunts scavenge and wield dropped gear/weapons & armor,
        // and the AI uses held weapons for extra damage.
        this.setCanPickUpLoot(true);
    }

    public boolean isScavenger() {
        return this.scavenger;
    }

    public void setScavenger(boolean scavenger) {
        this.scavenger = scavenger;
        if (scavenger) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0D);
            this.setHealth(10.0F);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(1.0D);
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4D);
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("IsScavenger", this.scavenger);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setScavenger(tag.getBoolean("IsScavenger"));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));

        // SCAVENGER GOALS (only active if scavenger is true)
        this.goalSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(this, net.minecraft.world.entity.player.Player.class, 16.0F, 1.2D, 1.6D) {
            @Override public boolean canUse() { return AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.ScavengerDeliverLootGoal(this, 1.3D));
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.ScavengerGatherItemsGoal(this));
        // Daytime workers also MINE natural resources (not just pick up dropped
        // items), so they always have something to gather and stash.
        this.goalSelector.addGoal(4, new com.example.alieninvasion.ai.ScavengerHarvestGoal(this));

        // TACTICS (only active if NOT scavenger)
        this.goalSelector.addGoal(1, new com.example.alieninvasion.ai.TacticalRetreatGoal(this, 1.3D) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });

        // SIEGE: when the player is above and out of reach, claw a tower/bridge up to
        // them. These are LAST-resort moves now (priority 3, BELOW the leap goal) so a
        // grunt jumps a 1-2 block gap instead of standing still building an afk pillar.
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.BridgeToTargetGoal(this, 1.15D) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.PillarUpGoal(this) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });

        // SMART SIEGE: dig through soft blocks toward the player when pathing is
        // blocked (excludes unbreakable / very hard blocks so it isn't a full Brute).
        this.goalSelector.addGoal(3, new com.example.alieninvasion.ai.BlockBreakGoal(this,
                state -> !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)
                        && !state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                        && !state.is(net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN)
                        && !state.is(net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE)
                        && !state.is(net.minecraft.world.level.block.Blocks.END_PORTAL_FRAME)
                        && !state.is(net.minecraft.world.level.block.Blocks.END_PORTAL)
                        && !state.is(net.minecraft.world.level.block.Blocks.COMMAND_BLOCK)
                        && !state.is(net.minecraft.world.level.block.Blocks.BARRIER),
                60) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });

        // Leap is now tried BEFORE the siege goals so short gaps are jumped, not towered.
        this.goalSelector.addGoal(2, new com.example.alieninvasion.ai.AlienLeapGoal(this, 0.5F) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.goalSelector.addGoal(4, new com.example.alieninvasion.ai.AlienAttackGoal(this, 1.0D) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });

        // BUILD: only WORKERS (scavengers) build now. Combat raiders no longer stand
        // around constructing huts when idle - that read as "afk" in the field.
        this.goalSelector.addGoal(5, new com.example.alieninvasion.ai.CooperativeBuildGoal(this, 1.0D) {
            @Override public boolean canUse() { return AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.goalSelector.addGoal(7, new com.example.alieninvasion.ai.FollowLeaderGoal(this,
                com.example.alieninvasion.entity.AlienBruteEntity.class, 1.2D) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(9, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this,
                net.minecraft.world.entity.player.Player.class, 8.0F));

        this.targetSelector.addGoal(1, new com.example.alieninvasion.ai.SquadAggroGoal(this, 32.0D) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.targetSelector.addGoal(2, new com.example.alieninvasion.ai.XrayTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, false) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.targetSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.npc.AbstractVillager.class, false) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        this.targetSelector.addGoal(4, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.animal.IronGolem.class, true) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
        // HOSTILE TO ALL: aliens attack every living thing that isn't one of their
        // own kind - players, livestock, vanilla zombies/skeletons, villagers, etc.
        this.targetSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !AlienUtils.isAlliedTo(AlienGruntEntity.this, e)) {
            @Override public boolean canUse() { return !AlienGruntEntity.this.isScavenger() && super.canUse(); }
        });
    }

    @Override
    public boolean canBreakDoors() {
        return true;
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
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
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)        // tougher than a zombie, not a tank
                .add(Attributes.MOVEMENT_SPEED, 0.28D)    // a touch faster than a zombie (0.23), still kiteable
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.FOLLOW_RANGE, 35.0D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE);
    }



    @Override
    public boolean isBaby() {
        return false;
    }
}
