package com.piranport.combat.cannon;

/** 装填状态决策；物品读条是唯一计时来源，槽位历史数据不参与推进。 */
enum CannonReloadPhase {
    LOADED, IDLE, START, WAIT, COMPLETE;

    static CannonReloadPhase resolve(boolean loaded, boolean automatic, Long endTick, long now) {
        if (loaded) return LOADED;
        if (endTick != null && endTick > 0) return endTick > now ? WAIT : COMPLETE;
        return automatic ? START : IDLE;
    }

    /** 已经装好的弹不退膛；没有读条的手动炮也不会被切弹操作自动启动。 */
    static boolean shouldRestartAfterSelection(boolean loaded, Long endTick) {
        return !loaded && endTick != null && endTick > 0;
    }
}
