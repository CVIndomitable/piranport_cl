package com.piranport.dungeon.event;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.NodeBattleField;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 已清节点出口传送门的推进逻辑。
 *
 * <p>《副本/12》明确：舰R 里打完就回港的非 Boss 终点，皮兰港不退本；该节点过关后
 * 应亮出通往下一节点的门。本类负责在玩家走进已清节点的出口时，直接把他送到节点图
 * 里的下一个节点。只有当前节点没有后继、实例已通关，或读不到讲台钥匙时才返回
 * false，由 {@link DungeonEventHandler#onPortalEntered} 按原逻辑送回讲台。</p>
 *
 * <p>多后继节点（菱形图的 B/C 支路）当前按 edges 顺序取第一个未清理目标。
 * 带分支条件的节点仍交给 {@link DungeonBranchRouter}；真正的“多扇门由玩家选择”
 * 属于后续工作。</p>
 */
public final class DungeonProgressionRouter {
    /** 推进失败（典型为过路费不足）后的重试间隔，避免传送门每 10 tick 重复刷提示。 */
    private static final int RETRY_COOLDOWN_TICKS = 40;
    /** 目标海域建造提示的节流间隔。 */
    private static final int BUILD_MESSAGE_INTERVAL = 40;

    private static final Map<UUID, Long> LAST_FAILED_ATTEMPT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BUILD_MESSAGE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLOCKED_MESSAGE = new ConcurrentHashMap<>();

    private DungeonProgressionRouter() {}

    /**
     * @return true 表示已接管：玩家正在传送，或仍站在门里等待目标海域建造；
     *         false 表示没有可推进的下一节点，调用方按原逻辑送回讲台。
     */
    public static boolean tryAdvance(ServerPlayer player, DungeonInstance instance,
                                     StageData stage, NodeData clearedNode) {
        return tryAdvance(player, instance, stage, clearedNode, "");
    }

    /**
     * @param targetNodeId 玩家走进的那扇门明确通往的节点；空串表示旧存档/单门自动选路。
     */
    public static boolean tryAdvance(ServerPlayer player, DungeonInstance instance,
                                     StageData stage, NodeData clearedNode, String targetNodeId) {
        if (player == null || instance == null || stage == null || clearedNode == null) return false;
        if (instance.getState() != DungeonInstance.State.ACTIVE) return false;

        // 分支节点的出口是按目标分扇的门：走入哪扇门由 DungeonBranchRouter 按定格快照裁决。
        if (clearedNode.hasBranches()) {
            return DungeonBranchRouter.routeFromPortal(player, instance, stage, clearedNode, targetNodeId);
        }

        String target = targetNodeId == null || targetNodeId.isBlank()
                ? chooseNext(instance, stage, clearedNode.nodeId())
                : targetNodeId;
        if (target == null) return false;
        return advanceTo(player, instance, stage, target);
    }

    /**
     * 优先取当前节点的直连后继；若直连后继都清完了（例如玩家走回旧门），
     * 再从其它已清节点的出边里找仍可进入的节点，避免被直接送回主世界。
     */
    static String chooseNext(DungeonInstance instance, StageData stage, String fromNode) {
        String direct = firstUncleared(instance, stage, fromNode);
        if (direct != null) return direct;
        for (String cleared : instance.getClearedNodes()) {
            if (cleared.equals(fromNode)) continue;
            String next = firstUncleared(instance, stage, cleared);
            if (next != null) return next;
        }
        return null;
    }

    /**
     * 只按拓扑找下一个未清节点，不在这里用 {@link DungeonEntryRules#canEnter} 过滤。
     * WHY：多人时别人正在打的节点会让 canEnter 短暂返回 false；若此时把目标过滤掉，
     * 玩家会被误判成“没有下一节点”而送回主世界。临时不可进入应当在门里等待。
     */
    private static String firstUncleared(DungeonInstance instance, StageData stage, String fromNode) {
        for (StageData.EdgeData edge : stage.edges()) {
            if (!edge.from().equals(fromNode)) continue;
            String to = edge.to();
            if (!stage.nodes().containsKey(to)) continue;
            if (instance.getClearedNodes().contains(to)) continue;
            return to;
        }
        return null;
    }

    private static boolean advanceTo(ServerPlayer player, DungeonInstance instance,
                                     StageData stage, String targetId) {
        NodeData target = stage.nodes().get(targetId);
        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        if (target == null || dungeonLevel == null) return false;
        if (!DungeonEntryRules.canEnter(instance, stage, targetId)) {
            // 典型：队友正在打当前节点，推进规则暂时不允许。留在门里等，不送回主世界。
            notifyBlocked(player, targetId);
            return true;
        }
        ItemStack key = DungeonEventHandler.readKeyFromLectern(instance, player.server);
        if (key.isEmpty()) return false;

        // 地形未就绪时不回主世界：原地推进建造，传送门每 10 tick 会再次调用本方法。
        // 目标节点的地形建好后，下一次调用就会真正传送。
        if (!instance.hasEnteredNode(targetId)
                && !NodeBattleField.isTerrainReady(dungeonLevel, instance, target)) {
            NodeBattleField.tickTerrain(dungeonLevel, instance, target);
            notifyBuilding(player, targetId);
            return true;
        }

        // 上一次 enterNode 没把人送走（典型：过路费不足）时，隔 40 tick 再试，避免刷屏。
        long now = player.server.getTickCount();
        Long lastFailed = LAST_FAILED_ATTEMPT.get(player.getUUID());
        if (lastFailed != null && now - lastFailed < RETRY_COOLDOWN_TICKS) return true;

        Vec3 before = player.position();
        DungeonNodeRouter.enterNode(player.serverLevel(), instance, target, stage, player, key);
        DungeonEventHandler.markLecternChanged(instance, player.server);
        if (player.position().distanceToSqr(before) > 1.0) {
            LAST_FAILED_ATTEMPT.remove(player.getUUID());
            LAST_BUILD_MESSAGE.remove(player.getUUID());
            LAST_BLOCKED_MESSAGE.remove(player.getUUID());
            return true;
        }
        // enterNode 已经给出具体原因（例如资源不足）；这里只记录失败，让玩家继续站在门里。
        LAST_FAILED_ATTEMPT.put(player.getUUID(), now);
        return true;
    }

    private static void notifyBuilding(ServerPlayer player, String targetId) {
        long now = player.server.getTickCount();
        Long last = LAST_BUILD_MESSAGE.get(player.getUUID());
        if (last != null && now - last < BUILD_MESSAGE_INTERVAL) return;
        LAST_BUILD_MESSAGE.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.translatable("dungeon.piranport.advance_building", targetId), true);
    }

    private static void notifyBlocked(ServerPlayer player, String targetId) {
        long now = player.server.getTickCount();
        Long last = LAST_BLOCKED_MESSAGE.get(player.getUUID());
        if (last != null && now - last < BUILD_MESSAGE_INTERVAL) return;
        LAST_BLOCKED_MESSAGE.put(player.getUUID(), now);
        player.displayClientMessage(
                Component.translatable("dungeon.piranport.advance_blocked", targetId), true);
    }
}
