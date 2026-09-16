package com.piranport.dungeon.instance;

/**
 * 提供副本难度信息的接口，用于舰娘属性归一化。
 * 初期由 StageData 或 DungeonInstance 扩展实现，
 * 具体难度数值和缩放公式后续填充。
 */
public interface DungeonDifficultyProvider {
    /**
     * 返回难度缩放系数。
     * 归一化公式：属性 * difficultyScale
     */
    float getDifficultyScale();
}
