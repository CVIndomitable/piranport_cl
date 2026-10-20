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
 * <p>依据《深海/01》§五、§六：高级轻巡为火炮防空（对编队 6 / 80 tick），
 * 防驱为导弹防空（对编队 8 或 10 / 100 tick）。伤害与间隔由实体 JSON 的 aa_damage、aa_fire_interval 给出。</p>
 *
 * <p>{@link AircraftEntity} 不是 {@code LivingEntity}，不能写入原版 target，
 * 因此目标放在 {@link AbstractDeepOceanEntity#getAirTarget()} 的独立字段里。</p>
 */
public class AntiAircraftAttackGoal extends Goal {

    /** 无目标时的重新扫描间隔（tick）。 */
    private static final int SCAN_INTERVAL = 20;
    /** 对空弹初速（格/tick）。 */
    private static final float PROJECTILE_SPEED = 1.8f;
    /** 对空弹散布（度）；防空炮允许一定误差。 */
    private static final float PROJECTILE_INACCURACY = 0.75f;

    private final AbstractDeepOceanEntity mob;
    private final float damage;
    private final int fireInterval;
    private int nextScanTick;
    private int fireCooldown;

    public AntiAircraftAttackGoal(AbstractDeepOceanEntity mob, float damage, int fireInterval) {
        this.mob = mob;
        this.damage = damage;
        this.fireInterval = fireInterval;
        // 不占用 MOVE/LOOK：防空与对海炮击、环绕走位可以同时进行。
        setFlags(EnumSet.noneOf(Flag.class));
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
     */
    private boolean isValidAirTarget(@Nullable AircraftEntity target) {
        if (target == null || !target.isAlive() || target.isRemoved()) return false;
        UUID ownerUuid = target.getOwnerUUID();
        if (ownerUuid == null) return false;
        Player owner = target.getOwner();
        if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.isCreative()) return false;
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return mob.distanceToSqr(target) <= range * range;
    }

    @Nullable
    private AircraftEntity findNearestHostileAircraft() {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
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
        double flightTicks = eye.distanceTo(targetCenter) / PROJECTILE_SPEED;

        // 提前量：按目标当前速度外推；再按炮弹重力抬高落点，
        // 越过顶点后仍由 DeepOceanProjectileEntity 的 PARABOLIC_TRACKING 修正。
        double gravity = ModEquipmentConfig.DEEP_OCEAN_PROJECTILE_GRAVITY.get();
        double gravityDrop = 0.5 * gravity * flightTicks * flightTicks;
        Vec3 aim = targetCenter.add(target.getDeltaMovement().scale(flightTicks))
                .add(0, gravityDrop, 0)
                .subtract(eye);

        DeepOceanProjectileEntity shell = new DeepOceanProjectileEntity(
                mob.level(), mob, damage, 0.0f,
                DeepOceanProjectileEntity.BallisticType.PARABOLIC_TRACKING);
        shell.setTrackingTarget(target);
        shell.setPos(eye.x, eye.y, eye.z);
        shell.shoot(aim.x, aim.y, aim.z, PROJECTILE_SPEED, PROJECTILE_INACCURACY);
        mob.level().addFreshEntity(shell);
    }
}
