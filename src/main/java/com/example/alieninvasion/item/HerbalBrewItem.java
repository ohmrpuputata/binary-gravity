package com.example.alieninvasion.item;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Травяной отвар — раннее, дешёвое облегчение выживания. В отличие от Антидота,
 * который СНИМАЕТ заражение целиком, отвар не лечит, а даёт временную
 * СОПРОТИВЛЯЕМОСТЬ: пока действует, шкала заражения растёт намного медленнее
 * (см. InfectionManager.applyBrewResistance) и сразу немного сбивается. Варится
 * из ранних природных ресурсов, так что доступен буквально с первого дня.
 */
public class HerbalBrewItem extends Item {
    /** Длительность сопротивляемости, тики (120 секунд). */
    private static final int DURATION_TICKS = 2400;
    /** Сколько процентов шкалы сбивается сразу при употреблении. */
    private static final float IMMEDIATE_RELIEF = 8.0F;

    public HerbalBrewItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof Player player) {
            com.example.alieninvasion.logic.InfectionManager.applyBrewResistance(
                    player, DURATION_TICKS, IMMEDIATE_RELIEF);
            level.playSound(null, player.blockPosition(), SoundEvents.BREWING_STAND_BREW,
                    SoundSource.PLAYERS, 0.6F, 1.2F);
            player.displayClientMessage(Component.literal(
                    "§2Тепло отвара разливается по телу — заражение отступает."), true);
        }
        if (user instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (stack.isEmpty()) {
                return bottle;
            }
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }
}
