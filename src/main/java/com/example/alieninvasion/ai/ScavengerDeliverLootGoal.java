package com.example.alieninvasion.ai;

import com.example.alieninvasion.block.AlienStashBlockEntity;
import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.EnumSet;

public class ScavengerDeliverLootGoal extends Goal {
    private final AlienGruntEntity grunt;
    private final double speedModifier;
    private BlockPos targetStashPos;
    private int searchCooldown;
    private int navigateCooldown;

    public ScavengerDeliverLootGoal(AlienGruntEntity grunt, double speedModifier) {
        this.grunt = grunt;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.grunt.isScavenger() && !this.grunt.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void start() {
        this.targetStashPos = null;
        this.searchCooldown = 0;
        this.navigateCooldown = 0;
    }

    @Override
    public void stop() {
        this.grunt.getNavigation().stop();
        this.targetStashPos = null;
    }

    @Override
    public void tick() {
        if (this.targetStashPos == null) {
            if (this.searchCooldown-- <= 0) {
                this.searchCooldown = 40; // Search every 2 seconds
                this.targetStashPos = findNearestStash(24);
                if (this.targetStashPos == null) {
                    // Place a stash dynamically
                    BlockPos buildPos = findSuitableBuildPos();
                    if (buildPos != null) {
                        this.grunt.level().setBlockAndUpdate(buildPos, ModBlocks.ALIEN_STASH.defaultBlockState());
                        this.grunt.level().playSound(null, buildPos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                        this.targetStashPos = buildPos;
                    }
                }
            }
        }

        if (this.targetStashPos != null) {
            // Check if target block is still a stash
            if (!this.grunt.level().getBlockState(this.targetStashPos).is(ModBlocks.ALIEN_STASH)) {
                this.targetStashPos = null;
                return;
            }

            double distSqr = this.grunt.distanceToSqr(this.targetStashPos.getX() + 0.5D, this.targetStashPos.getY() + 0.5D, this.targetStashPos.getZ() + 0.5D);
            if (distSqr <= 4.0D) { // Within 2 blocks
                BlockEntity be = this.grunt.level().getBlockEntity(this.targetStashPos);
                if (be instanceof AlienStashBlockEntity stash) {
                    ItemStack loot = this.grunt.getItemBySlot(EquipmentSlot.MAINHAND);
                    if (stash.depositItem(loot)) {
                        this.grunt.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                        this.grunt.level().playSound(null, this.targetStashPos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 1.0F, 1.2F);
                        
                        // Particle effects
                        if (this.grunt.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.PORTAL, 
                                this.targetStashPos.getX() + 0.5D, 
                                this.targetStashPos.getY() + 1.0D, 
                                this.targetStashPos.getZ() + 0.5D, 
                                15, 0.2, 0.2, 0.2, 0.1);
                        }
                    } else {
                        // Stash full: drop on ground
                        this.grunt.spawnAtLocation(loot);
                        this.grunt.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    }
                } else {
                    this.targetStashPos = null;
                }
            } else {
                if (this.navigateCooldown-- <= 0) {
                    this.navigateCooldown = 20;
                    this.grunt.getNavigation().moveTo(this.targetStashPos.getX() + 0.5D, this.targetStashPos.getY(), this.targetStashPos.getZ() + 0.5D, this.speedModifier);
                }
            }
        }
    }

    private BlockPos findNearestStash(int radius) {
        BlockPos center = this.grunt.blockPosition();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        double closestDist = Double.MAX_VALUE;
        BlockPos closest = null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -radius; z <= radius; z++) {
                    target.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (this.grunt.level().getBlockState(target).is(ModBlocks.ALIEN_STASH)) {
                        double dist = center.distSqr(target);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = target.immutable();
                        }
                    }
                }
            }
        }
        return closest;
    }

    private BlockPos findSuitableBuildPos() {
        BlockPos center = this.grunt.blockPosition();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 30; i++) { // try 30 random nearby points
            int x = center.getX() + this.grunt.getRandom().nextInt(11) - 5;
            int z = center.getZ() + this.grunt.getRandom().nextInt(11) - 5;
            int y = center.getY() + this.grunt.getRandom().nextInt(5) - 2;
            target.set(x, y, z);
            if (this.grunt.level().getBlockState(target).isAir() &&
                this.grunt.level().getBlockState(target.below()).isCollisionShapeFullBlock(this.grunt.level(), target.below())) {
                return target.immutable();
            }
        }
        return null;
    }
}
