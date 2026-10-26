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
 * <p><b>数据语义</b>: setTargets 重新赋值整个列表，而非修改现有列表。
 */
@OnlyIn(Dist.CLIENT)
public class ClientFireControlData {

    private static List<UUID> lockedTargets = new ArrayList<>();

    /** 替换整个目标列表（非修改现有列表）*/
    public static void setTargets(List<UUID> targets) {
        lockedTargets = new ArrayList<>(targets);
    }

    /** 返回目标列表的不可变副本 */
    public static List<UUID> getTargets() {
        return List.copyOf(lockedTargets);
    }

    /** 清空目标列表 */
    public static void clear() {
        lockedTargets.clear();
    }

    /**
     * 按条件剔除目标（客户端自检用）。
     *
     * <p>服务端同步正常时这里是兜底；服务端某个清空路径漏发包时，它也能在
     * 客户端把已确认死亡的目标剔掉，避免 HUD / 红框一直挂着。</p>
     *
     * @param shouldRemove 返回 true 表示该 UUID 应从列表移除
     * @return 是否有目标被移除
     */
    public static boolean removeWhere(java.util.function.Predicate<UUID> shouldRemove) {
        if (lockedTargets.isEmpty()) return false;
        List<UUID> kept = new ArrayList<>(lockedTargets.size());
        for (UUID uuid : lockedTargets) {
            if (!shouldRemove.test(uuid)) kept.add(uuid);
        }
        if (kept.size() == lockedTargets.size()) return false;
        lockedTargets = kept;
        return true;
    }
}
