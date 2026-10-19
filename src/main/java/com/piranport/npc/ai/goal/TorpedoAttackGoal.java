package com.piranport.npc.ai.goal;

import com.piranport.combat.CombatTargeting;
import com.piranport.entity.TorpedoEntity;
import com.piranport.npc.ai.NpcCombatTuning;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 深海舰船鱼雷攻击，流程依《深海/02》：
 * 持续瞄准 600 tick → 锁定水平航向（按提前点算一次）→ 40 tick 发射准备 → 沿锁定航向齐射。
 *
 * <p>WHY 锁定航向而不是边追边射：目标在准备期间转向或减速可以躲开，
 * 这是玩家的反制空间；直航鱼雷不制导，所以航向必须在锁定那一刻定死。
 *
 * <p>手感数值统一走 {@link NpcCombatTuning}（终端可调），不再本地写死。
 */
public class TorpedoAttackGoal extends Goal {

    private static final double LAUNCH_Y_OFFSET = 0.2;

    private final AbstractDeepOceanEntity mob;
    private int cooldown = 0;

    private LivingEntity aimTarget;
    private int aimElapsed = 0;

    private int prepElapsed = -1;
    private double lockedAngle;

    public TorpedoAttackGoal(AbstractDeepOceanEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.noneOf(Flag.class));
        // 生成初始装填（《深海/01》§六）：潜艇已装填，水面舰要等一轮。
        this.cooldown = mob.startsWithTorpedoesLoaded() ? 0 : mob.getTorpedoCooldownTicks();
    }

    @Override
    public boolean canUse() {
        if (!mob.canUseTorpedoes()) return false;
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return prepElapsed >= 0 || canUse();
    }

    @Override
    public void stop() {
        if (prepElapsed < 0) resetAim();
    }

    @Override
    public void tick() {
        if (prepElapsed >= 0) {
            tickPrep();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            resetAim();
            return;
        }
        if (!canHoldAim(target)) {
            resetAim();
            return;
        }
        if (target != aimTarget) {
            aimTarget = target;
            aimElapsed = 0;
        }
        aimElapsed++;
        if (aimElapsed >= NpcCombatTuning.torpedoAimTicks()) {
            beginPrep(target);
        }
    }

    /** 索敌距离与视线两条都要满足（《深海/02》持续瞄准）；任一断开即计时清零。 */
    private boolean canHoldAim(LivingEntity target) {
        if (mob.distanceTo(target) > mob.getTorpedoDetectionRange()) return false;
        Vec3 from = mob.getEyePosition();
        Vec3 to = CombatTargeting.aimPoint(target);
        BlockHitResult hit = mob.level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return hit.getType() == HitResult.Type.MISS;
    }

    private void resetAim() {
        aimTarget = null;
        aimElapsed = 0;
    }

    private void beginPrep(LivingEntity target) {
        Vec3 targetVel = target.getDeltaMovement();
        lockedAngle = leadAngle(mob.getX(), mob.getZ(), target.getX(), target.getZ(),
                targetVel.x, targetVel.z, NpcCombatTuning.torpedoSalvoSpeed(), NpcCombatTuning.torpedoPrepTicks());
        prepElapsed = 0;
        resetAim();

        if (mob.level() instanceof ServerLevel level) {
            level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                    SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.HOSTILE, 1.0f, 1.0f);
        }
    }

    private void tickPrep() {
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    mob.getX(), mob.getY() + LAUNCH_Y_OFFSET, mob.getZ(),
                    4, 0.3, 0.1, 0.3, 0.02);
        }

        prepElapsed++;
        if (prepElapsed < NpcCombatTuning.torpedoPrepTicks()) return;

        fireTorpedoSalvo(lockedAngle);
        prepElapsed = -1;
        cooldown = mob.getTorpedoCooldownTicks();
    }

    /**
     * 水平锁定航向（弧度）。提前点 = 目标当前位置 + 目标水平速度 × (水平距离 ÷ 鱼雷速度 + 准备时长)，
     * 垂直速度不参与。
     */
    static double leadAngle(double launchX, double launchZ, double targetX, double targetZ,
                            double targetVelX, double targetVelZ, double torpedoSpeed, int prepTicks) {
        double dx = targetX - launchX;
        double dz = targetZ - launchZ;
        double flightTicks = Math.sqrt(dx * dx + dz * dz) / torpedoSpeed + prepTicks;
        double leadX = targetX + targetVelX * flightTicks;
        double leadZ = targetZ + targetVelZ * flightTicks;
        return Math.atan2(leadZ - launchZ, leadX - launchX);
    }

    private void fireTorpedoSalvo(double baseAngle) {
        if (mob.level().isClientSide()) return;

        int count = NpcCombatTuning.torpedoesPerSalvo();
        float speed = NpcCombatTuning.torpedoSalvoSpeed();

        for (int i = 0; i < count; i++) {
            double offset = (i - (count - 1) / 2.0) * Math.toRadians(NpcCombatTuning.spreadAngle());
            double angle = baseAngle + offset;

            TorpedoEntity torpedo = new TorpedoEntity(ModEntityTypes.TORPEDO_ENTITY.get(), mob.level());
            torpedo.setPos(mob.getX(), mob.getY() + LAUNCH_Y_OFFSET, mob.getZ());
            torpedo.setOwner(mob);
            // 鱼雷伤害走按舰种 JSON 配置的 getTorpedoDamage()；ATTACK_DAMAGE 是近战属性（3.0），不是鱼雷伤害
            torpedo.setDamage(mob.getTorpedoDamage());

            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            torpedo.setDeltaMovement(dx * speed, 0, dz * speed);

            mob.level().addFreshEntity(torpedo);
        }
    }
}
