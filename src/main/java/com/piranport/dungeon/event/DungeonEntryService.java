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

    /**
     * 入口被拒的原因。
     *
     * <p>此前 {@link #enter} 的每个失败分支都是裸 {@code return}——玩家看到的是"走进门没反应"，
     * 却无从知道是维度没加载、关卡找不到，还是距离太远。教学门尤其致命：门只负责把玩家
     * 交给这里，这里拒了门也不知道，于是整条链路静默失败。</p>
     *
     * <p>刻意不抛异常、不写日志了事：需要的是<b>当场可见</b>的反馈，玩家不必去翻 log。</p>
     */
    private enum Reject {
        DEAD_OR_SPECTATOR("dead_or_spectator"),
        TOO_FAR("too_far"),
        NO_LECTERN_OR_KEY("no_lectern_or_key"),
        NO_DUNGEON_LEVEL("no_dungeon_level"),
        NO_SUCH_STAGE("no_such_stage"),
        REJECTED("rejected"),
        NO_NODE("no_node");

        private final String key;

        Reject(String key) {
            this.key = key;
        }

        /** 提示玩家并返回，替代原先的静默 return。 */
        void report(ServerPlayer player) {
            player.displayClientMessage(
                    Component.translatable("dungeon.piranport.entry_blocked." + key), false);
        }
    }

    public static void enter(ServerPlayer player, BlockPos lecternPos,
                             boolean fromCheckpoint, String requestedNode) {
        enter(player, lecternPos, fromCheckpoint, requestedNode, Mode.ADVANCE);
    }

    public static void enter(ServerPlayer player, BlockPos lecternPos,
                             boolean fromCheckpoint, String requestedNode, Mode mode) {
        if (!player.isAlive() || player.isSpectator()) {
            Reject.DEAD_OR_SPECTATOR.report(player);
            return;
        }
        if (player.distanceToSqr(lecternPos.getX() + 0.5, lecternPos.getY() + 0.5,
                lecternPos.getZ() + 0.5) > 64.0) {
            // 8 格上限：站在门里的玩家永远够得到，但门外远处的脚本调用会被挡掉。
            Reject.TOO_FAR.report(player);
            return;
        }
        if (!(player.level().getBlockEntity(lecternPos) instanceof DungeonLecternBlockEntity lectern)
                || !lectern.hasKey()) {
            Reject.NO_LECTERN_OR_KEY.report(player);
            return;
        }
        if (DungeonEventHandler.getDungeonLevel(player.server) == null) {
            // 副本维度拿不到（未注册/未加载）。这是"世界开着但门永远不工作"的典型原因。
            Reject.NO_DUNGEON_LEVEL.report(player);
            return;
        }
        ItemStack key = lectern.getKeyStack();
        // 必须解析 chapter_* → 真实关卡 ID：章节钥匙（ChapterKeyRecipe 产物）存的是章节 ID，
        // 直接查 stages 表恒为 null，玩家看到的会是"钥匙上的副本不存在"。
        StageData stage = DungeonRegistry.INSTANCE.getStage(DungeonKeyItem.resolveStageId(key));
        if (stage == null) {
            Reject.NO_SUCH_STAGE.report(player);
            return;
        }
        DungeonInstanceManager manager = DungeonInstanceManager.get(player.serverLevel());
        UUID id = DungeonKeyItem.getInstanceId(key);
        DungeonInstance instance = id == null ? null : manager.getInstance(id);
        // 回起点必须有已绑定的实例：它是"仅传送"路径，不能就地新建实例，
        // 否则会给空白实例绑定钥匙、擦掉钥匙对应进度（与下面的缺失守卫同一理由）。
        // 这条也要给反馈：钥匙未绑定实例（存档损坏）时按钮不能再次变成"点了没反应"。
        if (mode == Mode.RESTART && instance == null) {
            Reject.REJECTED.report(player);
            return;
        }
        // 已绑定但缺失的实例不能被空白实例覆盖，否则会擦掉钥匙对应进度。
        if (id != null && (instance == null || !instance.getStageId().equals(stage.stageId()))) {
            Reject.REJECTED.report(player);
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
            Reject.REJECTED.report(player);
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
        if (dungeonLevel == null || requested == null) {
            if (requested == null) {
                Reject.NO_NODE.report(player);
            } else {
                Reject.NO_DUNGEON_LEVEL.report(player);
            }
            return;
        }
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
