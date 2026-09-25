package com.piranport.combat;

import com.piranport.PiranPort;
import com.piranport.combat.cannon.CannonSalvos;
import com.piranport.combat.cannon.CannonInventory;
import com.piranport.combat.cannon.SalvoContext;
import com.piranport.artillery.ArtilleryItem;
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
 * 齐射延迟射击调度器。按当前火炮定义的齐射间隔安排剩余炮管。
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
     * 按当前有效火炮配置的 salvoInterval 调度同型炮；重复计划会替换旧计划。
     */
    public static void schedule(ServerPlayer player, Item expectedType, List<int[]> slotPairs,
                                 int aimMode, double ax, double ay, double az) {
        long now = player.serverLevel().getGameTime();
        SalvoContext context = new SalvoContext(player, player.serverLevel(), player.getMainHandItem());
        ArrayDeque<SalvoTask> queue = new ArrayDeque<>();

        // The interval belongs to the actual cannon definition, including runtime overrides.
        // A non-artillery caller keeps the historical immediate cadence.
        float interval = 0.0f;
        ItemStack reference = slotPairs.isEmpty() ? ItemStack.EMPTY
                : CannonInventory.weaponAt(player.getInventory(), slotPairs.get(0)[0]);
        if (reference.getItem() instanceof ArtilleryItem cannon) {
            interval = cannon.getEffectiveData(player.serverLevel()).salvoInterval();
        }
        SalvoPlan plan = SalvoPlan.remaining(slotPairs.size(), interval);

        // A zero interval is a real simultaneous salvo. Execute the remaining guns
        // before returning from the same server work item instead of waiting for the
        // next ServerTickEvent (which would turn it into a one-tick delay).
        if (interval == 0.0f) {
            // A same-tick plan supersedes any older delayed plan for this player.
            PENDING.remove(player.getUUID());
            for (int[] pair : slotPairs) {
                ItemStack weapon = CannonInventory.weaponAt(player.getInventory(), pair[0]);
                if (weapon.isEmpty() || weapon.getItem() != expectedType) continue;
                CannonSalvos.executeSalvoFire(player.serverLevel(), player,
                        pair[0], pair[1], expectedType, aimMode, ax, ay, az);
            }
            return;
        }

        for (int index = 0; index < slotPairs.size(); index++) {
            int[] pair = slotPairs.get(index);
            ItemStack weapon = CannonInventory.weaponAt(player.getInventory(), pair[0]);
            queue.add(new SalvoTask(pair[0], pair[1], expectedType, weapon, context,
                    aimMode, ax, ay, az, now + plan.delays().get(index)));
        }

        // Replacing a plan is an explicit cancellation policy for duplicate salvo input.
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
            if (player.getMainHandItem().getItem() != next.expectedWeaponType()) {
                it.remove();
                continue;
            }

            ServerLevel level = player.serverLevel();
            long now = level.getGameTime();
            ArrayDeque<SalvoTask> queue = entry.getValue();

            boolean cancelled = false;
            while (!queue.isEmpty() && queue.peek().fireTick() <= now) {
                SalvoTask task = queue.poll();
                // 同型炮被另一把替换也应取消，不能只比较注册类型。
                if (CannonInventory.weaponAt(player.getInventory(), task.weaponSlot()) != task.expectedWeapon()) {
                    cancelled = true;
                    break;
                }
                CannonSalvos.executeSalvoFire(level, player,
                        task.weaponSlot(), task.coreSlot(), task.expectedWeaponType(),
                        task.aimMode(),
                        task.aimTargetX(), task.aimTargetY(), task.aimTargetZ());
            }

            if (cancelled || queue.isEmpty()) {
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
