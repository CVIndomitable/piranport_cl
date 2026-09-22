package com.piranport.dungeon.event;

import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.NodeData;
import net.minecraft.server.level.ServerLevel;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.TerrainEntryQueue;
import com.piranport.dungeon.instance.TerrainGenerationPipeline;
import com.piranport.dungeon.key.DungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/** 讲台的首次进入、续关与节点选择共用同一套权威校验和实例绑定。 */
public final class DungeonEntryService {
    private DungeonEntryService() {}

    /**
     * 入口请求类型：决定走"推进"还是"仅传送"两条分支，避免再靠 boolean 组合猜语义。
     *
     * <ul>
     *   <li>{@link #ADVANCE} — 首次进入 / 节点选择 / 续关：按 DungeonEntryRules.canEnter 判定推进。</li>
     *   <li>{@link #RESTART} — 从头开始：只传送回 stage.startNode，不刷新怪物宝箱、不重新发奖。</li>
     * </ul>
     */
    public enum Mode { ADVANCE, RESTART }

    public static void enter(ServerPlayer player, BlockPos lecternPos,
                             boolean fromCheckpoint, String requestedNode) {
        enter(player, lecternPos, fromCheckpoint, requestedNode, Mode.ADVANCE);
    }

    public static void enter(ServerPlayer player, BlockPos lecternPos,
                             boolean fromCheckpoint, String requestedNode, Mode mode) {
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
        // 回起点必须有已绑定的实例：它是"仅传送"路径，不能就地新建实例，
        // 否则会给空白实例绑定钥匙、擦掉钥匙对应进度（与下面的缺失守卫同一理由）。
        // 这条也要给反馈：钥匙未绑定实例（存档损坏）时按钮不能再次变成"点了没反应"。
        if (mode == Mode.RESTART && instance == null) {
            player.sendSystemMessage(Component.translatable("dungeon.piranport.entry_rejected"));
            return;
        }
        // 已绑定但缺失的实例不能被空白实例覆盖，否则会擦掉钥匙对应进度。
        if (id != null && (instance == null || !instance.getStageId().equals(stage.stageId()))) {
            player.sendSystemMessage(Component.translatable("dungeon.piranport.entry_rejected"));
            return;
        }

        // 回起点固定落在关卡起点，忽略 checkpoint 与客户端指定的节点。
        CheckpointData checkpoint = mode == Mode.RESTART ? null
                : fromCheckpoint ? DungeonEntryRules.latestCheckpoint(instance, stage, player.getUUID()) : null;
        String nodeId = mode == Mode.RESTART ? stage.startNode()
                : requestedNode != null ? requestedNode
                : checkpoint != null ? checkpoint.nodeId() : stage.startNode();
        // 推进路径走 canEnter；回起点刻意绕开它（canEnter 在 current 未清时会拒绝请求），
        // 只保留 canRestartFromBeginning 的状态守卫，避免战斗中途按钮静默失效。
        boolean allowed = mode == Mode.RESTART
                ? DungeonEntryRules.canRestartFromBeginning(instance, stage)
                : DungeonEntryRules.canEnter(instance, stage, nodeId);
        if (!allowed) {
            // 拒绝时给玩家可见反馈：此前的静默 return 让"从头开始"看起来像按钮坏了。
            player.sendSystemMessage(Component.translatable("dungeon.piranport.entry_rejected"));
            return;
        }

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

        ServerLevel dungeonLevel = DungeonEventHandler.getDungeonLevel(player.server);
        NodeData requested = stage.nodes().get(nodeId);
        // 地形是入口的前置条件：只登记实例钥匙，不登记节点和传送玩家。
        if (dungeonLevel == null || requested == null) return;
        // 回起点：起点必然已经生成过地形（曾是入场节点），不做地形检查也不排队；
        // 即使地形因故未就绪，也只让传送落空/失效，绝不能让请求退回推进分支去重新发奖。
        if (mode == Mode.RESTART) {
            lectern.setChanged();
            manager.refreshPlayerPresence(player.server);
            DungeonNodeRouter.teleportToNode(player, instance, nodeId,
                    instance.getNodeSpawnPos(nodeId), player.getYRot());
            return;
        }
        if (!instance.hasEnteredNode(nodeId)
                && !TerrainGenerationPipeline.isReady(dungeonLevel, instance, requested)) {
            TerrainEntryQueue.get(player.server).enqueue(player, lecternPos, nodeId, fromCheckpoint);
            return;
        }

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
