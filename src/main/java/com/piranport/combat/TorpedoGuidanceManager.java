package com.piranport.combat;

import com.piranport.entity.TorpedoEntity;
import com.piranport.network.TorpedoGuidanceEndPayload;
import com.piranport.network.TorpedoGuidanceStartPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端：追踪哪个玩家在引导哪枚鱼雷及其待处理的方向输入。
 * 清理：引导结束时移除条目（鱼雷被摧毁、导线切断、玩家登出），
 * 以及服务端关闭时（GameEvents.onServerStopped）。
 */
public class TorpedoGuidanceManager {
    private static final Map<UUID, UUID> activeGuidance = new ConcurrentHashMap<>();
    private static final Map<UUID, float[]> pendingInput = new ConcurrentHashMap<>();

    public static void startGuidance(ServerPlayer player, TorpedoEntity torpedo) {
        UUID playerUUID = player.getUUID();
        UUID prev = activeGuidance.put(playerUUID, torpedo.getUUID());
        if (prev != null && !prev.equals(torpedo.getUUID())) {
            pendingInput.remove(playerUUID);
            PacketDistributor.sendToPlayer(player, new com.piranport.network.TorpedoGuidanceEndPayload());
        }
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceStartPayload(torpedo.getId()));
    }

    public static void endGuidance(UUID playerUUID) {
        activeGuidance.remove(playerUUID);
        pendingInput.remove(playerUUID);
    }

    /** 结束引导并通知客户端恢复摄像机。 */
    public static void endGuidance(ServerPlayer player) {
        endGuidance(player.getUUID());
        PacketDistributor.sendToPlayer(player, new TorpedoGuidanceEndPayload());
    }

    public static boolean isGuiding(UUID playerUUID) {
        return activeGuidance.containsKey(playerUUID);
    }

    @Nullable
    public static UUID getGuidedTorpedo(UUID playerUUID) {
        return activeGuidance.get(playerUUID);
    }

    public static void handleInput(UUID playerUUID, float dx, float dy, float dz) {
        if (activeGuidance.containsKey(playerUUID)) {
            pendingInput.put(playerUUID, new float[]{dx, dy, dz});
        }
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
    }
}
