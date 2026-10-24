package com.piranport.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 平海。当前只作为「实体核心·平海」的渲染替身使用：
 * 客户端用玩家状态驱动它，服务端不跑 AI。也可以直接 /summon 出来当展示模型。
 */
public class PinghaiEntity extends PathfinderMob {
    public PinghaiEntity(EntityType<? extends PinghaiEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    protected void registerGoals() {
        // 无 AI：实体核心替身只需要外观；被召唤时保持原地站立
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
