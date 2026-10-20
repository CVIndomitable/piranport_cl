package com.piranport.dungeon.event;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.data.VictoryCondition;
import com.piranport.dungeon.escort.EscortRuntime;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.saved.DungeonObjectiveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 《副本/12》海面目标圈：夺旗红圈（边缘从正北起顺时针随占领百分比变绿）、
 * 护航与迂回目标金圈 + 黄色信标光柱。用粒子画圈，不新增实体与贴图。
 *
 * <p>只发给实例内在场且距离内的玩家，与 {@link DungeonCheckpointBeacon} 同口径。</p>
 */
public final class DungeonNodeMarkers {
    private DungeonNodeMarkers() {}

    private static final int RING_POINTS = 48;
    private static final int BEACON_HEIGHT = 48;
    private static final double RING_RADIUS_TARGET = 4.0;
    private static final double MAX_DIST_SQ = 160.0 * 160.0;
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f, 0.2f, 0.2f), 1.2f);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.2f, 1f, 0.3f), 1.2f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.84f, 0f), 1.2f);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(new Vector3f(1f, 1f, 0.2f), 1.0f);

    /** 当前节点是夺旗节点时返回占领百分比（0–100），否则返回 -1 表示不显示进度条。 */
    public static float captureProgressPercent(ServerLevel level, DungeonInstance instance) {
        StageData stage = stageOf(instance);
        String nodeId = instance.getCurrentNode();
        if (stage == null || nodeId == null || !isFlagNode(stage, stage.nodes().get(nodeId))) return -1f;
        return (float) DungeonObjectiveData.get(level).captureProgress(instance.getInstanceId(), nodeId);
    }

    public static void tick(MinecraftServer server, ServerLevel dungeonLevel) {
        DungeonInstanceManager mgr = DungeonInstanceManager.get(dungeonLevel);
        for (DungeonInstance instance : mgr.getAllInstances()) {
            if (instance.getState() != DungeonInstance.State.ACTIVE) continue;
            StageData stage = stageOf(instance);
            String nodeId = instance.getCurrentNode();
            if (stage == null || nodeId == null || instance.getClearedNodes().contains(nodeId)) continue;
            NodeData node = stage.nodes().get(nodeId);
            if (node == null) continue;
            if (isFlagNode(stage, node)) {
                BlockPos center = instance.getNodeSpawnPos(nodeId);
                double progress = DungeonObjectiveData.get(dungeonLevel).captureProgress(
                        instance.getInstanceId(), nodeId);
                drawFlagRing(server, dungeonLevel, mgr, instance, center, stage.victoryObjectives().captureRadius(), progress);
            } else if (node.completesByArrival()) {
                Vec3 target = targetOf(instance, node);
                drawTargetRing(server, dungeonLevel, mgr, instance, target);
            }
        }
    }

    private static boolean isFlagNode(StageData stage, NodeData node) {
        return node != null && !node.completesByArrival()
                && stage.victoryConditions().contains(VictoryCondition.CAPTURE_FLAG);
    }

    private static Vec3 targetOf(DungeonInstance instance, NodeData node) {
        if (node.escort() != null) return EscortRuntime.targetPoint(instance, node);
        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        int[] offset = node.detour().targetOffset();
        return Vec3.atCenterOf(center.offset(offset[0], offset[1], offset[2]));
    }

    private static void drawFlagRing(MinecraftServer server, ServerLevel level, DungeonInstanceManager mgr,
                                     DungeonInstance instance, BlockPos center, int radius, double progress) {
        double cx = center.getX() + 0.5, cz = center.getZ() + 0.5;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, center.getX(), center.getZ());
        for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
            if (player.level() != level || player.distanceToSqr(cx, player.getY(), cz) > MAX_DIST_SQ) continue;
            for (int i = 0; i < RING_POINTS; i++) {
                double fraction = (double) i / RING_POINTS;
                boolean captured = fraction * 100.0 < progress;
                drawRingPoint(level, player, cx, cz, radius, fraction, surface,
                        captured ? GREEN : RED);
            }
            // 圈中心悬浮的不祥之兆图标没有美术素材，暂未绘制。
        }
    }

    private static void drawTargetRing(MinecraftServer server, ServerLevel level, DungeonInstanceManager mgr,
                                       DungeonInstance instance, Vec3 target) {
        int tx = (int) Math.floor(target.x), tz = (int) Math.floor(target.z);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, tx, tz);
        for (ServerPlayer player : mgr.getPresentPlayers(instance, server)) {
            if (player.level() != level || player.distanceToSqr(target.x, player.getY(), target.z) > MAX_DIST_SQ) continue;
            for (int i = 0; i < RING_POINTS; i++) {
                drawRingPoint(level, player, target.x, target.z, RING_RADIUS_TARGET,
                        (double) i / RING_POINTS, surface, GOLD);
            }
            for (int dy = 1; dy <= BEACON_HEIGHT; dy += 2) {
                level.sendParticles(player, YELLOW, true, target.x, surface + dy, target.z, 1, 0.05, 0.0, 0.05, 0.0);
            }
        }
    }

    /** 从正北起顺时针：北为 -Z，东为 +X。 */
    private static void drawRingPoint(ServerLevel level, ServerPlayer player, double cx, double cz,
                                      double radius, double fraction, int surface,
                                      DustParticleOptions color) {
        double theta = fraction * Math.PI * 2.0;
        double x = cx + radius * Math.sin(theta);
        double z = cz - radius * Math.cos(theta);
        level.sendParticles(player, color, true, x, surface + 0.2, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static StageData stageOf(DungeonInstance instance) {
        return DungeonRegistry.INSTANCE.getStage(instance.getStageId());
    }
}
