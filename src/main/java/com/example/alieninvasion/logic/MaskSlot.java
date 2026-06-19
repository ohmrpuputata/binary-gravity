package com.example.alieninvasion.logic;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.registry.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Слот маски (поверх брони). Стек хранится в attachment {@link ModAttachments#MASK}.
 * Надевается/снимается клавишей (тоггл): держишь предмет из тега {@code masks} —
 * клавиша надевает его; маска надета — снимает обратно в инвентарь. Слот отдельный
 * от шлема, поэтому маска и шлем носятся вместе.
 */
public final class MaskSlot {
    public static final TagKey<Item> MASKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "masks"));
    /** Герметичные маски: позволяют дышать в ядовитом газе, но тратят запас воздуха. */
    public static final TagKey<Item> SEALED_MASKS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "sealed_masks"));
    /** Полный запас воздуха герметичной маски в тиках (~60 секунд). */
    public static final int MAX_AIR = 1200;

    private MaskSlot() {
    }

    public static boolean isMask(ItemStack stack) {
        return stack.is(MASKS);
    }

    public static ItemStack get(LivingEntity entity) {
        return entity.getAttachedOrElse(ModAttachments.MASK, ItemStack.EMPTY);
    }

    public static boolean hasMask(LivingEntity entity) {
        return !get(entity).isEmpty();
    }

    /** Надета ли ГЕРМЕТИЧНАЯ маска (даёт дышать в ядовитом газе, пока есть воздух). */
    public static boolean hasSealedMask(LivingEntity entity) {
        return get(entity).is(SEALED_MASKS);
    }

    public static int getAir(LivingEntity entity) {
        return entity.getAttachedOrElse(ModAttachments.MASK_AIR, 0);
    }

    public static void setAir(LivingEntity entity, int air) {
        entity.setAttached(ModAttachments.MASK_AIR, Math.max(0, Math.min(MAX_AIR, air)));
    }

    /** Клавиша-тоггл: снять надетую маску, либо надеть маску из руки. */
    public static void toggle(ServerPlayer player) {
        ItemStack worn = get(player);
        if (!worn.isEmpty()) {
            if (!player.getInventory().add(worn)) {
                player.drop(worn, false);
            }
            player.removeAttached(ModAttachments.MASK);
            sound(player, 0.7F, 0.8F);
            return;
        }
        ItemStack hand = player.getMainHandItem();
        if (isMask(hand)) {
            ItemStack one = hand.copyWithCount(1);
            hand.shrink(1);
            player.setAttached(ModAttachments.MASK, one);
            sound(player, 0.9F, 1.0F);
        } else {
            player.displayClientMessage(Component.translatable("message.alien-invasion.mask.none"), true);
        }
    }

    private static void sound(ServerPlayer player, float vol, float pitch) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, vol, pitch);
    }
}
