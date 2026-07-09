package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.item.ShipCoreCombat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * 齐射延迟射击调度器。将同一类型火炮的发射分散到多个 tick 中，形成 0.05-0.2s 的随机间隔。
 * 所有 PENDING 操作在主线程，HashMap + ArrayDeque 安全。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class SalvoManager {

    private static final Map<UUID, ArrayDeque<SalvoTask>> PENDING = new HashMap<>();

    /**
     * 单次延迟射击任务。expectedWeaponType 用于执行时校验槽位未被换物品。
     */
    private record SalvoTask(
            int weaponSlot,
            int coreSlot,
            Item expectedWeaponType,
            int aimMode,
            double aimTargetX,
            double aimTargetY,
            double aimTargetZ,
            long fireTick
    ) {}

    /**
     * 调度一批槽位的延迟射击。间隔来自火炮配置的 salvoInterval。
     */
    public static void schedule(ServerPlayer player, Item expectedType, List<int[]> slotPairs,
                                 int aimMode, double ax, double ay, double az, int intervalTicks) {
        long now = player.serverLevel().getGameTime();
        int interval = Math.max(1, intervalTicks);
        long nextTick = now + interval;
        ArrayDeque<SalvoTask> queue = new ArrayDeque<>();

        for (int[] pair : slotPairs) {
            queue.add(new SalvoTask(pair[0], pair[1], expectedType,
                    aimMode, ax, ay, az, nextTick));
            nextTick += interval;
        }

        PENDING.put(player.getUUID(), queue);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, ArrayDeque<SalvoTask>>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArrayDeque<SalvoTask>> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());

            if (player == null) {
                it.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            ArrayDeque<SalvoTask> queue = entry.getValue();

            while (!queue.isEmpty() && queue.peek().fireTick() <= now) {
                SalvoTask task = queue.poll();
                ShipCoreCombat.executeSalvoFire(level, player,
                        task.weaponSlot(), task.coreSlot(), task.expectedWeaponType(),
                        task.aimMode(),
                        task.aimTargetX(), task.aimTargetY(), task.aimTargetZ());
            }

            if (queue.isEmpty()) {
                it.remove();
            }
        }
    }

    /** 玩家登出时清理其队列，避免残留任务。 */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        PENDING.remove(event.getEntity().getUUID());
    }

    /** 移除指定玩家的所有待发射任务。 */
    public static void removePlayer(UUID uuid) {
        PENDING.remove(uuid);
    }

    /** 清空所有待发射队列（关服时调用）。 */
    public static void clearAll() {
        PENDING.clear();
    }
}
