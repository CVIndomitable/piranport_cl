package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.TorpedoCruiserAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 深海雷巡（高级）— 单装主炮 + 两组四联 610 鱼雷。
 * HP:50 Armor:6 Speed:0.25 Detection:64 Orbit:24
 *
 * <p>依据《深海/01》§六：主炮 320 tick、单发 6；两组四联 610 各自装填 1800 tick，
 * 伤害 25、航程 40，生成时两管都已装填。鱼雷不套水面舰「先瞄准 2400 tick」，
 * 两管分开计算，因此由 {@link TorpedoCruiserAttackGoal} 接管而非通用
 * {@code TorpedoAttackGoal}。</p>
 *
 * <p>主炮装填/伤害/鱼雷伤害/索敌距离刻意不覆写，统一由基类读取
 * {@code data/piranport/npc/deep_ocean/torpedo_cruiser.json}。</p>
 */
public class DeepOceanTorpedoCruiserEntity extends AbstractDeepOceanEntity {

    public DeepOceanTorpedoCruiserEntity(EntityType<? extends DeepOceanTorpedoCruiserEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowLeaderGoal(this, 0.10));
        this.goalSelector.addGoal(1, new OrbitTargetGoal(this, 0.10));
        this.goalSelector.addGoal(2, new CannonAttackGoal(this));
        // 两组鱼雷按独立冷却发射，取代通用 TorpedoAttackGoal。
        this.goalSelector.addGoal(2, new TorpedoCruiserAttackGoal(this));
        this.goalSelector.addGoal(5, new IdleWanderGoal(this, 0.8, 32));
        this.goalSelector.addGoal(4, new FleetAlertGoal(this));
        // 雷巡不是大型舰，索敌用最近玩家即可。
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    @Override
    public double getOrbitDistance() {
        DeepOceanEntityData data = getJsonData();
        return data != null ? data.orbitDistance() : 24.0;
    }
}
