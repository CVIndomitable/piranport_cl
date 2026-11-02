package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.combat.AASilenceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreCombat;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.registry.ModDataComponents;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.UUID;

/**
 * 玩家 Tick 编排入口：协调逻辑端、变身门控、执行顺序与缓存生命周期。
 *
 * <p><b>线程模型</b>: 水上行走由各逻辑端的玩家实例独立驱动，其余逻辑仅在服务端主线程运行。
 * 燃料与负重状态分别由对应模块独占，入口只协调生命周期。
 * <p><b>缓存生命周期</b>:
 *   {@link PlayerLoadoutTick} / {@link PlayerFuelTick} 的缓存 —
 *   在 {@link #onPlayerLogout(UUID)} 中清理以单向释放内存，
 *   全局清理通过 {@link #clearCaches()} 在 {@link com.piranport.server.ServerGameEvents#onServerStopped} 中调用。
 * <p><b>访问限制</b>: 静态缓存仅供服务端使用，水上行走状态由 {@link WaterWalkingHandler} 隔离到玩家实例。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class PlayerTickHandler {

    /** 清理所有缓存（服务器关闭时调用）*/
    public static void clearCaches() {
        PlayerLoadoutTick.clear();
        PlayerFuelTick.clear();
    }

    /** 玩家登出时清理该玩家的缓存条目，防止长时间运行内存泄漏 */
    public static void onPlayerLogout(UUID uuid) {
        PlayerLoadoutTick.forget(uuid);
        PlayerFuelTick.forget(uuid);
    }

    /**
     * 强制使某玩家的载重缓存失效，让下一次 tick 无条件重算属性。
     *
     * <p>WHY 需要外部触发：tick 循环只在
     * {@code Objects.hash(weaponLoad, armorLoad, airborneLoad, maxLoad, engineBonus)} 变化时才重算。
     * 调试终端改的是速度覆盖值，它不在这些输入中，写入后缓存仍然"命中"，
     * 结果就是「数值改了但角色速度不变」。删掉缓存条目即可让下一 tick 重算。
     */
    public static void invalidateLoadCache(Player player) {
        if (player != null) {
            PlayerLoadoutTick.forget(player.getUUID());
        }
    }

    /** 定期清理离线玩家的缓存条目，防止服务器崩溃导致的内存泄漏 */
    public static void cleanupOfflinePlayers(net.minecraft.server.MinecraftServer server) {
        java.util.Set<UUID> onlineUuids = server.getPlayerList().getPlayers().stream()
                .map(net.minecraft.world.entity.Entity::getUUID)
                .collect(java.util.stream.Collectors.toSet());

        PlayerLoadoutTick.retainOnline(onlineUuids);
        PlayerFuelTick.retainOnline(onlineUuids);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        boolean isClientSide = player.level().isClientSide();

        // 水上行走需要在客户端和服务端都执行，否则客户端物理引擎会覆盖服务端设置的速度
        // 其他逻辑只在服务端执行
        if (!isClientSide) {
            PlayerEquipmentTick.tickEquipmentPassives(player);
            PlayerLoadoutTick.tick(player);
            PlayerReconTick.tickReconBodyLock(player);
        }

        boolean isTransformed = TransformationManager.isPlayerTransformed(player);

        if (com.piranport.debug.PiranPortDebug.isServerEnabled()) {
            com.piranport.debug.PiranPortDebug.event("PlayerTick | player={} transformed={}",
                    player.getName().getString(), isTransformed);
        }

        if (!isTransformed) {
            WaterWalkingHandler.clear(player);
            if (!isClientSide) {
                AASilenceManager.clear(player);
                PlayerFuelTick.forget(player.getUUID());

                // 调试日志：帮助诊断变身检测失败
                if (player.tickCount % 100 == 0 && player.isInWater()) {
                    ItemStack coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
                    if (coreStack.getItem() instanceof ShipCoreItem) {
                        boolean transformed = TransformationManager.isTransformed(coreStack);
                        if (!transformed) {
                            PiranPort.LOGGER.warn("Player {} has ship core in slot but not transformed. Core: {}, Slot: {}, DataComponent: {}",
                                player.getName().getString(),
                                coreStack.getItem(),
                                "offhand",
                                coreStack.get(ModDataComponents.SHIP_CORE_TRANSFORMED.get()));
                        }
                    }
                }
            }
            return;
        }

        // 服务端专属逻辑
        if (!isClientSide) {
            // 耗尽会发布解除变身事件，必须重新读取状态再决定是否继续本 tick。
            PlayerFuelTick.tick(player, PlayerLoadoutTick::forget);
            if (!TransformationManager.isPlayerTransformed(player)) {
                WaterWalkingHandler.clear(player);
                return;
            }
        }

        ItemStack transformedCore = TransformationManager.findTransformedCore(player);
        boolean isSubmarine = transformedCore.getItem() instanceof ShipCoreItem sci
                && sci.getShipType() == ShipType.SUBMARINE;

        // 门控前置：player.getName().getString() 每 tick 都会构造新 String，
        // 必须放到 isServerEnabled() 之后再求值，否则关调试也照样分配。
        if (com.piranport.debug.PiranPortDebug.isServerEnabled()) {
            com.piranport.debug.PiranPortDebug.event("WaterWalkCheck | player={} submarine={}",
                    player.getName().getString(), isSubmarine);
        }

        // 水上行走：客户端和服务端都需要执行
        WaterWalkingHandler.tick(player, isSubmarine);

        // 以下逻辑只在服务端执行
        if (!isClientSide) {
            if (transformedCore.getItem() instanceof ShipCoreItem activeCore) {
                com.piranport.combat.ShipHealthOverride.apply(
                        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH),
                        activeCore.getShipType().maxHealth());
                if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
            }
            PlayerEquipmentTick.tickSubmarineEffects(player, isSubmarine);
            // 装填到期结算不受 H 键自动模式开关影响，不能移进自动战斗模块。
            ShipCoreCombat.tickCannonAutoReload(player, transformedCore);
            // 舰载机与火炮共用 WEAPON_COOLDOWN 读条，到期结算同样放在服务端 tick
            com.piranport.aviation.AircraftFireStrategy.tickAircraftReload(player);
            PlayerDetectionTick.tickSonarGlow(player, transformedCore);
            PlayerReconTick.tickCleanupResidualSlowdown(player);
            PlayerAutoCombatTick.tickAutoCombatIfNeeded(player);
            // 决策/数值/05 §定稿修订 #3：防空静默窗口每 tick 衰减
            AASilenceManager.tickDown(player);
            // 策划决策/舰装/舰装-自动近防炮系统.md：H 键总开关在 server 侧生效时 tick 近防炮
            PlayerAutoCombatTick.tickCloseDefense(player, transformedCore);
        }
    }
}
