package com.piranport.dungeon.event;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.network.DungeonResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 节点级结算服务。奖励在节点完成时写入玩家背包，结算界面只展示已发放结果。
 * 击杀统计由死亡事件记录，服务端重启前后不会把客户端上报当作权威来源。
 */
public final class DungeonSettlementService {
    private static final Map<UUID, Integer> KILLS = new HashMap<>();

    private DungeonSettlementService() {}

    public static void recordKill(UUID instanceId) {
        if (instanceId != null) KILLS.merge(instanceId, 1, Integer::sum);
    }

    public static int killCount(UUID instanceId) {
        return instanceId == null ? 0 : KILLS.getOrDefault(instanceId, 0);
    }

    /** 在 markNodeCleared 成功后调用；重复调用不会重复发奖。 */
    public static boolean settleNode(MinecraftServer server, DungeonInstance instance,
                                     StageData stage, NodeData node) {
        if (server == null || instance == null || stage == null || node == null) return false;
        int kills = killCount(instance.getInstanceId());
        for (UUID uuid : instance.getPlayerUuids()) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) continue;
            var names = new ArrayList<String>();
            for (NodeData.RewardEntry reward : node.rewards()) {
                RewardDispatcher.give(player, reward, names);
            }
            // 每个节点均展示结算；首通标记由原版进度系统在关卡完成处发放。
            PacketDistributor.sendToPlayer(player, new DungeonResultPayload(
                    stage.displayName(), Math.max(0L, System.currentTimeMillis() - instance.getStartTimeMillis()),
                    false, names, kills));
        }
        return true;
    }

    /** 节点结算后清理本次实例的临时统计。 */
    public static void clear(UUID instanceId) {
        if (instanceId != null) KILLS.remove(instanceId);
    }
}
