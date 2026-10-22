package com.piranport.dungeon.event;

import com.piranport.advancement.ModAdvancements;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.network.NodeClearedToastPayload;
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
 * <p>Boss 节点通关时另走《副本/21》主线奖励（材料包、勋章、随机蓝图）。
 * 2026-10-22 起不再打开结算页：通关只发一条右上角进度式横幅，奖励照旧直接进包。</p>
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

            // 《副本/19》2026-10-22 补充：不再弹全屏结算页、不再需要玩家点确认。
            // 奖励已在上面直接进包，这里只发一条右上角进度式横幅（标题"本节点已通关"）。
            // 横幅与 buildPortalStructure 摆门是同一拍：看到横幅时后续节点的门已经立在水面上。
            PacketDistributor.sendToPlayer(p, new NodeClearedToastPayload(
                    s.displayName(), n.label(), firstClear));
        }
        return true;
    }
}
