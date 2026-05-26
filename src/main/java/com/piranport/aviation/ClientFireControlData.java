package com.piranport.aviation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户端火控数据镜像 — 映射服务端当前玩家的锁定目标。
 *
 * <p><b>线程模型</b>: 客户端（渲染线程，单线程），使用 ArrayList 无需同步。
 * <p><b>生命周期</b>: 由网络包更新，在 {@link com.piranport.client.ClientGameEvents#onClientDisconnect} 中清理。
 * <p><b>网络同步</b>: 通过 {@link com.piranport.network.FireControlPayload} 从服务端同步。
 */
@OnlyIn(Dist.CLIENT)
public class ClientFireControlData {

    private static List<UUID> lockedTargets = new ArrayList<>();

    public static void setTargets(List<UUID> targets) {
        lockedTargets = new ArrayList<>(targets);
    }

    public static List<UUID> getTargets() {
        return List.copyOf(lockedTargets);
    }

    public static void clear() {
        lockedTargets.clear();
    }
}
