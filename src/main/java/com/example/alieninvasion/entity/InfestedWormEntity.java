package com.example.alieninvasion.entity;

import com.example.alieninvasion.logic.InfectionManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * THE INFECTION'S LIFE CYCLE in one creature. Worms burst out of infested hosts
 * (and out of heavily infected players), bite to spread the infection, and
 * EVOLVE: every kill can grow a worm a stage —
 *   stage 0 «выводок»  — tiny, fast, weak;
 *   stage 1 «взрослый» — man-length, real damage;
 *   stage 2 «великий»  — a monster that outscales a grunt.
 * Scale, health and damage all grow with the stage.
 */
public class InfestedWormEntity extends Silverfish implements IAlienUnit {
    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(InfestedWormEntity.class, EntityDataSerializers.INT);

    public InfestedWormEntity(EntityType<? extends Silverfish> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAGE, 0);
    }

    public int getStage() {
        return this.entityData.get(STAGE);
    }

    public void setStage(int stage) {
        stage = Math.max(0, Math.min(2, stage));
        this.entityData.set(STAGE, stage);
        var scale = this.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(0.8D + 0.8D * stage);
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(8.0D + 10.0D * stage);
        var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(3.0D + 2.5D * stage);
        this.setHealth(this.getMaxHealth());
    }

    /** Worms grow by feeding: a kill has a good chance to push the next stage. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (getStage() < 2 && this.random.nextFloat() < 0.6F) {
            setStage(getStage() + 1);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL,
                    this.getX(), this.getY() + 0.4D, this.getZ(), 20, 0.4D, 0.3D, 0.4D, 0.03D);
            level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.SLIME_BLOCK_PLACE,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 0.5F);
        }
        return result;
    }

    /** Every bite pushes the infection meter — worms are the disease's teeth. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Player player && !this.level().isClientSide) {
            InfectionManager.addMeter(player, 6.0F + 3.0F * getStage());
        }
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("WormStage", getStage());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setStage(tag.getInt("WormStage"));
    }

    @Override
    public AlienRole getAlienRole() {
        return AlienRole.INFECTED;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 180;
    }

    @Override
    protected float getSoundVolume() {
        return 0.5F;
    }

    @Override
    public float getVoicePitch() {
        return 1.2F - 0.25F * getStage() + this.random.nextFloat() * 0.1F;
    }
}
