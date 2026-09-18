package com.piranport.dungeon.event;

import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.key.DungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** 讲台的首次进入、续关与节点选择共用同一套权威校验和实例绑定。 */
public final class DungeonEntryService {
    private DungeonEntryService() {}

    public static void enter(ServerPlayer player, BlockPos lecternPos,
                             boolean fromCheckpoint, String requestedNode) {
        if (!player.isAlive() || player.isSpectator()
                || player.distanceToSqr(lecternPos.getX() + 0.5, lecternPos.getY() + 0.5,
                lecternPos.getZ() + 0.5) > 64.0) return;
        if (!(player.level().getBlockEntity(lecternPos) instanceof DungeonLecternBlockEntity lectern)
                || !lectern.hasKey() || DungeonEventHandler.getDungeonLevel(player.server) == null) return;
        ItemStack key = lectern.getKeyStack();
        StageData stage = DungeonRegistry.INSTANCE.getStage(DungeonKeyItem.getStageId(key));
        if (stage == null) return;
        DungeonInstanceManager manager = DungeonInstanceManager.get(player.serverLevel());
        UUID id = DungeonKeyItem.getInstanceId(key);
        DungeonInstance instance = id == null ? null : manager.getInstance(id);
        // 已绑定但缺失的实例不能被空白实例覆盖，否则会擦掉钥匙对应进度。
        if (id != null && (instance == null || !instance.getStageId().equals(stage.stageId()))) return;

        CheckpointData checkpoint = fromCheckpoint
                ? DungeonEntryRules.latestCheckpoint(instance, stage, player.getUUID()) : null;
        String nodeId = requestedNode != null ? requestedNode
                : checkpoint != null ? checkpoint.nodeId() : stage.startNode();
        if (!DungeonEntryRules.canEnter(instance, stage, nodeId)) return;

        if (instance == null) {
            instance = manager.createInstance(stage.stageId(), player, lecternPos,
                    player.level().dimension().location().toString());
            if (instance == null) return;
        }
        lectern.bindInstance(instance.getInstanceId());
        instance.setLecternPos(lecternPos);
        instance.setLecternDimension(player.level().dimension().location().toString());
        instance.addPlayer(player.getUUID());
        manager.syncKey(instance, key);
        manager.setDirty();

        if (checkpoint != null) {
            float yaw = switch (checkpoint.facing()) {
                case "north" -> 180f;
                case "east" -> -90f;
                case "west" -> 90f;
                default -> 0f;
            };
            DungeonNodeRouter.teleportToNode(player, instance, nodeId,
                    DungeonEntryRules.checkpointPosition(instance, checkpoint), yaw);
        } else {
            DungeonNodeRouter.enterNode(player.serverLevel(), instance, stage.nodes().get(nodeId),
                    stage, player, key);
        }
        lectern.setChanged();
        manager.refreshPlayerPresence(player.server);
    }
}
