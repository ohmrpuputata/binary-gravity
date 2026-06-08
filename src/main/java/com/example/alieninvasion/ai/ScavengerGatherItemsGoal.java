package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienGruntEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.EnumSet;

public class ScavengerGatherItemsGoal extends Goal {
    private final AlienGruntEntity grunt;
    private ItemEntity targetItem;
    private int cooldown;

    public ScavengerGatherItemsGoal(AlienGruntEntity grunt) {
        this.grunt = grunt;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!this.grunt.isScavenger()) {
            return false;
        }
        // If already holding something in main hand, deliver it first!
        if (!this.grunt.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
            return false;
        }
        if (this.cooldown-- > 0) {
            return false;
        }
        this.cooldown = 20; // Check once a second
        this.targetItem = findNearestItem();
        return this.targetItem != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetItem != null && this.targetItem.isAlive() && this.grunt.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void start() {
        if (this.targetItem != null) {
            this.grunt.getNavigation().moveTo(this.targetItem, 1.2D);
        }
    }

    @Override
    public void stop() {
        this.grunt.getNavigation().stop();
        this.targetItem = null;
    }

    @Override
    public void tick() {
        if (this.targetItem == null || !this.targetItem.isAlive()) {
            return;
        }
        this.grunt.getLookControl().setLookAt(this.targetItem, 30.0F, 30.0F);
        
        // Move towards item
        if (this.grunt.distanceToSqr(this.targetItem) < 2.0D) {
            ItemStack stack = this.targetItem.getItem();
            this.grunt.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
            this.grunt.level().playSound(null, this.grunt.blockPosition(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.NEUTRAL, 0.2F, (this.grunt.getRandom().nextFloat() - this.grunt.getRandom().nextFloat()) * 0.2F + 1.0F);
            this.targetItem.discard();
            this.targetItem = null;
        } else {
            // Re-path periodically
            if (this.grunt.tickCount % 20 == 0) {
                this.grunt.getNavigation().moveTo(this.targetItem, 1.2D);
            }
        }
    }

    private ItemEntity findNearestItem() {
        List<ItemEntity> items = this.grunt.level().getEntitiesOfClass(
                ItemEntity.class,
                this.grunt.getBoundingBox().inflate(16.0D, 8.0D, 16.0D),
                item -> item.isAlive() && !item.getItem().isEmpty()
        );
        double closestDist = Double.MAX_VALUE;
        ItemEntity closest = null;
        for (ItemEntity item : items) {
            double dist = this.grunt.distanceToSqr(item);
            if (dist < closestDist) {
                closestDist = dist;
                closest = item;
            }
        }
        return closest;
    }
}
