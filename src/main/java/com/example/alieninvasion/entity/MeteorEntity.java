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

        // A REAL crater: a deep parabolic bowl dug into the actual terrain
        // (per-column surface height, so it bites into slopes correctly), with a
        // raised rim of scorched debris and burning patches at the heart.
        BlockPos center = this.blockPosition();
        final int R = 6;  // bowl radius
        final int D = 5;  // depth at the centre - a funnel, not a pancake
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -R - 1; dx <= R + 1; dx++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                double horiz = Math.sqrt(dx * dx + dz * dz);
                if (horiz > R + 1) continue;
                int wx = center.getX() + dx;
                int wz = center.getZ() + dz;
                int surfaceY = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        wx, wz) - 1;
                if (sl.isOutsideBuildHeight(surfaceY) || surfaceY <= sl.getMinBuildHeight() + D + 1) continue;

                if (horiz > R) {
                    // RIM: a ragged ridge of scorched rock thrown up around the bowl,
                    // randomly one or two blocks tall, occasionally on fire.
                    if (this.random.nextFloat() < 0.75F) {
                        cursor.set(wx, surfaceY + 1, wz);
                        if (sl.getBlockState(cursor).isAir()) {
                            sl.setBlock(cursor, ModBlocks.INFESTED_STONE.defaultBlockState(), 2);
                            if (this.random.nextFloat() < 0.3F) {
                                cursor.set(wx, surfaceY + 2, wz);
                                if (sl.getBlockState(cursor).isAir()) {
                                    sl.setBlock(cursor, this.random.nextBoolean()
                                            ? ModBlocks.INFESTED_STONE.defaultBlockState()
                                            : net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
                                }
                            }
                        }
                    }
                    continue;
                }

                // BOWL: parabolic depth from each column's own surface.
                int dig = (int) Math.ceil(D * (1.0 - (horiz * horiz) / (double) (R * R)));
                int top = Math.min(sl.getMaxBuildHeight() - 1, surfaceY + 4);
                int bottom = surfaceY - dig;
                for (int yy = top; yy > bottom; yy--) {
                    cursor.set(wx, yy, wz);
                    var state = sl.getBlockState(cursor);
                    if (state.isAir()) continue;
                    if (state.getDestroySpeed(sl, cursor) < 0) break; // bedrock: stop this column
                    sl.setBlock(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
                // Floor lining: infested scar, molten at the very heart.
                cursor.set(wx, bottom, wz);
                var floorState = sl.getBlockState(cursor);
                if (!floorState.isAir() && floorState.getDestroySpeed(sl, cursor) >= 0) {
                    net.minecraft.world.level.block.state.BlockState lining;
                    if (horiz < 1.6) {
                        lining = net.minecraft.world.level.block.Blocks.MAGMA_BLOCK.defaultBlockState();
                    } else if (this.random.nextFloat() < 0.3F) {
                        lining = ModBlocks.ALIEN_RESIDUE.defaultBlockState();
                    } else {
                        lining = ModBlocks.INFESTED_STONE.defaultBlockState();
                    }
                    sl.setBlock(cursor, lining, 2);
                    if (horiz < 3 && this.random.nextFloat() < 0.25F) {
                        cursor.set(wx, bottom + 1, wz);
                        if (sl.getBlockState(cursor).isAir()) {
                            sl.setBlock(cursor, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }

        // RADIOACTIVE STRIKE: about a third of impacts leave a glowing crystal
        // core in the crater floor - a fresh radiation hazard the player must
        // then deal with (or mine, carefully).
        if (this.random.nextFloat() < 0.35F) {
            BlockPos core = center.below(2);
            sl.setBlock(core, ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState(), 2);
            if (sl.getBlockState(core.above()).isAir()) {
                sl.setBlock(core.above(), ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState(), 2);
            }
            for (int i = 0; i < 4; i++) {
                BlockPos cl = center.offset(this.random.nextInt(5) - 2, -1, this.random.nextInt(5) - 2);
                if (sl.getBlockState(cl).isAir() && !sl.getBlockState(cl.below()).isAir()) {
                    sl.setBlock(cl, ModBlocks.PURE_RADIATION_BLOCK.defaultBlockState(), 2);
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
