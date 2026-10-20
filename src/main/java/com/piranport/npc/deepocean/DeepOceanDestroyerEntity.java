package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.RandomHarassTargetGoal;
import com.piranport.npc.ai.goal.TorpedoAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * 深海驱逐 — Fast, close-range. Small cannons + torpedoes.
 * HP:30 Armor:4 Speed:1.4 Detection:48 Orbit:12
 */
public class DeepOceanDestroyerEntity extends AbstractDeepOceanEntity {

    public DeepOceanDestroyerEntity(EntityType<? extends DeepOceanDestroyerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)  // 1.4x relative
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        // 有编队时领舰环绕/僚舰跟队，无编队时回落单舰环绕
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.14));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.14));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(2, new TorpedoAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        // 驱逐不锁定最近玩家，改为随机骚扰（《深海/01》§七）
        this.targetSelector.addGoal(1, new RandomHarassTargetGoal(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    /** 环绕距离走 JSON（《深海/01》orbit ≈ follow/2）；JSON 缺失时保留原硬编码值。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 12.0;
    }
    @Override public int getFireInterval() { return 100; }
    @Override public float getShellDamage() { return 4.0f; }
    @Override public float getExplosionPower() { return 1.2f; }
    @Override public int getTrackingIntervalMin() { return 3; }
    @Override public int getTrackingIntervalMax() { return 6; }
    @Override public boolean canUseTorpedoes() { return true; }
    @Override public float getTorpedoDamage() { return 8.0f; }
}
