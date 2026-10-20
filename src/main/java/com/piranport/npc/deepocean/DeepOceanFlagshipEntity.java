package com.piranport.npc.deepocean;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 深海旗舰。《深海/01》§六：只有一档，武装与战列舰 AI 相同，数值全部来自 JSON（flagship.json）。
 * 单独成类只为拿到独立的实体注册身份，行为继承战列舰。
 */
public class DeepOceanFlagshipEntity extends DeepOceanBattleshipEntity {

    public DeepOceanFlagshipEntity(EntityType<? extends DeepOceanFlagshipEntity> type, Level level) {
        super(type, level);
    }
}
