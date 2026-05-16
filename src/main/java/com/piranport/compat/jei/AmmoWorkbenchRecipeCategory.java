package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.ammo.AmmoRecipeRegistry;
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

import java.util.List;

public class AmmoWorkbenchRecipeCategory implements IRecipeCategory<AmmoWorkbenchJEIRecipe> {
    public static final RecipeType<AmmoWorkbenchJEIRecipe> RECIPE_TYPE =
            RecipeType.create(PiranPort.MOD_ID, "ammo_workbench", AmmoWorkbenchJEIRecipe.class);

    private static final int SLOT_SIZE = 18;
    private static final int INPUT_X = 1;
    private static final int INPUT_Y_ROW1 = 4;
    private static final int INPUT_Y_ROW2 = 22;
    private static final int SLOTS_PER_ROW = 3;
    private static final int ARROW_X = 66;
    private static final int ARROW_WIDTH = 28;
    private static final int ARROW_Y = 13;
    private static final int OUTPUT_X = 108;
    private static final int OUTPUT_Y = 4;

    private final IDrawable icon;
    private final Component title;

    public AmmoWorkbenchRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.AMMO_WORKBENCH.get()));
        this.title = Component.translatable("jei.piranport.ammo_workbench");
    }

    @Override
    public RecipeType<AmmoWorkbenchJEIRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 140; }

    @Override
    public int getHeight() { return 60; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AmmoWorkbenchJEIRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            int x = INPUT_X + col * SLOT_SIZE;
            int y = (row == 0) ? INPUT_Y_ROW1 : INPUT_Y_ROW2;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(inputs.get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(AmmoWorkbenchJEIRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // 箭头
        int arrowColor = 0xFF808080;
        gfx.fill(ARROW_X, ARROW_Y, ARROW_X + ARROW_WIDTH, ARROW_Y + 2, arrowColor);
        gfx.fill(ARROW_X + ARROW_WIDTH - 6, ARROW_Y - 3, ARROW_X + ARROW_WIDTH, ARROW_Y + 5, arrowColor);
        gfx.fill(ARROW_X + ARROW_WIDTH - 3, ARROW_Y - 2, ARROW_X + ARROW_WIDTH - 1, ARROW_Y + 4, arrowColor);

        // 类型/口径文字
        Minecraft mc = Minecraft.getInstance();
        String label = recipe.typeName() + " - " + recipe.caliberName();
        gfx.drawString(mc.font, label, INPUT_X, INPUT_Y_ROW2 + SLOT_SIZE + 2, 0xFF404040, false);

        // 合成时间
        String timeText = recipe.craftTimeTicks() / 20 + "s";
        gfx.drawString(mc.font, timeText, OUTPUT_X, OUTPUT_Y + SLOT_SIZE + 2, 0xFF808080, false);
    }
}
