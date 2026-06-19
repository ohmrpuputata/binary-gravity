package com.example.alieninvasion.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Доступ к leftPos/topPos экрана-контейнера, чтобы поставить кнопку маски относительно
 *  панели инвентаря (ширина у выживания и креатива разная). */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int alien$getLeftPos();

    @Accessor("topPos")
    int alien$getTopPos();
}
