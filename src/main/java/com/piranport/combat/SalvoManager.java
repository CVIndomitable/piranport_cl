package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.combat.cannon.CannonSalvos;
import com.piranport.combat.cannon.CannonInventory;
import com.piranport.combat.cannon.SalvoContext;
import net.minecraft.world.item.ItemStack;
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
     * 单次延迟射击任务。捕获玩家、世界和武器实例，执行时重新验证生命周期。
     */
    private record SalvoTask(
            int weaponSlot,
            int coreSlot,
            Item expectedWeaponType,
            ItemStack expectedWeapon,
            SalvoContext context,
            int aimMode,
            double aimTargetX,
            double aimTargetY,
            double aimTargetZ,
            long fireTick
    ) {}

    /**
     * 按策划决策/武器/10，以 1-4 tick 随机间隔调度同型炮；火炮自身的连发参数不控制跨武器队列。
     */
    public static void schedule(ServerPlayer player, Item expectedType, List<int[]> slotPairs,
                                 int aimMode, double ax, double ay, double az) {
        long now = player.serverLevel().getGameTime();
        long nextTick = now;
        SalvoContext context = new SalvoContext(player, player.serverLevel(), player.getMainHandItem());
        ArrayDeque<SalvoTask> queue = new ArrayDeque<>();

        for (int[] pair : slotPairs) {
            nextTick += 1 + player.getRandom().nextInt(4);
            ItemStack weapon = CannonInventory.weaponAt(player.getInventory(), pair[0]);
            queue.add(new SalvoTask(pair[0], pair[1], expectedType, weapon, context,
                    aimMode, ax, ay, az, nextTick));
        }

        PENDING.put(player.getUUID(), queue);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, ArrayDeque<SalvoTask>>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArrayDeque<SalvoTask>> entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());

            SalvoTask next = entry.getValue().peek();
            if (player == null || next == null || !next.context().isValid(player,
                    player.serverLevel(), player.getMainHandItem(), player.isAlive(), player.isSpectator())) {
                it.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            ArrayDeque<SalvoTask> queue = entry.getValue();

            while (!queue.isEmpty() && queue.peek().fireTick() <= now) {
                SalvoTask task = queue.poll();
                // 同型炮被另一把替换也应取消，不能只比较注册类型。
                if (CannonInventory.weaponAt(player.getInventory(), task.weaponSlot()) != task.expectedWeapon()) continue;
                CannonSalvos.executeSalvoFire(level, player,
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
