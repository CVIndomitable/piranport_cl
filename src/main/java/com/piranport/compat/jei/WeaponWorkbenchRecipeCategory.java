package com.piranport.compat.jei;

import com.piranport.PiranPort;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
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

/**
 * 武器制造台的 JEI 分类。布局对齐 {@link AmmoWorkbenchRecipeCategory}：
 * 输入按每行 3 个排列，箭头与输出槽位置一致；行数由注册表里最大的输入数决定。
 */
public class WeaponWorkbenchRecipeCategory implements IRecipeCategory<WeaponWorkbenchJEIRecipe> {
    public static final RecipeType<WeaponWorkbenchJEIRecipe> RECIPE_TYPE =
            RecipeType.create(PiranPort.MOD_ID, "weapon_workbench", WeaponWorkbenchJEIRecipe.class);

    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 3;
    private static final int INPUT_X = 1;
    private static final int INPUT_Y_TOP = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int ARROW_X = 66;
    private static final int ARROW_WIDTH = 28;
    private static final int ARROW_Y = 13;
    private static final int OUTPUT_X = 108;
    private static final int OUTPUT_Y = 4;

    // 注册表是硬编码的，这里直接统计整张表：材料数 + 蓝图（若需要）。
    // 当前最多 5 个输入（4 材料 + 1 蓝图），两行足够；将来配方变多会自动加行并调高分类高度。
    private static final int MAX_INPUTS = WeaponWorkbenchRecipeRegistry.getAllRecipes().stream()
            .mapToInt(r -> r.materials().size() + (r.requiredBlueprint() != null ? 1 : 0))
            .max().orElse(0);
    private static final int ROWS = Math.max(1, (MAX_INPUTS + SLOTS_PER_ROW - 1) / SLOTS_PER_ROW);
    /** 页签名/解锁说明文字放在最后一行输入槽下方。 */
    private static final int TEXT_Y = INPUT_Y_TOP + (ROWS - 1) * ROW_HEIGHT + SLOT_SIZE + 2;

    private final IDrawable icon;
    private final Component title;

    public WeaponWorkbenchRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.WEAPON_WORKBENCH.get()));
        this.title = Component.translatable("jei.piranport.weapon_workbench");
    }

    @Override
    public RecipeType<WeaponWorkbenchJEIRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 140; }

    @Override
    public int getHeight() { return Math.max(60, TEXT_Y + 22); }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WeaponWorkbenchJEIRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;
            int x = INPUT_X + col * SLOT_SIZE;
            int y = INPUT_Y_TOP + row * ROW_HEIGHT;
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addItemStack(inputs.get(i));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.output());
    }

    @Override
    public void draw(WeaponWorkbenchJEIRecipe recipe, IRecipeSlotsView slotsView, GuiGraphics gfx, double mouseX, double mouseY) {
        // 箭头（与弹药合成台一致）
        int arrowColor = 0xFF808080;
        gfx.fill(ARROW_X, ARROW_Y, ARROW_X + ARROW_WIDTH, ARROW_Y + 2, arrowColor);
        gfx.fill(ARROW_X + ARROW_WIDTH - 6, ARROW_Y - 3, ARROW_X + ARROW_WIDTH, ARROW_Y + 5, arrowColor);
        gfx.fill(ARROW_X + ARROW_WIDTH - 3, ARROW_Y - 2, ARROW_X + ARROW_WIDTH - 1, ARROW_Y + 4, arrowColor);

        Minecraft mc = Minecraft.getInstance();
        // 页签名
        gfx.drawString(mc.font, recipe.tabName(), INPUT_X, TEXT_Y, 0xFF404040, false);
        // 通关门槛：需要时再占一行，避免把「无门槛」写出来
        if (recipe.unlockStage() != null) {
            Component unlock = Component.translatable(
                    "jei.piranport.weapon_workbench.unlock", recipe.unlockStage());
            gfx.drawString(mc.font, unlock, INPUT_X, TEXT_Y + 10, 0xFF404040, false);
        }

        // 合成时间
        String timeText = recipe.craftTimeTicks() / 20 + "s";
        gfx.drawString(mc.font, timeText, OUTPUT_X, OUTPUT_Y + SLOT_SIZE + 2, 0xFF808080, false);
    }
}
