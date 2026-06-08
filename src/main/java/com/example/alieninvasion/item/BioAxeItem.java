package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.AlienUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

// Bio-Axe: a brutal alien cleaver. Hits inflict Weakness, and aliens are also
// crippled with Slowness, making it a strong anti-swarm sidearm.
public class BioAxeItem extends AxeItem {
    public BioAxeItem(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (result && !target.level().isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
            if (AlienUtils.isAlliedTo(attacker, target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
            }
        }
        return result;
    }
}
