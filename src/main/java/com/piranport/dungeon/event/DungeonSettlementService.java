package com.piranport.dungeon.event;

import com.piranport.advancement.ModAdvancements;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.DungeonResultPayload;
import com.piranport.dungeon.saved.DungeonSavedData;
import com.piranport.dungeon.saved.DungeonSettlementData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 副本/19：每节点即时发奖。
 * <p>Boss 节点通关时另走《副本/21》主线奖励（材料包、勋章、随机蓝图），
 * 并把整关结算页合并到同一次发包，避免 completeDungeon 再发一页把奖励盖掉。</p>
 */
public final class DungeonSettlementService {
    private DungeonSettlementService() {}

    public static boolean isCounting(DungeonInstance i, String n) {
        return i != null && n != null && i.getState() == DungeonInstance.State.ACTIVE
                && i.hasEnteredNode(n) && !i.getClearedNodes().contains(n);
    }

    public static void tickActiveNode(ServerLevel l, DungeonInstance i) {
        if (isCounting(i, i.getCurrentNode())) {
            DungeonSettlementData.get(l).tick(i.getInstanceId(), i.getCurrentNode());
        }
    }

    public static void recordKill(ServerLevel l, DungeonInstance i, String n, UUID e) {
        if (e != null && isCounting(i, n)) {
            DungeonSettlementData.get(l).recordKill(i.getInstanceId(), n, e);
        }
    }

    public static boolean settleNode(ServerLevel l, DungeonInstance i, StageData s, NodeData n,
                                     boolean completes) {
        DungeonSettlementData d = DungeonSettlementData.get(l);
        if (!i.getClearedNodes().contains(n.nodeId())
                || !d.claimSettlement(i.getInstanceId(), n.nodeId())) {
            return false;
        }
        DungeonInstanceManager m = DungeonInstanceManager.get(l);
        DungeonSavedData first = DungeonSavedData.get(l);

        // 节点奖励直接进各玩家背包（每个在场玩家独立 roll 一份），出口只留光柱+传送门。
        for (UUID id : i.getPlayerUuids()) {
            ServerPlayer p = l.getServer().getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive() || p.isSpectator()
                    || m.getInstanceForPlayer(p) != i) {
                continue;
            }

            List<String> rs = new ArrayList<>();
            for (NodeData.RewardEntry r : n.rewards()) {
                RewardDispatcher.give(p, r, rs);
            }

            boolean firstClear = false;
            if (completes) {
                ModAdvancements.award(p, "dungeon/first_clear");
                // 主线奖励按《副本/22》只给通关瞬间在 Boss 节点范围内的玩家。
                // 无快照（旧存档/脚本 Boss）时 eligible 全员兜底，避免升级后有人漏领。
                if (FirstClearRules.eligible(i, id)) {
                    firstClear = !first.hasFirstCleared(s.stageId(), id);
                    if (firstClear) first.markFirstCleared(s.stageId(), id);
                    MainlineRewardService.grant(p, s, rs);
                }
            }

            long time = completes ? i.getElapsedMillis() : d.elapsedMillis(i.getInstanceId(), n.nodeId());
            PacketDistributor.sendToPlayer(p, new DungeonResultPayload(
                    completes ? s.displayName() : s.displayName() + " · " + n.nodeId(),
                    time, firstClear, rs,
                    d.kills(i.getInstanceId(), n.nodeId()),
                    completes ? s.ending() : ""));
        }
        return true;
    }
}
