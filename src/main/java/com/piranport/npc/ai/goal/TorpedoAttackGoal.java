package com.piranport.npc.ai.goal;

import com.piranport.entity.TorpedoEntity;
import com.piranport.npc.ai.NpcCombatTuning;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Fires a fan-spread torpedo salvo at the target when within range.
 *
 * <p>手感数值统一走 {@link NpcCombatTuning}（终端可调），不再本地写死。
 */
public class TorpedoAttackGoal extends Goal {

    private final AbstractDeepOceanEntity mob;
    private int cooldown = 0;

    public TorpedoAttackGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!mob.canUseTorpedoes()) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        return mob.distanceTo(target) <= NpcCombatTuning.torpedoRange();
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
        cooldown = NpcCombatTuning.salvoCooldown();
    }

    private void fireTorpedoSalvo(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        Vec3 aim = target.position().subtract(mob.position()).normalize();
        double baseAngle = Math.atan2(aim.z, aim.x);
        int count = NpcCombatTuning.torpedoesPerSalvo();
        float speed = NpcCombatTuning.torpedoSalvoSpeed();

        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0) * Math.toRadians(NpcCombatTuning.spreadAngle());
            double angle = baseAngle + offset;

            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), mob.level());
            torpedo.setPos(mob.getX(), mob.getY() + 0.2, mob.getZ());
            torpedo.setOwner(mob);
            torpedo.setDamage((float) mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            torpedo.setDeltaMovement(dx * speed, 0, dz * speed);

            mob.level().addFreshEntity(torpedo);
        }
    }
}
