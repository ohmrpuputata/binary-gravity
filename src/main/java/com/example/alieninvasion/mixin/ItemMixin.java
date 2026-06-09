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
        if (!key.getNamespace().equals("alien-invasion")) return;
        String base = "tooltip.alien-invasion." + key.getPath();
        var lang = net.minecraft.locale.Language.getInstance();
        // Multi-line: try base.1, base.2, base.3 ... first
        boolean hasNumbered = lang.has(base + ".1");
        if (hasNumbered) {
            for (int i = 1; lang.has(base + "." + i); i++) {
                tooltip.add(Component.translatable(base + "." + i).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
        } else if (lang.has(base)) {
            tooltip.add(Component.translatable(base).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
