package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.MaskSlot;
import com.example.alieninvasion.registry.ModAttachments;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Меню слота маски: одна ячейка под маску + инвентарь игрока, куда можно перетащить.
 * Ячейка — это вид на attachment {@link ModAttachments#MASK}: при любом изменении
 * стек пишется обратно в attachment (и на сервере сразу обновляется рендер на лице).
 * Маска живёт в attachment, поэтому при закрытии ничего не выпадает.
 */
public class MaskMenu extends AbstractContainerMenu {
    private static final int MASK_SLOT = 0;
    private static final int INV_START = 1;
    private static final int INV_END = 37; // 1 маска + 36 инвентаря

    private final Player player;
    private final SimpleContainer maskContainer = new SimpleContainer(1);

    /** Клиентский конструктор (контент ячейки приходит синком меню). */
    public MaskMenu(int containerId, Inventory inv) {
        this(containerId, inv, ItemStack.EMPTY);
    }

    /** Серверный конструктор — текущая надетая маска из attachment. */
    public MaskMenu(int containerId, Inventory inv, ItemStack equipped) {
        super(ModBlocks.MASK_MENU, containerId);
        this.player = inv.player;
        this.maskContainer.setItem(0, equipped.copy());
        this.maskContainer.addListener(c -> this.slotsChanged(c));

        this.addSlot(new Slot(maskContainer, 0, 80, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MaskSlot.isMask(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }

    private void syncToAttachment() {
        if (player instanceof ServerPlayer sp) {
            ItemStack mask = maskContainer.getItem(0);
            if (mask.isEmpty()) {
                sp.removeAttached(ModAttachments.MASK);
            } else {
                sp.setAttached(ModAttachments.MASK, mask.copy());
            }
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        syncToAttachment();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        syncToAttachment(); // маска остаётся в attachment, не выпадает
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return copy;
        }
        ItemStack current = slot.getItem();
        copy = current.copy();
        if (index == MASK_SLOT) {
            if (!this.moveItemStackTo(current, INV_START, INV_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (MaskSlot.isMask(current)) {
            if (!this.moveItemStackTo(current, MASK_SLOT, MASK_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
