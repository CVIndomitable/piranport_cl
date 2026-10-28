package com.piranport.combat.cannon;

/**
 * 装填状态决策；物品读条是唯一计时来源，槽位历史数据不参与推进。
 *
 * <p>automatic 由调用方按炮种传入（{@code ArtilleryItem#isAutoLoading()}）：
 * 自动炮空膛且无读条时返回 START，由服务端 tick 启动读条；手动炮返回 IDLE，等 R 键。
 */
enum CannonReloadPhase {
    LOADED, IDLE, START, WAIT, COMPLETE;

    static CannonReloadPhase resolve(boolean loaded, boolean automatic, Long endTick, long now) {
        if (loaded) return LOADED;
        if (endTick != null && endTick > 0) return endTick > now ? WAIT : COMPLETE;
        // 手动炮的空膛是稳定状态（等 R），自动炮的空膛是待启动状态。
        return automatic ? START : IDLE;
    }

    /** 已经装好的弹不退膛；没有读条的手动炮也不会被切弹操作自动启动。 */
    static boolean shouldRestartAfterSelection(boolean loaded, Long endTick) {
        return !loaded && endTick != null && endTick > 0;
    }
}
