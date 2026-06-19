package com.example.alieninvasion.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * «Шахтёр с x-ray»: когда нет боя, моб сканирует руду в радиусе СКВОЗЬ СТЕНЫ (как
 * чит x-ray), идёт к ближайшей и выламывает её; дроп подбирает (если canPickUpLoot).
 * Так пещерные выжившие-NPC реально добывают ресурсы, а не просто бродят.
 */
public class MineOreGoal extends Goal {
    private final PathfinderMob mob;
    private final int radius;
    private final int breakSpeed; // тиков на блок
    private BlockPos ore;
    private int breakTime;
    private int lastProgress = -1;
    private int repathCd;

    public MineOreGoal(PathfinderMob mob, int radius, int breakSpeed) {
        this.mob = mob;
        this.radius = radius;
        this.breakSpeed = breakSpeed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false;
        }
        if (this.mob.getRandom().nextInt(40) != 0) {
            return false; // не каждый тик — скан дорогой
        }
        this.ore = findOre();
        return this.ore != null;
    }

    private BlockPos findOre() {
        BlockPos c = this.mob.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (isOre(this.mob.level().getBlockState(m))) {
                        double d = c.distSqr(m);
                        if (d < bestD) {
                            bestD = d;
                            best = m.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean isOre(BlockState s) {
        if (s.isAir()) {
            return false;
        }
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.endsWith("_ore");
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getTarget() == null && this.ore != null && isOre(this.mob.level().getBlockState(this.ore));
    }

    @Override
    public void start() {
        this.breakTime = 0;
        this.lastProgress = -1;
        this.repathCd = 0;
    }

    @Override
    public void stop() {
        if (this.ore != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.ore, -1);
        }
        this.ore = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.ore == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.ore.getX() + 0.5D, this.ore.getY() + 0.5D, this.ore.getZ() + 0.5D);
        double d = this.mob.distanceToSqr(this.ore.getX() + 0.5D, this.ore.getY() + 0.5D, this.ore.getZ() + 0.5D);
        if (d > 4.0D) {
            if (this.repathCd-- <= 0) {
                this.mob.getNavigation().moveTo(this.ore.getX() + 0.5D, this.ore.getY(), this.ore.getZ() + 0.5D, 1.0D);
                this.repathCd = 20;
            }
            return; // ещё не дошёл до руды
        }
        this.breakTime++;
        int p = (int) ((float) this.breakTime / (float) this.breakSpeed * 10.0F);
        if (p != this.lastProgress) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.ore, p);
            this.lastProgress = p;
        }
        if (this.breakTime >= this.breakSpeed) {
            this.mob.level().destroyBlock(this.ore, true, this.mob); // дроп — моб подберёт
            this.ore = null;
            this.breakTime = 0;
            this.lastProgress = -1;
        }
    }
}
