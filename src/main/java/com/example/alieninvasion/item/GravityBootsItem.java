package com.example.alieninvasion.item;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public class GravityBootsItem extends ArmorItem {
    private static final String LEVITATION_ENABLED_KEY = "LevitationEnabled";

    public GravityBootsItem(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.BOOTS, properties);
    }

    public static boolean isLevitationEnabled(ItemStack stack) {
        if (!stack.is(ItemRegistry.GRAVITY_BOOTS)) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return !tag.contains(LEVITATION_ENABLED_KEY) || tag.getBoolean(LEVITATION_ENABLED_KEY);
    }

    public static boolean toggleLevitation(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(ItemRegistry.GRAVITY_BOOTS)) {
            player.displayClientMessage(Component.translatable(
                    "message.alien-invasion.gravity_boots.missing"), true);
            return false;
        }

        boolean enabled = !isLevitationEnabled(boots);
        CompoundTag tag = boots.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(LEVITATION_ENABLED_KEY, enabled);
        boots.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        player.displayClientMessage(Component.translatable(enabled
                ? "message.alien-invasion.gravity_boots.enabled"
                : "message.alien-invasion.gravity_boots.disabled"), true);
        player.level().playSound(null, player.blockPosition(), enabled
                        ? SoundEvents.BEACON_ACTIVATE
                        : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS, 0.35F, enabled ? 1.35F : 0.85F);
        return enabled;
    }
}
