package com.piranport.npc.ai.goal;

import com.piranport.entity.TorpedoEntity;
import com.piranport.npc.ai.NpcCombatTuning;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 雷巡专用鱼雷 Goal：两组四联 610 各自独立装填、独立发射。
 *
 * <p>WHY 不复用 {@link TorpedoAttackGoal}：通用 Goal 只有一条冷却，且每次齐射枚数
 * 走全局 {@code torpedo_salvo.count}。雷巡的设计是「两组四联，各自 1800 tick，
 * 生成时两管都已装填」；两管可以同一 tick 一起打，也可以错开，因此这里用两个
 * 互不干扰的冷却组。</p>
 *
 * <p>依据《深海/01》§六：610 鱼雷伤害 25、航程 40，高低级同值；不套水面舰
 * 「先瞄准 2400 tick」的规则，接敌即可发射。速度/散布走 {@link NpcCombatTuning}
 * （终端可调），但组内枚数固定为 4、组冷却固定为 1800，与策划表一致。</p>
 */
public class TorpedoCruiserAttackGoal extends Goal {

    /** 每组鱼雷管数（四联装）。 */
    private static final int TORPEDOES_PER_GROUP = 4;
    /** 每组装填时间（tick），两管分开计算。 */
    private static final int GROUP_COOLDOWN_TICKS = 1800;
    /** 610 鱼雷航程（格）。 */
    private static final double TORPEDO_RANGE = 40.0;

    private final AbstractDeepOceanEntity mob;
    /** 两组的剩余装填 tick；生成即 0，接敌即可发射。 */
    private final int[] cooldowns = new int[2];

    public TorpedoCruiserAttackGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!mob.canUseTorpedoes()) return false;
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;
        // 鱼雷索敌单独定（40 格），不复用 follow_range。
        return mob.distanceTo(target) <= mob.getTorpedoDetectionRange();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        // 两组独立倒计时：同一 tick 都归零时两组一起发射，允许 8 雷齐出。
        for (int group = 0; group < cooldowns.length; group++) {
            if (cooldowns[group] > 0) {
                cooldowns[group]--;
                continue;
            }
            fireSalvo(target);
            cooldowns[group] = GROUP_COOLDOWN_TICKS;
        }
    }

    private void fireSalvo(LivingEntity target) {
        if (mob.level().isClientSide()) return;

        Vec3 aim = target.position().subtract(mob.position()).normalize();
        double baseAngle = Math.atan2(aim.z, aim.x);
        float speed = NpcCombatTuning.torpedoSalvoSpeed();
        // 航程按「寿命 × 航速」反推：speed 随终端调整时仍保持约 40 格。
        int lifetime = Math.max(1, (int) Math.ceil(TORPEDO_RANGE / Math.max(speed, 0.01f)));

        for (int i = 0; i < TORPEDOES_PER_GROUP; i++) {
            double offset = (i - (TORPEDOES_PER_GROUP - 1) / 2.0)
                    * Math.toRadians(NpcCombatTuning.spreadAngle());
            double angle = baseAngle + offset;

            // 610 口径走重雷构造器，保证口径显示与爆炸半径正确；随后按 JSON 覆盖伤害/航程。
            TorpedoEntity torpedo = new TorpedoEntity(mob.level(), mob, 610);
            torpedo.setPos(mob.getX(), mob.getY() + 0.2, mob.getZ());
            torpedo.setOwner(mob);
            torpedo.setDamage(mob.getTorpedoDamage());
            torpedo.setLifetime(lifetime);
            // 鱼雷运动 AI 用 torpedoSpeed 字段巡航；只设 deltaMovement 会在下一 tick 被 0.3 覆盖。
            torpedo.setSpeed(speed);

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            torpedo.setDeltaMovement(dx * speed, 0, dz * speed);

            // 直航雷：不调用 setAcoustic/setMagnetic/setWireGuided，交给物理直航。
            mob.level().addFreshEntity(torpedo);
        }
    }
}
