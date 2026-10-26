package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.AircraftLaunchGoal;
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
 * 深海轻母 — Light carrier, launches 2 fighters.
 * HP:60 Armor:6 Speed:1.0 Detection:48 Orbit:32
 */
public class DeepOceanLightCarrierEntity extends AbstractDeepOceanEntity {

    public DeepOceanLightCarrierEntity(EntityType<? extends DeepOceanLightCarrierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.ARMOR, 10.0);
    }

    @Override
    protected void registerGoals() {
        // 有编队时领舰环绕/僚舰跟队，无编队时回落单舰环绕
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.10));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.10));
        this.goalSelector.addGoal(2, new AircraftLaunchGoal(this));
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
        return data != null ? data.orbitDistance() : 32.0;
    }
    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 2; }
    @Override public int getAircraftLaunchInterval() { return 200; }
}
