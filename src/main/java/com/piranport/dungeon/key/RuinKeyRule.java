package com.piranport.dungeon.key;

/**
 * 书台遗迹首次开箱发放的钥匙（策划决策/副本/17 §二）。
 *
 * <p>第一个打开箱子的玩家没有「已进入过副本」标记 → 1-1 空白钥匙；有标记 → 金猫猫活动钥匙。
 * 之后箱子是普通容器，不再重抽（原版战利品箱语义：loot table 只在首次打开时展开）。
 */
public final class RuinKeyRule {
    private RuinKeyRule() {}

    public static final String FIRST_STAGE = "1-1";
    public static final String EVENT_STAGE = "goldencatcat";

    public static String stageFor(boolean hasEnteredDungeon) {
        return hasEnteredDungeon ? EVENT_STAGE : FIRST_STAGE;
    }
}
