package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

// Climb to an elevated, otherwise-unreachable target: the mob jumps and seals a
// block beneath itself, towering straight up. If something (a roof, a floor the
// player is standing on) is directly overhead, it mines through it first. This is
// how the swarm reaches a player perched up high with open sky between them.
public class PillarUpGoal extends Goal {
    private final Mob mob;
    private final BlockState pillarBlock;
    private double climbStartY;
    private int placeTimer;
    private BlockPos breakPos;
    private int breakTime;
    private int lastBreakProgress = -1;
    private static final int BREAK_SPEED = 30; // ticks to chew one overhead block

    public PillarUpGoal(Mob mob) {
        // Aliens tower up with their own residue by default.
        this(mob, com.example.alieninvasion.registry.ModBlocks.ALIEN_RESIDUE.defaultBlockState());
    }

    public PillarUpGoal(Mob mob, BlockState pillarBlock) {
        this.mob = mob;
        this.pillarBlock = pillarBlock;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (!this.mob.onGround()) {
            return false;
        }
        double yDiff = target.getY() - this.mob.getY();
        // Target must be clearly above (higher than a single step) and roughly
        // overhead, so we tower up instead of trying to walk around.
        if (yDiff <= 1.5D) {
            return false;
        }
        return horizontalDistSqr(target) < 9.0D; // within ~3 blocks horizontally
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target.getY() <= this.mob.getY() + 0.5D) {
            return false; // reached their level - let melee/leap finish it
        }
        if (this.mob.getY() - this.climbStartY > 30.0D) {
            return false; // safety cap so it never towers to the build limit
        }
        return horizontalDistSqr(target) < 16.0D;
    }

    @Override
    public void start() {
        this.climbStartY = this.mob.getY();
        this.placeTimer = 0;
        this.breakPos = null;
        this.breakTime = 0;
        this.lastBreakProgress = -1;
    }

    @Override
    public void stop() {
        if (this.breakPos != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.breakPos, -1);
        }
        this.breakPos = null;
    }

    @Override
    public void tick() {
        // Keep the mob from wandering off the column.
        this.mob.getNavigation().stop();

        BlockPos feet = this.mob.blockPosition();
        BlockPos overhead = feet.above(Mth.ceil(this.mob.getBbHeight())); // block just above the head

        // 1. Punch through anything directly overhead before trying to rise.
        if (isBreakable(overhead)) {
            breakOverhead(overhead);
            return;
        }
        // Finished breaking / nothing to break: clear any leftover crack overlay.
        if (this.breakPos != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.breakPos, -1);
            this.breakPos = null;
            this.breakTime = 0;
            this.lastBreakProgress = -1;
        }

        // 2. Jump and seal a block beneath us once we've risen above the current cell.
        this.mob.getJumpControl().jump();
        if (this.mob.getY() > feet.getY() + 0.5D) {
            BlockState state = this.mob.level().getBlockState(feet);
            if (state.isAir() || state.canBeReplaced()) {
                this.mob.level().setBlockAndUpdate(feet, this.pillarBlock);
                this.mob.playSound(SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 0.8F);
            }
        }
    }

    private void breakOverhead(BlockPos pos) {
        if (!pos.equals(this.breakPos)) {
            if (this.breakPos != null) {
                this.mob.level().destroyBlockProgress(this.mob.getId(), this.breakPos, -1);
            }
            this.breakPos = pos;
            this.breakTime = 0;
            this.lastBreakProgress = -1;
        }

        this.breakTime++;
        int progress = (int) ((float) this.breakTime / (float) BREAK_SPEED * 10.0F);
        if (progress != this.lastBreakProgress) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), pos, progress);
            this.lastBreakProgress = progress;
        }
        if (this.breakTime % 8 == 0) {
            this.mob.level().levelEvent(1019, pos, 0); // banging sound
        }

        if (this.breakTime >= BREAK_SPEED) {
            int blockId = Block.getId(this.mob.level().getBlockState(pos));
            this.mob.level().destroyBlock(pos, true);
            this.mob.level().levelEvent(2001, pos, blockId);
            this.breakPos = null;
            this.breakTime = 0;
            this.lastBreakProgress = -1;
        }
    }

    private boolean isBreakable(BlockPos pos) {
        BlockState state = this.mob.level().getBlockState(pos);
        if (state.isAir() || !state.blocksMotion()) {
            return false;
        }
        // Skip unbreakable blocks (bedrock etc. report a negative destroy speed).
        return state.getDestroySpeed(this.mob.level(), pos) >= 0;
    }

    private double horizontalDistSqr(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return dx * dx + dz * dz;
    }
}
