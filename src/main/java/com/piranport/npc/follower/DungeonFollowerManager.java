package com.piranport.npc.follower;

import com.piranport.config.ModEquipmentConfig;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * 副本内随从管理：携带位限制、大破撤退、真人玩家优先级。
 * <p>数据暂存内存，后续可持久化到 DungeonInstance NBT。</p>
 */
public class DungeonFollowerManager {
    /** 携带位上限（终端可调，与 ShipGirlProgressionManager 共用 global.follower_progression.max_followers）。 */
    private static int maxFollowersPerPlayer() {
        return ModEquipmentConfig.FOLLOW_PROGRESSION_MAX_FOLLOWERS.get();
    }

    /** 大破阈值：HP < 此比例时撤退（终端可调）。默认 0.20。 */
    private static float heavyDamageThreshold() {
        return (float) (double) ModEquipmentConfig.FOLLOW_RETREAT_HEAVY_DAMAGE_THRESHOLD.get();
    }

    /** 大破撤退间隔（终端可调），防止同一 tick 多次触发。默认 100 tick。 */
    private static int retreatCooldownTicks() {
        return ModEquipmentConfig.FOLLOW_RETREAT_COOLDOWN.get();
    }

    /** dungeonInstanceId -> (playerUuid -> followers) */
    private static final Map<UUID, Map<UUID, List<FollowerRecord>>> instanceFollowers = new HashMap<>();
    /** dungeonInstanceId -> 已撤退的舰娘 UUID */
    private static final Map<UUID, Set<UUID>> retreatedThisRun = new HashMap<>();

    private DungeonFollowerManager() {}

    // ===== 添加 / 移除 =====

    /**
     * 玩家进入副本时注册随从。
     * 若超出携带位上限，多余的随从自动撤退。
     * 返回成功进入的随从列表。
     */
    public static List<ShipGirlEntity> onPlayerEnter(ServerLevel level, UUID instanceId,
                                                     UUID playerUuid, List<ShipGirlEntity> followers) {
        Map<UUID, List<FollowerRecord>> instanceMap =
                instanceFollowers.computeIfAbsent(instanceId, k -> new HashMap<>());
        List<FollowerRecord> playerRecords = instanceMap.computeIfAbsent(playerUuid, k -> new ArrayList<>());

        List<ShipGirlEntity> accepted = new ArrayList<>();
        for (ShipGirlEntity follower : followers) {
            if (playerRecords.size() >= maxFollowersPerPlayer()) {
                retreatFollower(level, instanceId, follower, playerUuid, "message.piranport.follower_slot_full");
            } else {
                playerRecords.add(new FollowerRecord(follower, playerUuid));
                accepted.add(follower);
            }
        }
        return accepted;
    }

    /**
     * 玩家离开副本时清空该玩家的随从记录。
     */
    public static void onPlayerLeave(UUID instanceId, UUID playerUuid) {
        Map<UUID, List<FollowerRecord>> instanceMap = instanceFollowers.get(instanceId);
        if (instanceMap == null) return;
        List<FollowerRecord> records = instanceMap.get(playerUuid);
        if (records != null) {
            for (FollowerRecord record : records) {
                // 玩家离开时随从也撤退
                if (!record.follower.isRemoved()) {
                    record.follower.discard();
                }
            }
        }
        instanceMap.remove(playerUuid);
        if (instanceMap.isEmpty()) {
            instanceFollowers.remove(instanceId);
        }
    }

    /**
     * 副本结束时清理所有记录。
     */
    public static void onDungeonEnd(UUID instanceId) {
        Map<UUID, List<FollowerRecord>> instanceMap = instanceFollowers.get(instanceId);
        if (instanceMap != null) {
            for (List<FollowerRecord> records : instanceMap.values()) {
                for (FollowerRecord record : records) {
                    if (!record.follower.isRemoved()) {
                        record.follower.discard();
                    }
                }
            }
        }
        instanceFollowers.remove(instanceId);
        retreatedThisRun.remove(instanceId);
    }

    // ===== 查询 =====

    /**
     * 获取某玩家在当前副本的活跃随从列表。
     */
    public static List<ShipGirlEntity> getFollowers(UUID instanceId, UUID playerUuid) {
        Map<UUID, List<FollowerRecord>> instanceMap = instanceFollowers.get(instanceId);
        if (instanceMap == null) return List.of();
        List<FollowerRecord> records = instanceMap.get(playerUuid);
        if (records == null) return List.of();

        List<ShipGirlEntity> result = new ArrayList<>();
        for (FollowerRecord record : records) {
            if (!record.follower.isRemoved() && record.follower.isAlive()) {
                result.add(record.follower);
            }
        }
        return result;
    }

    /**
     * 获取某玩家当前携带位剩余数量。
     */
    public static int getRemainingSlots(UUID instanceId, UUID playerUuid) {
        Map<UUID, List<FollowerRecord>> instanceMap = instanceFollowers.get(instanceId);
        if (instanceMap == null) return maxFollowersPerPlayer();
        List<FollowerRecord> records = instanceMap.get(playerUuid);
        if (records == null) return maxFollowersPerPlayer();

        int active = 0;
        for (FollowerRecord record : records) {
            if (!record.follower.isRemoved() && record.follower.isAlive()) {
                active++;
            }
        }
        return Math.max(0, maxFollowersPerPlayer() - active);
    }

    // ===== 帧级逻辑 =====

    /**
     * 每 tick 调用：检查大破撤退。
     */
    public static void tick(ServerLevel level, UUID instanceId) {
        Map<UUID, List<FollowerRecord>> instanceMap = instanceFollowers.get(instanceId);
        if (instanceMap == null) return;
        Set<UUID> retreatedSet = retreatedThisRun.computeIfAbsent(instanceId, k -> new HashSet<>());

        for (List<FollowerRecord> records : instanceMap.values()) {
            Iterator<FollowerRecord> it = records.iterator();
            while (it.hasNext()) {
                FollowerRecord record = it.next();
                ShipGirlEntity follower = record.follower;
                if (follower.isRemoved() || !follower.isAlive()) {
                    it.remove();
                    continue;
                }
                if (record.lastRetreatTick > 0 && level.getGameTime() < record.lastRetreatTick + retreatCooldownTicks()) {
                    continue;
                }
                if (retreatedSet.contains(follower.getUUID())) {
                    it.remove();
                    continue;
                }
                if (isHeavilyDamaged(follower)) {
                    retreatFollower(level, instanceId, follower, record.ownerUuid, "message.piranport.follower_heavy_damage");
                    it.remove();
                }
            }
        }
    }

    // ===== 内部工具 =====

    private static boolean isHeavilyDamaged(ShipGirlEntity follower) {
        return follower.getMaxHealth() > 0
                && (follower.getHealth() / follower.getMaxHealth()) < heavyDamageThreshold();
    }

    private static void retreatFollower(ServerLevel level, UUID instanceId, ShipGirlEntity follower,
                                        UUID ownerUuid, String messageKey) {
        Set<UUID> retreatedSet = retreatedThisRun.computeIfAbsent(instanceId, k -> new HashSet<>());
        if (retreatedSet.contains(follower.getUUID())) return;
        retreatedSet.add(follower.getUUID());

        // 通知主人
        Player owner = level.getServer().getPlayerList().getPlayer(ownerUuid);
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable(messageKey));
        }

        follower.discard();
    }

    private static class FollowerRecord {
        final ShipGirlEntity follower;
        final UUID ownerUuid;
        int lastRetreatTick;

        FollowerRecord(ShipGirlEntity follower, UUID ownerUuid) {
            this.follower = follower;
            this.ownerUuid = ownerUuid;
            this.lastRetreatTick = 0;
        }
    }
}
