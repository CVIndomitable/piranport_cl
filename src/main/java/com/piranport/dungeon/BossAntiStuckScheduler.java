package com.piranport.dungeon;

import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.instance.DungeonInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Boss 节点防卡 tick 调度器 — 策划决策/副本/07-Boss战环境防卡地形设计.md。
 *
 * <p>注册：在 {@code NodeBattleField.spawnEnemies} 末尾对 BOSS 节点调用
 * {@link #register}；注销：在 instance 完成或失效时调用 {@link #unregister}。
 * tick：在 server level tick（{@code LevelTickEvents.END_SERVER_TICK}）调度
 * {@link BossAntiStuckArea#maybeTick}。</p>
 */
public final class BossAntiStuckScheduler {

    /** instanceId → boss flagship UUID（dungeon_flagship 标记实体） */
    private static final Map<UUID, UUID> BOSS_BY_INSTANCE = new HashMap<>();

    private BossAntiStuckScheduler() {}

    /**
     * 注册 boss 节点：调用时机 = {@code NodeBattleField.spawnEnemies} 末尾对 BOSS 节点。
     *
     * @param instance  副本实例
     * @param node      节点（须为 BOSS 类型）
     * @param spawned   spawnEnemies 返回的实体列表（含 flagship）
     */
    public static void register(DungeonInstance instance, NodeData node, java.util.List<Entity> spawned) {
        if (instance == null || node == null || spawned == null) return;
        if (node.type() != NodeData.NodeType.BOSS) return;
        for (Entity e : spawned) {
            if (e instanceof LivingEntity living && e.getTags().contains("dungeon_flagship")) {
                BOSS_BY_INSTANCE.put(instance.getInstanceId(), living.getUUID());
                return;
            }
        }
    }

    /** 注销（instance 完成 / 失效） */
    public static void unregister(DungeonInstance instance) {
        if (instance == null) return;
        BOSS_BY_INSTANCE.remove(instance.getInstanceId());
    }

    /** 由 LevelTickEvents 调用 — 找当前 level 内该 instance 的 boss 并 tick 防卡 */
    public static void tickInstances(ServerLevel level) {
        if (level == null) return;
        for (Map.Entry<UUID, UUID> entry : BOSS_BY_INSTANCE.entrySet()) {
            UUID bossUuid = entry.getValue();
            Entity boss = level.getEntity(bossUuid);
            if (!(boss instanceof LivingEntity living)) continue;
            BossAntiStuckArea.maybeTick(level, living);
        }
    }
}