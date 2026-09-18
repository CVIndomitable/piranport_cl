package com.piranport.entity;

import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 金猫猫战列舰（Goldencatcat）——活动关卡打靶练习专属敌人。
 *
 * <p>设计定位（策划决策/副本/18）：高血量耐打 + 低威胁 + 零防空 + 低级装甲，
 * 供玩家长时间练习炮击/雷击/航空打击与弹种切换。</p>
 *
 * <p>本实体不主动攻击玩家，仅正常航行与规避，作为训练靶（TRAINING_TARGET）。</p>
 */
public class GoldencatcatEntity extends AbstractDeepOceanEntity {

    public GoldencatcatEntity(EntityType<? extends GoldencatcatEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0)       // 高级战列血量
                .add(Attributes.MOVEMENT_SPEED, 0.20)    // 正常航速
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)      // 极低火力
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 24.0);            // 低级战列装甲
    }

    @Override
    protected void registerGoals() {
        // 金猫猫是训练靶，不主动威胁玩家
        // 仅保留编队跟随 +  idle 巡逻，不添加任何攻击/索敌目标
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.08));
        this.goalSelector.addGoal(2, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(5, new FleetAlertGoal(this));
        // 不注册 NearestAttackableTargetGoal / HurtByTargetGoal → 不主动追击玩家
    }

    @Override
    public boolean removeWhenFarAway(double distSq) {
        return false; // 副本内不自然消失
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    // 金猫猫被击中时有正常的受击反馈，但不会主动反击
    //  inherits water-walking and sinking death from AbstractDeepOceanEntity
}
