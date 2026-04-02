package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.menu.CookingPotMenu;
import com.piranport.menu.StoneMillMenu;
import com.piranport.recipe.CookingPotRecipe;
import com.piranport.recipe.CuttingBoardRecipe;
import com.piranport.recipe.StoneMillRecipe;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModMenuTypes;
import com.piranport.registry.ModRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

@JeiPlugin
public class PiranPortJEIPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return UID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CookingPotRecipeCategory(guiHelper),
                new StoneMillRecipeCategory(guiHelper),
                new CuttingBoardRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RecipeManager rm = level.getRecipeManager();

        registration.addRecipes(CookingPotRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipeTypes.COOKING_POT_TYPE.get())
                        .stream().map(RecipeHolder::value).toList());

        registration.addRecipes(StoneMillRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipeTypes.STONE_MILL_TYPE.get())
                        .stream().map(RecipeHolder::value).toList());

        registration.addRecipes(CuttingBoardRecipeCategory.RECIPE_TYPE,
                rm.getAllRecipesFor(ModRecipeTypes.CUTTING_BOARD_TYPE.get())
                        .stream().map(RecipeHolder::value).toList());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COOKING_POT.get()),
                CookingPotRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STONE_MILL.get()),
                StoneMillRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CUTTING_BOARD.get()),
                CuttingBoardRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Cooking pot: input slots 0-8, player inventory starts at slot 10, 36 slots
        registration.addRecipeTransferHandler(CookingPotMenu.class,
                ModMenuTypes.COOKING_POT_MENU.get(),
                CookingPotRecipeCategory.RECIPE_TYPE,
                0, 9, 10, 36);

        // Stone mill: input slots 0-3, player inventory starts at slot 6, 36 slots
        registration.addRecipeTransferHandler(StoneMillMenu.class,
                ModMenuTypes.STONE_MILL_MENU.get(),
                StoneMillRecipeCategory.RECIPE_TYPE,
                0, 4, 6, 36);
    }
}
