package com.piranport.aviation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 服务端火控状态 — 管理玩家→锁定目标的映射。
 *
 * <p><b>线程模型</b>: 服务端主线程访问为主，使用 ConcurrentHashMap / CopyOnWriteArrayList
 * 防御服务器关闭时并发清理导致的竞态条件。
 * <p><b>生命周期</b>: 玩家登出时通过 {@link #clearTargets(UUID)} 清理，
 * 服务端关闭时通过 {@link #clearAll()} 清理。
 * <p><b>容量限制</b>: 每个玩家最多 {@value #MAX_TARGETS} 个目标。
 */
public class FireControlManager {

    private static final Map<UUID, List<UUID>> LOCKED_TARGETS = new ConcurrentHashMap<>();
    private static final int MAX_TARGETS = 4;

    private FireControlManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Replace the target list with a single target. */
    public static void lock(UUID playerUUID, UUID targetUUID) {
        List<UUID> list = new CopyOnWriteArrayList<>();
        list.add(targetUUID);
        LOCKED_TARGETS.put(playerUUID, list);
        com.piranport.debug.PiranPortDebug.event(
                "FireControl LOCK | player={} target={}", playerUUID, targetUUID);
    }

    /** Append a target (up to MAX_TARGETS). Does nothing if already in list. */
    public static void addTarget(UUID playerUUID, UUID targetUUID) {
        List<UUID> list = LOCKED_TARGETS.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>());
        if (!list.contains(targetUUID) && list.size() < MAX_TARGETS) {
            list.add(targetUUID);
            com.piranport.debug.PiranPortDebug.event(
                    "FireControl ADD | player={} target={} total={}", playerUUID, targetUUID, list.size());
        }
    }

    /** Remove all locked targets for this player. */
    public static void clearTargets(UUID playerUUID) {
        LOCKED_TARGETS.remove(playerUUID);
        com.piranport.debug.PiranPortDebug.event("FireControl CANCEL | player={}", playerUUID);
    }

    /** Remove specific dead target UUIDs. Only fully clears if no targets remain. */
    public static void removeDeadTargets(UUID playerUUID, java.util.function.Predicate<UUID> isDead) {
        List<UUID> list = LOCKED_TARGETS.get(playerUUID);
        if (list == null) return;
        // 批量收集再移除，避免 CopyOnWriteArrayList.removeIf 在每次删除时复制整个数组
        List<UUID> toRemove = new java.util.ArrayList<>();
        for (UUID uuid : list) {
            if (isDead.test(uuid)) toRemove.add(uuid);
        }
        list.removeAll(toRemove);
        if (list.isEmpty()) {
            LOCKED_TARGETS.remove(playerUUID);
        }
    }

    /** Returns an unmodifiable snapshot of the player's locked targets. */
    public static List<UUID> getTargets(UUID playerUUID) {
        List<UUID> list = LOCKED_TARGETS.get(playerUUID);
        return list == null ? List.of() : List.copyOf(list);
    }

    // ===== Fighter Ground-Attack Mode (default OFF = air-only) =====
    private static final Set<UUID> FIGHTER_GROUND_ENABLED = ConcurrentHashMap.newKeySet();

    /** Toggle fighter ground-attack mode. Returns true if ground attack is now enabled. */
    public static boolean toggleFighterGround(UUID playerUUID) {
        if (!FIGHTER_GROUND_ENABLED.remove(playerUUID)) {
            FIGHTER_GROUND_ENABLED.add(playerUUID);
            return true;
        }
        return false;
    }

    /** Returns true if the player's fighters should only attack airborne FC targets (default). */
    public static boolean isFighterAirOnly(UUID playerUUID) {
        return !FIGHTER_GROUND_ENABLED.contains(playerUUID);
    }

    // ===== Phase 27：策划 §7.7 "被发现!" Buff 候选池 =====
    // 火控锁定选目标时优先从本集合中选；具体填充由 ServerLevel 扫描 SpottedEffect 的事件处理完成。
    private static final Set<UUID> SPOTTED_ENTITIES = ConcurrentHashMap.newKeySet();

    /** 注册一个"被发现"实体 UUID（带 effect 自动触发）。幂等。 */
    public static void markSpotted(UUID entityUuid) {
        SPOTTED_ENTITIES.add(entityUuid);
    }

    /** 移除"被发现"标记。幂等。 */
    public static void clearSpotted(UUID entityUuid) {
        SPOTTED_ENTITIES.remove(entityUuid);
    }

    /** 当前服务器所有"被发现"实体快照。 */
    public static List<UUID> getSpottedEntities() {
        return List.copyOf(SPOTTED_ENTITIES);
    }

    /** 实体是否被标记"被发现"。 */
    public static boolean isSpotted(UUID entityUuid) {
        return SPOTTED_ENTITIES.contains(entityUuid);
    }

    /** Remove all state (call on server stop / world unload). */
    public static void clearAll() {
        LOCKED_TARGETS.clear();
        FIGHTER_GROUND_ENABLED.clear();
        SPOTTED_ENTITIES.clear();
    }
}
