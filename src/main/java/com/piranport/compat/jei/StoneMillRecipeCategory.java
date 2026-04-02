package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.recipe.StoneMillRecipe;
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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class StoneMillRecipeCategory implements IRecipeCategory<StoneMillRecipe> {
    public static final RecipeType<StoneMillRecipe> RECIPE_TYPE =
            RecipeType.create(PiranPort.MOD_ID, "stone_mill", StoneMillRecipe.class);

    private final IDrawable icon;
    private final Component title;

    public StoneMillRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.STONE_MILL.get()));
        this.title = Component.translatable("jei.piranport.stone_mill");
    }

    @Override
    public RecipeType<StoneMillRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 110; }

    @Override
    public int getHeight() { return 42; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StoneMillRecipe recipe, IFocusGroup focuses) {
        var ingredients = recipe.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1 + (i % 2) * 18, 5 + (i / 2) * 18)
                    .addIngredients(ingredients.get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 13)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(StoneMillRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // Arrow shaft
        gfx.fill(44, 18, 78, 20, 0xFF808080);
        // Arrow head
        gfx.fill(72, 15, 78, 23, 0xFF808080);
        gfx.fill(78, 16, 80, 22, 0xFF808080);
        gfx.fill(80, 17, 82, 21, 0xFF808080);
    }
}
