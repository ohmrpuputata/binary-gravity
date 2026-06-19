package com.example.alieninvasion.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Рисует фон ячейки маски (x=77,y=44) в инвентаре, чтобы слот выглядел как родной
 *  (сам предмет в слоте рисует уже меню). */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "renderBg", at = @At("TAIL"))
    private void alien$maskSlotBg(GuiGraphics g, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        int x = ((AbstractContainerScreenAccessor) screen).alien$getLeftPos() + 77;
        int y = ((AbstractContainerScreenAccessor) screen).alien$getTopPos() + 44;
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
}
