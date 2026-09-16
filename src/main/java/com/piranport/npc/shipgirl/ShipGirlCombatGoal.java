package com.piranport.npc.shipgirl;

import com.piranport.entity.DeepOceanProjectileEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 舰娘随从战斗 AI：炮击 + 雷击。
 * 参考 CannonAttackGoal（抛物线追踪弹）和 TorpedoAttackGoal（鱼雷齐射）。
 */
public class ShipGirlCombatGoal extends Goal {
    private final ShipGirlEntity shipGirl;
    private int fireCooldown = 0;
    private int shotsFired = 0;
    private int nextTrackingShotAt;

    private static final float SHELL_SPEED = 1.5f;
    private static final float SHELL_INACCURACY = 2.0f;
    private static final double TORPEDO_RANGE = 20.0;
    private static final int SALVO_COOLDOWN = 200;
    private static final int TORPEDOES_PER_SALVO = 3;
    private static final double SPREAD_ANGLE = 8.0;
    private static final float TORPEDO_SPEED = 0.8f;
    private static final int MIN_TRACKING_INTERVAL = 3;
    private static final int MAX_TRACKING_INTERVAL = 6;

    public ShipGirlCombatGoal(ShipGirlEntity shipGirl) {
        this.shipGirl = shipGirl;
        int span = Math.max(MAX_TRACKING_INTERVAL - MIN_TRACKING_INTERVAL + 1, 1);
        this.nextTrackingShotAt = MIN_TRACKING_INTERVAL + shipGirl.getRandom().nextInt(span);
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = shipGirl.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = shipGirl.distanceTo(target);
        return dist <= shipGirl.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (fireCooldown > 0) {
            fireCooldown--;
            return;
        }

        LivingEntity target = shipGirl.getTarget();
        if (target == null || !target.isAlive()) return;

        fireCannon(target);
        fireCooldown = 80;

        // 雷击冷却独立计算
        if (shipGirl.distanceTo(target) <= TORPEDO_RANGE && shipGirl.getRandom().nextInt(200) == 0) {
            fireTorpedoSalvo(target);
        }
    }

    private void fireCannon(LivingEntity target) {
        if (shipGirl.level().isClientSide()) return;

        shotsFired++;
        boolean tracking = (shotsFired >= nextTrackingShotAt);
        if (tracking) {
            int span = Math.max(MAX_TRACKING_INTERVAL - MIN_TRACKING_INTERVAL + 1, 1);
            shotsFired = 0;
            nextTrackingShotAt = MIN_TRACKING_INTERVAL + shipGirl.getRandom().nextInt(span);
        }

        DeepOceanProjectileEntity.BallisticType ballistic = tracking
                ? DeepOceanProjectileEntity.BallisticType.PARABOLIC_TRACKING
                : DeepOceanProjectileEntity.BallisticType.PARABOLIC;

        // 伤害受归一化属性影响：取 entity 属性值，若未归一化则回退到默认
        float damage = (float) shipGirl.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        DeepOceanProjectileEntity shell = new DeepOceanProjectileEntity(
                shipGirl.level(), shipGirl, damage, 1.5f, ballistic);

        if (tracking) {
            shell.setTrackingTarget(target);
        }

        Vec3 aim = target.getEyePosition().subtract(shipGirl.getEyePosition());
        double hDist = aim.horizontalDistance();
        double arcY = hDist * 0.05;
        shell.shoot(aim.x, aim.y + arcY, aim.z, SHELL_SPEED, SHELL_INACCURACY);

        shipGirl.level().addFreshEntity(shell);
    }

    private void fireTorpedoSalvo(LivingEntity target) {
        if (shipGirl.level().isClientSide()) return;

        Vec3 aim = target.position().subtract(shipGirl.position()).normalize();
        double baseAngle = Math.atan2(aim.z, aim.x);

        for (int i = 0; i < TORPEDOES_PER_SALVO; i++) {
            double offset = (i - (TORPEDOES_PER_SALVO - 1) / 2.0) * Math.toRadians(SPREAD_ANGLE);
            double angle = baseAngle + offset;

            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), shipGirl.level());
            torpedo.setPos(shipGirl.getX(), shipGirl.getY() + 0.2, shipGirl.getZ());
            torpedo.setOwner(shipGirl);
            torpedo.setDamage((float) shipGirl.getAttributeValue(
                    net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            torpedo.setDeltaMovement(dx * TORPEDO_SPEED, 0, dz * TORPEDO_SPEED);

            shipGirl.level().addFreshEntity(torpedo);
        }
    }
}
