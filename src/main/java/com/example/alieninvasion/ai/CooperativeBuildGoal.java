package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

// When idle, the swarm cooperatively raises a small infested hut around a hive
// core: a 5x5 footprint with 3-tall infested-stone walls, a doorway and a roof,
// with the glowing hive block as its heart. Several aliens converge on the same
// core and each places the next missing block, so the structure goes up fast and
// always looks like an actual little building.
public class CooperativeBuildGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private BlockPos centerPos;
    private BlockPos targetBuildPos;
    private int scanCooldown;
    private int buildCooldown;
    private int navigateCooldown;

    // House footprint radius (5x5) and wall height.
    private static final int RADIUS = 2;
    private static final int WALL_HEIGHT = 3;
    // Doorway faces +X (East): the wall cell at (dx=RADIUS, dz=0).
    private static final int DOOR_DX = RADIUS;
    private static final int DOOR_DZ = 0;

    public CooperativeBuildGoal(Mob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }

        // Periodic check for an existing hive/stash core to build around.
        if (this.scanCooldown-- <= 0) {
            this.scanCooldown = 100; // scan every 5 seconds
            this.centerPos = findNearestOutpostCenter(24);

            // If the nearest hut is already finished, treat it as "no work here" so
            // we look to expand the settlement with a fresh core elsewhere.
            if (this.centerPos != null && nextBuildTarget(this.centerPos) == null) {
                this.centerPos = null;
            }

            if (this.centerPos == null) {
                // Try to establish a new hive core if we have allies nearby and we're
                // standing somewhere a hut can actually be built.
                if (this.mob.getRandom().nextInt(100) == 0) {
                    List<Mob> allies = this.mob.level().getEntitiesOfClass(Mob.class,
                            this.mob.getBoundingBox().inflate(10.0D),
                            e -> e != this.mob && AlienUtils.isAlliedTo(this.mob, e));

                    if (!allies.isEmpty()) {
                        BlockPos feet = this.mob.blockPosition();
                        if (this.mob.level().getBlockState(feet).isAir() &&
                                this.mob.level().getBlockState(feet.below())
                                        .isCollisionShapeFullBlock(this.mob.level(), feet.below())) {

                            this.mob.level().setBlockAndUpdate(feet, ModBlocks.ALIEN_HIVE.defaultBlockState());
                            this.mob.playSound(SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 1.0F);
                            this.centerPos = feet;
                        }
                    }
                }
            }
        }

        return this.centerPos != null;
    }

    @Override
    public void start() {
        this.targetBuildPos = null;
        this.buildCooldown = 0;
        this.navigateCooldown = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        this.targetBuildPos = null;
    }

    @Override
    public void tick() {
        // Bridge small chasms while moving so builders don't get stranded.
        checkAndBuildBridge();

        // Once the hut is finished and a few of us have gathered, switch from
        // building to hunting: pour out of the door and rush the nearest player.
        List<Mob> allies = this.mob.level().getEntitiesOfClass(Mob.class,
                this.mob.getBoundingBox().inflate(12.0D),
                e -> e != this.mob && AlienUtils.isAlliedTo(this.mob, e));

        if (allies.size() >= 2 && this.centerPos != null && nextBuildTarget(this.centerPos) == null) {
            net.minecraft.world.entity.player.Player nearestPlayer = this.mob.level().getNearestPlayer(this.mob, 48.0D);
            if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
                this.mob.setTarget(nearestPlayer);
                for (Mob ally : allies) {
                    ally.setTarget(nearestPlayer);
                }
                this.centerPos = null;
                this.targetBuildPos = null;
                return;
            }
        }

        if (this.centerPos == null) {
            return;
        }

        // The core must still be there, or we abandon the site.
        BlockState centerState = this.mob.level().getBlockState(this.centerPos);
        if (!centerState.is(ModBlocks.ALIEN_HIVE) && !centerState.is(ModBlocks.ALIEN_STASH)) {
            this.centerPos = null;
            this.targetBuildPos = null;
            return;
        }

        // Place the next missing block of the hut.
        if (this.buildCooldown-- <= 0) {
            if (this.targetBuildPos == null) {
                this.targetBuildPos = nextBuildTarget(this.centerPos);
            }

            if (this.targetBuildPos != null) {
                double distSqr = this.mob.distanceToSqr(this.targetBuildPos.getX() + 0.5D,
                        this.targetBuildPos.getY() + 0.5D, this.targetBuildPos.getZ() + 0.5D);
                if (distSqr <= 9.0D) { // within 3 blocks
                    // Re-check it's still placeable (another builder may have beaten us).
                    if (isPlaceable(this.targetBuildPos)) {
                        this.mob.level().setBlockAndUpdate(this.targetBuildPos,
                                ModBlocks.INFESTED_STONE.defaultBlockState());
                        this.mob.level().playSound(null, this.targetBuildPos, SoundEvents.STONE_PLACE,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    this.targetBuildPos = null;
                    this.buildCooldown = 30; // ~1.5s between blocks per builder
                } else {
                    if (this.navigateCooldown-- <= 0) {
                        this.navigateCooldown = 20;
                        this.mob.getNavigation().moveTo(this.targetBuildPos.getX() + 0.5D,
                                this.targetBuildPos.getY(), this.targetBuildPos.getZ() + 0.5D, this.speedModifier);
                    }
                }
            } else {
                // Hut complete: occasionally infest the floor for flavour.
                if (this.mob.getRandom().nextInt(100) == 0) {
                    spreadResidue();
                }
                this.buildCooldown = 40;
            }
        }
    }

    // Returns the next hut block that still needs placing (walls bottom-up, then
    // roof), or null when the building is finished.
    private BlockPos nextBuildTarget(BlockPos center) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        // Walls: perimeter of the 5x5, WALL_HEIGHT tall, with a 2-block doorway.
        for (int h = 0; h < WALL_HEIGHT; h++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != RADIUS) {
                        continue; // interior, not a wall
                    }
                    // Leave the lower two blocks of the door cell open.
                    if (dx == DOOR_DX && dz == DOOR_DZ && h < 2) {
                        continue;
                    }
                    p.set(center.getX() + dx, center.getY() + h, center.getZ() + dz);
                    if (isPlaceable(p)) {
                        return p.immutable();
                    }
                }
            }
        }

        // Roof: full 5x5 cap one block above the walls.
        int roofY = center.getY() + WALL_HEIGHT;
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                p.set(center.getX() + dx, roofY, center.getZ() + dz);
                if (isPlaceable(p)) {
                    return p.immutable();
                }
            }
        }

        return null;
    }

    // A cell can be built into if it's empty (air / grass / replaceable) or old
    // residue, and isn't the hive core itself.
    private boolean isPlaceable(BlockPos pos) {
        if (this.centerPos != null && pos.equals(this.centerPos)) {
            return false;
        }
        BlockState state = this.mob.level().getBlockState(pos);
        return state.isAir() || state.canBeReplaced() || state.is(ModBlocks.ALIEN_RESIDUE);
    }

    private void checkAndBuildBridge() {
        if (!this.mob.getNavigation().isInProgress()) {
            return;
        }
        BlockPos feetPos = this.mob.blockPosition();
        Vec3 velocity = this.mob.getDeltaMovement();
        if (velocity.horizontalDistanceSqr() < 0.001D) {
            return;
        }
        Direction dir = Direction.getNearest(velocity.x, 0.0, velocity.z);
        BlockPos aheadPos = feetPos.relative(dir);
        BlockPos belowAhead = aheadPos.below();

        if (this.mob.level().getBlockState(aheadPos).isAir() &&
                this.mob.level().getBlockState(belowAhead).isAir() &&
                this.mob.level().getBlockState(feetPos.below()).isCollisionShapeFullBlock(this.mob.level(),
                        feetPos.below())) {

            this.mob.level().setBlockAndUpdate(belowAhead, ModBlocks.ALIEN_RESIDUE.defaultBlockState());
            this.mob.playSound(SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 1.0F);
        }
    }

    private BlockPos findNearestOutpostCenter(int radius) {
        BlockPos center = this.mob.blockPosition();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        double closestDist = Double.MAX_VALUE;
        BlockPos closest = null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -radius; z <= radius; z++) {
                    target.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = this.mob.level().getBlockState(target);
                    if (state.is(ModBlocks.ALIEN_HIVE) || state.is(ModBlocks.ALIEN_STASH)) {
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

    private void spreadResidue() {
        BlockPos center = this.centerPos;
        if (center == null) {
            return;
        }
        int dx = this.mob.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS;
        int dz = this.mob.getRandom().nextInt(RADIUS * 2 + 1) - RADIUS;
        BlockPos floor = center.offset(dx, 0, dz);
        if (!floor.equals(center) && this.mob.level().getBlockState(floor).isAir() &&
                this.mob.level().getBlockState(floor.below()).isCollisionShapeFullBlock(this.mob.level(),
                        floor.below())) {
            this.mob.level().setBlockAndUpdate(floor, ModBlocks.ALIEN_RESIDUE.defaultBlockState());
        }
    }
}
