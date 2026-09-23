package com.piranport.dungeon;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.VictoryCondition;
import com.piranport.dungeon.instance.DungeonInstance;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

/**
 * 关卡胜利条件评估器（业务判定）。
 * 依据：策划决策/副本/12-关卡多样化扩展枚举.md、§副本/06 过关判定多样化
 *
 * <p>2026-09-10 实现：8 种胜利条件（KILL_ALL / KILL_BOSS / SURVIVE / DEFEND_POINT /
 * COLLECT_ITEMS / ESCORT / PUZZLE / REACH_POINT / CAPTURE_FLAG）业务判定入口。
 * 业务参数来自 {@link StageData.VictoryObjectives}（关卡 JSON 顶层 victory_objectives 段）。</p>
 *
 * <p>策划已定稿的判定细则（2026-09-09）：</p>
 * <ul>
 *   <li>运输（REACH_POINT）：玩家到达指定位置后可以过关</li>
 *   <li>歼灭（KILL_ALL）：玩家全灭副本敌人后可以过关</li>
 *   <li>护航（ESCORT）：护航对象到达指定位置后可以过关</li>
 *   <li>夺旗（CAPTURE_FLAG）：玩家在指定范围内保持存活指定时间 <b>或</b> 全灭副本敌人后可以过关（双路径 OR）</li>
 *   <li>存活（SURVIVE）：坚持 N 秒（秒数在 victory_objectives.survive_seconds）</li>
 *   <li>击破首领（KILL_BOSS）：由 DungeonEventHandler 的节点完成事件流处理</li>
 * </ul>
 *
 * <p>关卡脚本集成提示：</p>
 * <pre>
 *   if (VictoryEvaluator.checkSingle(level, instance, VictoryCondition.REACH_POINT)) {
 *       // 触发关卡完成逻辑
 *   }
 * </pre>
 */
public final class VictoryEvaluator {

    private VictoryEvaluator() {}

    /** 重载：保留旧 API（仅 instance 输入，缺业务字段 → 走 stage JSON 默认） */
    public static boolean check(DungeonInstance instance,
                                 Iterable<VictoryCondition> victoryConditions) {
        if (instance == null || victoryConditions == null) return false;
        for (VictoryCondition cond : victoryConditions) {
            if (checkSingle(null, instance, cond)) return true;
        }
        return false;
    }

    /** 单条件判定（推荐使用） */
    public static boolean checkSingle(ServerLevel level, DungeonInstance instance, VictoryCondition cond) {
        if (instance == null || cond == null) return false;
        StageData stage = com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        StageData.VictoryObjectives obj = (stage != null) ? stage.victoryObjectives() : StageData.VictoryObjectives.EMPTY;

        return switch (cond) {
            case KILL_ALL -> checkKillAll(level, instance, stage, obj);
            case KILL_BOSS -> stage != null && !stage.bossNodes().isEmpty()
                    && instance.getClearedNodes().containsAll(stage.bossNodes());
            case SURVIVE -> checkSurvive(level, instance, obj);
            case ESCORT -> checkEscort(level, instance, obj);
            case REACH_POINT -> checkReachPoint(level, instance, stage, obj);
            case CAPTURE_FLAG -> checkCaptureFlag(level, instance, stage, obj);
            case DEFEND_POINT, COLLECT_ITEMS, PUZZLE ->
                    // 业务判定由关卡脚本侧填充（决策 §副本/06：业务判定留待关卡脚本）
                    false;
        };
    }

    // ===== 业务判定 =====

    /**
     * KILL_ALL：关卡 JSON 配置 require_all_nodes_cleared=true 时，
     * 检查 instance 的 clearedNodes 覆盖所有 battle/boss 节点。
     * 若未配置，默认 false（关卡脚本侧自行判定）。
     */
    private static boolean checkKillAll(ServerLevel level, DungeonInstance instance, StageData stage,
                                        StageData.VictoryObjectives obj) {
        if (stage == null) return false;
        // 优先检查当前实例中仍存活的副本实体；节点已清空时允许使用持久化进度兜底。
        if (level != null) {
            String instanceTag = "dungeon_instance_" + instance.getInstanceId();
            boolean active = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(instance.getUsableMinX(), level.getMinBuildHeight(), instance.getUsableMinZ(),
                            instance.getUsableMaxX() + 1, level.getMaxBuildHeight(), instance.getUsableMaxZ() + 1),
                    e -> e.isAlive() && e.getTags().contains(instanceTag)).stream().findAny().isPresent();
            if (active) return false;
        }
        if (!obj.requireAllNodesCleared()) return false;
        for (NodeData n : stage.nodes().values()) {
            if ((n.type() == NodeData.NodeType.BATTLE || n.type() == NodeData.NodeType.BOSS)
                    && !instance.getClearedNodes().contains(n.nodeId())) {
                return false;
            }
        }
        return true;
    }

    /** SURVIVE：坚持 N 秒（决策 §副本/06）。 */
    private static boolean checkSurvive(ServerLevel level, DungeonInstance instance, StageData.VictoryObjectives obj) {
        if (level == null || obj.surviveSeconds() <= 0 || instance.getCurrentNode() == null) return false;
        return com.piranport.dungeon.saved.DungeonSettlementData.get(level)
                .elapsedMillis(instance.getInstanceId(), instance.getCurrentNode()) >= obj.surviveSeconds() * 1000L;
    }

    /**
     * ESCORT：护送目标实体到达指定位置。
     * 关卡 JSON 顶层 victory_objectives.escort_entity 配置实体 TagKey；
     * 当前实现要求关卡内至少有一个该实体类型的 LivingEntity 已存在（到达判定留给脚本）。
     */
    private static boolean checkEscort(ServerLevel level, DungeonInstance instance,
                                       StageData.VictoryObjectives obj) {
        if (level == null || obj.escortEntityKey() == null || obj.escortEntityKey().isEmpty()
                || obj.reachPointOffset() == null || stageFor(instance) == null) return false;
        StageData stage = stageFor(instance);
        BlockPos target = instance.getNodeSpawnPos(stage.startNode()).offset(
                obj.reachPointOffset()[0], obj.reachPointOffset()[1], obj.reachPointOffset()[2]);
        AABB box = new AABB(
                instance.getUsableMinX(), level.getMinBuildHeight(), instance.getUsableMinZ(),
                instance.getUsableMaxX(), level.getMaxBuildHeight(), instance.getUsableMaxZ());
        return !level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e.getTags().contains("dungeon_instance_" + instance.getInstanceId())
                        && escortMatches(e, obj.escortEntityKey())
                        && e.distanceToSqr(target.getX() + .5, target.getY() + .5, target.getZ() + .5) <= 16.0).isEmpty();
    }

    private static StageData stageFor(DungeonInstance instance) {
        return com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(instance.getStageId());
    }

    private static boolean escortMatches(LivingEntity entity, String key) {
        boolean tag = key.startsWith("#");
        var id = net.minecraft.resources.ResourceLocation.tryParse(tag ? key.substring(1) : key);
        if (id == null) return false;
        return tag ? entity.getType().is(net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ENTITY_TYPE, id))
                : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(id);
    }

    private static boolean isParticipant(Player player, DungeonInstance instance) {
        return player.isAlive() && !player.isSpectator()
                && instance.getPlayerUuids().contains(player.getUUID())
                && instance.isInsideUsableArea(player.blockPosition());
    }

    /** 只对显式配置目标的任务计时；无玩家时暂停，离开占领区立即重置连续计时。 */
    public static void tick(ServerLevel level, DungeonInstance instance) {
        if (instance.getState() != DungeonInstance.State.ACTIVE || instance.getCurrentNode() == null
                || instance.getClearedNodes().contains(instance.getCurrentNode())) return;
        StageData stage = stageFor(instance);
        if (stage == null) return;
        var obj = stage.victoryObjectives();
        boolean occupied = false;
        if (obj.reachPointOffset() != null && obj.captureRadius() > 0) {
            int[] offset = obj.reachPointOffset();
            BlockPos flag = instance.getNodeSpawnPos(stage.startNode()).offset(offset[0], offset[1], offset[2]);
            occupied = !level.getEntitiesOfClass(Player.class, new AABB(flag).inflate(obj.captureRadius()),
                    p -> isParticipant(p, instance)).isEmpty();
        }
        com.piranport.dungeon.saved.DungeonObjectiveData.get(level)
                .tickCapture(instance.getInstanceId(), instance.getCurrentNode(), occupied);
        for (VictoryCondition condition : stage.victoryConditions()) {
            if ((condition == VictoryCondition.REACH_POINT || condition == VictoryCondition.ESCORT
                    || condition == VictoryCondition.CAPTURE_FLAG || condition == VictoryCondition.SURVIVE)
                    && checkSingle(level, instance, condition)) {
                com.piranport.dungeon.block.PortalStructureHelper.buildPortalStructure(level,
                        instance.getNodeSpawnPos(instance.getCurrentNode()).offset(4, 0, 0),
                        instance.getInstanceId(), instance.getCurrentNode());
                break;
            }
        }
    }

    /**
     * REACH_POINT：玩家到达节点坐标 + reach_point_offset 偏移位置。
     * 任意 instance 内玩家距离目标点 ≤ 4 格即视为抵达。
     */
    private static boolean checkReachPoint(ServerLevel level, DungeonInstance instance,
                                           StageData stage, StageData.VictoryObjectives obj) {
        if (level == null || stage == null || obj.reachPointOffset() == null) return false;
        BlockPos center = instance.getNodeSpawnPos(stage.startNode());
        BlockPos target = center.offset(
                obj.reachPointOffset()[0],
                obj.reachPointOffset()[1],
                obj.reachPointOffset()[2]);
        AABB box = new AABB(target).inflate(4.0);
        return !level.getEntitiesOfClass(Player.class, box, p -> isParticipant(p, instance)).isEmpty();
    }

    /**
     * CAPTURE_FLAG：双路径 OR — 玩家在夺旗点存活 N 秒 <b>或</b> 触发 KILL_ALL。
     * 夺旗点位置与 KILL_ALL 走相同定位（reach_point_offset），存活计时由 instance.startTimeMillis 起算。
     */
    private static boolean checkCaptureFlag(ServerLevel level, DungeonInstance instance,
                                            StageData stage, StageData.VictoryObjectives obj) {
        // OR 路径 1：触发 KILL_ALL
        if (checkKillAll(level, instance, stage, obj)) return true;
        // OR 路径 2：玩家在夺旗半径内持续 capture_hold_seconds
        if (level == null || obj.captureRadius() <= 0 || obj.captureHoldSeconds() <= 0) return false;
        if (stage == null || obj.reachPointOffset() == null) return false;
        BlockPos center = instance.getNodeSpawnPos(stage.startNode());
        BlockPos flagPos = center.offset(
                obj.reachPointOffset()[0], obj.reachPointOffset()[1], obj.reachPointOffset()[2]);
        AABB box = new AABB(flagPos).inflate(obj.captureRadius());
        if (level.getEntitiesOfClass(Player.class, box, p -> isParticipant(p, instance)).isEmpty()) return false;
        return instance.getCurrentNode() != null && com.piranport.dungeon.saved.DungeonObjectiveData.get(level)
                .captureTicks(instance.getInstanceId(), instance.getCurrentNode()) >= obj.captureHoldSeconds() * 20L;
    }
}
