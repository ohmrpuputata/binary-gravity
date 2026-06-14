package com.example.alieninvasion.block;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Smithing-style container menu for the Palladium Anvil.
 * <p>
 * Slots:
 *  0: Catalyst (amethyst shard / radiation crystal)
 *  1: Base (item to upgrade)
 *  2: Addition (ingot/material)
 *  3: Result
 *  4-30: Player inventory
 *  31-39: Player hotbar
 */
public class PalladiumAnvilMenu extends AbstractContainerMenu {

    private static final int RESULT_INDEX = 3;
    private static final int INV_START = 4;
    private static final int INV_END = 40; // exclusive

    private final ContainerLevelAccess access;
    private final Player player;
    final SimpleContainer inputSlots = new SimpleContainer(3);
    final ResultContainer resultSlots = new ResultContainer();

    /** Client-side constructor */
    public PalladiumAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** Server-side constructor */
    public PalladiumAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModBlocks.PALLADIUM_ANVIL_MENU, containerId);
        this.access = access;
        this.player = playerInventory.player;

        this.inputSlots.addListener(c -> this.slotsChanged(c));

        // Slot 0: Catalyst (x=8, y=48)
        this.addSlot(new CatalystSlot(inputSlots, 0, 8, 48));

        // Slot 1: Base (x=26, y=48)
        this.addSlot(new Slot(inputSlots, 1, 26, 48));

        // Slot 2: Addition (x=44, y=48)
        this.addSlot(new Slot(inputSlots, 2, 44, 48));

        // Slot 3: Result (x=98, y=48)
        this.addSlot(new AnvilResultSlot(this));

        // Player Inventory (slots 4-30)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (slots 31-39)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        RecipeInput input = createRecipeInput();
        Optional<RecipeHolder<PalladiumAnvilRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(ModBlocks.PALLADIUM_ANVIL_RECIPE_TYPE, input, level);

        if (recipe.isPresent()) {
            resultSlots.setItem(0, recipe.get().value().assemble(input, level.registryAccess()));
        } else {
            resultSlots.setItem(0, ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    private RecipeInput createRecipeInput() {
        return new RecipeInput() {
            @Override public ItemStack getItem(int slot) { return inputSlots.getItem(slot); }
            @Override public int size()                  { return inputSlots.getContainerSize(); }
        };
    }

    void consumeIngredients() {
        for (int i = 0; i < 3; i++) {
            if (!inputSlots.getItem(i).isEmpty()) {
                inputSlots.removeItem(i, 1);
            }
        }
        access.execute((level, pos) -> {
            if (level.random.nextFloat() < 0.12F) {
                PalladiumAnvilBlock.damage(level, pos);
            }
        });
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.inputSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!(level.getBlockState(pos).getBlock() instanceof PalladiumAnvilBlock)) return false;
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack current = slot.getItem();
        copy = current.copy();

        if (index == RESULT_INDEX) {
            if (!this.moveItemStackTo(current, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(current, copy);
        } else if (index >= INV_START) {
            if (isCatalystItem(current)) {
                if (!this.moveItemStackTo(current, 0, 1, false)) {
                    if (!this.moveItemStackTo(current, 1, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else {
                if (!this.moveItemStackTo(current, 1, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
            if (!this.moveItemStackTo(current, INV_START, INV_END, false)) return ItemStack.EMPTY;
        }

        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (current.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, current);
        return copy;
    }

    public static boolean isCatalystItem(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD)
                || stack.is(ItemRegistry.RADIATION_CRYSTAL);
    }

    private static class CatalystSlot extends Slot {
        CatalystSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return isCatalystItem(stack); }
    }

    private static class AnvilResultSlot extends Slot {
        private final PalladiumAnvilMenu menu;
        AnvilResultSlot(PalladiumAnvilMenu menu) {
            super(menu.resultSlots, 0, 98, 48);
            this.menu = menu;
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public void onTake(Player player, ItemStack stack) {
            menu.consumeIngredients();
            super.onTake(player, stack);
        }
    }
}
