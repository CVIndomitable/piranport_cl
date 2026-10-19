package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 深海运输船 — 无武装的箱船，随编队行动或单独漂航。
 * HP:120 Armor:4 Speed:0.15（约 3 米/秒）Detection:32
 *
 * <p>依据《深海/01》§九：运输船没有攻击手段，因此不注册任何 targetSelector，
 * 只保留跟随领舰与闲逛；被攻击时也不会反击。</p>
 */
public class DeepOceanTransportEntity extends AbstractDeepOceanEntity {

    public DeepOceanTransportEntity(EntityType<? extends DeepOceanTransportEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        // 运输船不主动索敌，只跟队与闲逛；不加 targetSelector。
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.10));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
    }

    /** 运输船不参与环绕交战；JSON 缺失时也保持 0，避免误走环绕逻辑。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 0.0;
    }

    @Override public boolean canUseTorpedoes() { return false; }
    @Override public boolean canLaunchAircraft() { return false; }
    @Override public int getMaxAircraft() { return 0; }
}
