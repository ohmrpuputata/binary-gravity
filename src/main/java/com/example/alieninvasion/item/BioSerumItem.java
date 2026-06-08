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

// Bio-Serum: an injectable cure. Purges Infection, Radiation and Psychic Pressure
// and grants Regeneration. Right-click to use.
public class BioSerumItem extends Item {
    public BioSerumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addTag("CuredByAntidote");
            com.example.alieninvasion.logic.RadiationManager.clearDose(player);
            com.example.alieninvasion.logic.InfectionManager.clear(player);
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.PSYCHIC_PRESSURE));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
