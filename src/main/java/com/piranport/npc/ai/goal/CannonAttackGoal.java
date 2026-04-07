package com.piranport.npc.ai.goal;

import com.piranport.entity.CannonProjectileEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
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

        CannonProjectileEntity shell = new CannonProjectileEntity(
                mob.level(), mob,
                new ItemStack(ModItems.SMALL_HE_SHELL.get()),
                mob.getShellDamage(), true, mob.getExplosionPower());

        // Aim with arc compensation
        Vec3 aim = target.getEyePosition().subtract(mob.getEyePosition());
        double hDist = aim.horizontalDistance();
        double arcY = hDist * 0.05; // parabolic drop compensation
        shell.shoot(aim.x, aim.y + arcY, aim.z, SHELL_SPEED, SHELL_INACCURACY);

        if (tracking) {
            shell.setTracking(target.getId());
        }

        mob.level().addFreshEntity(shell);
    }
}
