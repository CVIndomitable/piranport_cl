package com.piranport.npc.ai.goal;

import com.piranport.entity.TorpedoEntity;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fires a fan-spread torpedo salvo at the target when within range.
 */
public class TorpedoAttackGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int cooldown = 0;

    /** Torpedo launch distance (blocks). */
    private static final double TORPEDO_RANGE = 20.0;
    /** Cooldown between salvos (ticks). */
    private static final int SALVO_COOLDOWN = 200; // 10 seconds
    /** Number of torpedoes per salvo. */
    private static final int TORPEDOES_PER_SALVO = 3;
    /** Spread angle per torpedo (degrees). */
    private static final double SPREAD_ANGLE = 8.0;
    /** Torpedo speed. */
    private static final float TORPEDO_SPEED = 0.8f;

    public TorpedoAttackGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!mob.canUseTorpedoes()) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        return mob.distanceTo(target) <= TORPEDO_RANGE;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        fireTorpedoSalvo(target);
        cooldown = SALVO_COOLDOWN;
    }

    private void fireTorpedoSalvo(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        Vec3 aim = target.position().subtract(mob.position()).normalize();
        double baseAngle = Math.atan2(aim.z, aim.x);

        for (int i = 0; i < TORPEDOES_PER_SALVO; i++) {
            double offset = (i - (TORPEDOES_PER_SALVO - 1) / 2.0) * Math.toRadians(SPREAD_ANGLE);
            double angle = baseAngle + offset;

            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), mob.level());
            torpedo.setPos(mob.getX(), mob.getY() + 0.2, mob.getZ());
            torpedo.setOwner(mob);
            torpedo.setDamage((float) mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            torpedo.setDeltaMovement(dx * TORPEDO_SPEED, 0, dz * TORPEDO_SPEED);

            mob.level().addFreshEntity(torpedo);
        }
    }
}
