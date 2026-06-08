package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import java.util.EnumSet;

public class BuildHiveGoal extends Goal {
    private final Mob mob;
    private int buildTimer;

    public BuildHiveGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Build if no target and random chance
        return this.mob.getTarget() == null && this.mob.getRandom().nextInt(200) == 0;
    }

    @Override
    public void start() {
        this.buildTimer = 0;
    }

    @Override
    public void tick() {
        this.buildTimer++;
        if (this.buildTimer >= 20) {
            BlockPos pos = this.mob.blockPosition();
            if (this.mob.level().getBlockState(pos).isAir() &&
                    this.mob.level().getBlockState(pos.below()).isCollisionShapeFullBlock(this.mob.level(),
                            pos.below())) {

                // Place Hive Block
                this.mob.level().setBlockAndUpdate(pos,
                        com.example.alieninvasion.registry.ModBlocks.ALIEN_HIVE.defaultBlockState());
                this.mob.playSound(SoundEvents.SLIME_BLOCK_PLACE, 1.0F, 1.0F);
            }
            this.buildTimer = 0;
        }
    }
}
