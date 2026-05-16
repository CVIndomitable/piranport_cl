package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.ArmorPlateItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.item.SonarItem;
import com.piranport.item.TorpedoReloadItem;
import com.piranport.item.EngineItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 变身管理器 — 管理玩家从"人类形态"到"舰娘形态"的切换及相关属性计算。
 *
 * 职责概览：
 *   - 变身/解除变身（setTransformed / removeTransformationAttributes）
 *   - 属性计算: applyTransformationAttributes（GUI 模式和非 GUI 模式双路径）
 *   - 负重系统: WEAPON_LOAD_MAP 静态武器重量注册表 + getItemLoad 查询
 *   - 超重惩罚: applyOverweightPenalty / removeOverweightPenalty
 *   - 武器循环: cycleWeapon（GUI 模式下切换武器槽位）
 *   - 装填加速: boostedCooldown（根据 RELOAD_BOOST 效果等级缩减冷却）
 *   - 装备检测: hasSonarEquipped / hasTorpedoReloadEquipped / isFireableWeapon
 *
 * 两种模式的区分：
 *   GUI 模式    (ModCommonConfig.SHIP_CORE_GUI_ENABLED=true):
 *       武器和装甲通过核心物品内部的 ItemContainerContents 管理，玩家打开 GUI 拖放装备。
 *   无GUI 模式  (ModCommonConfig.SHIP_CORE_GUI_ENABLED=false):
 *       武器来自玩家快捷栏 (hotbar 0-8)，装甲/声纳/引擎存储在核心的 SHIP_CORE_ARMOR 组件中。
 *
 * 武器重量注册表 (WEAPON_LOAD_MAP)：
 *   使用 IdentityHashMap（Item 是注册表单例，引用相等更高效）。
 *   新增武器时在此注册表中添加条目。
 */
public class TransformationManager {

    public static final ResourceLocation ARMOR_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_armor");
    public static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_speed");
    public static final ResourceLocation HEALTH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_health");
    public static final ResourceLocation TOUGHNESS_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_toughness");
    public static final ResourceLocation WATER_SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ship_core_water_speed");

    public static boolean isTransformed(ItemStack coreStack) {
        return coreStack.getOrDefault(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), false);
    }

    /** 查找当前激活的变身核心。无GUI模式：仅副手。 */
    public static ItemStack findTransformedCore(net.minecraft.world.entity.player.Player player) {
        // 无GUI模式：仅副手核心有效
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof ShipCoreItem && isTransformed(offhand)) return offhand;
        return ItemStack.EMPTY;
    }

    /** 检查玩家是否持有任意已变身核心 */
    public static boolean isPlayerTransformed(net.minecraft.world.entity.player.Player player) {
        return !findTransformedCore(player).isEmpty();
    }

    public static void setTransformed(ItemStack coreStack, boolean transformed) {
        coreStack.set(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), transformed);
    }

    public static void cycleWeapon(Player player) {
        // 无GUI模式：武器由玩家手持决定，不需要切换
    }

    /** 返回 true 表示可发射/投放的物品（火炮、鱼雷发射器、飞机 — 不含核心、装甲、弹药） */
    public static boolean isFireableWeapon(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShipCoreItem) return false;
        if (stack.getItem() instanceof ArmorPlateItem) return false;
        if (stack.getItem() instanceof SonarItem) return false;
        if (stack.getItem() instanceof EngineItem) return false;
        if (stack.getItem() instanceof TorpedoReloadItem) return false;
        // 鱼雷弹药、炮弹、航空消耗品的 getItemLoad 均为 0，自然排除
        return getItemLoad(stack) > 0;
    }

    /**
     * Apply armor and speed attribute modifiers based on current ship core equipment.
     * Call when player transforms or when GUI closes (to recalculate after changes).
     *
     * When SHIP_CORE_GUI_ENABLED is false: the ship core with the highest maxLoad in the
     * player's inventory provides the capacity; all weapon items in the inventory consume load.
     * When SHIP_CORE_GUI_ENABLED is true: reads weapons/armor from the core's ItemContainerContents.
     */
    public static void applyTransformationAttributes(Player player, ItemStack coreStack) {
        if (player.level().isClientSide()) return;
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        applyAttributesInventoryMode(player);
    }

    /** 快捷栏槽位数（Inventory.items 的 0–8 号槽） */
    private static final int HOTBAR_SIZE = 9;

    /**
     * Inventory mode (GUI disabled): scan the player's inventory for load and attributes.
     * Only hotbar slots (0–8) are considered for weight calculation.
     * The ship core with the highest maxLoad provides the weight capacity.
     */
    private static void applyAttributesInventoryMode(Player player) {
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        // 找到 maxLoad 最高的核心并记录其 ShipType
        ShipType bestType = null;
        int scanLimit = HOTBAR_SIZE;
        for (int i = 0; i < scanLimit; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ShipCoreItem sci) {
                if (bestType == null || sci.getShipType().maxLoad > bestType.maxLoad) {
                    bestType = sci.getShipType();
                }
            }
        }
        // 始终检查副手 — 无GUI模式要求核心在副手
        {
            ItemStack offhandStack = inv.offhand.get(0);
            if (offhandStack.getItem() instanceof ShipCoreItem sci) {
                if (bestType == null || sci.getShipType().maxLoad > bestType.maxLoad) {
                    bestType = sci.getShipType();
                }
            }
        }

        removeTransformationAttributes(player);
        if (bestType == null) return;

        // 护甲和引擎加成来自副手核心内存储的物品
        ItemStack offhandCore = inv.offhand.get(0);
        int armorBonus = getCoreArmorBonus(offhandCore);
        double engineSpeedBonus = getCoreEngineSpeedBonus(offhandCore);
        int armorLoad  = getCoreArmorLoad(offhandCore);
        int totalLoad  = getInventoryWeaponLoad(inv) + armorLoad;

        double loadRatio = bestType.maxLoad > 0 ? (double) totalLoad / bestType.maxLoad : 0;
        double speedMult = bestType.emptySpeed - (bestType.emptySpeed - bestType.fullLoadSpeed) * Math.min(loadRatio, 1.0);
        speedMult += engineSpeedBonus;

        applyTypeAttributes(player, bestType, armorBonus, speedMult);
        applyOverweightPenalty(player, totalLoad, bestType.maxLoad);
    }

    /**
     * Sum the load of all weapons and armor plates (not ship cores) in hotbar. Used in inventory mode.
     * Only scans hotbar slots (0–8).
     */
    public static int getInventoryWeaponLoad(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = inv.items.get(i);
            if (isLoadItem(stack)) total += getItemLoad(stack);
        }
        return total;
    }

    /** 统计玩家快捷栏中装甲板的护甲加成之和（仅扫描 0–8 号槽） */
    public static int getInventoryArmorBonus(net.minecraft.world.entity.player.Inventory inv) {
        int total = 0;
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        return total;
    }

    /**
     * Returns the total armor bonus from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreArmorBonus(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        return total;
    }

    /**
     * Returns the total protection level from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreProtectionLevel(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getProtectionLevel();
        }
        return total;
    }

    /** Returns the total protection level from ArmorPlateItems stored in SHIP_CORE_ARMOR. */
    public static int getEquippedProtectionLevel(Player player, ItemStack coreStack) {
        return getCoreProtectionLevel(coreStack);
    }

    /**
     * Returns the total load contributed by ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static int getCoreArmorLoad(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        int total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getWeight();
            else if (s.getItem() instanceof SonarItem sonar) total += sonar.getWeight();
            else if (s.getItem() instanceof EngineItem engine) total += engine.getWeight();
            else if (s.getItem() instanceof TorpedoReloadItem tr) total += tr.getWeight();
        }
        return total;
    }

    /**
     * Returns the total speed bonus from EngineItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent (no-GUI mode).
     */
    public static double getCoreEngineSpeedBonus(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return 0;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        double total = 0;
        for (ItemStack s : stored) {
            if (s.getItem() instanceof EngineItem engine) total += engine.getSpeedBonus();
        }
        return total;
    }

    /**
     * Returns true for items that consume load in inventory mode (weapons only, not ship cores).
     * ArmorPlateItems are excluded here — they must be stored inside the ship core to count.
     */
    private static boolean isLoadItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ShipCoreItem) return false;
        // 强化物品仅在存入核心(SHIP_CORE_ARMOR)时计入负重
        if (stack.getItem() instanceof ArmorPlateItem) return false;
        if (stack.getItem() instanceof EngineItem) return false;
        if (stack.getItem() instanceof SonarItem) return false;
        if (stack.getItem() instanceof TorpedoReloadItem) return false;
        return getItemLoad(stack) > 0;
    }

    /**
     * Apply overweight debuffs based on how much totalLoad exceeds maxLoad.
     * cost = maxLoad - totalLoad. Negative cost means overweight.
     *   cost < 0:    (speed penalty only, no extra debuff)
     *   cost <= -20:  Mining Fatigue I + Weakness I
     *   cost <= -50:  Mining Fatigue III + Weakness II
     *   cost <= -100: Mining Fatigue III + Poison II
     * Effects last 60 ticks (3s), refreshed each recalculation.
     */
    public static void applyOverweightPenalty(Player player, int totalLoad, int maxLoad) {
        if (player.level().isClientSide()) return;
        int cost = maxLoad - totalLoad;

        int duration = 60; // 3秒，每次重算刷新
        if (cost <= -100) {
            // 挖掘疲劳 III + 中毒 II
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, false, false, true));
        } else if (cost <= -50) {
            // 挖掘疲劳 III + 虚弱 II
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, false, true));
        } else if (cost <= -20) {
            // 挖掘疲劳 I + 虚弱 I
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0, false, false, true));
        } else if (cost > -20) {
            // 不再超重时移除所有超重惩罚（仅移除本系统施加的效果）
            if (player.hasEffect(MobEffects.DIG_SLOWDOWN)) {
                MobEffectInstance effect = player.getEffect(MobEffects.DIG_SLOWDOWN);
                if (effect != null && effect.getAmplifier() <= 2 && effect.getDuration() <= duration) {
                    player.removeEffect(MobEffects.DIG_SLOWDOWN);
                }
            }
            if (player.hasEffect(MobEffects.WEAKNESS)) {
                MobEffectInstance effect = player.getEffect(MobEffects.WEAKNESS);
                if (effect != null && effect.getAmplifier() <= 1 && effect.getDuration() <= duration) {
                    player.removeEffect(MobEffects.WEAKNESS);
                }
            }
            if (player.hasEffect(MobEffects.POISON)) {
                MobEffectInstance effect = player.getEffect(MobEffects.POISON);
                if (effect != null && effect.getAmplifier() <= 1 && effect.getDuration() <= duration) {
                    player.removeEffect(MobEffects.POISON);
                }
            }
        }
    }

    /** 移除超重惩罚效果，应在玩家解除变身时调用 */
    public static void removeOverweightPenalty(Player player) {
        player.removeEffect(MobEffects.DIG_SLOWDOWN);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.POISON);
    }

    /** 移除核心属性修饰器，应在玩家解除变身时调用 */
    public static void removeTransformationAttributes(Player player) {
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (armorAttr != null) armorAttr.removeModifier(ARMOR_MODIFIER_ID);
        if (speedAttr != null) speedAttr.removeModifier(SPEED_MODIFIER_ID);
        if (healthAttr != null) healthAttr.removeModifier(HEALTH_MODIFIER_ID);
        if (toughnessAttr != null) toughnessAttr.removeModifier(TOUGHNESS_MODIFIER_ID);
        if (waterAttr != null) waterAttr.removeModifier(WATER_SPEED_MODIFIER_ID);
    }

    /**
     * Apply all type-based attribute modifiers: health, armor (base + plates), toughness, speed.
     */
    private static void applyTypeAttributes(Player player, ShipType type,
                                             int plateArmorBonus, double speedMult) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        AttributeInstance toughnessAttr = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (healthAttr != null && type.healthBonus != 0) {
            healthAttr.addTransientModifier(new AttributeModifier(
                    HEALTH_MODIFIER_ID, type.healthBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        int totalArmor = type.baseArmor + plateArmorBonus;
        if (armorAttr != null && totalArmor > 0) {
            armorAttr.addTransientModifier(new AttributeModifier(
                    ARMOR_MODIFIER_ID, totalArmor, AttributeModifier.Operation.ADD_VALUE));
        }
        if (toughnessAttr != null && type.armorToughness > 0) {
            toughnessAttr.addTransientModifier(new AttributeModifier(
                    TOUGHNESS_MODIFIER_ID, type.armorToughness, AttributeModifier.Operation.ADD_VALUE));
        }
        if (speedAttr != null && speedMult != 1.0) {
            speedAttr.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, speedMult - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        // 舰娘变身态：免除水阻力。WATER_MOVEMENT_EFFICIENCY 原版 0，设 1 表示水中与陆地同速。
        AttributeInstance waterAttr = player.getAttribute(Attributes.WATER_MOVEMENT_EFFICIENCY);
        if (waterAttr != null) {
            waterAttr.removeModifier(WATER_SPEED_MODIFIER_ID);
            waterAttr.addTransientModifier(new AttributeModifier(
                    WATER_SPEED_MODIFIER_ID, 1.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /**
     * Phase 26: Apply RELOAD_BOOST effect to a base cooldown duration.
     *   Level I  (amplifier 0) → cooldown ÷ 2  (2× reload speed)
     *   Level II (amplifier 1) → cooldown ÷ 3  (3× reload speed)
     * Only effective while the player is transformed (checked at call sites in ShipCoreItem).
     */
    public static int boostedCooldown(Player player, int baseTicks) {
        var effect = player.getEffect(ModMobEffects.RELOAD_BOOST);
        if (effect == null) return baseTicks;
        int divisor = effect.getAmplifier() + 2; // I=÷2, II=÷3
        return Math.max(1, baseTicks / divisor);
    }

    // P3 #36: data-driven weapon load map (static initialization for thread safety)
    // IdentityHashMap 适用：Item 是注册表单例，引用相等(==)既安全又快于 hashCode/equals
    private static final java.util.Map<net.minecraft.world.item.Item, Integer> WEAPON_LOAD_MAP;

    static {
        WEAPON_LOAD_MAP = new java.util.IdentityHashMap<>();
        WEAPON_LOAD_MAP.put(ModItems.SINGLE_SMALL_GUN.get(), 4);
        WEAPON_LOAD_MAP.put(ModItems.SMALL_GUN.get(), 6);
        WEAPON_LOAD_MAP.put(ModItems.MEDIUM_GUN.get(), 16);
        WEAPON_LOAD_MAP.put(ModItems.LARGE_GUN.get(), 30);
        WEAPON_LOAD_MAP.put(ModItems.TWIN_TORPEDO_LAUNCHER.get(), 8);
        WEAPON_LOAD_MAP.put(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 12);
        WEAPON_LOAD_MAP.put(ModItems.QUAD_TORPEDO_LAUNCHER.get(), 20);
        WEAPON_LOAD_MAP.put(ModItems.SY1_LAUNCHER.get(), 14);
        WEAPON_LOAD_MAP.put(ModItems.MK14_HARPOON_LAUNCHER.get(), 16);
        WEAPON_LOAD_MAP.put(ModItems.TERRIER_LAUNCHER.get(), 10);
        WEAPON_LOAD_MAP.put(ModItems.SHIP_ROCKET_LAUNCHER.get(), 8);
        WEAPON_LOAD_MAP.put(ModItems.SEA_DART_LAUNCHER.get(), 12);
        WEAPON_LOAD_MAP.put(ModItems.SEACAT_LAUNCHER.get(), 6);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER.get(), 2);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), 3);
        WEAPON_LOAD_MAP.put(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), 5);
    }

    public static int getItemLoad(ItemStack stack) {
        Integer load = WEAPON_LOAD_MAP.get(stack.getItem());
        if (load != null) return load;
        if (stack.getItem() instanceof ArmorPlateItem plate) return plate.getWeight();
        if (stack.getItem() instanceof SonarItem sonar) return sonar.getWeight();
        if (stack.getItem() instanceof EngineItem engine) return engine.getWeight();
        if (stack.getItem() instanceof TorpedoReloadItem tr) return tr.getWeight();
        if (stack.getItem() instanceof com.piranport.item.AircraftItem) {
            com.piranport.component.AircraftInfo info =
                    stack.get(ModDataComponents.AIRCRAFT_INFO.get());
            return info != null ? info.weight() : 0;
        }
        return 0;
    }

    /** Check if a SonarItem is stored in SHIP_CORE_ARMOR. */
    public static boolean hasSonarEquipped(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        for (ItemStack s : stored) {
            if (s.getItem() instanceof SonarItem) return true;
        }
        return false;
    }

    /** Check if a TorpedoReloadItem is stored in SHIP_CORE_ARMOR. */
    public static boolean hasTorpedoReloadEquipped(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(sci.getShipType().enhancementSlots, ItemStack.EMPTY);
        contents.copyInto(stored);
        for (ItemStack s : stored) {
            if (s.getItem() instanceof TorpedoReloadItem) return true;
        }
        return false;
    }
}
