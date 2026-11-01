package com.piranport.compat.jei;

import com.piranport.crafting.WeaponWorkbenchRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 武器制造台的 JEI 展示配方。配方本体来自硬编码注册表
 * {@link com.piranport.crafting.WeaponWorkbenchRecipeRegistry}，这里只做展示层的展开。
 */
public record WeaponWorkbenchJEIRecipe(
        int tab,
        List<ItemStack> inputs,
        ItemStack output,
        int craftTimeTicks,
        @Nullable String unlockStage
) {
    /** 页签顺序与 WeaponWorkbenchScreen 的 TAB_TOOLTIPS 保持一致。 */
    private static final String[] TAB_KEYS = {
            "gui.piranport.workbench.tab.cannon",
            "gui.piranport.workbench.tab.torpedo",
            "gui.piranport.workbench.tab.missile",
            "gui.piranport.workbench.tab.depth_charge",
            "gui.piranport.workbench.tab.aircraft",
            "gui.piranport.workbench.tab.equipment"
    };

    public static WeaponWorkbenchJEIRecipe fromRecipe(WeaponWorkbenchRecipe recipe) {
        // 复制材料栈，避免 JEI 侧的展示逻辑改动注册表里的共享 ItemStack。
        List<ItemStack> inputs = new ArrayList<>(recipe.materials().stream()
                .map(ItemStack::copy)
                .toList());
        // 蓝图作为额外输入展示：它不消耗，但合成时必须在蓝图槽里。
        if (recipe.requiredBlueprint() != null) {
            inputs.add(new ItemStack(recipe.requiredBlueprint()));
        }
        return new WeaponWorkbenchJEIRecipe(
                recipe.tab(), List.copyOf(inputs), recipe.getResultStack(),
                recipe.craftingTime(), recipe.unlockStage());
    }

    /** 页签翻译 key；越界时夹到合法范围，避免 JEI 里出现空白标题。 */
    public static String tabKey(int tab) {
        int index = Math.max(0, Math.min(TAB_KEYS.length - 1, tab));
        return TAB_KEYS[index];
    }

    public Component tabName() {
        return Component.translatable(tabKey(tab));
    }
}
