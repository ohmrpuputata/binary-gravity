package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlienStashBlockEntity extends BlockEntity implements Container, MenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public AlienStashBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ALIEN_STASH_BLOCK_ENTITY, pos, state);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : this.items) {
            if (!itemStack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(this.items, slot);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.alien-invasion.alien_stash");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    public boolean depositItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        // First try to merge with existing stacks
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack existing = this.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int maxCount = Math.min(existing.getMaxStackSize(), this.getMaxStackSize());
                int countToTransfer = Math.min(stack.getCount(), maxCount - existing.getCount());
                if (countToTransfer > 0) {
                    existing.grow(countToTransfer);
                    stack.shrink(countToTransfer);
                    this.setChanged();
                    if (stack.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        // Then try to put in empty slots
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack existing = this.getItem(i);
            if (existing.isEmpty()) {
                this.setItem(i, stack.copy());
                stack.setCount(0);
                this.setChanged();
                return true;
            }
        }
        return false;
    }
}
