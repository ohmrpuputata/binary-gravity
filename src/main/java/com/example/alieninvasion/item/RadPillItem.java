package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RadPillItem extends Item {
    public RadPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Apply gradual radiation cleanse effect for 1 minute (1200 ticks)
            player.addEffect(new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION_CLEANSE), 1200, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 0, false, true));
            level.playSound(null, player.blockPosition(), SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.8F, 1.25F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
