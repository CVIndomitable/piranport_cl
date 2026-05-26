package com.piranport.combat;

import com.piranport.entity.TorpedoEntity;
import com.piranport.network.TorpedoGuidanceStatePayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端鱼雷制导管理器 — 追踪哪个玩家在引导哪枚鱼雷及其待处理的方向输入。
 *
 * <p><b>线程模型</b>: 服务端主线程（主要访问），ConcurrentHashMap 保护 shutdown 时的并发。
 * <p><b>生命周期</b>:
 *   引导结束时移除条目（鱼雷被摧毁、导线切断、玩家登出 {@link #endGuidance(UUID)}），
 *   服务端关闭时通过 {@link #clearAll()} 在
 *   {@link com.piranport.server.ServerGameEvents#onServerStopped} 中清理。
 * <p><b>网络同步</b>: 通过 {@link com.piranport.network.TorpedoGuidanceInputPayload} 接收客户端输入，
 *   通过 {@link com.piranport.network.TorpedoGuidanceStatePayload} 通知客户端开始/结束。
 */
public class TorpedoGuidanceManager {

    private TorpedoGuidanceManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<UUID, UUID> activeGuidance = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> pendingInput = new ConcurrentHashMap<>();
    /** 频率限制：记录每个玩家上次输入的时间戳（毫秒），防止 DoS 攻击 */
    private static final Map<UUID, Long> lastInputTime = new ConcurrentHashMap<>();
    /** 最小输入间隔（毫秒）：50ms = 2.5tick，限制客户端发包频率 */
    private static final long MIN_INPUT_INTERVAL_MS = 50;

    public static void startGuidance(ServerPlayer player, TorpedoEntity torpedo) {
        UUID playerUUID = player.getUUID();
        UUID prev = activeGuidance.put(playerUUID, torpedo.getUUID());
        if (prev != null && !prev.equals(torpedo.getUUID())) {
            pendingInput.remove(playerUUID);
            PacketDistributor.sendToPlayer(player, new TorpedoGuidanceStatePayload(false, 0));
        }
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceStatePayload(true, torpedo.getId()));
    }

    public static void endGuidance(UUID playerUUID) {
        activeGuidance.remove(playerUUID);
        pendingInput.remove(playerUUID);
        lastInputTime.remove(playerUUID);
    }

    /** 结束引导并通知客户端恢复摄像机。 */
    public static void endGuidance(ServerPlayer player) {
        endGuidance(player.getUUID());
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceStatePayload(false, 0));
    }

    public static boolean isGuiding(UUID playerUUID) {
        return activeGuidance.containsKey(playerUUID);
    }

    @Nullable
    public static UUID getGuidedTorpedo(UUID playerUUID) {
        return activeGuidance.get(playerUUID);
    }

    public static void handleInput(UUID playerUUID, float dx, float dy, float dz) {
        if (!activeGuidance.containsKey(playerUUID)) return;

        // 频率限制：防止客户端每tick发送输入导致服务端卡顿
        long now = System.currentTimeMillis();
        Long lastTime = lastInputTime.get(playerUUID);
        if (lastTime != null && now - lastTime < MIN_INPUT_INTERVAL_MS) {
            return; // 拒绝过于频繁的输入
        }

        lastInputTime.put(playerUUID, now);
        pendingInput.put(playerUUID, new float[]{dx, dy, dz});
    }

    /** 消费最新的方向输入。如果没有输入则返回 null（鱼雷漂移）。 */
    @Nullable
    public static float[] consumeInput(UUID playerUUID) {
        return pendingInput.remove(playerUUID);
    }

    /** 当被引导的鱼雷被摧毁/导线切断时调用，以便通知客户端退出引导。 */
    public static void onTorpedoGone(ServerPlayer player) {
        endGuidance(player);
    }

    public static void clearAll() {
        activeGuidance.clear();
        pendingInput.clear();
        lastInputTime.clear();
    }
}
