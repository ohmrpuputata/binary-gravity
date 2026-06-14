package com.example.alieninvasion.jei;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.PalladiumAnvilRecipe;
import com.example.alieninvasion.block.PlatinumAnvilRecipe;
import com.example.alieninvasion.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class AlienInvasionJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IJeiHelpers helpers = registration.getJeiHelpers();
        registration.addRecipeCategories(
                new PalladiumAnvilRecipeCategory(helpers.getGuiHelper()),
                new PlatinumAnvilRecipeCategory(helpers.getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        RecipeManager manager = mc.level.getRecipeManager();

        // Register Palladium Anvil Recipes
        List<PalladiumAnvilRecipe> palladiumRecipes = manager.getAllRecipesFor(ModBlocks.PALLADIUM_ANVIL_RECIPE_TYPE)
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());
        registration.addRecipes(PalladiumAnvilRecipeCategory.TYPE, palladiumRecipes);

        // Register Platinum Anvil Recipes
        List<PlatinumAnvilRecipe> platinumRecipes = manager.getAllRecipesFor(ModBlocks.PLATINUM_ANVIL_RECIPE_TYPE)
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());
        registration.addRecipes(PlatinumAnvilRecipeCategory.TYPE, platinumRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PALLADIUM_ANVIL), PalladiumAnvilRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PLATINUM_ANVIL), PlatinumAnvilRecipeCategory.TYPE);
    }
}
