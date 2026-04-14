package com.piranport.crafting;

import com.piranport.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 武器合成台配方注册表（静态，懒加载）。
 * 客户端和服务端共享同一份数据，通过 tab + index 定位配方。
 */
public class WeaponWorkbenchRecipeRegistry {
    private static volatile List<WeaponWorkbenchRecipe> ALL_RECIPES;

    public static List<WeaponWorkbenchRecipe> getAllRecipes() {
        if (ALL_RECIPES == null) {
            synchronized (WeaponWorkbenchRecipeRegistry.class) {
                if (ALL_RECIPES == null) {
                    List<WeaponWorkbenchRecipe> recipes = new ArrayList<>();
                    ALL_RECIPES = recipes;
                    registerAll();
                }
            }
        }
        return ALL_RECIPES;
    }

    public static List<WeaponWorkbenchRecipe> getRecipesForTab(int tab) {
        return getAllRecipes().stream()
                .filter(r -> r.tab() == tab)
                .toList();
    }

    @Nullable
    public static WeaponWorkbenchRecipe getRecipe(int tab, int index) {
        List<WeaponWorkbenchRecipe> tabRecipes = getRecipesForTab(tab);
        if (index >= 0 && index < tabRecipes.size()) {
            return tabRecipes.get(index);
        }
        return null;
    }

    private static void add(int tab, Item result, List<ItemStack> materials,
                            @Nullable Item blueprint, int craftingTime) {
        ALL_RECIPES.add(new WeaponWorkbenchRecipe(tab, result, materials, blueprint, craftingTime));
    }

    private static void registerAll() {
        // ===== Tab 0: 火炮 =====
        add(0, ModItems.SINGLE_SMALL_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GUNPOWDER, 1)
        ), null, 80);

        add(0, ModItems.SMALL_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GUNPOWDER, 1)
        ), null, 100);

        add(0, ModItems.MEDIUM_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), ModItems.MEDIUM_GUN_BLUEPRINT.get(), 200);

        add(0, ModItems.LARGE_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.GOLD_INGOT, 4),
                new ItemStack(Items.DIAMOND, 2)
        ), ModItems.LARGE_GUN_BLUEPRINT.get(), 400);

        // ===== Tab 1: 鱼雷发射器 =====
        add(1, ModItems.TWIN_TORPEDO_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2)
        ), null, 150);

        add(1, ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 3)
        ), null, 200);

        add(1, ModItems.QUAD_TORPEDO_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4),
                new ItemStack(Items.DIAMOND, 1)
        ), null, 300);

        // ===== Tab 2: 导弹发射器 =====
        add(2, ModItems.TERRIER_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(2, ModItems.SEA_DART_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(2, ModItems.SEACAT_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(2, ModItems.SY1_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 300);

        add(2, ModItems.MK14_HARPOON_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 300);

        add(2, ModItems.SHIP_ROCKET_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GUNPOWDER, 2)
        ), null, 150);

        // ===== Tab 3: 深弹 =====
        add(3, ModItems.DEPTH_CHARGE_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.REDSTONE, 1)
        ), null, 100);

        add(3, ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 5),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 150);

        add(3, ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        // ===== Tab 4: 飞机 =====
        add(4, ModItems.FIGHTER_SQUADRON.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 3),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GUNPOWDER, 1)
        ), null, 200);

        add(4, ModItems.DIVE_BOMBER_SQUADRON.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 3),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.TNT, 1)
        ), null, 200);



        add(4, ModItems.B25_BOMBER.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.TNT, 1)
        ), null, 250);

        add(4, ModItems.RECON_SQUADRON.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 150);
    }
}
