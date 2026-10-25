package com.piranport.compat.jei;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JEI 弹药合成台必须展示硬编码注册表里的全部五类弹药，而不是只显示炮弹。
 *
 * <p>这里用源码扫描而不是直接调用 {@code AmmoRecipeRegistry}：它的初始化会调用模组物品
 * {@code DeferredHolder#get()}，普通单测 JVM 没有 bootstrap 注册表会直接抛错；
 * 项目里同类轻量校验（如 {@code ReloadCreativeAmmoTest}）也采用读源码的写法。</p>
 */
class AmmoWorkbenchJeiCoverageTest {

    private static final Path REGISTRY =
            Path.of("src/main/java/com/piranport/ammo/AmmoRecipeRegistry.java");
    private static final Path PLUGIN =
            Path.of("src/main/java/com/piranport/compat/jei/PiranPortJEIPlugin.java");
    private static final Path CATEGORY =
            Path.of("src/main/java/com/piranport/compat/jei/AmmoWorkbenchRecipeCategory.java");

    @Test
    void registryHoldsAllCategories() throws Exception {
        String source = Files.readString(REGISTRY);

        assertEquals(14, count(source, "AmmoCategory.SHELL"), "炮弹配方数");
        assertEquals(23, count(source, "AmmoCategory.TORPEDO"), "鱼雷配方数");
        assertEquals(4, count(source, "AmmoCategory.AERIAL"), "航弹配方数");
        assertEquals(1, count(source, "AmmoCategory.DEPTH_CHARGE"), "深弹配方数");
        assertEquals(5, count(source, "AmmoCategory.MISSILE"), "导弹配方数");
        assertEquals(47, count(source, "add(\""), "注册表配方总数");
    }

    @Test
    void pluginRegistersEveryCategoryInsteadOfShellOnly() throws Exception {
        String source = Files.readString(PLUGIN);
        int start = source.indexOf("public void registerRecipes(");
        int end = source.indexOf("public void registerRecipeCatalysts(");
        assertTrue(start >= 0 && end > start, "未找到 registerRecipes 方法");
        String method = source.substring(start, end);

        assertTrue(method.contains("AmmoCategory.values()"),
                "JEI 注册必须遍历 AmmoCategory.values()，否则新增类别会漏");
        assertFalse(method.contains("AmmoCategory.SHELL"),
                "JEI 注册不能只过滤 SHELL，否则鱼雷/航弹/深弹/导弹看不到");
    }

    @Test
    void everyRecipeFitsTheTwoRowLayout() throws Exception {
        String registry = Files.readString(REGISTRY);
        String category = Files.readString(CATEGORY);

        int slotsPerRow = intConstant(category, "SLOTS_PER_ROW");
        long rows = Pattern.compile("INPUT_Y_ROW\\d+ = \\d+").matcher(category).results().count();
        assertTrue(rows > 0, "未找到 INPUT_Y_ROW* 布局常量");

        // 每条配方块 = 从 add(" 到下一个 add(" 之间的源码
        String[] blocks = registry.split("\\n\\s*add\\(\"");
        int maxInputs = 0;
        for (int i = 1; i < blocks.length; i++) {
            int inputs = count(blocks[i], "mat(")
                    + (blocks[i].contains("_BLUEPRINT") ? 1 : 0);
            maxInputs = Math.max(maxInputs, inputs);
        }

        assertTrue(maxInputs <= slotsPerRow * rows,
                "最多 " + maxInputs + " 个输入（含蓝图），超出 " + slotsPerRow + "×" + rows
                        + " 的槽位容量，JEI 里会重叠或越界");
    }

    @Test
    void blankCaliberDoesNotLeaveATrailingDash() {
        // 只测文字逻辑：普通单测 JVM 没有 bootstrap，连 ItemStack.EMPTY 的静态初始化都会失败，
        // 而 displayLabel 不读取产物字段，所以产物传 null。
        AmmoWorkbenchJEIRecipe recipe = new AmmoWorkbenchJEIRecipe(
                List.of(), null, 40, "深水炸弹", "");
        assertEquals("深水炸弹", recipe.displayLabel());

        AmmoWorkbenchJEIRecipe withCaliber = new AmmoWorkbenchJEIRecipe(
                List.of(), null, 40, "标准", "533mm");
        assertEquals("标准 - 533mm", withCaliber.displayLabel());
    }

    private static int count(String source, String needle) {
        int count = 0;
        for (int i = source.indexOf(needle); i >= 0; i = source.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    private static int intConstant(String source, String name) {
        Matcher matcher = Pattern.compile(name + " = (\\d+)").matcher(source);
        assertTrue(matcher.find(), "未找到常量 " + name);
        return Integer.parseInt(matcher.group(1));
    }
}
