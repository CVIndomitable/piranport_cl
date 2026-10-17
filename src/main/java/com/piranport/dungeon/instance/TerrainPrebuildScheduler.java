package com.piranport.dungeon.instance;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 后台预生成后续节点地图。
 *
 * <p>玩家进入第一个节点后（实例 ACTIVE），每个服务端 tick 在入口队列和起点建造
 * 之后推进一个尚未就绪的节点。优先预生成当前节点/已清节点的直连后继，再按节点序号
 * 补齐其余节点；玩家走到出口时目标海域通常已经建好，不必在门里等。</p>
 *
 * <p>预算走共享的 {@link TerrainWorkBudget}：入口队列和起点建造先消费，预生成只吃
 * 剩余额度。每 tick 全局只推进一个节点，避免多实例互相抢光预算。</p>
 */
public final class TerrainPrebuildScheduler {
    /** 多实例轮转游标；不持久化，重启后从第一个实例重新开始。 */
    private static int cursor;

    private TerrainPrebuildScheduler() {}

    /** ServerTickEvent.Post 调用；必须排在 TerrainEntryQueue / LecternBuildScheduler 之后。 */
    public static void tick(MinecraftServer server) {
        ServerLevel dungeon = DungeonEventHandler.getDungeonLevel(server);
        if (dungeon == null) return;
        List<DungeonInstance> instances = new ArrayList<>(
                DungeonInstanceManager.get(server.overworld()).getAllInstances());
        if (instances.isEmpty()) {
            cursor = 0;
            return;
        }
        int size = instances.size();
        if (cursor >= size) cursor = 0;
        for (int offset = 0; offset < size; offset++) {
            DungeonInstance instance = instances.get(Math.floorMod(cursor + offset, size));
            // 只在有玩家实际进入副本时后台建造；SUSPENDED/通关/清理中的实例不占预算。
            if (instance.getState() != DungeonInstance.State.ACTIVE) continue;
            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            if (stage == null) continue;
            NodeData next = firstNotReady(dungeon, instance, stage);
            if (next == null) continue;
            TerrainGenerationPipeline.tick(dungeon, instance, next);
            cursor = Math.floorMod(cursor + offset + 1, size);
            return;
        }
        cursor = 0;
    }

    /**
     * 优先直连后继（edges 书写顺序），再按节点序号补齐其余节点。
     * 起点节点未进本时交给 {@link com.piranport.dungeon.block.LecternBuildScheduler}。
     */
    private static NodeData firstNotReady(ServerLevel dungeon, DungeonInstance instance, StageData stage) {
        for (String id : priorityOrder(instance, stage)) {
            if (id.equals(stage.startNode()) && !instance.hasEnteredNode(id)) {
                continue;
            }
            NodeData node = stage.nodes().get(id);
            if (node == null) continue;
            if (TerrainGenerationPipeline.isReady(dungeon, instance, node)) continue;
            return node;
        }
        return null;
    }

    private static List<String> priorityOrder(DungeonInstance instance, StageData stage) {
        LinkedHashSet<String> order = new LinkedHashSet<>();
        Set<String> sources = new HashSet<>(instance.getClearedNodes());
        if (instance.getCurrentNode() != null) sources.add(instance.getCurrentNode());
        // 刚进起点、还没有已清节点时，也先把起点的后继排到前面。
        if (sources.isEmpty()) sources.add(stage.startNode());
        for (StageData.EdgeData edge : stage.edges()) {
            if (sources.contains(edge.from()) && stage.nodes().containsKey(edge.to())) {
                order.add(edge.to());
            }
        }
        List<String> all = new ArrayList<>(stage.nodes().keySet());
        all.sort(Comparator.comparingInt(stage::nodeIndexOf));
        order.addAll(all);
        return List.copyOf(order);
    }
}
