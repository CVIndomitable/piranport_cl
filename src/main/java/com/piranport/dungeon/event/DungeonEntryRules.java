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
