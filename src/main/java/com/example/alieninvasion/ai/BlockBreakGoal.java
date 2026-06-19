package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;
import java.util.function.Predicate;

// Siege digging: when the mob has a target it cannot reach (walled in, buried, or
// the player is perched somewhere it can't path to) it mines whatever block is in
// the way, tunnelling horizontally toward the target and straight up when the
// target is overhead. This is what lets the swarm "dig to get to you" instead of
// piling up against a wall.
public class BlockBreakGoal extends Goal {
    private static final int MODE_DIG = 0;
    private static final int MODE_UNSTICK = 1;
    // How long a single unstick burst runs before the goal yields and re-evaluates.
    private static final int UNSTICK_MAX_TICKS = 30;
    // Anti-freeze: if the mob has been chewing on the CURRENT block this long without
    // ever destroying one (unreachable / can't make progress), bail so it re-paths
    // instead of standing locked in one spot forever. Reset every time a block breaks.
    private static final int MAX_STALL_TICKS = 200;

    protected final PathfinderMob mob;
    protected BlockPos blockPos = BlockPos.ZERO;
    protected int breakTime;
    protected int lastBreakProgress = -1;
    protected final Predicate<BlockState> validBlockPredicate;
    protected final int breakSpeed; // lower is faster (ticks to break one block)
    private int mode = MODE_DIG;
    private int unstickTicks;
    private int stallTicks;

    // Совместная ломка: прогресс по позиции блока ОБЩИЙ, чтобы несколько мобов на одном
    // блоке складывали усилия и ломали его БЫСТРЕЕ. Чистится при ломке/забрасывании.
    private static final class Shared { float progress; long lastTick; }
    private static final java.util.Map<Long, Shared> SHARED_BREAK = new java.util.concurrent.ConcurrentHashMap<>();

    public BlockBreakGoal(PathfinderMob mob, Predicate<BlockState> validBlockPredicate, int breakSpeed) {
        this.mob = mob;
        this.validBlockPredicate = validBlockPredicate;
        this.breakSpeed = breakSpeed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }

        double distSqr = this.mob.distanceToSqr(target);
        // Don't dig if we're already on top of the target, and don't tunnel across
        // the whole map - only siege when the target is reasonably close.
        if (distSqr < 4.0D || distSqr > 256.0D) {
            return false;
        }

        boolean targetAbove = target.getY() > this.mob.getY() + 1.5D;
        // Only kick in when the mob is actually stuck (pressed against terrain) or
        // when the target is above us with something overhead to chew through.
        if (!this.mob.horizontalCollision && !targetAbove) {
            return false;
        }

        if (findBlockToBreak(target)) {
            this.mode = MODE_DIG;
            return true;
        }
        // It WANTS to reach the target but everything in the way is unbreakable
        // (obsidian / bedrock / a protected block). Without a fallback the mob just
        // stands frozen pushing into the wall - so flip to an unstick maneuver that
        // hops and sidesteps instead of locking up.
        if (this.mob.horizontalCollision) {
            this.mode = MODE_UNSTICK;
            return true;
        }
        return false;
    }

    protected boolean findBlockToBreak(LivingEntity target) {
        BlockPos feet = this.mob.blockPosition();
        Direction dir = horizontalDirectionTo(target);
        boolean targetAbove = target.getY() > this.mob.getY() + 1.5D;
        double horizSqr = horizontalDistSqr(target);
        int headOffset = Mth.ceil(this.mob.getBbHeight()); // block index just above the head

        // If the target is more or less straight overhead, dig the ceiling out.
        if (targetAbove && horizSqr < 4.0D) {
            BlockPos above = feet.above(headOffset);
            if (isValidBlock(above)) {
                this.blockPos = above;
                return true;
            }
        }

        // Otherwise tunnel toward the target: clear the body-level block first so we
        // can step in, then the head-level block.
        BlockPos feetFront = feet.relative(dir);
        if (isValidBlock(feetFront)) {
            this.blockPos = feetFront;
            return true;
        }
        BlockPos headFront = feetFront.above();
        if (isValidBlock(headFront)) {
            this.blockPos = headFront;
            return true;
        }

        // Target is up and the way forward is clear: make a staircase by removing
        // the block above-and-forward.
        if (targetAbove) {
            BlockPos stepUp = feet.above(headOffset).relative(dir);
            if (isValidBlock(stepUp)) {
                this.blockPos = stepUp;
                return true;
            }
            BlockPos straightUp = feet.above(headOffset);
            if (isValidBlock(straightUp)) {
                this.blockPos = straightUp;
                return true;
            }
        }

        return false;
    }

    private Direction horizontalDirectionTo(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        if (Math.abs(dx) < 1.0E-4 && Math.abs(dz) < 1.0E-4) {
            return this.mob.getDirection();
        }
        return Direction.getNearest(dx, 0.0, dz);
    }

    private double horizontalDistSqr(LivingEntity target) {
        double dx = target.getX() - this.mob.getX();
        double dz = target.getZ() - this.mob.getZ();
        return dx * dx + dz * dz;
    }

    protected boolean isValidBlock(BlockPos pos) {
        BlockState state = this.mob.level().getBlockState(pos);
        if (state.isAir() || !state.blocksMotion()) {
            return false;
        }
        // Never chew unbreakable blocks (bedrock returns -1 destroy speed).
        if (state.getDestroySpeed(this.mob.level(), pos) < 0) {
            return false;
        }
        return this.validBlockPredicate.test(state);
    }

    @Override
    public void start() {
        this.breakTime = 0;
        this.lastBreakProgress = -1;
        this.unstickTicks = 0;
        this.stallTicks = 0;
        if (this.mode == MODE_DIG) {
            // Face and walk into the block we're mining so the body keeps pushing
            // through as the tunnel opens up.
            this.mob.getLookControl().setLookAt(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D,
                    this.blockPos.getZ() + 0.5D);
            this.mob.getNavigation().moveTo(this.blockPos.getX() + 0.5D, this.mob.getY(), this.blockPos.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.breakTime = 0;
        this.mob.level().destroyBlockProgress(this.mob.getId(), this.blockPos, -1);
        this.mob.getNavigation().stop();
        this.mode = MODE_DIG;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.mob.getTarget() == null) {
            return false;
        }
        if (this.mode == MODE_UNSTICK) {
            // Short burst only, then yield so the mob can re-path or re-evaluate.
            return this.unstickTicks < UNSTICK_MAX_TICKS;
        }
        return this.stallTicks < MAX_STALL_TICKS
                && this.mob.level().getBlockState(this.blockPos).blocksMotion()
                && isValidBlock(this.blockPos)
                && this.mob.distanceToSqr(this.blockPos.getX() + 0.5, this.blockPos.getY() + 0.5,
                        this.blockPos.getZ() + 0.5) < 9.0;
    }

    @Override
    public void tick() {
        if (this.mode == MODE_UNSTICK) {
            tickUnstick();
            return;
        }

        this.mob.getLookControl().setLookAt(this.blockPos.getX() + 0.5D, this.blockPos.getY() + 0.5D,
                this.blockPos.getZ() + 0.5D);

        // (Removed the loud 1019 door-bang spam; the cracking animation + the final
        // break sound below are enough feedback and far less annoying.)

        // Keep walking into the opening so the body advances as the tunnel clears,
        // and count stall time toward the give-up cap (reset whenever a block falls).
        this.stallTicks++;
        if (this.stallTicks % 20 == 0) {
            this.mob.getNavigation().moveTo(this.blockPos.getX() + 0.5D, this.mob.getY(),
                    this.blockPos.getZ() + 0.5D, 1.0D);
        }

        // Вклад ЭТОГО моба идёт в ОБЩИЙ прогресс блока — двое и более ломают вдвое-втрое быстрее.
        long key = this.blockPos.asLong();
        long now = this.mob.level().getGameTime();
        Shared sh = SHARED_BREAK.computeIfAbsent(key, k -> new Shared());
        if (now - sh.lastTick > 4L) {
            sh.progress = 0.0F; // блок забросили — прогресс начинается заново
        }
        sh.lastTick = now;
        sh.progress += 1.0F / (float) this.breakSpeed;
        int i = Math.min(9, (int) (sh.progress * 10.0F));
        if (i != this.lastBreakProgress) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.blockPos, i);
            this.lastBreakProgress = i;
        }

        if (sh.progress >= 1.0F) {
            // Event 2001 = собственный звук/частицы блока (без визга «зомби ломает дверь»).
            int blockId = Block.getId(this.mob.level().getBlockState(this.blockPos));
            this.mob.level().destroyBlock(this.blockPos, true);
            this.mob.level().levelEvent(2001, this.blockPos, blockId);
            SHARED_BREAK.remove(key);
            if (SHARED_BREAK.size() > 256) {
                SHARED_BREAK.values().removeIf(s -> now - s.lastTick > 40L); // чистим заброшенные
            }
            this.breakTime = 0;
            this.lastBreakProgress = -1;
            this.stallTicks = 0; // progress! it broke something — not stuck.
        }
    }

    // Anti-freeze: the mob wants the target but the way is unbreakable. Hop, try to
    // path around the obstacle, and give a small sideways nudge so it never just
    // stands locked against the wall.
    private void tickUnstick() {
        this.unstickTicks++;
        LivingEntity target = this.mob.getTarget();
        if (target != null) {
            this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        this.mob.getJumpControl().jump();

        Direction toward = target != null ? horizontalDirectionTo(target) : this.mob.getDirection();
        Direction side = this.mob.getRandom().nextBoolean() ? toward.getClockWise() : toward.getCounterClockWise();

        if (this.unstickTicks % 8 == 1) {
            BlockPos around = this.mob.blockPosition().relative(side, 2).relative(toward, 1);
            this.mob.getNavigation().moveTo(around.getX() + 0.5D, around.getY(), around.getZ() + 0.5D, 1.0D);
        }
        if (this.unstickTicks % 4 == 0) {
            this.mob.setDeltaMovement(this.mob.getDeltaMovement()
                    .add(side.getStepX() * 0.12D, 0.0D, side.getStepZ() * 0.12D));
            this.mob.hurtMarked = true;
        }
    }
}
