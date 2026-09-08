package com.piranport.dungeon.data;

/**
 * 关卡记录点（整合版 §3.2）。
 *
 * <p>位置由关卡设计决定；默认规范：每关 1-2 个、Boss 节点前必设。</p>
 *
 * @param id              记录点唯一 ID
 * @param nodeId          关联的节点 ID（玩家在该节点通关时记录点可解锁）
 * @param posX/posY/posZ 记录点坐标（讲台坐标系）
 * @param facing          玩家朝向（"north"/"south"/"east"/"west"）
 * @param unlockCondition 解锁条件字符串（"any" 默认；"boss_defeated" 表示关联节点为 BOSS 且已击败）
 */
public record CheckpointData(
        String id,
        String nodeId,
        int posX,
        int posY,
        int posZ,
        String facing,
        String unlockCondition
) {
    public static final String UNLOCK_ANY = "any";
    public static final String UNLOCK_BOSS_DEFEATED = "boss_defeated";

    /** 检查给定节点类型与通关状态是否满足解锁条件。 */
    public boolean isUnlocked(boolean nodeCleared) {
        return switch (unlockCondition == null ? UNLOCK_ANY : unlockCondition) {
            case UNLOCK_BOSS_DEFEATED -> nodeCleared;
            default -> true;
        };
    }
}