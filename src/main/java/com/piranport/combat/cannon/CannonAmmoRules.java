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
import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.combat.cannon.ammo.AmmoDefinitionService;
import static com.piranport.combat.cannon.CannonStats.getBarrelCount;

/** 火炮弹药规则：标签口径匹配、弹种识别、默认弹药与已装弹校验。 */
public final class CannonAmmoRules {
    private CannonAmmoRules() {}

    /** 口径族：唯一事实来源。玩家路径的标签匹配与女仆路径的候选表选择都必须用它，否则两条路径会漂移。 */
    public enum CaliberFamily {
        SMALL, MEDIUM, LARGE
    }

    /**
     * 由**口径**判定口径族。
     * <p>判据必须是 caliber 而非 damage：caliber 与 damage 都是可被 CSV 运行时覆盖的独立字段，
     * 而 {@code ArtilleryCannonData} 不校验两者是否同族，故「damage 大的炮口径也大」只是当前数值表的巧合、
     * 不是不变量。以 damage 为判据会在有人改配置或新增炮时静默选错族的弹。
     */
    public static CaliberFamily familyForCaliber(int caliber) {
        if (caliber <= 4) return CaliberFamily.SMALL;
        if (caliber <= 8) return CaliberFamily.MEDIUM;
        return CaliberFamily.LARGE;
    }

    /** 从规范数据派生口径族；分类只依赖真实口径，不读取具体物品 ID。 */
    public static CaliberFamily familyForData(ArtilleryCannonData data) {
        if (data == null) throw new IllegalArgumentException("cannon data must not be null");
        return familyForCaliber(data.caliber());
    }

    /** 取武器当前生效的口径（含配置覆盖），供 {@link #familyForCaliber} 使用。 */
    public static CaliberFamily familyForWeapon(ItemStack weapon, @Nullable Level level) {
        int caliber = weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai
                ? (level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber())
                : 0;
        return familyForCaliber(caliber);
    }

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        return matchesCaliber(ammo, weapon, null);
    }

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            return switch (familyForWeapon(weapon, level)) {
                case SMALL -> ammo.is(ShipCoreItem.SMALL_SHELLS);
                case MEDIUM -> ammo.is(ShipCoreItem.MEDIUM_SHELLS);
                case LARGE -> ammo.is(ShipCoreItem.LARGE_SHELLS);
            };
        }
        return false;
    }

    public static boolean isHEShell(ItemStack stack) {
        var definition = AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (definition.isPresent()) return definition.get().isHighExplosive();
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get())
                || isMK23Shell(stack);
    }

    public static boolean isMK23Shell(ItemStack stack) {
        return stack.is(ModItems.MK23_NUCLEAR_SHELL.get());
    }

    public static boolean isVTShell(ItemStack stack) {
        var definition = AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (definition.isPresent()) return definition.get().behavior()
                == com.piranport.combat.cannon.ammo.AmmoBehavior.VT;
        return stack.is(ModItems.SMALL_VT_SHELL.get());
    }

    public static boolean isType3Shell(ItemStack stack) {
        var definition = AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (definition.isPresent()) return definition.get().behavior()
                == com.piranport.combat.cannon.ammo.AmmoBehavior.TYPE3;
        return stack.is(ModItems.SMALL_TYPE3_SHELL.get())
                || stack.is(ModItems.MEDIUM_TYPE3_SHELL.get())
                || stack.is(ModItems.LARGE_TYPE3_SHELL.get());
    }

    public static boolean isAPShell(ItemStack stack) {
        var definition = AmmoDefinitionService.find(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (definition.isPresent()) return definition.get().isArmorPiercing();
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
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            return switch (familyForWeapon(weapon, level)) {
                case SMALL -> ModItems.SMALL_AP_SHELL.get();
                case MEDIUM -> ModItems.MEDIUM_AP_SHELL.get();
                case LARGE -> ModItems.LARGE_AP_SHELL.get();
            };
        }
        return null;
    }
}
