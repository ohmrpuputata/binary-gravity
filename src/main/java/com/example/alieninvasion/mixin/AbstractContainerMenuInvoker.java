package com.example.alieninvasion.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Доступ к protected {@code addSlot}, объявленному в AbstractContainerMenu, чтобы
 *  добавить ячейку маски из миксина по InventoryMenu (метод унаследован, не свой). */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {
    @Invoker("addSlot")
    Slot alien$addSlot(Slot slot);
}
