package com.piranport.dungeon.block;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.instance.TerrainGenerationPipeline;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * 插钥匙后的异步建造（策划决策/副本/17 §3.1、§3.2）。
 *
 * <p>建造挂在实例上，不挂在书台方块上：每个服务端 tick 为所有「起点节点尚未建好」的实例推进
 * 起点地形，书台被破坏、钥匙被撤走都不影响。预算与 {@code TerrainEntryQueue} 共用
 * {@code TerrainWorkBudget}，不会叠加卡服。
 */
public final class LecternBuildScheduler {
    private LecternBuildScheduler() {}

    /** 「建造完成」= 起点节点已进入过（老实例）或起点地形已就绪。 */
    public static boolean isBuilt(MinecraftServer server, DungeonInstance instance, StageData stage) {
        if (instance == null || stage == null) return false;
        if (instance.hasEnteredNode(stage.startNode())) return true;
        ServerLevel dungeon = DungeonEventHandler.getDungeonLevel(server);
        NodeData start = stage.nodes().get(stage.startNode());
        if (dungeon == null || start == null) return false;
        return TerrainGenerationPipeline.isReady(dungeon, instance, start);
    }

    /** ServerTickEvent.Post 调用。 */
    public static void tick(MinecraftServer server) {
        ServerLevel dungeon = DungeonEventHandler.getDungeonLevel(server);
        if (dungeon == null) return;
        DungeonInstanceManager manager = DungeonInstanceManager.get(server.overworld());
        for (DungeonInstance instance : manager.getAllInstances()) {
            if (instance.getState() == DungeonInstance.State.COMPLETED
                    || instance.getState() == DungeonInstance.State.CLEANUP) continue;
            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            if (stage == null || instance.hasEnteredNode(stage.startNode())) continue;
            NodeData start = stage.nodes().get(stage.startNode());
            if (start == null || TerrainGenerationPipeline.isReady(dungeon, instance, start)) continue;
            TerrainGenerationPipeline.tick(dungeon, instance, start);
        }
    }
}
