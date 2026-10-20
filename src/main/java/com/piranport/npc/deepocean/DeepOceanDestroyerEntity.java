package com.piranport.npc.deepocean;

import com.piranport.npc.ai.goal.CannonAttackGoal;
import com.piranport.npc.ai.goal.FleetAlertGoal;
import com.piranport.npc.ai.goal.OrbitTargetGoal;
import com.piranport.npc.ai.goal.FollowLeaderGoal;
import com.piranport.npc.ai.goal.IdleWanderGoal;
import com.piranport.npc.ai.goal.TorpedoAttackGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 深海驱逐 — Fast, close-range. Small cannons + torpedoes.
 * HP:30 Armor:4 Speed:1.4 Detection:24 Orbit:12
 */
public class DeepOceanDestroyerEntity extends AbstractDeepOceanEntity {

    /** 随机骚扰间隔（tick）；《深海/01》§七：驱逐随机骚扰，不锁定单一目标。 */
    private int harassCooldown = 40;

    public DeepOceanDestroyerEntity(EntityType<? extends DeepOceanDestroyerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)  // 1.4x relative
                .add(Attributes.FOLLOW_RANGE, 24.0)
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
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, AbstractDeepOceanEntity.class));
    }

    /**
     * 驱逐随机骚扰：每 60~120 tick 从附近玩家中随机挑一个（优先换掉当前目标）。
     * 不覆盖 {@code targetSelector} 的最近目标兜底；没有玩家时保持原行为。
     */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (level().isClientSide()) return;
        if (--harassCooldown > 0) return;
        harassCooldown = 60 + getRandom().nextInt(61);

        double range = getAttributeValue(Attributes.FOLLOW_RANGE);
        List<Player> candidates = level().getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(range),
                player -> player.isAlive() && !player.isSpectator() && !player.isCreative()
                        && canAttack(player));
        if (candidates.isEmpty()) return;

        // 有多个候选时排除当前目标，确保随机骚扰会真正换人而不是反复锁同一艘。
        if (getTarget() instanceof Player current && candidates.size() > 1) {
            candidates.remove(current);
        }
        setTarget(candidates.get(getRandom().nextInt(candidates.size())));
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
