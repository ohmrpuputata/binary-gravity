package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.RogueScavengerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * «Шахтёр/лесоруб с интеллектом»: вне боя выживший сканирует РУДУ и ДЕРЕВО сквозь
 * стены (x-ray), достаёт нужный ИНСТРУМЕНТ (кирку для руды/камня, топор для брёвен —
 * не голыми руками!), идёт к цели и выламывает её, переходя к следующей. В бою — прячет
 * инструмент и возвращает оружие в руку.
 */
public class GatherResourcesGoal extends Goal {
    private final RogueScavengerEntity mob;
    private final int radius;
    private final int breakSpeed;
    private BlockPos target;
    private ItemStack savedMainhand = ItemStack.EMPTY;
    private int breakTime;
    private int lastProgress = -1;
    private int repathCd;

    public GatherResourcesGoal(RogueScavengerEntity mob, int radius, int breakSpeed) {
        this.mob = mob;
        this.radius = radius;
        this.breakSpeed = breakSpeed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.mob.getTarget() != null) {
            return false; // в бою не копаем
        }
        if (this.mob.getRandom().nextInt(40) != 0) {
            return false;
        }
        this.target = findResource();
        return this.target != null;
    }

    private BlockPos findResource() {
        BlockPos c = this.mob.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (isResource(this.mob.level().getBlockState(m))) {
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

    private boolean isResource(BlockState s) {
        if (s.isAir()) {
            return false;
        }
        if (s.is(BlockTags.LOGS)) {
            return true;
        }
        return BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("_ore");
    }

    @Override
    public boolean canContinueToUse() {
        return this.mob.getTarget() == null && this.target != null
                && isResource(this.mob.level().getBlockState(this.target));
    }

    @Override
    public void start() {
        this.breakTime = 0;
        this.lastProgress = -1;
        this.repathCd = 0;
        // достаём нужный инструмент; прежний предмет руки (меч) запоминаем и вернём в stop().
        this.savedMainhand = this.mob.getMainHandItem().copy();
        equipToolFor(this.target);
    }

    private void equipToolFor(BlockPos pos) {
        this.mob.setItemSlot(EquipmentSlot.MAINHAND, this.mob.toolFor(this.mob.level().getBlockState(pos)));
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.target, -1);
        }
        this.mob.setItemSlot(EquipmentSlot.MAINHAND, this.savedMainhand); // вернули оружие в руку
        this.target = null;
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }
        this.mob.getLookControl().setLookAt(this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D);
        double d = this.mob.distanceToSqr(this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D);
        if (d > 5.0D) {
            if (this.repathCd-- <= 0) {
                this.mob.getNavigation().moveTo(this.target.getX() + 0.5D, this.target.getY(), this.target.getZ() + 0.5D, 1.0D);
                this.repathCd = 20;
            }
            return;
        }
        this.breakTime++;
        int p = (int) ((float) this.breakTime / (float) this.breakSpeed * 10.0F);
        if (p != this.lastProgress) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.target, p);
            this.lastProgress = p;
        }
        if (this.breakTime >= this.breakSpeed) {
            this.mob.level().destroyBlock(this.target, true, this.mob); // дроп падает (подберёт пригодное)
            BlockPos next = findResource();           // сразу ищем следующую жилу/дерево
            this.target = next;
            this.breakTime = 0;
            this.lastProgress = -1;
            if (next != null) {
                equipToolFor(next);                   // под новый блок — свой инструмент
            }
        }
    }
}
