package com.piranport.npc.ai.goal;

import com.piranport.entity.DeepOceanProjectileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fires cannon shells at the target with parabolic arc.
 * Every N shots fires a tracking round.
 */
public class CannonAttackGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int fireCooldown = 0;
    private int shotsFired = 0;
    private int nextTrackingShotAt;

    private static final float SHELL_SPEED = 1.5f;
    private static final float SHELL_INACCURACY = 2.0f;

    public CannonAttackGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        this.nextTrackingShotAt = mob.getTrackingIntervalMin()
                + mob.getRandom().nextInt(mob.getTrackingIntervalMax() - mob.getTrackingIntervalMin() + 1);
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        double dist = mob.distanceTo(target);
        return dist <= mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
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

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        fireAtTarget(target);
        fireCooldown = mob.getFireInterval();
    }

    private void fireAtTarget(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        shotsFired++;
        boolean tracking = (shotsFired >= nextTrackingShotAt);
        if (tracking) {
            nextTrackingShotAt = shotsFired + mob.getTrackingIntervalMin()
                    + mob.getRandom().nextInt(mob.getTrackingIntervalMax() - mob.getTrackingIntervalMin() + 1);
        }

        DeepOceanProjectileEntity.BallisticType ballistic = tracking
                ? DeepOceanProjectileEntity.BallisticType.PARABOLIC_TRACKING
                : DeepOceanProjectileEntity.BallisticType.PARABOLIC;
        DeepOceanProjectileEntity shell = new DeepOceanProjectileEntity(
                mob.level(), mob, mob.getShellDamage(), mob.getExplosionPower(), ballistic);

        if (tracking) {
            shell.setTrackingTarget(target.getId());
        }

        // Aim with arc compensation
        Vec3 aim = target.getEyePosition().subtract(mob.getEyePosition());
        double hDist = aim.horizontalDistance();
        double arcY = hDist * 0.05;
        shell.shoot(aim.x, aim.y + arcY, aim.z, SHELL_SPEED, SHELL_INACCURACY);

        mob.level().addFreshEntity(shell);
    }
}
