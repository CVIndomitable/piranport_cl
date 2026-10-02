package com.piranport.dungeon.instance;

/**
 * 策划《副本/00》多人缩放纯公式（不碰 MC 类，便于单测）。
 */
public final class DungeonScaling {
    private DungeonScaling() {}

    public static final int MAX_PLAYERS = 4;

    private static int clampPlayers(int players) {
        return Math.max(1, Math.min(MAX_PLAYERS, players));
    }

    /** 波数 = ceil(基础波数 × (1 + 0.2(n−1)))，整数运算避免浮点误差：ceil(base·(4+n)/5)。 */
    public static int waveCount(int baseWaves, int players) {
        int base = Math.max(1, baseWaves);
        int n = clampPlayers(players);
        return (base * (4 + n) + 4) / 5;
    }

    /** 血量倍率 = 1 + 0.5(n−1)。 */
    public static double healthScale(int players) {
        return 1.0 + 0.5 * (clampPlayers(players) - 1);
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

    /** 《经济/01》迷路的运输舰：第二章起每波 10%。 */
    public static final double LOST_TRANSPORT_CHANCE = 0.10;

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
