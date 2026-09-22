package com.piranport.dungeon.event;

import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import net.minecraft.core.BlockPos;

import java.util.UUID;

/** 入口决策只读取进度，非法请求不改变参与者、计时器或暂停状态。 */
public final class DungeonEntryRules {
    private DungeonEntryRules() {}

    public static boolean canEnter(DungeonInstance instance, StageData stage, String nodeId) {
        if (!stage.nodes().containsKey(nodeId)) return false;
        if (instance == null) return stage.startNode().equals(nodeId);
        if (instance.getState() == DungeonInstance.State.CLEANUP
                || instance.getState() == DungeonInstance.State.CREATING) return false;
        if (instance.hasEnteredNode(nodeId)) return true;
        if (instance.getState() == DungeonInstance.State.COMPLETED) return false;
        String current = instance.getCurrentNode();
        if (current != null && !instance.getClearedNodes().contains(current)) return false;
        if (instance.getClearedNodes().isEmpty()) return stage.startNode().equals(nodeId);
        return instance.getClearedNodes().stream().anyMatch(id -> stage.getReachableFrom(id).contains(nodeId));
    }

    /**
     * "从头开始"（仅传送回起点）的放行判定。
     *
     * <p>与 {@link #canEnter} 不同，本方法<b>刻意不复用推进规则</b>：canEnter 在
     * <pre>currentNode != null && !clearedNodes.contains(currentNode)</pre> 时会拒绝一切请求
     * （防止玩家在战斗中途直接跳到下一个节点）。但玩家阵亡/掉线时 currentNode 恰好未清，
     * 该分支会把"回起点"也一并拒掉，按钮于是静默失效。</p>
     *
     * <p>语义（整合版 §3.1）：回起点只传送、不刷新怪物与宝箱、不清空 clearedNodes/enteredNodes，
     * 因此它天然不会产生推进收益，可以安全绕开推进规则。仍需排除的只有两类状态：
     * 实例正在创建（数据未就绪）与正在清理（即将销毁）。</p>
     */
    public static boolean canRestartFromBeginning(DungeonInstance instance, StageData stage) {
        if (stage == null || instance == null) return false;
        if (!stage.nodes().containsKey(stage.startNode())) return false;
        return instance.getState() != DungeonInstance.State.CREATING
                && instance.getState() != DungeonInstance.State.CLEANUP;
    }

    public static CheckpointData latestCheckpoint(DungeonInstance instance, StageData stage, UUID player) {
        if (instance == null) return null;
        String id = instance.getLatestCheckpointFor(player);
        return stage.checkpoints().stream().filter(cp -> cp.id().equals(id)
                && instance.hasEnteredNode(cp.nodeId())).findFirst().orElse(null);
    }

    /** X/Z 相对所属节点平台，Y 为副本绝对高度，与现有 JSON 的默认 Y=64 一致。 */
    public static BlockPos checkpointPosition(DungeonInstance instance, CheckpointData checkpoint) {
        BlockPos center = instance.getNodeSpawnPos(checkpoint.nodeId());
        return new BlockPos(center.getX() + checkpoint.posX(), checkpoint.posY(),
                center.getZ() + checkpoint.posZ());
    }
}
