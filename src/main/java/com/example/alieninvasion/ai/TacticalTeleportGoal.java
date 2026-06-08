package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.EnumSet;

public class TacticalTeleportGoal extends Goal {
    private final PathfinderMob mob;
    private LivingEntity target;
    private int cooldown;

    public TacticalTeleportGoal(PathfinderMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.target = this.mob.getTarget();
        return this.target != null && this.mob.getRandom().nextDouble() < 0.05D; // 5% chance per tick when ready
    }

    @Override
    public void start() {
        if (this.target == null)
            return;

        // Try to teleport BEHIND the player
        Vec3 localView = this.target.getViewVector(1.0F);
        // Position behind is targetPos - viewVector * distance
        Vec3 behindPos = this.target.position().subtract(localView.scale(3.0D));

        if (tryTeleportTo(behindPos.x, behindPos.y, behindPos.z)) {
            // this.mob.lookAt(org.joml.Math.clamp(0, 0, 0), 0, 0); // Invalid
            this.mob.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.position()); // Snap
                                                                                                                   // look
                                                                                                                   // to
                                                                                                                   // target
            // target
            this.cooldown = 100; // 5 seconds cooldown
        } else {
            this.cooldown = 20; // Retry sooner if failed
        }
    }

    private boolean tryTeleportTo(double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > this.mob.level().getMinBuildHeight()
                && !this.mob.level().getBlockState(pos).blocksMotion()) {
            pos.move(net.minecraft.core.Direction.DOWN);
        }

        BlockState blockState = this.mob.level().getBlockState(pos);
        if (blockState.blocksMotion()) {
            boolean success = this.mob.randomTeleport(x, pos.getY() + 1, z, true);
            if (success) {
                this.mob.level().playSound(null, this.mob.xo, this.mob.yo, this.mob.zo,
                        net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, this.mob.getSoundSource(), 1.0F, 1.0F);
                this.mob.playSound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
            }
            return success;
        }
        return false;
    }
}
