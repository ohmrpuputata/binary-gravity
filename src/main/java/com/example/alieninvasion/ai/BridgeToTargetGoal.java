package com.example.alieninvasion.ai;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// Combat bridge builder: siege mobs used to tower upward or chew blocks, but they
// had no goal for horizontal gaps. This goal lays alien residue in front of the
// mob when the target is across air, water, lava or a cliff, then walks onto it.
public class BridgeToTargetGoal extends Goal {
    // Hard cap so a single bridging session can never tile the world with residue.
    private static final int MAX_BRIDGE_BLOCKS = 24;

    private final PathfinderMob mob;
    private final double speedModifier;
    private int placeCooldown;
    private int repathCooldown;
    private int pathCheckCooldown;
    private boolean targetUnreachable;
    private int placed;
    private BlockPos currentStandPos;

    public BridgeToTargetGoal(PathfinderMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive() || !this.mob.onGround()) {
            return false;
        }
        double distSqr = this.mob.distanceToSqr(target);
        // 2..24 blocks: don't bridge from across the map.
        if (distSqr < 4.0D || distSqr > 576.0D) {
            return false;
        }
        if (target.getY() > this.mob.getY() + 8.0D || target.getY() < this.mob.getY() - 6.0D) {
            return false;
        }
        // KEY ANTI-SPAM GATE: only lay a bridge when normal pathfinding genuinely
        // cannot reach the target. If the mob can just walk/jump there, it never
        // places residue. (Previously it bridged over any little dip it saw.)
        if (!isTargetUnreachable(target)) {
            return false;
        }
        return findBridgeBlock(target) != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.placed < MAX_BRIDGE_BLOCKS && canUse();
    }

    // Recomputed at most every 20 ticks (cheap enough, and bridging is rare): asks
    // the navigator for a path to the target and treats "no path / can't reach" as
    // the trigger for bridging.
    private boolean isTargetUnreachable(LivingEntity target) {
        if (this.pathCheckCooldown-- <= 0) {
            this.pathCheckCooldown = 20;
            net.minecraft.world.level.pathfinder.Path path = this.mob.getNavigation().createPath(target, 0);
            this.targetUnreachable = path == null || !path.canReach();
        }
        return this.targetUnreachable;
    }

    @Override
    public void start() {
        this.placeCooldown = 0;
        this.repathCooldown = 0;
        this.placed = 0;
        this.currentStandPos = null;
    }

    @Override
    public void stop() {
        this.currentStandPos = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        BlockPos bridgeBlock = findBridgeBlock(target);
        if (bridgeBlock == null) {
            return;
        }

        if (this.placeCooldown-- <= 0 && this.placed < MAX_BRIDGE_BLOCKS && canPlaceBridgeBlock(bridgeBlock)) {
            this.mob.level().setBlockAndUpdate(bridgeBlock, ModBlocks.ALIEN_RESIDUE.defaultBlockState());
            this.mob.level().playSound(null, bridgeBlock, SoundEvents.SLIME_BLOCK_PLACE,
                    SoundSource.BLOCKS, 0.9F, 0.75F + this.mob.getRandom().nextFloat() * 0.25F);
            this.currentStandPos = bridgeBlock.above();
            this.placed++;
            this.placeCooldown = 8;
            this.repathCooldown = 0;
        }

        BlockPos standPos = this.currentStandPos != null ? this.currentStandPos : bridgeBlock.above();
        if (this.repathCooldown-- <= 0) {
            this.repathCooldown = 10;
            this.mob.getNavigation().moveTo(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D,
                    this.speedModifier);
        }
        if (this.mob.blockPosition().distSqr(standPos) <= 2.0D) {
            this.currentStandPos = null;
        }
    }

    private BlockPos findBridgeBlock(LivingEntity target) {
        BlockPos feet = this.mob.blockPosition();
        Direction dir = horizontalDirectionTo(target);

        BlockPos frontFeet = feet.relative(dir);
        BlockPos frontFloor = frontFeet.below();
        if (isOpenForBody(frontFeet) && isRealGap(frontFloor) && canPlaceBridgeBlock(frontFloor)) {
            return frontFloor;
        }

        // If the mob is looking at a slightly higher player over a gap, place the
        // first stair block at foot level so vanilla pathing can step up next.
        if (target.getY() > this.mob.getY() + 0.75D && isOpenForBody(frontFeet.above())
                && needsBridge(frontFeet) && canPlaceBridgeBlock(frontFeet)) {
            return frontFeet;
        }

        // Look one more cell ahead so a mob at the edge can keep extending a bridge
        // even before it has fully stepped onto the last placed block.
        BlockPos secondFeet = frontFeet.relative(dir);
        BlockPos secondFloor = secondFeet.below();
        if (isOpenForBody(frontFeet) && isOpenForBody(secondFeet)
                && !needsBridge(frontFloor) && isRealGap(secondFloor)
                && canPlaceBridgeBlock(secondFloor)) {
            return secondFloor;
        }

        return null;
    }

    // A genuine gap worth bridging: the floor cell is passable AND either the cell
    // below it is ALSO passable (a real >=2 drop / chasm) or it's a liquid. This
    // stops the mob from paving over shallow 1-block dips it could just step across.
    private boolean isRealGap(BlockPos floor) {
        if (!needsBridge(floor)) {
            return false;
        }
        BlockState state = this.mob.level().getBlockState(floor);
        boolean liquid = state.is(Blocks.WATER) || state.is(Blocks.LAVA) || !state.getFluidState().isEmpty();
        return liquid || needsBridge(floor.below());
    }

    private Direction horizontalDirectionTo(LivingEntity target) {
        Vec3 toTarget = target.position().subtract(this.mob.position());
        if (toTarget.horizontalDistanceSqr() < 1.0E-4D) {
            return this.mob.getDirection();
        }
        return Direction.getNearest(toTarget.x, 0.0D, toTarget.z);
    }

    private boolean needsBridge(BlockPos pos) {
        if (this.mob.level().isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = this.mob.level().getBlockState(pos);
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA) || !state.getFluidState().isEmpty()
                || !state.blocksMotion();
    }

    private boolean canPlaceBridgeBlock(BlockPos pos) {
        if (this.mob.level().isOutsideBuildHeight(pos)) {
            return false;
        }
        BlockState state = this.mob.level().getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.WATER) || state.is(Blocks.LAVA)
                || !state.getFluidState().isEmpty();
    }

    private boolean isOpenForBody(BlockPos feet) {
        if (this.mob.level().isOutsideBuildHeight(feet) || this.mob.level().isOutsideBuildHeight(feet.above())) {
            return false;
        }
        BlockState feetState = this.mob.level().getBlockState(feet);
        BlockState headState = this.mob.level().getBlockState(feet.above());
        return !feetState.blocksMotion() && !headState.blocksMotion();
    }
}
