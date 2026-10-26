package com.piranport.npc.ai.goal;

import com.piranport.config.ModEquipmentConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.DeepOceanProjectileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.UUID;

/**
 * 敌方防空层：定期扫描本舰索敌半径内的玩家飞机，以固定间隔发射对空弹。
 *
 * <p>依据《深海/01》§五、§六：高级轻巡为火炮防空（对编队 6 / 80 tick，直射 + 近炸），
 * 防驱为导弹防空（对编队 8 或 10 / 100 tick，全程制导）。伤害与间隔由实体 JSON 的
 * aa_damage、aa_fire_interval 给出，本 Goal 不改这两个数值。</p>
 *
 * <p>{@link AircraftEntity} 不是 {@code LivingEntity}，不能写入原版 target，
 * 因此目标放在 {@link AbstractDeepOceanEntity#getAirTarget()} 的独立字段里。</p>
 */
public class AntiAircraftAttackGoal extends Goal {

    /** 无目标时的重新扫描间隔（tick）。 */
    private static final int SCAN_INTERVAL = 20;
    /** 弹丸每 tick 的速度衰减系数（与 ThrowableProjectile 的空气阻力一致）。 */
    private static final double DRAG = 0.99;
    /** 提前量迭代次数。5 次足以把解算误差收敛到 0.01 tick 以内。 */
    private static final int LEAD_ITERATIONS = 5;

    private final AbstractDeepOceanEntity mob;
    private final float damage;
    private final int fireInterval;
    /** 防驱 = 导弹（GUIDED，全程比例导引）；高级轻巡 = 火炮（DIRECT 直射 + 近炸）。 */
    private final boolean guided;
    /**
     * 对空索敌距离（格）；&lt;= 0 时沿用 {@link Attributes#FOLLOW_RANGE}。
     *
     * <p>专职防空舰用 FOLLOW_RANGE（区域防空伞）；大型船的保底防空传一个更短的自卫距离，
     * 否则航母/Boss 的 128 格对海索敌会变成全图对空。</p>
     */
    private final double range;
    private int nextScanTick;
    private int fireCooldown;

    public AntiAircraftAttackGoal(AbstractDeepOceanEntity mob, float damage, int fireInterval, boolean guided) {
        this(mob, damage, fireInterval, guided, -1.0);
    }

    public AntiAircraftAttackGoal(AbstractDeepOceanEntity mob, float damage, int fireInterval,
                                  boolean guided, double range) {
        this.mob = mob;
        this.damage = damage;
        this.fireInterval = fireInterval;
        this.guided = guided;
        this.range = range;
        // 不占用 MOVE/LOOK：防空与对海炮击、环绕走位可以同时进行。
        setFlags(EnumSet.noneOf(Flag.class));
    }

    /** 实际对空索敌距离：显式传入优先，否则回退 FOLLOW_RANGE。 */
    private double aaRange() {
        return range > 0.0 ? range : mob.getAttributeValue(Attributes.FOLLOW_RANGE);
    }

    @Override
    public boolean canUse() {
        if (mob.level().isClientSide()) return false;
        AircraftEntity current = mob.getAirTarget();
        if (isValidAirTarget(current)) return true;
        if (mob.tickCount < nextScanTick) return false;
        nextScanTick = mob.tickCount + SCAN_INTERVAL;

        AircraftEntity target = findNearestHostileAircraft();
        mob.setAirTarget(target);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return isValidAirTarget(mob.getAirTarget());
    }

    @Override
    public void stop() {
        mob.setAirTarget(null);
    }

    @Override
    public void tick() {
        AircraftEntity target = mob.getAirTarget();
        if (!isValidAirTarget(target)) {
            mob.setAirTarget(null);
            return;
        }
        if (fireCooldown > 0) {
            fireCooldown--;
            return;
        }
        fireAt(target);
        fireCooldown = fireInterval;
    }

    /**
     * 敌对飞机判定：必须有玩家主人。
     *
     * <p>深海航母放飞的自主飞机 {@code AircraftEntity.createAutonomous} 不写 ownerUUID，
     * 因此这里能天然排除友军飞机；玩家飞机在 {@code AircraftEntity.create} 中写入主人 UUID。</p>
     *
     * <p>WHY 不再排除创造模式主人：防空打的是「飞机实体」，不是玩家本体。玩家在创造模式
     * 起飞测试时飞机照样是可被击落的实体；旧实现排除创造模式主人，会让创造模式下的
     * 防空测试永远不触发。玩家本体受不受创造模式保护与此无关。</p>
     */
    private boolean isValidAirTarget(@Nullable AircraftEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) return false;
        UUID ownerUuid = target.getOwnerUUID();
        if (ownerUuid == null) return false;
        Player owner = target.getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator()) return false;
        double range = aaRange();
        return mob.distanceToSqr(target) <= range * range;
    }

    @Nullable
    private AircraftEntity findNearestHostileAircraft() {
        double range = aaRange();
        double bestDistance = range * range;
        AircraftEntity best = null;
        for (AircraftEntity aircraft : mob.level().getEntitiesOfClass(AircraftEntity.class,
                mob.getBoundingBox().inflate(range), this::isValidAirTarget)) {
            double distance = mob.distanceToSqr(aircraft);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = aircraft;
            }
        }
        return best;
    }

    private void fireAt(AircraftEntity target) {
        if (mob.level().isClientSide()) return;

        Vec3 eye = mob.getEyePosition();
        Vec3 targetCenter = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 targetVelocity = target.getDeltaMovement();

        double speed = guided
                ? ModEquipmentConfig.AA_GUIDED_SHELL_SPEED.get()
                : ModEquipmentConfig.AA_GUN_SHELL_SPEED.get();
        float inaccuracy = (float) (double) (guided
                ? ModEquipmentConfig.AA_GUIDED_INACCURACY.get()
                : ModEquipmentConfig.AA_GUN_INACCURACY.get());

        // 提前量：把目标外推到弹丸抵达时刻。DIRECT/GUIDED 都不吃重力，因此只需匹配
        // 「沿直线飞过的路程」，不需要抛物线抬升。
        double flightTicks = solveLeadTicks(eye, targetCenter, targetVelocity, speed);
        Vec3 aimPoint = targetCenter.add(targetVelocity.scale(flightTicks));
        Vec3 aim = aimPoint.subtract(eye);
        if (aim.lengthSqr() < 1.0E-6) {
            // 目标与炮口几乎重合：给一个朝上的兜底方向，避免 normalize 出 NaN。
            aim = new Vec3(0.0, 1.0, 0.0);
        }

        DeepOceanProjectileEntity.BallisticType ballistic = guided
                ? DeepOceanProjectileEntity.BallisticType.GUIDED
                : DeepOceanProjectileEntity.BallisticType.DIRECT;
        DeepOceanProjectileEntity shell = new DeepOceanProjectileEntity(
                mob.level(), mob, damage, 0.0f, ballistic);
        // DIRECT 也记录目标：近炸结算时优先打这个目标，而不是范围内随便一架飞机。
        shell.setTrackingTarget(target);
        // 近炸半径复用深海炮弹参数（默认 3.0）；防空弹爆炸威力为 0，杀伤完全走近炸直接伤害。
        shell.setProximityFuse(true,
                ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_PROXIMITY_RANGE.get(), damage);
        shell.setPos(eye.x, eye.y, eye.z);
        shell.shoot(aim.x, aim.y, aim.z, (float) speed, inaccuracy);
        mob.level().addFreshEntity(shell);
    }

    /**
     * 解算飞行 tick 数：弹丸每 tick 先位移、再把速度乘 0.99，n tick 内沿直线飞过的路程为
     * <pre>pathLength(n) = speed * (1 - 0.99^n) / 0.01</pre>
     *
     * <p>WHY 不能再用「直线距离 / 初速」：那等于忽略电阻，远距离会系统性少算飞行时间，
     * 提前量一直偏小；目标越高、水平越远，脱靶越明显。这里先按静止目标估飞行时间，
     * 再用目标外推点反复迭代到不动点。</p>
     */
    private static double solveLeadTicks(Vec3 eye, Vec3 targetCenter, Vec3 targetVelocity, double speed) {
        double maxTicks = ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_LIFETIME_TICKS.get();
        double ticks = 0.0;
        for (int i = 0; i < LEAD_ITERATIONS; i++) {
            Vec3 predicted = targetCenter.add(targetVelocity.scale(ticks));
            ticks = flightTicksForDistance(predicted.distanceTo(eye), speed, maxTicks);
            if (!Double.isFinite(ticks)) ticks = 0.0;
        }
        return Math.min(Math.max(ticks, 1.0), maxTicks);
    }

    /** 由 {@code pathLength(n)} 反解 n；超出寿命内总射程时返回 {@code maxTicks}。 */
    private static double flightTicksForDistance(double distance, double speed, double maxTicks) {
        if (distance <= 0.0 || speed <= 0.0) return 0.0;
        double ratio = distance * (1.0 - DRAG) / speed; // = 1 - 0.99^n
        if (ratio >= 1.0) return maxTicks;              // 该初速在寿命内飞不到这个距离
        return Math.min(Math.max(Math.log(1.0 - ratio) / Math.log(DRAG), 0.0), maxTicks);
    }
}
