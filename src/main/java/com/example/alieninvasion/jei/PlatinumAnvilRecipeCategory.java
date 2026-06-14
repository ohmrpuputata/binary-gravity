package com.example.alieninvasion.jei;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.PlatinumAnvilRecipe;
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
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public class PlatinumAnvilRecipeCategory implements IRecipeCategory<PlatinumAnvilRecipe> {
    public static final RecipeType<PlatinumAnvilRecipe> TYPE =
            RecipeType.create(AlienInvasionMod.MODID, "platinum_anvil", PlatinumAnvilRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;
    private final IDrawable slotDrawable;

    public PlatinumAnvilRecipeCategory(IGuiHelper helper) {
        // Create a blank area: 120 wide, 60 high
        this.background = helper.createBlankDrawable(120, 60);
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.PLATINUM_ANVIL));
        this.title = Component.translatable("block.alien-invasion.platinum_anvil");
        this.slotDrawable = helper.getSlotDrawable();
    }

    @Override
    public RecipeType<PlatinumAnvilRecipe> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, PlatinumAnvilRecipe recipe, IFocusGroup focuses) {
        NonNullList<Ingredient> ingredients = recipe.getIngredientsForJei();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                builder.addSlot(RecipeIngredientRole.INPUT, 3 + c * 18, 3 + r * 18)
                       .addIngredients(ingredients.get(index));
            }
        }

        builder.addSlot(RecipeIngredientRole.OUTPUT, 95, 21)
               .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(PlatinumAnvilRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw grid slot backgrounds
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                slotDrawable.draw(guiGraphics, 3 + c * 18, 3 + r * 18);
            }
        }

        // Draw result slot background
        slotDrawable.draw(guiGraphics, 95, 21);

        // Draw the arrow in between
        Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font, "\u25B6", 67, 26, 0x404040, false);
    }
}
