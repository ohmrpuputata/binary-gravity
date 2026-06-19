package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ModAttachments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Контейнер на одну ячейку, который читает/пишет маску прямо в attachment игрока.
 * Используется слотом маски в инвентаре: положил/убрал в ячейку — сразу обновился
 * attachment (а значит и рендер на лице, и защита). Маска не лежит в инвентаре —
 * только в attachment, поэтому её нельзя продублировать.
 */
public class MaskAttachmentContainer implements Container {
    private final Player player;

    public MaskAttachmentContainer(Player player) {
        this.player = player;
    }

    private ItemStack get() {
        return player.getAttachedOrElse(ModAttachments.MASK, ItemStack.EMPTY);
    }

    private void set(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            player.removeAttached(ModAttachments.MASK);
        } else {
            player.setAttached(ModAttachments.MASK, stack);
        }
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? get() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack cur = get().copy();
        if (cur.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = cur.split(amount);
        set(cur);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack cur = get();
        set(ItemStack.EMPTY);
        return cur;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            set(stack);
        }
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player p) {
        return p == this.player;
    }

    @Override
    public void clearContent() {
        set(ItemStack.EMPTY);
    }
}
