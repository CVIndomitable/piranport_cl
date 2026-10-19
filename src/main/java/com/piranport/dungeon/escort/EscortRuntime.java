package com.piranport.dungeon.escort;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonBossBroadcast;
import com.piranport.dungeon.event.DungeonNodeRouter;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.dungeon.instance.TerrainGenerationPipeline;
import com.piranport.dungeon.saved.DungeonObjectiveData;
import com.piranport.npc.deepocean.DeepOceanTransportEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 《副本/12》护航节点的副本侧运行时：刷运输船、为运输船规划航线、判定抵达、失败时重置本节点。
 *
 * <p>运输船只靠实体标签找回，不在实例存档里存 UUID：区块卸载或重启后仍能按标签接上。
 * 航线不落盘，重启后由 {@link #tickWaves} 按目标点重新规划。</p>
 */
public final class EscortRuntime {

    public static final String TRANSPORT_TAG = "piranport_escort_transport";

    /** 寻路单元边长（格）。运输船约 2 格宽，单元取 4 格才能在航线上留出余量。 */
    private static final int CELL_SIZE = 4;
    private static final int NODE_HALF = DungeonConstants.NODE_AREA_SIZE / 2;
    private static final int CELLS = DungeonConstants.NODE_AREA_SIZE / CELL_SIZE;

    private EscortRuntime() {}

    /** 玩家首次进入护航节点时刷新运输船于节点中心，并登记 Boss 条。 */
    public static void spawnTransport(ServerLevel level, DungeonInstance instance, NodeData node) {
        NodeData.EscortSpec spec = node.escort();
        if (spec == null) return;
        Entity transport = NodeBattleField.createEntity(level, spec.entityId());
        if (transport == null) return;

        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        transport.setPos(center.getX() + 0.5, DungeonConstants.SPAWN_Y, center.getZ() + 0.5);
        transport.addTag(instanceTag(instance));
        transport.addTag(nodeTag(node.nodeId()));
        transport.addTag(TRANSPORT_TAG);
        transport.addTag("piranport_dungeon");
        if (transport instanceof DeepOceanTransportEntity ship) {
            ship.setEscortTarget(targetPoint(instance, node));
        }
        level.addFreshEntity(transport);
        DungeonBossBroadcast.trackEscort(instance, transport, node.nodeId());
    }

    /** 当前护航节点的运输船已抵达目标点（碰撞箱边缘到达目标点中心）。 */
    public static boolean hasArrived(ServerLevel level, DungeonInstance instance, NodeData node) {
        return findTransport(level, instance, node.nodeId()) instanceof DeepOceanTransportEntity ship
                && ship.hasArrived();
    }

    /**
     * 按运输船进度推进护航节点波次：第 n 波在进度达到 (n-1)/总波数 时刷出。进度是航线弧长占比，
     * 无航线时（区块未就绪）不刷新。船已到达或已死亡时停止推进；死亡后的整节点重置由 {@link #onTransportLost} 负责。
     */
    public static void tickWaves(ServerLevel level, DungeonInstance instance, NodeData node) {
        if (node.escort() == null) return;
        if (!(findTransport(level, instance, node.nodeId()) instanceof DeepOceanTransportEntity ship)
                || ship.hasArrived()) return;

        refreshRoute(level, instance, node, ship);
        EscortRoute route = ship.getEscortRoute();
        if (route == null) return;

        double progress = route.progress(ship.getX(), ship.getZ());
        int spawned = instance.getNodeWave(node.nodeId());
        int total = instance.getNodeWaveTotal(node.nodeId());
        if (spawned >= total || progress < (double) spawned / total) return;

        instance.setNodeWave(node.nodeId(), spawned + 1);
        DungeonInstanceManager.get(level).setDirty();
        NodeBattleField.spawnEnemies(level, instance, node, route, progress);
    }

    /**
     * 航线过期（100 tick 或卡住）或尚无航线时重新规划。地形未就绪时不规划，船先按直线驶向目标。
     */
    private static void refreshRoute(ServerLevel level, DungeonInstance instance, NodeData node,
                                     DeepOceanTransportEntity ship) {
        EscortRoute current = ship.getEscortRoute();
        if (current != null && !current.advanceClock(ship.getX(), ship.getZ())) return;
        if (!TerrainGenerationPipeline.isReady(level, instance, node)) return;

        EscortRoute next = planRoute(level, instance, node, ship.position(), targetPoint(instance, node));
        if (next.unreachable() && (current == null || !current.unreachable())) {
            PiranPort.LOGGER.warn("Escort node {}: no water route to target, transport falls back to a straight line",
                    node.nodeId());
        }
        ship.setEscortRoute(next);
    }

    /**
     * 以节点出生中心为原点、{@link #CELL_SIZE} 格为单元，把节点区域按“整单元都是海水”判可通行，
     * 再交给 {@link EscortPathPlanner} 寻路。航线从船当前位置出发，路径点之后接上目标点。
     */
    private static EscortRoute planRoute(ServerLevel level, DungeonInstance instance, NodeData node,
                                         Vec3 from, Vec3 target) {
        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        int minX = center.getX() - NODE_HALF;
        int minZ = center.getZ() - NODE_HALF;
        boolean[][] passable = new boolean[CELLS][CELLS];
        for (int i = 0; i < CELLS; i++) {
            for (int j = 0; j < CELLS; j++) {
                passable[i][j] = isWaterCell(level, minX + i * CELL_SIZE, minZ + j * CELL_SIZE);
            }
        }

        EscortPathPlanner.Cell start = cellOf(from.x, from.z, minX, minZ);
        EscortPathPlanner.Cell goal = cellOf(target.x, target.z, minX, minZ);
        List<EscortPathPlanner.Cell> cells = EscortPathPlanner.plan(passable, start, goal);
        if (cells.isEmpty()) return EscortRoute.unreachable(from, target);

        List<Vec3> points = new ArrayList<>(cells.size() + 1);
        points.add(from);
        for (EscortPathPlanner.Cell cell : cells) {
            points.add(new Vec3(minX + cell.x() * CELL_SIZE + CELL_SIZE / 2.0, target.y,
                    minZ + cell.z() * CELL_SIZE + CELL_SIZE / 2.0));
        }
        // 目标单元可通行时终点用精确目标点，保证抵达判定与原目标一致；否则终点是就近的海面单元中心
        if (cells.get(cells.size() - 1).equals(goal)) {
            points.set(points.size() - 1, target);
        }
        return EscortRoute.planned(points);
    }

    private static boolean isWaterCell(ServerLevel level, int x0, int z0) {
        for (int dx = 0; dx < CELL_SIZE; dx++) {
            for (int dz = 0; dz < CELL_SIZE; dz++) {
                if (!level.getFluidState(new BlockPos(x0 + dx, DungeonConstants.SEA_LEVEL, z0 + dz))
                        .is(FluidTags.WATER)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static EscortPathPlanner.Cell cellOf(double x, double z, int minX, int minZ) {
        int i = clampCell((int) Math.floor((x - minX) / CELL_SIZE));
        int j = clampCell((int) Math.floor((z - minZ) / CELL_SIZE));
        return new EscortPathPlanner.Cell(i, j);
    }

    private static int clampCell(int index) {
        return Math.max(0, Math.min(CELLS - 1, index));
    }

    /** 节点过关后撤下运输船（它已到达目标或本节点不再需要）。 */
    public static void releaseTransport(ServerLevel level, DungeonInstance instance, String nodeId) {
        Entity transport = findTransport(level, instance, nodeId);
        if (transport != null) transport.discard();
    }

    /**
     * 运输船被击毁：撤掉本节点的敌人与运输船，重新刷新敌人与运输船，并把本实例玩家送回上一节点。
     * 只重置本节点；不发奖励、不推进钥匙进度。
     */
    public static void onTransportLost(ServerLevel level, DungeonInstance instance, String nodeId) {
        StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
        NodeData node = stage == null ? null : stage.nodes().get(nodeId);
        if (node == null || node.escort() == null) return;

        discardNodeEntities(level, instance, nodeId);
        DungeonObjectiveData.get(level).reset(instance.getInstanceId(), nodeId);
        instance.setNodeWave(nodeId, 1);
        DungeonInstanceManager.get(level).setDirty();

        NodeBattleField.spawnEnemies(level, instance, node);
        spawnTransport(level, instance, node);
        returnPlayersToEntry(level, instance, stage, nodeId);
    }

    private static void discardNodeEntities(ServerLevel level, DungeonInstance instance, String nodeId) {
        String instanceTag = instanceTag(instance);
        String nodeTag = nodeTag(nodeId);
        level.getEntitiesOfClass(Entity.class, usableBox(level, instance),
                e -> e.getTags().contains(instanceTag) && e.getTags().contains(nodeTag))
                .forEach(Entity::discard);
    }

    private static void returnPlayersToEntry(ServerLevel level, DungeonInstance instance,
                                             StageData stage, String nodeId) {
        String previous = stage.edges().stream()
                .filter(edge -> edge.to().equals(nodeId))
                .map(StageData.EdgeData::from)
                .findFirst()
                .orElse(null);
        // 起点没有入边：退回本节点自己的入口，否则起点护航失败时玩家留在原地
        String target = previous != null ? previous : nodeId;
        BlockPos end = instance.getNodeSpawnPos(target).offset(4, 0, 0);
        for (ServerPlayer player : DungeonInstanceManager.get(level).getPresentPlayers(instance, level.getServer())) {
            DungeonNodeRouter.teleportToNode(player, instance, target, end, player.getYRot());
        }
    }

    private static Entity findTransport(ServerLevel level, DungeonInstance instance, String nodeId) {
        String instanceTag = instanceTag(instance);
        String nodeTag = nodeTag(nodeId);
        return level.getEntitiesOfClass(Entity.class, usableBox(level, instance),
                        e -> e.isAlive() && e.getTags().contains(TRANSPORT_TAG)
                                && e.getTags().contains(instanceTag) && e.getTags().contains(nodeTag))
                .stream().findFirst().orElse(null);
    }

    public static Vec3 targetPoint(DungeonInstance instance, NodeData node) {
        BlockPos center = instance.getNodeSpawnPos(node.nodeId());
        int[] offset = node.escort().targetOffset();
        return Vec3.atCenterOf(center.offset(offset[0], offset[1], offset[2]));
    }

    private static AABB usableBox(ServerLevel level, DungeonInstance instance) {
        return new AABB(instance.getUsableMinX(), level.getMinBuildHeight(), instance.getUsableMinZ(),
                instance.getUsableMaxX() + 1, level.getMaxBuildHeight(), instance.getUsableMaxZ() + 1);
    }

    private static String instanceTag(DungeonInstance instance) {
        return "dungeon_instance_" + instance.getInstanceId();
    }

    private static String nodeTag(String nodeId) {
        return "dungeon_node_" + nodeId;
    }
}
