package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Зараженный скелет (Infested Skeleton):
 * Дальнобойный мутант, который стреляет кристаллическими стрелами, накладывающими эффект радиации.
 */
public class InfestedSkeletonEntity extends Skeleton implements IAlienUnit {

    public InfestedSkeletonEntity(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public AlienRole getAlienRole() { return AlienRole.INFECTED; }

    @Override
    protected AbstractArrow getArrow(ItemStack arrowStack, float distanceFactor, ItemStack bowStack) {
        AbstractArrow arrow = super.getArrow(arrowStack, distanceFactor, bowStack);
        if (arrow instanceof Arrow regularArrow) {
            regularArrow.addEffect(new MobEffectInstance(
                BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION),
                200, 0
            ));
        }
        return arrow;
    }

    @Override
    public boolean isAlliedTo(net.minecraft.world.entity.Entity other) {
        return AlienUtils.isAlliedTo(this, other) || super.isAlliedTo(other);
    }
}
