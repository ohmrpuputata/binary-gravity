package com.example.alieninvasion.item;

import com.example.alieninvasion.logic.MaskSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Баллон воздуха: ПКМ заправляет запас воздуха надетой ГЕРМЕТИЧНОЙ маски доверху.
 *  Расходник для вылазок в ядовитые зоны (там воздух в маске тратится). */
public class AirCanisterItem extends Item {
    public AirCanisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!MaskSlot.hasSealedMask(player)) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.alien-invasion.air_canister.no_mask"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (MaskSlot.getAir(player) >= MaskSlot.MAX_AIR) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide) {
            MaskSlot.setAir(player, MaskSlot.MAX_AIR);
            level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_FILL,
                    SoundSource.PLAYERS, 0.8F, 0.7F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
