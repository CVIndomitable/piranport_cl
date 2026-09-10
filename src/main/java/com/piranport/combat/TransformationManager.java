package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.config.ModCommonConfig;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 变身管理器 — 管理玩家从"人类形态"到"舰娘形态"的切换及相关属性计算。
 *
 * 职责概览:
 *   - 变身/解除变身（setTransformed / removeTransformationAttributes）
 *   - 属性计算: applyTransformationAttributes（基于快捷栏武器）
 *   - 负重系统: WEAPON_LOAD_MAP 静态武器重量注册表 + getItemLoad 查询
 *   - 超重惩罚: applyOverweightPenalty / removeOverweightPenalty
 *   - 装填加速: boostedCooldown（根据 RELOAD_BOOST 效果等级缩减冷却）
 *   - 装备检测: hasSonarEquipped / hasTorpedoReloadEquipped / isFireableWeapon
 *
 * 武器重量注册表 (WEAPON_LOAD_MAP)：
 *   使用 IdentityHashMap（Item 是注册表单例，引用相等更高效）。
 *   新增武器时在此注册表中添加条目。
 */
public class TransformationManager {

    private TransformationManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static boolean chestModeWarningLogged = false;

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

    /**
     * 根据配置获取舰装核心所在槽位的物品。
     *
     * @param player 玩家
     * @return 核心物品栈，如果未装备则返回 ItemStack.EMPTY
     */
    public static ItemStack getCoreFromConfiguredSlot(Player player) {
        String slotMode = ModCommonConfig.SHIP_CORE_SLOT_MODE.get();

        // 向后兼容：将旧的 "chest" 配置自动映射到 "helmet"
        if ("chest".equalsIgnoreCase(slotMode)) {
            if (!chestModeWarningLogged) {
                PiranPort.LOGGER.warn("Config value 'chest' for shipCoreSlotMode is deprecated. " +
                        "Please update to 'helmet'. Auto-migrating to helmet slot.");
                chestModeWarningLogged = true;
            }
            return player.getItemBySlot(EquipmentSlot.HEAD);
        } else if ("helmet".equalsIgnoreCase(slotMode)) {
            return player.getItemBySlot(EquipmentSlot.HEAD);
        } else {
            // 默认：副手模式
            return player.getOffhandItem();
        }
    }

    /** 查找当前激活的变身核心。根据配置检测副手或胸甲槽位。 */
    public static ItemStack findTransformedCore(net.minecraft.world.entity.player.Player player) {
        ItemStack coreStack = getCoreFromConfiguredSlot(player);
        if (coreStack.getItem() instanceof ShipCoreItem && isTransformed(coreStack)) {
            return coreStack;
        }
        return ItemStack.EMPTY;
    }

    /** 检查玩家是否持有任意已变身核心 */
    public static boolean isPlayerTransformed(net.minecraft.world.entity.player.Player player) {
        return !findTransformedCore(player).isEmpty();
    }

    public static void setTransformed(ItemStack coreStack, boolean transformed) {
        coreStack.set(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), transformed);
    }

    /**
     * 将核心ItemStack写回配置的槽位。
     *
     * @param player 玩家
     * @param coreStack 核心物品栈
     */
    public static void writeCoreToConfiguredSlot(Player player, ItemStack coreStack) {
        String slotMode = ModCommonConfig.SHIP_CORE_SLOT_MODE.get();

        // 向后兼容：将旧的 "chest" 配置自动映射到 "helmet"
        if ("chest".equalsIgnoreCase(slotMode)) {
            slotMode = "helmet";
        }

        if ("helmet".equalsIgnoreCase(slotMode)) {
            player.setItemSlot(EquipmentSlot.HEAD, coreStack);
        } else {
            // 默认：副手模式
            player.getInventory().offhand.set(0, coreStack);
        }
    }

    /**
     * 设置变身状态并将修改后的 ItemStack 写回配置的槽位。
     *
     * <p>变身状态的修改必须显式写回配置槽位才能持久化。
     * 本方法封装了"设置状态 + 写回槽位"的完整流程，确保变身状态正确同步。
     *
     * @param player 玩家
     * @param coreStack 核心物品栈（必须是从 getCoreFromConfiguredSlot 获取的引用）
     * @param transformed 变身状态
     */
    public static void setTransformedAndWriteBack(Player player, ItemStack coreStack, boolean transformed) {
        // 设置变身状态
        coreStack.set(ModDataComponents.SHIP_CORE_TRANSFORMED.get(), transformed);

        // 统一使用辅助方法写回
        writeCoreToConfiguredSlot(player, coreStack);
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
     * Scans the player's hotbar for weapons and calculates load/attributes.
     */
    public static void applyTransformationAttributes(Player player, ItemStack coreStack) {
        if (player.level().isClientSide()) return;
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        applyAttributesInventoryMode(player, coreStack);
    }

    /** 快捷栏槽位数（Inventory.items 的 0–8 号槽） */
    private static final int HOTBAR_SIZE = 9;

    /**
     * Inventory mode (GUI disabled): scan the player's inventory for load and attributes.
     * Only hotbar slots (0–8) are considered for weight calculation.
     * The transformed core passed by the caller provides the weight capacity and base attributes.
     */
    private static void applyAttributesInventoryMode(Player player, ItemStack coreStack) {
        // P2-8: PERF 埋点 — 仅在有任一会话激活时计时（门控）
        long t0 = 0L;
        boolean perfEnabled = com.piranport.debug.PiranPortDebug.isServerEnabled();
        if (perfEnabled) t0 = System.nanoTime();

        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        ShipType activeType = ((ShipCoreItem) coreStack.getItem()).getShipType();

        removeTransformationAttributes(player);

        int armorBonus = getCoreArmorBonus(coreStack);
        double engineSpeedBonus = getCoreEngineSpeedBonus(coreStack);
        int armorLoad  = getCoreArmorLoad(coreStack);
        int totalLoad  = getInventoryWeaponLoad(inv) + armorLoad;

        double loadRatio = activeType.maxLoad > 0 ? (double) totalLoad / activeType.maxLoad : 0;
        double speedMult = activeType.emptySpeed - (activeType.emptySpeed - activeType.fullLoadSpeed) * Math.min(loadRatio, 1.0);
        speedMult += engineSpeedBonus;

        applyTypeAttributes(player, activeType, armorBonus, speedMult);
        applyOverweightPenalty(player, totalLoad, activeType.maxLoad);

        if (perfEnabled) {
            long ns = System.nanoTime() - t0;
            com.piranport.debug.PiranPortDebug.perf("WeightScan", ns,
                    "player=" + (player == null ? "?" : player.getName().getString())
                    + " load=" + totalLoad + "/" + activeType.maxLoad);
        }
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

    /** 读取核心内的有效强化件列表。 */
    private static NonNullList<ItemStack> getCoreStoredContents(ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return NonNullList.create();
        int enhancementSlots = getCoreEnhancementSlots(coreStack, sci);
        NonNullList<ItemStack> stored = NonNullList.withSize(enhancementSlots, ItemStack.EMPTY);
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        contents.copyInto(stored);
        return stored;
    }

    private static int getCoreEnhancementSlots(ItemStack coreStack, ShipCoreItem sci) {
        com.piranport.component.CustomCoreConfig config =
                coreStack.get(ModDataComponents.CUSTOM_CORE_CONFIG.get());
        return config != null && config.isCustomized()
                ? config.getEnhancementSlots()
                : sci.getShipType().enhancementSlots;
    }

    /**
     * Returns the total armor bonus from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent.
     */
    public static int getCoreArmorBonus(ItemStack coreStack) {
        int total = 0;
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getArmorBonus();
        }
        return total;
    }

    /**
     * Returns the total protection level from ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent.
     */
    public static int getCoreProtectionLevel(ItemStack coreStack) {
        int total = 0;
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getProtectionLevel();
        }
        return total;
    }

    /** Returns the total protection level from ArmorPlateItems stored in SHIP_CORE_ARMOR. */
    public static int getEquippedProtectionLevel(ItemStack coreStack) {
        return getCoreProtectionLevel(coreStack);
    }

    /**
     * Returns the total load contributed by ArmorPlateItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent.
     */
    public static int getCoreArmorLoad(ItemStack coreStack) {
        int total = 0;
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof ArmorPlateItem plate) total += plate.getWeight();
            else if (s.getItem() instanceof SonarItem sonar) total += sonar.getWeight();
            else if (s.getItem() instanceof EngineItem engine) total += engine.getWeight();
            else if (s.getItem() instanceof TorpedoReloadItem tr) total += tr.getWeight();
        }
        return total;
    }

    /**
     * Returns the total speed bonus from EngineItems stored inside a ship core's
     * SHIP_CORE_ARMOR DataComponent.
     */
    public static double getCoreEngineSpeedBonus(ItemStack coreStack) {
        double total = 0;
        for (ItemStack s : getCoreStoredContents(coreStack)) {
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
     * Apply overweight debuffs based on load ratio relative to maxLoad.
     * 依据：策划决策/数值/03-超载惩罚分级.md（115%/130%/150% 三档阈值）
     *   ratio > 115%: Mining Fatigue I
     *   ratio > 130%: Mining Fatigue II + Weakness II
     *   ratio > 150%: Mining Fatigue III + Poison II
     * Effects last 60 ticks (3s), refreshed each recalculation.
     */
    public static void applyOverweightPenalty(Player player, int totalLoad, int maxLoad) {
        if (player.level().isClientSide()) return;
        if (maxLoad <= 0) return;
        int cost = maxLoad - totalLoad;

        // P1-7: 超载时记录 error 埋点（含玩家、当前/最大值、触发上下文）
        if (totalLoad > maxLoad) {
            com.piranport.debug.PiranPortDebug.weightOverload(player, totalLoad, maxLoad);
        }

        int duration = 60; // 3秒，每次重算刷新
        // 仅在超载时计算阈值；不超载 = cost >= 0，跳过
        if (cost < 0) {
            // 决策 §3.5：百分比阈值（115% / 130% / 150%）映射三档惩罚
            // 当 totalLoad > 115% maxLoad 时即视为"超载 115%"
            int totalLoadTimes100 = (int) ((double) totalLoad * 100 / maxLoad);
            if (totalLoadTimes100 > 150) {
                // > 150%：挖掘疲劳 III + 中毒 II
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 2, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.POISON, duration, 1, false, false, true));
            } else if (totalLoadTimes100 > 130) {
                // > 130%：挖掘疲劳 II + 虚弱 II
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 1, false, false, true));
            } else if (totalLoadTimes100 > 115) {
                // > 115%：挖掘疲劳 I
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, duration, 0, false, false, true));
            } else {
                // 100% < totalLoad ≤ 115%：航速已在 Weight 曲线处理，不再施加额外 debuff
                removeOverweightPenalty(player);
                return;
            }
        } else {
            // 不再超重时移除所有超重惩罚（仅移除本系统施加的效果）
            removeOverweightPenalty(player);
        }
    }

    /** 移除超重惩罚效果，应在玩家解除变身时调用。
     *  仅移除由本系统施加的效果（通过 amplifier 和 duration 甄别），
     *  避免无差别清除来自其他模组或原版的效果（如远古守卫者的挖掘疲劳）。 */
    public static void removeOverweightPenalty(Player player) {
        int removalDuration = 60;
        // 仅移除 amplifier <= 2 且 duration <= 60tick 的 DIG_SLOWDOWN（由本系统施加的强度）
        MobEffectInstance dig = player.getEffect(MobEffects.DIG_SLOWDOWN);
        if (dig != null && dig.getAmplifier() <= 2 && dig.getDuration() <= removalDuration) {
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
        MobEffectInstance weak = player.getEffect(MobEffects.WEAKNESS);
        if (weak != null && weak.getAmplifier() <= 1 && weak.getDuration() <= removalDuration) {
            player.removeEffect(MobEffects.WEAKNESS);
        }
        MobEffectInstance poison = player.getEffect(MobEffects.POISON);
        if (poison != null && poison.getAmplifier() <= 1 && poison.getDuration() <= removalDuration) {
            player.removeEffect(MobEffects.POISON);
        }
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
     * Apply RELOAD_BOOST to a base cooldown duration.
     * Design values: I/II/III => 0.9/0.8/0.7x original time.
     */
    public static int boostedCooldown(Player player, int baseTicks) {
        if (baseTicks <= 0) return baseTicks;
        return Math.max(1, (int) Math.ceil(baseTicks * reloadBoostMultiplier(player)));
    }

    /** Returns the design time multiplier for RELOAD_BOOST: no effect = 1.0, I/II/III = 0.9/0.8/0.7. */
    public static double reloadBoostMultiplier(Player player) {
        var effect = player.getEffect(ModMobEffects.RELOAD_BOOST);
        if (effect == null) return 1.0D;
        int level = Math.min(2, Math.max(0, effect.getAmplifier()));
        return 0.9D - level * 0.1D;
    }

    /** Converts actual draw/use ticks into boosted effective progress ticks. */
    public static int boostedUseProgress(Player player, int elapsedTicks) {
        if (elapsedTicks <= 0) return elapsedTicks;
        return Math.max(1, (int) Math.floor(elapsedTicks / reloadBoostMultiplier(player)));
    }

    // P3 #36: data-driven weapon load map (延迟初始化，避免类加载时访问注册表)
    // IdentityHashMap 适用：Item 是注册表单例，引用相等(==)既安全又快于 hashCode/equals
    // 使用 unmodifiableMap 包装以明确只读语义，防止误修改
    private static java.util.Map<net.minecraft.world.item.Item, Integer> weaponLoadMap;

    /** 延迟初始化武器载重映射表（避免静态初始化时访问注册表）*/
    private static java.util.Map<net.minecraft.world.item.Item, Integer> getWeaponLoadMap() {
        if (weaponLoadMap == null) {
            java.util.Map<net.minecraft.world.item.Item, Integer> temp = new java.util.IdentityHashMap<>();
            temp.put(ModItems.SINGLE_SMALL_GUN.get(), 4);
            temp.put(ModItems.SMALL_GUN.get(), 6);
            temp.put(ModItems.MEDIUM_GUN.get(), 16);
            temp.put(ModItems.LARGE_GUN.get(), 30);
            temp.put(ModItems.FRENCH_QUAD_380MM_GUN.get(), 35);
            temp.put(ModItems.SEVEN_BARREL_GUN.get(), 35);
            temp.put(ModItems.SALVO_TEST_GUN.get(), 50);
            temp.put(ModItems.TWIN_TORPEDO_LAUNCHER.get(), 8);
            temp.put(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 12);
            temp.put(ModItems.QUAD_TORPEDO_LAUNCHER.get(), 20);
            temp.put(ModItems.SY1_LAUNCHER.get(), 14);
            temp.put(ModItems.MK14_HARPOON_LAUNCHER.get(), 16);
            temp.put(ModItems.TERRIER_LAUNCHER.get(), 10);
            temp.put(ModItems.SHIP_ROCKET_LAUNCHER.get(), 8);
            temp.put(ModItems.SEA_DART_LAUNCHER.get(), 12);
            temp.put(ModItems.SEACAT_LAUNCHER.get(), 6);
            temp.put(ModItems.DEPTH_CHARGE_LAUNCHER.get(), 2);
            temp.put(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), 3);
            temp.put(ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), 5);
            weaponLoadMap = java.util.Collections.unmodifiableMap(temp);
        }
        return weaponLoadMap;
    }

    public static int getItemLoad(ItemStack stack) {
        Integer load = getWeaponLoadMap().get(stack.getItem());
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
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof SonarItem) return true;
        }
        return false;
    }

    /**
     * Phase 27：策划 §3.6 表 3.2 - 取装备的声呐扫描半径。
     * 优先取 first-found SonarItem.getRadius()；无装备返回标准型 24。
     */
    public static double getEquippedSonarRadius(Player player, ItemStack coreStack) {
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof SonarItem sonar) return sonar.getRadius();
        }
        return 24.0;
    }

    /** Check if a TorpedoReloadItem is stored in SHIP_CORE_ARMOR. */
    public static boolean hasTorpedoReloadEquipped(Player player, ItemStack coreStack) {
        for (ItemStack s : getCoreStoredContents(coreStack)) {
            if (s.getItem() instanceof TorpedoReloadItem) return true;
        }
        return false;
    }
}
