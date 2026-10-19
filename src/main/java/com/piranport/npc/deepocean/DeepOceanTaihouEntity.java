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
 * Boss 大凤 — 装甲航母。
 * HP:200 Armor:24 Speed:0.18 Detection:128 Orbit:48
 * 舰载机 3 战 2 轰 2 雷（甲板容量 7），起飞间隔 160 tick。
 *
 * <p>TODO 《深海/01》§八「晴空万里」本轮未实现：需要在场时把节点天气改为晴天白天，
 * 并使同编队（含自己）受到的航空炸弹/航空鱼雷伤害降为 20%，离场后恢复原天气与减伤。
 * 天气与伤害减免分属场景状态与伤害事件两条链路，待副本天气系统接线后再补。</p>
 *
 * <p>模型未制作前用所属舰种的 Alex 占位渲染。</p>
 */
public class DeepOceanTaihouEntity extends AbstractDeepOceanEntity {

    public DeepOceanTaihouEntity(EntityType<? extends DeepOceanTaihouEntity> type, Level level) {
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

    @Override public boolean canLaunchAircraft() { return true; }
    @Override public int getMaxAircraft() { return 7; }
    @Override public int getAircraftLaunchInterval() { return 160; }
}
