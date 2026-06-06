package com.piranport.ammo;

import com.piranport.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AmmoRecipeRegistry {
    private static final List<AmmoRecipe> ALL_RECIPES = new ArrayList<>();
    private static volatile boolean initialized = false;

    private static synchronized void ensureInitialized() {
        if (initialized) return;
        initialized = true;
        registerShells();
        registerTorpedoes();
        registerAerialAmmo();
        registerDepthCharges();
        registerMissiles();
    }

    // ===== Query methods =====

    public static List<AmmoRecipe> getRecipesForCategory(AmmoCategory cat) {
        ensureInitialized();
        return ALL_RECIPES.stream().filter(r -> r.category() == cat).toList();
    }

    public static List<String> getTypesForCategory(AmmoCategory cat) {
        ensureInitialized();
        return ALL_RECIPES.stream()
                .filter(r -> r.category() == cat)
                .map(AmmoRecipe::typeName)
                .distinct()
                .collect(Collectors.toList());
    }

    public static List<String> getCalibersForType(AmmoCategory cat, String typeName) {
        ensureInitialized();
        return ALL_RECIPES.stream()
                .filter(r -> r.category() == cat && r.typeName().equals(typeName))
                .map(AmmoRecipe::caliberName)
                .distinct()
                .collect(Collectors.toList());
    }

    public static AmmoRecipe findRecipe(AmmoCategory cat, String typeName, String caliberName) {
        ensureInitialized();
        return ALL_RECIPES.stream()
                .filter(r -> r.category() == cat
                        && r.typeName().equals(typeName)
                        && r.caliberName().equals(caliberName))
                .findFirst().orElse(null);
    }

    public static AmmoRecipe findById(String id) {
        ensureInitialized();
        return ALL_RECIPES.stream().filter(r -> r.id().equals(id)).findFirst().orElse(null);
    }

    // ===== Helpers =====

    private static AmmoRecipe.MaterialRequirement mat(Supplier<Item> item, int count) {
        return new AmmoRecipe.MaterialRequirement(item, count);
    }

    private static void add(String id, AmmoCategory cat, String type, String caliber,
                            Supplier<Item> result, int output, int timeTicks,
                            AmmoRecipe.MaterialRequirement... mats) {
        ALL_RECIPES.add(new AmmoRecipe(id, cat, type, caliber, result, output,
                List.of(mats), timeTicks));
    }

    // ===== Shells =====

    private static void registerShells() {
        Supplier<Item> iron = () -> Items.IRON_INGOT;
        Supplier<Item> gp   = () -> Items.GUNPOWDER;
        Supplier<Item> gold = () -> Items.GOLD_NUGGET;
        Supplier<Item> rs   = () -> Items.REDSTONE;
        Supplier<Item> comp = () -> Items.COMPARATOR;

        // HE
        add("shell_he_s", AmmoCategory.SHELL, "HE弹", "小口径",
                () -> ModItems.SMALL_HE_SHELL.get(), 8, 40,
                mat(iron, 2), mat(gp, 1));
        add("shell_he_m", AmmoCategory.SHELL, "HE弹", "中口径",
                () -> ModItems.MEDIUM_HE_SHELL.get(), 8, 60,
                mat(iron, 3), mat(gp, 2));
        add("shell_he_l", AmmoCategory.SHELL, "HE弹", "大口径",
                () -> ModItems.LARGE_HE_SHELL.get(), 4, 80,
                mat(iron, 5), mat(gp, 3));

        // AP
        add("shell_ap_s", AmmoCategory.SHELL, "AP弹", "小口径",
                () -> ModItems.SMALL_AP_SHELL.get(), 8, 40,
                mat(iron, 2), mat(gp, 1), mat(gold, 2));
        add("shell_ap_m", AmmoCategory.SHELL, "AP弹", "中口径",
                () -> ModItems.MEDIUM_AP_SHELL.get(), 8, 60,
                mat(iron, 3), mat(gp, 2), mat(gold, 3));
        add("shell_ap_l", AmmoCategory.SHELL, "AP弹", "大口径",
                () -> ModItems.LARGE_AP_SHELL.get(), 4, 80,
                mat(iron, 5), mat(gp, 3), mat(gold, 5));

        // VT (small only)
        add("shell_vt_s", AmmoCategory.SHELL, "VT弹", "小口径",
                () -> ModItems.SMALL_VT_SHELL.get(), 4, 60,
                mat(iron, 2), mat(gp, 1), mat(rs, 2), mat(comp, 1));

        // Type 3
        add("shell_t3_s", AmmoCategory.SHELL, "三式弹", "小口径",
                () -> ModItems.SMALL_TYPE3_SHELL.get(), 8, 40,
                mat(iron, 2), mat(gp, 2), mat(rs, 1));
        add("shell_t3_m", AmmoCategory.SHELL, "三式弹", "中口径",
                () -> ModItems.MEDIUM_TYPE3_SHELL.get(), 8, 60,
                mat(iron, 3), mat(gp, 3), mat(rs, 1));
        add("shell_t3_l", AmmoCategory.SHELL, "三式弹", "大口径",
                () -> ModItems.LARGE_TYPE3_SHELL.get(), 4, 80,
                mat(iron, 5), mat(gp, 4), mat(rs, 2));
    }

    // ===== Torpedoes =====

    private static void registerTorpedoes() {
        Supplier<Item> iron = () -> Items.IRON_INGOT;
        Supplier<Item> gp   = () -> Items.GUNPOWDER;
        Supplier<Item> fuel = () -> ModItems.AVIATION_FUEL.get();
        Supplier<Item> rs   = () -> Items.REDSTONE;
        Supplier<Item> comp = () -> Items.COMPARATOR;
        Supplier<Item> torch = () -> Items.REDSTONE_TORCH;
        Supplier<Item> hook = () -> Items.TRIPWIRE_HOOK;
        Supplier<Item> str  = () -> Items.STRING;

        // === Generic ===
        // Standard
        add("torp_std_533", AmmoCategory.TORPEDO, "标准", "533mm",
                () -> ModItems.TORPEDO_533MM.get(), 4, 60,
                mat(iron, 4), mat(gp, 2), mat(fuel, 1));
        add("torp_std_610", AmmoCategory.TORPEDO, "标准", "610mm",
                () -> ModItems.TORPEDO_610MM.get(), 2, 80,
                mat(iron, 6), mat(gp, 3), mat(fuel, 2));
        // Magnetic
        add("torp_mag_533", AmmoCategory.TORPEDO, "磁性", "533mm",
                () -> ModItems.MAGNETIC_TORPEDO_533MM.get(), 4, 60,
                mat(iron, 4), mat(gp, 2), mat(fuel, 1), mat(rs, 3));
        // Wire-guided
        add("torp_wire_533", AmmoCategory.TORPEDO, "线导", "533mm",
                () -> ModItems.WIRE_GUIDED_TORPEDO_533MM.get(), 4, 60,
                mat(iron, 4), mat(gp, 2), mat(fuel, 1), mat(hook, 1), mat(str, 2));
        // Acoustic
        add("torp_aco_533", AmmoCategory.TORPEDO, "声导", "533mm",
                () -> ModItems.ACOUSTIC_TORPEDO_533MM.get(), 4, 60,
                mat(iron, 4), mat(gp, 2), mat(fuel, 1), mat(comp, 1), mat(torch, 1));

        // === Named 533mm standard ===
        add("torp_std_g7a", AmmoCategory.TORPEDO, "标准", "533mm G7a",
                () -> ModItems.TORPEDO_533MM_G7A.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1));
        add("torp_std_mk17", AmmoCategory.TORPEDO, "标准", "533mm MK17",
                () -> ModItems.TORPEDO_533MM_MK17.get(), 2, 80,
                mat(iron, 6), mat(gp, 3), mat(fuel, 2));
        add("torp_std_mk14", AmmoCategory.TORPEDO, "标准", "533mm MK14",
                () -> ModItems.TORPEDO_533MM_MK14.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1));
        add("torp_std_mk16", AmmoCategory.TORPEDO, "标准", "533mm MK16",
                () -> ModItems.TORPEDO_533MM_MK16.get(), 2, 80,
                mat(iron, 6), mat(gp, 3), mat(fuel, 2));
        add("torp_std_t95", AmmoCategory.TORPEDO, "标准", "530mm 九五式",
                () -> ModItems.TORPEDO_530MM_TYPE95.get(), 4, 60,
                mat(iron, 5), mat(gp, 3), mat(fuel, 1));

        // Named 610mm standard
        add("torp_std_t91", AmmoCategory.TORPEDO, "标准", "610mm 九一式",
                () -> ModItems.TORPEDO_610MM_TYPE91.get(), 2, 80,
                mat(iron, 7), mat(gp, 3), mat(fuel, 2));
        add("torp_std_t93m1", AmmoCategory.TORPEDO, "标准", "610mm 九三式一型",
                () -> ModItems.TORPEDO_610MM_TYPE93_MK1.get(), 2, 80,
                mat(iron, 8), mat(gp, 4), mat(fuel, 3));
        add("torp_std_t93m3", AmmoCategory.TORPEDO, "标准", "610mm 九三式三型",
                () -> ModItems.TORPEDO_610MM_TYPE93_MK3.get(), 1, 100,
                mat(iron, 9), mat(gp, 5), mat(fuel, 3));
        add("torp_std_t95m2", AmmoCategory.TORPEDO, "标准", "610mm 九五式二型",
                () -> ModItems.TORPEDO_610MM_TYPE95_MK2.get(), 2, 80,
                mat(iron, 8), mat(gp, 4), mat(fuel, 3));

        // Named 720mm
        add("torp_std_t0", AmmoCategory.TORPEDO, "标准", "720mm 零式",
                () -> ModItems.TORPEDO_720MM_TYPE0.get(), 1, 100,
                mat(iron, 10), mat(gp, 5), mat(fuel, 4));

        // Named magnetic
        add("torp_mag_g7a", AmmoCategory.TORPEDO, "磁性", "533mm G7a",
                () -> ModItems.MAGNETIC_TORPEDO_533MM_G7A.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1), mat(rs, 3));
        add("torp_mag_g7e", AmmoCategory.TORPEDO, "磁性", "533mm G7e",
                () -> ModItems.MAGNETIC_TORPEDO_533MM_G7E.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1), mat(rs, 3));

        // Named wire-guided
        add("torp_wire_g7e", AmmoCategory.TORPEDO, "线导", "533mm G7e",
                () -> ModItems.WIRE_GUIDED_TORPEDO_533MM_G7E.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1), mat(hook, 1), mat(str, 2));

        // Named acoustic
        add("torp_aco_g7e", AmmoCategory.TORPEDO, "声导", "533mm G7e",
                () -> ModItems.ACOUSTIC_TORPEDO_533MM_G7E.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1), mat(comp, 1), mat(torch, 1));
        add("torp_aco_mk27", AmmoCategory.TORPEDO, "声导", "533mm MK27",
                () -> ModItems.ACOUSTIC_TORPEDO_533MM_MK27.get(), 4, 60,
                mat(iron, 5), mat(gp, 2), mat(fuel, 1), mat(comp, 1), mat(torch, 1));
    }

    // ===== Aerial Ammo =====

    private static void registerAerialAmmo() {
        Supplier<Item> iron = () -> Items.IRON_INGOT;
        Supplier<Item> gp   = () -> Items.GUNPOWDER;
        Supplier<Item> fuel = () -> ModItems.AVIATION_FUEL.get();
        Supplier<Item> rs   = () -> Items.REDSTONE;

        add("aerial_bomb", AmmoCategory.AERIAL, "航弹", "标准",
                () -> ModItems.AERIAL_BOMB.get(), 8, 40,
                mat(iron, 2), mat(gp, 2), mat(rs, 1));
        add("aerial_torpedo", AmmoCategory.AERIAL, "航空鱼雷", "标准",
                () -> ModItems.AERIAL_TORPEDO.get(), 4, 60,
                mat(iron, 3), mat(gp, 2), mat(fuel, 1));
        add("fighter_ammo", AmmoCategory.AERIAL, "机枪弹", "标准",
                () -> ModItems.FIGHTER_AMMO.get(), 16, 40,
                mat(iron, 1), mat(gp, 1));
    }

    // ===== Depth Charges =====

    private static void registerDepthCharges() {
        Supplier<Item> iron = () -> Items.IRON_INGOT;
        Supplier<Item> gp   = () -> Items.GUNPOWDER;
        Supplier<Item> rs   = () -> Items.REDSTONE;

        add("depth_charge", AmmoCategory.DEPTH_CHARGE, "深水炸弹", "标准",
                () -> ModItems.DEPTH_CHARGE.get(), 4, 40,
                mat(iron, 3), mat(gp, 2), mat(rs, 1));
    }

    // ===== Missiles =====

    private static void registerMissiles() {
        Supplier<Item> iron = () -> Items.IRON_INGOT;
        Supplier<Item> gp   = () -> Items.GUNPOWDER;
        Supplier<Item> fuel = () -> ModItems.AVIATION_FUEL.get();
        Supplier<Item> rs   = () -> Items.REDSTONE;
        Supplier<Item> comp = () -> Items.COMPARATOR;

        add("missile_sy1", AmmoCategory.MISSILE, "反舰导弹", "SY-1",
                () -> ModItems.SY1_MISSILE.get(), 2, 100,
                mat(iron, 4), mat(gp, 3), mat(fuel, 1), mat(rs, 2), mat(comp, 1));
        add("missile_harpoon", AmmoCategory.MISSILE, "反舰导弹", "鱼叉",
                () -> ModItems.HARPOON_MISSILE.get(), 2, 100,
                mat(iron, 4), mat(gp, 2), mat(fuel, 1), mat(rs, 2), mat(comp, 1));
        add("missile_terrier", AmmoCategory.MISSILE, "防空导弹", "小猎犬",
                () -> ModItems.TERRIER_MISSILE.get(), 2, 80,
                mat(iron, 3), mat(gp, 2), mat(rs, 2), mat(comp, 1));
        add("missile_aa", AmmoCategory.MISSILE, "防空导弹", "通用",
                () -> ModItems.ANTI_AIR_MISSILE.get(), 4, 80,
                mat(iron, 2), mat(gp, 1), mat(rs, 2), mat(comp, 1));
        add("missile_rocket", AmmoCategory.MISSILE, "火箭弹", "标准",
                () -> ModItems.ROCKET_AMMO.get(), 8, 60,
                mat(iron, 2), mat(gp, 2), mat(rs, 1));
    }
}
