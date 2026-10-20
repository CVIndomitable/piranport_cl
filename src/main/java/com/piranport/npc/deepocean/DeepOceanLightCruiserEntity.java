package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.AntiAircraftAttackGoal;
import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.TorpedoAttackGoal;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海轻巡 — Medium cannons + torpedoes.
 * HP:50 Armor:8 Speed:1.2 Detection:32 Orbit:16
 */
public class DeepOceanLightCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanLightCruiserEntity(EntityType<? extends DeepOceanLightCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.30)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void registerGoals() {
        // 有编队时领舰环绕/僚舰跟队，无编队时回落单舰环绕
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.12));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.12));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        this.goalSelector.addGoal(2, new TorpedoAttackGoal(this));
        // 高级轻巡 = 敌方火炮防空层（《深海/01》§五）：独立对空目标，不占用对海 target。
        // 低级轻巡无防空火炮，不挂此 Goal。
        if (getType() == ModEntityTypes.DEEP_OCEAN_HIGH_LIGHT_CRUISER.get()) {
            this.goalSelector.addGoal(2, new AntiAircraftAttackGoal(this));
        }
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    /** 环绕距离走 JSON（《深海/01》orbit ≈ follow/2）；JSON 缺失时保留原硬编码值。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 16.0;
    }
    @Override public float getExplosionPower() { return 1.5f; }
    @Override public int getTrackingIntervalMin() { return 3; }
    @Override public int getTrackingIntervalMax() { return 6; }
    @Override public boolean canUseTorpedoes() { return true; }
}
