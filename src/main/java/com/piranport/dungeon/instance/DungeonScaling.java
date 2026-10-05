package com.piranport.dungeon.instance;

import com.piranport.config.ModEquipmentConfig;

/**
 * 策划《副本/00》多人缩放纯公式（不碰 MC 类，便于单测）。
 *
 * <p>手感数值（人数上限、血量/波数每玩家增幅、迷路运输舰概率）走调试终端
 * （global.dungeon_scaling.*，见 {@link ModEquipmentConfig}）。终端参数在无覆盖时回落
 * 到基准值，因此单测默认行为与下沉前一致。</p>
 */
public final class DungeonScaling {
    private DungeonScaling() {}

    private static int clampPlayers(int players) {
        return Math.max(1, Math.min(ModEquipmentConfig.DUNGEON_MAX_PLAYERS.get(), players));
    }

    /**
     * 波数 = ceil(基础波数 × (1 + 每玩家增幅×(n−1)))。
     * 减去极小 epsilon 抵消二进制浮点误差——否则 5 × 1.2 可能得到 6.000000000000001 被 ceil 成 7。
     */
    public static int waveCount(int baseWaves, int players) {
        int base = Math.max(1, baseWaves);
        int n = clampPlayers(players);
        double scaled = base * (1.0 + ModEquipmentConfig.DUNGEON_WAVE_SCALE_PER_PLAYER.get() * (n - 1));
        return (int) Math.ceil(scaled - 1e-9);
    }

    /** 血量倍率 = 1 + 每玩家增幅 × (n−1)。 */
    public static double healthScale(int players) {
        return 1.0 + ModEquipmentConfig.DUNGEON_HEALTH_SCALE_PER_PLAYER.get() * (clampPlayers(players) - 1);
    }

    /** difficulty_scale：节点级（>0）优先，否则关卡级兜底；下限 0.1。 */
    public static double effectiveDifficulty(double nodeScale, double stageScale) {
        double s = nodeScale > 0 ? nodeScale : stageScale;
        if (!(s > 0)) s = 1.0;
        return Math.max(0.1, s);
    }

    /** 从 "chapter_N" 解析章节序号；无法解析（活动/教程章）返回 0。 */
    public static int chapterNumber(String chapterId) {
        if (chapterId == null || !chapterId.startsWith("chapter_")) return 0;
        try {
            return Integer.parseInt(chapterId.substring("chapter_".length()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 《经济/01》迷路的运输舰：第二章起每波替换概率走 global.dungeon_scaling.lost_transport_chance。 */
    public static boolean lostTransportEligible(String chapterId) {
        return chapterNumber(chapterId) >= 2;
    }

    /**
     * 首通参与判定：点是否在节点的 tile 范围内（节点中心 ± size/2，XZ 平面）。
     */
    public static boolean isInsideNodeTile(double x, double z, double centerX, double centerZ, int tileSize) {
        double half = tileSize / 2.0;
        return x >= centerX - half && x < centerX + half
                && z >= centerZ - half && z < centerZ + half;
    }
}
