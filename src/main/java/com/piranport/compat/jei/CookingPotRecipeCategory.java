package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.recipe.CookingPotRecipe;
import com.piranport.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
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
import org.jetbrains.annotations.Nullable;

public class CookingPotRecipeCategory implements IRecipeCategory<CookingPotRecipe> {
    public static final RecipeType<CookingPotRecipe> RECIPE_TYPE =
            RecipeType.create(PiranPort.MOD_ID, "cooking_pot", CookingPotRecipe.class);

    private final IDrawable icon;
    private final Component title;

    public CookingPotRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.COOKING_POT.get()));
        this.title = Component.translatable("jei.piranport.cooking_pot");
    }

    @Override
    public RecipeType<CookingPotRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 140; }

    @Override
    public int getHeight() { return 60; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CookingPotRecipe recipe, IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 3) * 18, 4 + (i / 3) * 18)
                    .addIngredients(ingredients.get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 118, 22)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(CookingPotRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // Arrow shaft
        gfx.fill(64, 28, 106, 30, 0xFF808080);
        // Arrow head
        gfx.fill(100, 25, 106, 33, 0xFF808080);
        gfx.fill(106, 26, 108, 32, 0xFF808080);
        gfx.fill(108, 27, 110, 31, 0xFF808080);
        // Cooking time
        String timeText = recipe.getCookingTime() / 20 + "s";
        gfx.drawString(Minecraft.getInstance().font, timeText, 78, 34, 0xFF808080, false);
    }
}
