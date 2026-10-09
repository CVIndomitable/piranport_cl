package com.piranport.crafting;

import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.component.EquipmentTier;
import com.piranport.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeaponWorkbenchRecipeRegistry {
    private static final List<WeaponWorkbenchRecipe> ALL_RECIPES;
    private static final Map<Integer, List<WeaponWorkbenchRecipe>> TAB_CACHE;

    static {
        List<WeaponWorkbenchRecipe> recipes = new ArrayList<>();
        registerAll(recipes);
        ALL_RECIPES = List.copyOf(recipes);

        Map<Integer, List<WeaponWorkbenchRecipe>> byTab = new HashMap<>();
        for (WeaponWorkbenchRecipe r : ALL_RECIPES) {
            byTab.computeIfAbsent(r.tab(), k -> new ArrayList<>()).add(r);
        }
        Map<Integer, List<WeaponWorkbenchRecipe>> frozen = new HashMap<>();
        for (Map.Entry<Integer, List<WeaponWorkbenchRecipe>> e : byTab.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        TAB_CACHE = Map.copyOf(frozen);
    }

    private WeaponWorkbenchRecipeRegistry() {}

    public static List<WeaponWorkbenchRecipe> getAllRecipes() {
        return ALL_RECIPES;
    }

    public static List<WeaponWorkbenchRecipe> getRecipesForTab(int tab) {
        return TAB_CACHE.getOrDefault(tab, List.of());
    }

    @Nullable
    public static WeaponWorkbenchRecipe getRecipe(int tab, int index) {
        List<WeaponWorkbenchRecipe> tabRecipes = getRecipesForTab(tab);
        if (index >= 0 && index < tabRecipes.size()) {
            return tabRecipes.get(index);
        }
        return null;
    }

    private static void add(List<WeaponWorkbenchRecipe> list, int tab, Item result, List<ItemStack> materials,
                            @Nullable Item blueprint, int craftingTime) {
        list.add(new WeaponWorkbenchRecipe(tab, result, materials, blueprint, craftingTime));
    }

    /** 档位 → 所需蓝图；初期返回 null（无需蓝图）。 */
    @Nullable
    public static Item blueprintFor(EquipmentTier tier) {
        return switch (tier) {
            case INITIAL -> null;
            case STANDARD -> ModItems.STANDARD_WEAPON_BLUEPRINT.get();
            case IMPROVED -> ModItems.IMPROVED_WEAPON_BLUEPRINT.get();
            case ADVANCED -> ModItems.ADVANCED_WEAPON_BLUEPRINT.get();
        };
    }

    /** 能放进武器制造台蓝图格 / 蓝图箱的物品。 */
    public static boolean isWorkbenchBlueprint(ItemStack stack) {
        return stack.is(ModItems.CREATIVE_BLUEPRINT.get())
                || stack.is(ModItems.MEDIUM_GUN_BLUEPRINT.get())
                || stack.is(ModItems.LARGE_GUN_BLUEPRINT.get())
                || stack.is(ModItems.STANDARD_WEAPON_BLUEPRINT.get())
                || stack.is(ModItems.IMPROVED_WEAPON_BLUEPRINT.get())
                || stack.is(ModItems.ADVANCED_WEAPON_BLUEPRINT.get());
    }

    /**
     * 配方蓝图校验：requiredBlueprint 为 null = 无需蓝图（蓝图格可空）；创造蓝图通吃。
     * 依据：副本/21 §1.2「其余配方传 null = 直接可用」。旧实现把 null 当成「不可合成」，
     * 导致鱼雷/导弹/深弹/飞机配方在生存模式永远做不出来。
     */
    public static boolean blueprintSatisfied(WeaponWorkbenchRecipe recipe, ItemStack inSlot) {
        if (!inSlot.isEmpty() && inSlot.is(ModItems.CREATIVE_BLUEPRINT.get())) return true;
        if (recipe.requiredBlueprint() == null) return true;
        return !inSlot.isEmpty() && inSlot.is(recipe.requiredBlueprint());
    }

    /**
     * 火炮材料（项目所有者 2026-09-30 授权由实现方编配方）：
     * 按口径族定基础材料，按联装数放大，档位追加稀有材料。数值是首轮占位，待实测。
     */
    static List<ItemStack> gunMaterials(CannonAmmoRules.CaliberFamily family, int barrels, EquipmentTier tier) {
        int n = Math.max(1, barrels);
        List<ItemStack> m = new ArrayList<>();
        switch (family) {
            case SMALL -> {
                m.add(new ItemStack(Items.IRON_INGOT, 2 + n));
                m.add(new ItemStack(Items.COPPER_INGOT, 2));
                m.add(new ItemStack(Items.REDSTONE, 1));
            }
            case MEDIUM -> {
                m.add(new ItemStack(Items.IRON_INGOT, 4 + 2 * n));
                m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2 + n));
                m.add(new ItemStack(Items.REDSTONE, 2));
            }
            case LARGE -> {
                m.add(new ItemStack(Items.IRON_INGOT, 8 + 4 * n));
                m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4 + 2 * n));
                m.add(new ItemStack(Items.REDSTONE, 4));
                m.add(new ItemStack(Items.DIAMOND, 1));
            }
        }
        addTierExtras(m, tier, family == CannonAmmoRules.CaliberFamily.LARGE);
        return List.copyOf(m);
    }

    /** 鱼雷发射器材料：按口径（533/610/720）与管数放大，档位追加同火炮。 */
    static List<ItemStack> torpedoMaterials(int caliberMm, int tubes, EquipmentTier tier) {
        int n = Math.max(1, tubes);
        int sizeStep = caliberMm >= 720 ? 2 : caliberMm >= 610 ? 1 : 0;
        List<ItemStack> m = new ArrayList<>();
        m.add(new ItemStack(Items.IRON_INGOT, 2 + n + 2 * sizeStep));
        m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 1 + n + sizeStep));
        m.add(new ItemStack(Items.REDSTONE, 1 + sizeStep));
        addTierExtras(m, tier, sizeStep == 2);
        return List.copyOf(m);
    }

    private static void addTierExtras(List<ItemStack> m, EquipmentTier tier, boolean large) {
        switch (tier) {
            case INITIAL -> { }
            case STANDARD -> m.add(new ItemStack(Items.GOLD_INGOT, 1));
            case IMPROVED -> {
                m.add(new ItemStack(Items.GOLD_INGOT, 3));
                addOrMerge(m, Items.DIAMOND, 1);
            }
            case ADVANCED -> {
                // 不再叠金锭：材料种类上限 5（+蓝图 = 6 格，制造台需求区 3 列×2 行）
                addOrMerge(m, Items.DIAMOND, large ? 3 : 2);
                m.add(new ItemStack(Items.NETHERITE_SCRAP, 1));
            }
        }
    }

    private static void addOrMerge(List<ItemStack> m, Item item, int count) {
        for (int i = 0; i < m.size(); i++) {
            if (m.get(i).is(item)) {
                m.set(i, new ItemStack(item, m.get(i).getCount() + count));
                return;
            }
        }
        m.add(new ItemStack(item, count));
    }

    static int craftTime(int baseTicks, EquipmentTier tier) {
        return switch (tier) {
            case INITIAL -> baseTicks;
            case STANDARD -> baseTicks * 5 / 4;
            case IMPROVED -> baseTicks * 3 / 2;
            case ADVANCED -> baseTicks * 2;
        };
    }

    private static void registerAll(List<WeaponWorkbenchRecipe> r) {
        // ===== Tab 0: 火炮 =====
        add(r, 0, ModItems.SINGLE_SMALL_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GUNPOWDER, 1)
        ), null, 80);

        add(r, 0, ModItems.SINGLE_SMALL_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GUNPOWDER, 1)
        ), null, 100);

        add(r, 0, ModItems.MEDIUM_GUN.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 200);

        // 数值/07 注册表 60 门：按档位验稀有度蓝图，按口径族与联装定材料（2026-09-30）。
        for (Item gun : com.piranport.registry.WeaponItems.allCatalogGuns()) {
            if (!(gun instanceof com.piranport.artillery.ArtilleryItem artillery)) continue;
            var data = artillery.getData();
            EquipmentTier tier = data.tierOrDefault();
            var family = CannonAmmoRules.familyForData(data);
            int base = switch (family) { case SMALL -> 100; case MEDIUM -> 200; case LARGE -> 400; };
            add(r, 0, gun, gunMaterials(family, data.barrels(), tier), blueprintFor(tier), craftTime(base, tier));
        }

        // ===== Tab 1: 鱼雷发射器 =====
        // 通用发射器 + 数值/08 图鉴发射器，统一按档位验蓝图。
        List<com.piranport.item.TorpedoLauncherItem> launchers = new ArrayList<>();
        launchers.add(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
        launchers.add(ModItems.QUAD_TORPEDO_LAUNCHER.get());
        launchers.add(ModItems.QUINTUPLE_TORPEDO_LAUNCHER.get());
        ModItems.CATALOG_TORPEDO_LAUNCHERS.forEach(l -> launchers.add(l.get()));
        for (var launcher : launchers) {
            EquipmentTier tier = launcher.getTier();
            int base = 150 + 25 * launcher.getTubeCount();
            add(r, 1, launcher, torpedoMaterials(launcher.getCaliber(), launcher.getTubeCount(), tier),
                    blueprintFor(tier), craftTime(base, tier));
        }

        // ===== Tab 2: 导弹发射器 =====
        add(r, 2, ModItems.TERRIER_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(r, 2, ModItems.SEA_DART_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(r, 2, ModItems.SEACAT_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        add(r, 2, ModItems.SY1_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 300);

        add(r, 2, ModItems.MK14_HARPOON_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GOLD_INGOT, 2),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 300);

        add(r, 2, ModItems.SHIP_ROCKET_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GUNPOWDER, 2)
        ), null, 150);

        // ===== Tab 3: 深弹 =====
        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 3),
                new ItemStack(Items.REDSTONE, 1)
        ), null, 100);

        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 5),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 150);

        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 200);

        // ===== Tab 4: 飞机 =====
        add(r, 4, ModItems.B25_BOMBER.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4),
                new ItemStack(Items.REDSTONE, 2),
                new ItemStack(Items.TNT, 1)
        ), null, 250);

        add(r, 4, ModItems.SAIUN_RECON.get(), List.of(
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2),
                new ItemStack(Items.REDSTONE, 1),
                new ItemStack(Items.GOLD_INGOT, 1)
        ), null, 150);
    }
}
