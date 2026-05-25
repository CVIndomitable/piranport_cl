package com.piranport.handler;

/**
 * 玩家持久化数据键名常量集中管理。
 * 使用 player.getPersistentData() 存储的 NBT 标记。
 */
public class PlayerDataHelper {
    /** 玩家是否已收到指南书 */
    public static final String NBT_KEY_RECEIVED_GUIDEBOOK = "piranport:received_guidebook";

    /** 玩家是否已看过燃料引导消息 */
    public static final String NBT_KEY_SEEN_FUEL_GUIDE = "piranport:seen_fuel_guide";

    private PlayerDataHelper() {}
}
