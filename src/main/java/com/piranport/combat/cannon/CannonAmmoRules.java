package com.piranport.combat.cannon;

import com.piranport.combat.data.WeaponState;
import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.piranport.item.ShipCoreItem;
import static com.piranport.combat.cannon.CannonStats.getBarrelCount;

/** 火炮弹药规则：标签口径匹配、弹种识别、默认弹药与已装弹校验。 */
public final class CannonAmmoRules {
    private CannonAmmoRules() {}

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        return matchesCaliber(ammo, weapon, null);
    }

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ammo.is(ShipCoreItem.SMALL_SHELLS);
            if (caliber <= 8) return ammo.is(ShipCoreItem.MEDIUM_SHELLS);
            return ammo.is(ShipCoreItem.LARGE_SHELLS);
        }
        return false;
    }

    public static boolean isHEShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get())
                || isMK23Shell(stack);
    }

    public static boolean isMK23Shell(ItemStack stack) {
        return stack.is(ModItems.MK23_NUCLEAR_SHELL.get());
    }

    public static boolean isVTShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_VT_SHELL.get());
    }

    public static boolean isType3Shell(ItemStack stack) {
        return stack.is(ModItems.SMALL_TYPE3_SHELL.get())
                || stack.is(ModItems.MEDIUM_TYPE3_SHELL.get())
                || stack.is(ModItems.LARGE_TYPE3_SHELL.get());
    }

    public static boolean isAPShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_AP_SHELL.get())
                || stack.is(ModItems.MEDIUM_AP_SHELL.get())
                || stack.is(ModItems.LARGE_AP_SHELL.get())
                // 依据：弹药-AP弹穿甲设计.md —— 91 式 / 一式 / 超重弹均为 AP 子类
                || stack.is(ModItems.TYPE_91_AP_SHELL.get())
                || stack.is(ModItems.TYPE_1_AP_SHELL.get())
                || stack.is(ModItems.SUPER_HEAVY_AP_SHELL.get());
    }

    public static boolean isLoadedCannonAmmoValid(LoadedAmmo loaded, ItemStack weapon,
            int barrelCount, @Nullable Level level) {
        if (!loaded.hasAmmo() || loaded.count() < barrelCount) return false;
        ItemStack ammo = createAmmoStack(loaded.ammoItemId());
        return !ammo.isEmpty() && matchesCaliber(ammo, weapon, level);
    }

    public static boolean isCannonReadyToFire(ItemStack weapon, @Nullable Level level) {
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) return false;
        int barrelCount = getBarrelCount(weapon, level);
        WeaponState ws = new WeaponState(weapon);
        LoadedAmmo loaded = ws.getLoadedAmmo();
        return isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level);
    }

    public static ItemStack createAmmoStack(String ammoItemId) {
        if (ammoItemId == null || ammoItemId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation rl = ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    public static Item getDefaultAmmoForWeapon(ItemStack weapon) {
        return getDefaultAmmoForWeapon(weapon, null);
    }

    public static Item getDefaultAmmoForWeapon(ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ModItems.SMALL_AP_SHELL.get();
            if (caliber <= 8) return ModItems.MEDIUM_AP_SHELL.get();
            return ModItems.LARGE_AP_SHELL.get();
        }
        return null;
    }
}
