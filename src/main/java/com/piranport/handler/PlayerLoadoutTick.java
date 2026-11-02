package com.piranport.handler;

import com.piranport.combat.TransformationManager;
import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.component.FuelData;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipCoreCombat;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 核心槽位驱动变身、负重属性与超载更新，仅由服务端主线程调用。
 *
 * <p>独占负重缓存（包括缺油提示哨兵）；入口负责登出/停服清理，
 * 调试终端经原有入口使缓存失效。检查周期仍为10 tick。
 */
final class PlayerLoadoutTick {
    private PlayerLoadoutTick() {}

    private static final int INVENTORY_LOAD_CHECK_INTERVAL = 10;
    /** 玩家 UUID → 属性输入哈希，或变身/缺油提示哨兵。 */
    private static final Map<UUID, Integer> lastWeaponLoad = new ConcurrentHashMap<>();

    static void clear() {
        lastWeaponLoad.clear();
    }

    static void forget(UUID uuid) {
        lastWeaponLoad.remove(uuid);
    }

    static void retainOnline(java.util.Set<UUID> onlineUuids) {
        lastWeaponLoad.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    /** 检测背包武器变化并重算属性 */
    static void tick(Player player) {
        // 每10tick检查一次，减少重复计算
        if (player.tickCount % INVENTORY_LOAD_CHECK_INTERVAL == 0) {
            tickInventoryLoadCheck(player);
        }
    }

    /**
     * 通过配置的槽位驱动变身。
     * 配置槽位放入核心 → 自动变身；移除核心 → 自动解除；核心不动 → 仅武器变化时重算属性。
     */
    private static void tickInventoryLoadCheck(Player player) {
        Inventory inv = player.getInventory();
        ItemStack coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        boolean hasCoreEquipped = coreStack.getItem() instanceof ShipCoreItem;

        if (hasCoreEquipped) {
            if (!TransformationManager.isTransformed(coreStack)) {
                FuelData fuel = coreStack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                        new FuelData(0, ((ShipCoreItem) coreStack.getItem()).getShipType().effectiveFuelCapacity()));
                // 弹药库勾选了燃料：视为无限燃料，空油箱也允许变身（下一 tick 会补满）。
                boolean infiniteFuel = CreativeAmmoCache.isInfinite(player, ModItems.FUEL.get());
                if (fuel.isEmpty() && !infiniteFuel) {
                    // 燃料不足提示（聊天框 + 动作栏）
                    Integer cached = lastWeaponLoad.get(player.getUUID());
                    if (cached == null || cached != -999) {
                        lastWeaponLoad.put(player.getUUID(), -999);
                        player.sendSystemMessage(
                            Component.translatable("message.piranport.no_fuel_enhanced"));
                        player.displayClientMessage(
                            Component.translatable("message.piranport.no_fuel"), true);
                    }
                    return;
                }
                Integer cached = lastWeaponLoad.get(player.getUUID());
                if (cached != null && cached == -999) {
                    lastWeaponLoad.remove(player.getUUID());
                }
                TransformationManager.setTransformedAndWriteBack(player, coreStack, true);
                // 重新读取核心以获取最新的ItemStack引用（DataComponents可能创建新实例）
                coreStack = TransformationManager.getCoreFromConfiguredSlot(player);

                com.piranport.debug.PiranPortDebug.event("Transform ON | player={} core={} transformed={}",
                    player.getName().getString(),
                    coreStack.getItem().getClass().getSimpleName(),
                    TransformationManager.isTransformed(coreStack));

                // 发布变身激活事件 (监听器会处理属性/消息/粒子)
                com.piranport.event.EventBus.getInstance().post(
                        new com.piranport.event.TransformationEvent(player, coreStack, true));

                // 非监听器处理的逻辑保留在这里
                ShipCoreCombat.refillAircraftFuel(player, coreStack);
                lastWeaponLoad.put(player.getUUID(), -1);
                return;
            }

            int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
            int armorLoad  = TransformationManager.getCoreArmorLoad(coreStack);
            // 2026-10-19：在飞飞机也计入核心负重。把它放进缓存键，
            // 放飞/回收时才会触发航速与超载重算。
            int airborneLoad = TransformationManager.getAirborneAircraftLoad(player);
            double engineBonus = TransformationManager.getCoreEngineSpeedBonus(coreStack);
            int maxLoad    = ((ShipCoreItem) coreStack.getItem()).getShipType().effectiveMaxLoad();
            int cacheKey   = java.util.Objects.hash(weaponLoad, armorLoad, airborneLoad, maxLoad, engineBonus);
            Integer cached = lastWeaponLoad.get(player.getUUID());
            if (cached == null || cached != cacheKey) {
                lastWeaponLoad.put(player.getUUID(), cacheKey);
                TransformationManager.applyTransformationAttributes(player, coreStack);
            } else {
                int totalLoad = weaponLoad + armorLoad + airborneLoad;
                if (totalLoad > maxLoad && player.tickCount % 40 == 0) {
                    TransformationManager.applyOverweightPenalty(player, totalLoad, maxLoad);
                }
            }
        } else {
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack stack = inv.items.get(i);
                if (stack.getItem() instanceof ShipCoreItem
                        && TransformationManager.isTransformed(stack)) {
                    TransformationManager.setTransformed(stack, false);
                    // P1修复: 显式写回以确保状态同步（DataComponents可能创建新ItemStack）
                    inv.items.set(i, stack);
                }
            }
            if (lastWeaponLoad.remove(player.getUUID()) != null) {
                // 发布变身解除事件 (监听器会处理属性/消息)
                ItemStack core = TransformationManager.findTransformedCore(player);
                if (!core.isEmpty()) {
                    com.piranport.event.EventBus.getInstance().post(
                            new com.piranport.event.TransformationEvent(player, core, false));
                }

                // 非监听器处理的逻辑保留在这里
                player.removeEffect(MobEffects.WATER_BREATHING);
                // 取下核心属于正常解除：召回飞机并把物品放回背包，不能走登出用的硬清理。
                PlayerAircraftHelper.recallAircraftToInventory(player);
            }
        }
    }
}
