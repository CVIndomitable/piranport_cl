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
 * 深海装甲航母 — 只有高级档，比高级航母血更多、甲更厚。
 * HP:200 Armor:24 Speed:0.18 Detection:128 Orbit:40
 * 舰载机 1 战 2 轰 1 雷（甲板容量 4），起飞间隔 160 tick。
 *
 * <p>依据《深海/01》§二、§六：只在妄想舰队、地狱群岛使用，不进主线 30 关。
 * 模型未制作前用高级航母的 Alex 占位渲染。</p>
 */
public class DeepOceanArmoredCarrierEntity extends AbstractDeepOceanEntity {

    public DeepOceanArmoredCarrierEntity(EntityType<? extends DeepOceanArmoredCarrierEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.FOLLOW_RANGE, 128.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 24.0);
    }

    @Override
    protected void registerGoals() {
        // 与高级航母同构：领舰环绕/僚舰跟队，空袭为主，保持远距。
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.09));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.09));
        this.goalSelector.addGoal(2, new AircraftLaunchGoal(this));
        // 装甲航母同样属于大型船，补最低档防空。
        this.addMinimalAntiAircraftGoal();
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.6, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        // 《深海/01》§七：航母优先锁定大型船，没有大船时回落到最近玩家。
        this.targetSelector.addGoal(0, new LargeShipPriorityTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 40.0;
    }

    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 4; }
    @Override public int getAircraftLaunchInterval() { return 160; }
}
