package com.example.alieninvasion.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void appendCustomTooltips(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        Item item = (Item) (Object) this;
        var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item);
        if (key.getNamespace().equals("alien-invasion")) {
            String tooltipKey = "tooltip.alien-invasion." + key.getPath();
            // Only show a description line if one actually exists - otherwise the raw
            // "tooltip.alien-invasion.xxx" key would be printed in the tooltip.
            if (net.minecraft.locale.Language.getInstance().has(tooltipKey)) {
                tooltip.add(Component.translatable(tooltipKey).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        }
    }
}
