package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.recipe.CuttingBoardRecipe;
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

public class CuttingBoardRecipeCategory implements IRecipeCategory<CuttingBoardRecipe> {
    public static final RecipeType<CuttingBoardRecipe> RECIPE_TYPE =
            RecipeType.create(PiranPort.MOD_ID, "cutting_board", CuttingBoardRecipe.class);

    private final IDrawable icon;
    private final Component title;

    public CuttingBoardRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.CUTTING_BOARD.get()));
        this.title = Component.translatable("jei.piranport.cutting_board");
    }

    @Override
    public RecipeType<CuttingBoardRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 110; }

    @Override
    public int getHeight() { return 30; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CuttingBoardRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 7)
                .addIngredients(recipe.getIngredient());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 7)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(CuttingBoardRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // Arrow shaft
        gfx.fill(26, 13, 60, 15, 0xFF808080);
        // Arrow head
        gfx.fill(54, 10, 60, 18, 0xFF808080);
        gfx.fill(60, 11, 62, 17, 0xFF808080);
        gfx.fill(62, 12, 64, 16, 0xFF808080);
        // Cuts count
        gfx.drawString(Minecraft.getInstance().font, "\u00d7" + recipe.getCuts(),
                66, 10, 0xFF808080, false);
    }
}
