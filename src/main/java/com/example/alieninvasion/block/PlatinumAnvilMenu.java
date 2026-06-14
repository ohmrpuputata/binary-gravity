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
 * Container (menu) for the Platinum Anvil.
 * <p>
 * Slot layout:
 * <pre>
 *  0-8   crafting grid (3×3)
 *  9     catalyst (amethyst / radiation / dark-matter / cosmic shard)
 *  10    result
 *  11-37 player inventory
 *  38-46 hotbar
 * </pre>
 */
public class PlatinumAnvilMenu extends AbstractContainerMenu {

    private static final int GRID_SIZE = 9;
    private static final int CATALYST_INDEX = 4;
    private static final int RESULT_INDEX = 9;
    private static final int INV_START = 10;
    private static final int INV_END = 46; // exclusive

    private final ContainerLevelAccess access;
    private final Player player;
    final SimpleContainer craftSlots = new SimpleContainer(9); // 9 slots (0-8) where index 4 is catalyst
    final ResultContainer resultSlots = new ResultContainer();

    /* ---------- constructors ---------- */

    /** Client-side (no level access). */
    public PlatinumAnvilMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    /** Server-side (full access). */
    public PlatinumAnvilMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModBlocks.PLATINUM_ANVIL_MENU, containerId);
        this.access = access;
        this.player = playerInventory.player;

        this.craftSlots.addListener(c -> this.slotsChanged(c));

        // ---- grid 3×3 (slots 0-8) with catalyst at center (4) ----
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                if (index == 4) {
                    this.addSlot(new CatalystSlot(craftSlots, 4, 31 + col * 18, 18 + row * 18));
                } else {
                    this.addSlot(new Slot(craftSlots, index, 31 + col * 18, 18 + row * 18));
                }
            }
        }

        // ---- result (slot 9) ----
        this.addSlot(new AnvilResultSlot(this));

        // ---- player inventory (slots 10-36) ----
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));

        // ---- hotbar (slots 37-45) ----
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
    }

    /* ---------- recipe logic ---------- */

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.access.execute((level, pos) -> updateResult(level));
    }

    private void updateResult(Level level) {
        RecipeInput input = createRecipeInput();
        Optional<RecipeHolder<PlatinumAnvilRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(ModBlocks.PLATINUM_ANVIL_RECIPE_TYPE, input, level);

        if (recipe.isPresent()) {
            resultSlots.setItem(0, recipe.get().value().assemble(input, level.registryAccess()));
        } else {
            resultSlots.setItem(0, ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    private RecipeInput createRecipeInput() {
        return new RecipeInput() {
            @Override public ItemStack getItem(int slot) { return craftSlots.getItem(slot); }
            @Override public int size()                  { return craftSlots.getContainerSize(); }
        };
    }

    /** Consume one of every non-empty ingredient and the catalyst; roll anvil damage. */
    void consumeIngredients() {
        for (int i = 0; i < 9; i++) {
            if (!craftSlots.getItem(i).isEmpty()) {
                craftSlots.removeItem(i, 1);
            }
        }
        // 12% chance to damage the anvil (same as vanilla)
        access.execute((level, pos) -> {
            if (level.random.nextFloat() < 0.12F) {
                PlatinumAnvilBlock.damage(level, pos);
            }
        });
    }

    /* ---------- standard menu overrides ---------- */

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return this.access.evaluate((level, pos) -> {
            if (!(level.getBlockState(pos).getBlock() instanceof PlatinumAnvilBlock)) return false;
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
            // result → player inventory
            if (!this.moveItemStackTo(current, INV_START, INV_END, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(current, copy);
        } else if (index >= INV_START) {
            // player inventory → catalyst or grid
            if (isCatalystItem(current)) {
                if (!this.moveItemStackTo(current, CATALYST_INDEX, CATALYST_INDEX + 1, false))
                    if (!this.moveItemStackTo(current, 0, GRID_SIZE, false))
                        return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(current, 0, GRID_SIZE, false)) return ItemStack.EMPTY;
            }
        } else {
            // grid/catalyst → player inventory
            if (!this.moveItemStackTo(current, INV_START, INV_END, false)) return ItemStack.EMPTY;
        }

        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (current.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, current);
        return copy;
    }

    /* ---------- helpers ---------- */

    public static boolean isCatalystItem(ItemStack stack) {
        return stack.is(Items.AMETHYST_SHARD)
                || stack.is(ItemRegistry.RADIATION_CRYSTAL)
                || stack.is(ItemRegistry.DARK_MATTER_SHARD)
                || stack.is(ItemRegistry.COSMIC_SHARD)
                || stack.is(Items.EMERALD);
    }

    /* ---------- inner slot classes ---------- */

    /** Only accepts recognised catalyst items. */
    private static class CatalystSlot extends Slot {
        CatalystSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return isCatalystItem(stack); }
    }

    /** Read-only result slot that consumes ingredients on take. */
    private static class AnvilResultSlot extends Slot {
        private final PlatinumAnvilMenu menu;
        AnvilResultSlot(PlatinumAnvilMenu menu) {
            super(menu.resultSlots, 0, 125, 36);
            this.menu = menu;
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public void onTake(Player player, ItemStack stack) {
            menu.consumeIngredients();
            super.onTake(player, stack);
        }
    }
}
