package com.example.alieninvasion.mixin;

import com.example.alieninvasion.logic.MaskAttachmentContainer;
import com.example.alieninvasion.logic.MaskSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Встраивает ячейку маски прямо в инвентарь игрока (рядом с щитом, x=77,y=44).
 * Маску надевают перетаскиванием в эту ячейку — без клавиш. Ячейка завязана на
 * attachment (см. {@link MaskAttachmentContainer}), поэтому переживает и синкается.
 * addSlot унаследован от AbstractContainerMenu — зовём через {@link AbstractContainerMenuInvoker}.
 */
@Mixin(InventoryMenu.class)
public class InventoryMenuMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void alien$addMaskSlot(Inventory inventory, boolean isServerSide, Player owner, CallbackInfo ci) {
        Slot maskSlot = new Slot(new MaskAttachmentContainer(owner), 0, 77, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MaskSlot.isMask(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        ((AbstractContainerMenuInvoker) (Object) this).alien$addSlot(maskSlot);
    }
}
