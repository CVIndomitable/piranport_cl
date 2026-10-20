package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.AircraftLaunchGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.LargeShipPriorityTargetGoal;
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
 * Boss 瑞鹤 — 航母。
 * HP:180 Armor:20 Speed:0.19 Detection:128 Orbit:48
 * 舰载机 2 战 3 轰 3 雷（甲板容量 8），起飞间隔 160 tick。
 *
 * <p>依据《深海/01》§八：瑞鹤与翔鹤同编成、同数值口径。
 * TODO 「同翔鹤」技能本轮未实现：设计要求与翔鹤保持同一套行为（翔鹤本身无特殊技能），
 * 后续若翔鹤补充专属技能，需在此同步接线，避免两舰漂移。</p>
 *
 * <p>模型未制作前用所属舰种的 Alex 占位渲染。</p>
 */
public class DeepOceanZuikakuEntity extends AbstractDeepOceanEntity {

    public DeepOceanZuikakuEntity(EntityType<? extends DeepOceanZuikakuEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.FOLLOW_RANGE, 128.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 20.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.09));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.09));
        this.goalSelector.addGoal(2, new AircraftLaunchGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        this.targetSelector.addGoal(0, new LargeShipPriorityTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 48.0;
    }

    @Override public boolean keepsFixedHealth() { return true; }
    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 8; }
    @Override public int getAircraftLaunchInterval() { return 160; }
}
