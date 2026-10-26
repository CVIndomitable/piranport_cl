package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海战巡 — Large cannons, high tracking frequency, no torpedoes.
 * HP:100 Armor:16 Speed:1.1 Detection:40 Orbit:24
 */
public class DeepOceanBattleCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanBattleCruiserEntity(EntityType<? extends DeepOceanBattleCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)

                .add(Attributes.MOVEMENT_SPEED, 0.275)
                .add(Attributes.FOLLOW_RANGE, 80.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ARMOR, 12.0);
    }

    @Override
    protected void registerGoals() {
        // 有编队时领舰环绕/僚舰跟队，无编队时回落单舰环绕
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.11));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.11));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        // 大型船保底防空（高级档复用本类，低/高级各由自己的 JSON 给数值）。
        this.addMinimalAntiAircraftGoal();
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.7, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    /** 环绕距离走 JSON（《深海/01》orbit ≈ follow/2）；JSON 缺失时保留原硬编码值。 */
    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 24.0;
    }
    @Override public float getExplosionPower() { return 2.5f; }
    @Override public int getTrackingIntervalMin() { return 2; } // More frequent tracking
    @Override public int getTrackingIntervalMax() { return 4; }
}
