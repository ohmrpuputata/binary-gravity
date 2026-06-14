package com.example.alieninvasion.jei;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.PalladiumAnvilRecipe;
import com.example.alieninvasion.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class PalladiumAnvilRecipeCategory implements IRecipeCategory<PalladiumAnvilRecipe> {
    public static final RecipeType<PalladiumAnvilRecipe> TYPE =
            RecipeType.create(AlienInvasionMod.MODID, "palladium_anvil", PalladiumAnvilRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;
    private final IDrawable slotDrawable;

    public PalladiumAnvilRecipeCategory(IGuiHelper helper) {
        // Create a blank area: 120 wide, 28 high
        this.background = helper.createBlankDrawable(120, 28);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.PALLADIUM_ANVIL));
        this.title = Component.translatable("block.alien-invasion.palladium_anvil");
        this.slotDrawable = helper.getSlotDrawable();
    }

    @Override
    public RecipeType<PalladiumAnvilRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PalladiumAnvilRecipe recipe, IFocusGroup focuses) {
        // Slot 0 (Catalyst): x=1, y=5
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 5)
               .addIngredients(recipe.getCatalyst());

        // Slot 1 (Base): x=19, y=5
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 5)
               .addIngredients(recipe.getBase());

        // Slot 2 (Addition): x=37, y=5
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 5)
               .addIngredients(recipe.getAddition());

        // Slot 3 (Result): x=95, y=5
        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 5)
               .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(PalladiumAnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw slot backgrounds (centered: 1px left and up relative to item slot)
        slotDrawable.draw(guiGraphics, 0, 4);
        slotDrawable.draw(guiGraphics, 18, 4);
        slotDrawable.draw(guiGraphics, 36, 4);
        slotDrawable.draw(guiGraphics, 94, 4);

        // Draw the arrow in between
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, "\u25B6", 67, 10, 0x404040, false);
    }
}
