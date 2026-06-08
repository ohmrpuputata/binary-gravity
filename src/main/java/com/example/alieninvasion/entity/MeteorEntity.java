package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;

// A falling meteor: trails fire, craters on impact and disgorges an alien squad.
public class MeteorEntity extends Entity {
    private int life;
    private int difficulty;

    public MeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.difficulty = tag.getInt("Difficulty");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putInt("Difficulty", this.difficulty);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY() + 0.4, this.getZ(), 0, 0, 0);
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.7, this.getZ(),
                    0, 0.02, 0);
            return;
        }

        this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.05, 0.0));
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 4, 0.3, 0.3, 0.3, 0.01);
        }

        this.life++;
        if (this.onGround() || this.horizontalCollision || this.verticalCollision || this.life > 400) {
            impact();
        }
    }

    private void impact() {
        if (!(this.level() instanceof ServerLevel sl)) {
            this.discard();
            return;
        }
        double x = this.getX(), y = this.getY(), z = this.getZ();

        // MASSIVE blast - much bigger than before.
        sl.explode(this, x, y, z, 6.0F, false, Level.ExplosionInteraction.BLOCK);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 5, 2.5, 1.0, 2.5, 0.0);
        sl.sendParticles(ParticleTypes.LAVA, x, y, z, 90, 3.5, 1.0, 3.5, 0.2);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 140, 4.5, 2.5, 4.5, 0.1);

        // Carve a bowl-shaped crater and line it with infestation (NO aliens spawn).
        BlockPos center = this.blockPosition();
        int R = 7;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                double horiz = Math.sqrt(dx * dx + dz * dz);
                if (horiz > R) continue;
                int depth = (int) Math.round(3.0 - horiz * 0.35);
                for (int dy = 3; dy >= -depth; dy--) {
                    BlockPos p = center.offset(dx, dy, dz);
                    if (sl.isOutsideBuildHeight(p)) continue;
                    var state = sl.getBlockState(p);
                    if (state.getDestroySpeed(sl, p) < 0) continue; // skip bedrock etc.
                    if (dy > -depth) {
                        if (!state.isAir()) {
                            sl.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                        }
                    } else {
                        // crater floor -> infested scar
                        sl.setBlock(p, this.random.nextFloat() < 0.3F
                                ? ModBlocks.ALIEN_RESIDUE.defaultBlockState()
                                : ModBlocks.INFESTED_STONE.defaultBlockState(), 2);
                    }
                }
            }
        }

        // Infect everything living caught in the blast (the meteor seeds the plague).
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(R + 3))) {
            if (!AlienUtils.isAlliedTo(null, e)) {
                e.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION), 1200, 0, false, true));
            }
        }

        this.discard();
    }
}
