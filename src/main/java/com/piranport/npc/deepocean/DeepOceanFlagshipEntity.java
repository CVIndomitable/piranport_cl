package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.AircraftLaunchGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 深海旗舰。《深海/01》§六：只有一档，武装与战列舰 AI 相同，数值全部来自 JSON（flagship.json）。
 * 单独成类只为拿到独立的实体注册身份，行为继承战列舰。
 *
 * <p>旗舰是航空战列舰式辅攻：战斗机 ×1 + 轰炸机 ×1，起飞间隔 240 tick。
 * 放飞 Goal 只挂在旗舰上，普通战列舰仍不出机。</p>
 */
public class DeepOceanFlagshipEntity extends DeepOceanBattleshipEntity {

    public DeepOceanFlagshipEntity(EntityType<? extends DeepOceanFlagshipEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new AircraftLaunchGoal(this));
    }

    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 2; }
    @Override public int getAircraftLaunchInterval() { return 240; }
}
