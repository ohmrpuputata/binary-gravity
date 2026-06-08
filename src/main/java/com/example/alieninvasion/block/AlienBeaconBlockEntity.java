package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienChickenEntity;
import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AlienBeaconBlockEntity extends BlockEntity {
    private int timer = 0;

    public AlienBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ALIEN_BEACON_BLOCK_ENTITY, pos, state);
    }

    public void tickServer() {
        if (level == null || level.isClientSide) {
            return;
        }

        timer++;
        ServerLevel sl = (ServerLevel) level;
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 0.5D;
        double z = worldPosition.getZ() + 0.5D;

        // Visual charge effects: particles shoot up
        for (int i = 0; i < 15; i++) {
            sl.sendParticles(ParticleTypes.END_ROD, x, y + i * 2.0D, z, 3, 0.1, 0.5, 0.1, 0.05);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + i * 2.0D, z, 2, 0.2, 0.5, 0.2, 0.01);
        }

        // Sound effects
        if (timer % 20 == 0) {
            level.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.5F, 0.5F + (timer / 200.0F));
            level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.BLOCKS, 1.0F, 0.5F + (timer / 200.0F));
        }

        // Spawn defensive wave
        if (timer == 40 || timer == 120) {
            spawnDefensiveUnit();
        }

        // Orbital strike trigger
        if (timer >= 200) {
            triggerOrbitalStrike();
        }
    }

    private void spawnDefensiveUnit() {
        ServerLevel sl = (ServerLevel) level;
        BlockPos spawnPos = worldPosition.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
        
        // Spawn Grunt
        AlienGruntEntity grunt = EntityRegistry.ALIEN_GRUNT.create(sl);
        if (grunt != null) {
            grunt.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
            grunt.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            sl.addFreshEntity(grunt);
            sl.sendParticles(ParticleTypes.PORTAL, grunt.getX(), grunt.getY() + 1.0D, grunt.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
        }

        // Spawn Alien Chicken
        AlienChickenEntity chicken = EntityRegistry.ALIEN_CHICKEN.create(sl);
        if (chicken != null) {
            chicken.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D, 0.0F, 0.0F);
            chicken.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null);
            sl.addFreshEntity(chicken);
        }
    }

    private void triggerOrbitalStrike() {
        ServerLevel sl = (ServerLevel) level;
        BlockPos pos = worldPosition;

        // Summon lightning bolt for visual/sound drama
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            sl.addFreshEntity(bolt);
        }

        // Carve 3x3 laser shaft down to bedrock
        for (int dy = 30; dy >= -64 - pos.getY(); dy--) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos targetPos = pos.offset(dx, dy, dz);
                    BlockState state = sl.getBlockState(targetPos);
                    if (state.getDestroySpeed(sl, targetPos) >= 0) { // Keep bedrock intact
                        if (dx == 0 && dz == 0) {
                            sl.setBlockAndUpdate(targetPos, Blocks.LAVA.defaultBlockState());
                        } else {
                            sl.setBlockAndUpdate(targetPos, sl.random.nextBoolean() ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.FIRE.defaultBlockState());
                        }
                    }
                }
            }
        }

        // Cylinder zone damage
        AABB damageZone = new AABB(pos.getX() - 3, -64, pos.getZ() - 3, pos.getX() + 4, pos.getY() + 40, pos.getZ() + 4);
        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, damageZone)) {
            entity.hurt(sl.damageSources().magic(), 50.0F);
            entity.setRemainingFireTicks(200);
        }

        // Big explosion at the target
        sl.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 6.0F, true, Level.ExplosionInteraction.BLOCK);

        // Remove beacon block
        sl.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.timer = tag.getInt("Timer");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Timer", this.timer);
    }
}
