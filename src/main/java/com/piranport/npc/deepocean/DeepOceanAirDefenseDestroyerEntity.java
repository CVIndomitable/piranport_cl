package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.AntiAircraftAttackGoal;
import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海防驱 — 导弹防空驱逐舰。《深海/01》§五：敌方导弹防空层（区域防空伞）。
 *
 * <p>自卫武装是小口径火炮（与驱逐相同），因此对海仍走 {@link CannonAttackGoal}，
 * 不挂鱼雷。低级与高级共用本类，防空伤害、间隔、血量、护甲全部由 JSON 给出。</p>
 */
public class DeepOceanAirDefenseDestroyerEntity extends AbstractDeepOceanEntity {

    public DeepOceanAirDefenseDestroyerEntity(EntityType<? extends DeepOceanAirDefenseDestroyerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        DeepOceanEntityData data = getJsonData();
        float aaDamage = data != null ? data.aaDamage() : 8.0f;
        int aaFireInterval = data != null ? data.aaFireInterval() : 100;

        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.12));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.12));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        // 防驱 = 导弹防空（《深海/01》§五）：GUIDED 全程制导，适合拦截会机动的飞机。
        this.goalSelector.addGoal(2, new AntiAircraftAttackGoal(this, aaDamage, aaFireInterval, true));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    /** 环绕距离走 JSON；JSON 缺失时保留驱逐同档的 12。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 12.0;
    }

    @Override public float getExplosionPower() { return 1.2f; }
    @Override public int getTrackingIntervalMin() { return 3; }
    @Override public int getTrackingIntervalMax() { return 6; }
}
