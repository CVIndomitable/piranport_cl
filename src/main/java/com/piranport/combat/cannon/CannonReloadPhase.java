package com.piranport.combat.cannon;

/**
 * 装填状态决策；物品读条是唯一计时来源，槽位历史数据不参与推进。
 *
 * 空膛且无读条时自动启动，炮种旧装填模式不参与判定。
 */
enum CannonReloadPhase {
    LOADED, START, WAIT, COMPLETE;

    static CannonReloadPhase resolve(boolean loaded, Long endTick, long now) {
        if (loaded) return LOADED;
        if (endTick != null && endTick > 0) return endTick > now ? WAIT : COMPLETE;
        return START;
    }

    /** 已经装好的弹不退膛；只重启已有读条。 */
    static boolean shouldRestartAfterSelection(boolean loaded, Long endTick) {
        return !loaded && endTick != null && endTick > 0;
    }
}
