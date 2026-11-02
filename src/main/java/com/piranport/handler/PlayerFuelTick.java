package com.piranport.handler;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.combat.TransformationManager;
import com.piranport.component.FuelData;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 移动燃料结算，仅由服务端主线程调用。
 *
 * <p>位置与距离缓存由本类独占；解除变身、登出和停服通过入口清理。
 * 不直接依赖负重模块，耗尽回调由入口注入，并在事件发布前执行。
 */
final class PlayerFuelTick {
    private PlayerFuelTick() {}

    /** 玩家 UUID → 上次采样位置。 */
    private static final Map<UUID, Vec3> lastPlayerPos = new ConcurrentHashMap<>();
    /** 玩家 UUID → 未结算的移动距离。 */
    private static final Map<UUID, Double> accumulatedDistance = new ConcurrentHashMap<>();

    static void clear() {
        lastPlayerPos.clear();
        accumulatedDistance.clear();
    }

    static void forget(UUID uuid) {
        lastPlayerPos.remove(uuid);
        accumulatedDistance.remove(uuid);
    }

    static void retainOnline(java.util.Set<UUID> onlineUuids) {
        lastPlayerPos.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
        accumulatedDistance.keySet().removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    /** 燃料消耗：基于移动距离，耗尽时自动解除变身（每5tick计算一次） */
    static void tick(Player player, java.util.function.Consumer<UUID> onFuelDepleted) {
        // 性能优化：每5tick计算一次燃料消耗
        if (player.tickCount % 5 != 0) return;

        // 弹药库勾选燃料：不消耗，并把油箱维持满格（避免 HUD 显示 0 油）。
        if (CreativeAmmoCache.isInfinite(player, ModItems.FUEL.get())) {
            ItemStack core = TransformationManager.findTransformedCore(player);
            if (core.getItem() instanceof ShipCoreItem sci) {
                FuelData fuel = core.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                        new FuelData(0, sci.getShipType().effectiveFuelCapacity()));
                if (!fuel.isFull()) {
                    core.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                            fuel.withCurrentFuel(fuel.maxFuel()));
                    TransformationManager.writeCoreToConfiguredSlot(player, core);
                }
            }
            return;
        }

        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastPlayerPos.put(uuid, currentPos);
        if (lastPos == null) return;

        double dist = currentPos.distanceTo(lastPos);
        // P1修复: 降低传送检测阈值至32格，避免误判高速鞘翅飞行
        final double TELEPORT_THRESHOLD = 32.0;
        if (dist > TELEPORT_THRESHOLD || player.isFallFlying()) {
            lastPlayerPos.put(uuid, currentPos);
            accumulatedDistance.remove(uuid);
            return;
        }
        if (dist < 0.001) return;

        ItemStack core = TransformationManager.findTransformedCore(player);
        if (!(core.getItem() instanceof ShipCoreItem sci)) return;

        double threshold = sci.getShipType().effectiveDistancePerFuel();
        double acc = accumulatedDistance.getOrDefault(uuid, 0.0) + dist;
        FuelData fuel = core.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, sci.getShipType().effectiveFuelCapacity()));

        while (acc >= threshold && fuel.currentFuel() > 0) {
            acc -= threshold;
            fuel = fuel.withCurrentFuel(fuel.currentFuel() - 1);
        }
        core.set(ModDataComponents.SHIP_CORE_FUEL.get(), fuel);

        // 写回槽位以确保燃料数据同步
        TransformationManager.writeCoreToConfiguredSlot(player, core);

        if (fuel.isEmpty()) {
            // 先使负重缓存失效并清空距离，再发布耗尽/解除事件，避免监听器读到旧状态。
            onFuelDepleted.accept(uuid);
            forget(uuid);
            TransformationManager.setTransformedAndWriteBack(player, core, false);

            // 发布燃料耗尽事件
            com.piranport.event.EventBus.getInstance().post(
                    new com.piranport.event.FuelDepletedEvent(player, core));

            // 发布变身解除事件
            com.piranport.event.EventBus.getInstance().post(
                    new com.piranport.event.TransformationEvent(player, core, false));

            // 非监听器处理的逻辑
            // 燃料耗尽同样是正常解除：召回并返还飞机物品。
            PlayerAircraftHelper.recallAircraftToInventory(player);
            player.displayClientMessage(
                    Component.translatable("message.piranport.fuel_depleted"), true);
            return;
        }

        accumulatedDistance.put(uuid, acc);
    }
}
