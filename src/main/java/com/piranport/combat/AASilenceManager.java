package com.piranport.combat;

import com.piranport.item.ShipCoreItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 大口径主炮防空静默状态机 — 策划决策/数值/05-船型职能分化修订.md §定稿修订 #3。
 *
 * <p>当玩家开火发射大口径主炮炮弹（{@link ShipCoreItem#LARGE_SHELLS}）时，触发
 * 5 秒防空静默窗口，期间所有自动近防炮/防空导弹禁用；窗口结束后自动恢复
 * （H 键仍可手动控制总开关）。</p>
 *
 * <p>线程模型：仅服务端读写；tick 由 {@code PlayerTickHandler} 调用。决策
 * §2026-09-07：静默改为 5 秒定时窗口 + 自动恢复（替代原"不自动恢复 + 手动重启"）。</p>
 */
public final class AASilenceManager {

    /** 5 秒 = 100 tick */
    public static final int SILENCE_DURATION_TICKS = 100;

    /** 玩家 UUID → 静默剩余 ticks（>0 表示正在静默） */
    private static final Map<UUID, Integer> SILENCE_TICKS = new HashMap<>();

    private AASilenceManager() {}

    /**
     * 启动静默窗口。已存在窗口时刷新为完整时长（避免连续开火无限延后恢复）。
     */
    public static void startSilence(Player player) {
        if (player == null) return;
        SILENCE_TICKS.put(player.getUUID(), SILENCE_DURATION_TICKS);
    }

    /** 查询玩家当前是否处于静默窗口 */
    public static boolean isSilenced(Player player) {
        if (player == null) return false;
        Integer ticks = SILENCE_TICKS.get(player.getUUID());
        return ticks != null && ticks > 0;
    }

    /**
     * 服务端 tick 衰减：每 tick -1，到 0 自动从 map 中移除。
     * 由 {@code PlayerTickHandler} 在每 tick 调用。
     */
    public static void tickDown(Player player) {
        if (player == null) return;
        Integer ticks = SILENCE_TICKS.get(player.getUUID());
        if (ticks == null) return;
        int next = ticks - 1;
        if (next <= 0) {
            SILENCE_TICKS.remove(player.getUUID());
        } else {
            SILENCE_TICKS.put(player.getUUID(), next);
        }
    }

    /** 清空所有状态（玩家退出副本/重载世界时清理） */
    public static void clear(Player player) {
        if (player == null) return;
        SILENCE_TICKS.remove(player.getUUID());
    }

    /**
     * 检测武器发射的弹药是否为大口径炮弹；若是大口径则触发静默窗口。
     * 由 {@code ShipCoreCombat} 在开火成功路径调用。
     *
     * @param ammoFired 实际发射的弹药 ItemStack（非空）
     */
    public static void onCannonFire(Player player, ItemStack ammoFired) {
        if (player == null || ammoFired == null || ammoFired.isEmpty()) return;
        if (ammoFired.is(ShipCoreItem.LARGE_SHELLS)) {
            startSilence(player);
        }
    }
}