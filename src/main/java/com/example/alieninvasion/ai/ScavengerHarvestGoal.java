package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienGruntEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

// What daytime "workers" actually DO: they prospect for natural resources (wood,
// stone, ores, dirt), mine the nearest one with a cracking animation, and pick the
// haul up into their hands. The existing ScavengerDeliverLootGoal then carries it
// off to a stash - so the swarm visibly gathers materials during the day instead
// of standing around.
public class ScavengerHarvestGoal extends Goal {
    private final AlienGruntEntity grunt;
    private BlockPos targetBlock;
    private int searchCooldown;
    private int breakTime;
    private int lastProgress = -1;
    private static final int BREAK_SPEED = 45;

    public ScavengerHarvestGoal(AlienGruntEntity grunt) {
        this.grunt = grunt;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.grunt.isScavenger()) {
            return false;
        }
        if (!this.grunt.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            return false; // hands full - go deliver it first
        }
        if (this.searchCooldown-- > 0) {
            return false;
        }
        this.searchCooldown = 30;
        this.targetBlock = findResource();
        return this.targetBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetBlock != null && this.grunt.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()
                && isHarvestable(this.targetBlock);
    }

    @Override
    public void start() {
        this.breakTime = 0;
        this.lastProgress = -1;
        if (this.targetBlock != null) {
            this.grunt.getNavigation().moveTo(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(),
                    this.targetBlock.getZ() + 0.5D, 1.1D);
        }
    }

    @Override
    public void stop() {
        if (this.targetBlock != null) {
            this.grunt.level().destroyBlockProgress(this.grunt.getId(), this.targetBlock, -1);
        }
        this.grunt.getNavigation().stop();
        this.targetBlock = null;
        this.breakTime = 0;
        this.lastProgress = -1;
    }

    @Override
    public void tick() {
        if (this.targetBlock == null) {
            return;
        }
        this.grunt.getLookControl().setLookAt(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 0.5D,
                this.targetBlock.getZ() + 0.5D);

        double distSqr = this.grunt.distanceToSqr(this.targetBlock.getX() + 0.5D, this.targetBlock.getY() + 0.5D,
                this.targetBlock.getZ() + 0.5D);
        if (distSqr > 6.25D) {
            if (this.grunt.getNavigation().isDone()) {
                this.grunt.getNavigation().moveTo(this.targetBlock.getX() + 0.5D, this.targetBlock.getY(),
                        this.targetBlock.getZ() + 0.5D, 1.1D);
            }
            return;
        }

        this.breakTime++;
        if (this.breakTime % 6 == 0) {
            this.grunt.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.grunt.level().playSound(null, this.targetBlock, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        int progress = (int) ((float) this.breakTime / (float) BREAK_SPEED * 10.0F);
        if (progress != this.lastProgress) {
            this.grunt.level().destroyBlockProgress(this.grunt.getId(), this.targetBlock, progress);
            this.lastProgress = progress;
        }

        if (this.breakTime >= BREAK_SPEED) {
            BlockState state = this.grunt.level().getBlockState(this.targetBlock);
            int id = Block.getId(state);
            ItemStack haul = new ItemStack(state.getBlock());
            if (haul.isEmpty()) {
                haul = new ItemStack(Items.COBBLESTONE);
            }
            this.grunt.level().destroyBlock(this.targetBlock, false);
            this.grunt.level().levelEvent(2001, this.targetBlock, id);
            // Pick the haul up so the deliver goal can stash it.
            this.grunt.setItemSlot(EquipmentSlot.MAINHAND, haul);
            this.targetBlock = null;
            this.breakTime = 0;
            this.lastProgress = -1;
        }
    }

    private BlockPos findResource() {
        BlockPos origin = this.grunt.blockPosition();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = 10;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (isHarvestable(m)) {
                        double d = origin.distSqr(m);
                        if (d < bestDist) {
                            bestDist = d;
                            best = m.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    // Only natural materials - the workers mine the land, not the player's builds.
    private boolean isHarvestable(BlockPos pos) {
        BlockState state = this.grunt.level().getBlockState(pos);
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)) {
            return true;
        }
        return state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.COAL_ORE) || state.is(Blocks.IRON_ORE)
                || state.is(Blocks.COPPER_ORE);
    }
}
