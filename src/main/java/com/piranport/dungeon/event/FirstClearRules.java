package com.piranport.dungeon.event;

import com.piranport.dungeon.instance.DungeonInstance;

import java.util.UUID;

/** 《副本/00》首通参与判定（纯逻辑）：只有 Boss 击杀瞬间在 Boss 节点范围内的玩家有首通资格。 */
public final class FirstClearRules {
    private FirstClearRules() {}

    /** 无快照（旧存档）时按全员兜底，避免升级后有人拿不到首通。 */
    public static boolean eligible(DungeonInstance instance, UUID player) {
        return !instance.hasBossParticipantSnapshot() || instance.getBossParticipants().contains(player);
    }
}
