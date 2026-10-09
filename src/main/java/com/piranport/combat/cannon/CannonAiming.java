package com.piranport.combat.cannon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.piranport.combat.BallisticSolver;
import net.neoforged.neoforge.network.PacketDistributor;
import static com.piranport.combat.cannon.CannonStats.getMaxElevationRadians;
import static com.piranport.combat.cannon.CannonStats.getProjectileGravity;
import static com.piranport.combat.cannon.CannonStats.getProjectileDrag;

/** 火炮瞄准：炮口变换、弹道解算和目标方向。 */
final class CannonAiming {
    private CannonAiming() {}

    /** 所有有目标点的指令共用弹道解算，开闭镜只在发射层影响散布。 */
    static Vec3 resolveDirection(Player player, ItemStack weapon, float velocity,
            CannonAim aim, Vec3 aimOrigin, Vec3 spawnPos) {
        if (aim instanceof CannonAim.Aimed aimed) {
            return computeAimDirection(player, weapon, velocity, aimed.target(), aimOrigin);
        }
        if (aim instanceof CannonAim.DirectAim direct) {
            return computeAimDirection(player, weapon, velocity, direct.target(), aimOrigin);
        }
        if (aim instanceof CannonAim.MaxRange) {
            float gravity = getProjectileGravity(weapon, player.level());
            double mcGravity = gravity > 0f ? gravity / 196.0 : BallisticSolver.DEFAULT_GRAVITY;
            // 「最大射程射击」现在按终端参数「最大射程」取落点，而不是物理最大射程角：
            // 参数一旦调小，最大射程模式也跟着收紧（截断语义），不再拉满仰角把弹丸打过上限。
            // 参数超过物理射程时 truncatedAngle 内部自然回退为物理最大射程角，与旧行为一致。
            double configuredRange = com.piranport.config.ModArtilleryConfig.ARTILLERY_MAX_RANGE.get();
            double pitch = BallisticSolver.truncatedAngle(velocity,
                    getProjectileDrag(weapon, player.level()), mcGravity, configuredRange,
                    BallisticSolver.UNRESTRICTED_MIN_ANGLE, getMaxElevationRadians(weapon, player.level()));
            double yaw = Math.toRadians(player.getYRot());
            double cosPitch = Math.cos(pitch);
            return new Vec3(-Math.sin(yaw) * cosPitch, Math.sin(pitch), Math.cos(yaw) * cosPitch).normalize();
        }
        return player.getLookAngle();
    }

    static Vec3 rotateMuzzleByPlayerView(Player player, com.piranport.artillery.config.MuzzlePos muzzle) {
        // 炮口位置定义：x=左右，y=上下，z=前后（相对武器）
        Vec3 localOffset = new Vec3(muzzle.x(), muzzle.y(), muzzle.z());

        // 获取玩家视角方向
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        // 转换为弧度
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // 构建旋转矩阵（先俯仰后偏航）
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        // 应用旋转变换
        double x = localOffset.x * cosYaw - localOffset.z * sinYaw;
        double y = localOffset.y * cosPitch + localOffset.z * sinPitch;
        double z = localOffset.x * sinYaw + localOffset.z * cosYaw * cosPitch;

        return new Vec3(x, y, z);
    }

    static Vec3 computeAimDirection(Player player, ItemStack weapon,
                                             float velocity, Vec3 aimTarget, Vec3 muzzlePos) {
        Vec3 toTarget = aimTarget.subtract(muzzlePos);
        double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double verticalDist = toTarget.y;

        if (horizontalDist < 1.0) {
            return player.getLookAngle();
        }

        float drag = getProjectileDrag(weapon, player.level());
        float gravity = getProjectileGravity(weapon, player.level());
        double mcGravity = gravity > 0f ? gravity / 196.0 : BallisticSolver.DEFAULT_GRAVITY;
        double maxElevation = getMaxElevationRadians(weapon, player.level());

        // 射程截断（2026-10-09 项目所有者）：目标水平距离超过终端「最大射程」时，不再对目标解算
        // （目标不可达，解算没有意义），改把落点钉在最大射程点（沿目标方向、与炮口同高）求解，
        // 并向玩家提示「超过射程」。WHY 不是最大仰角：最大射程角会把弹丸送到物理射程上限，
        // 越过策划设定的截断线，与「截断」语义正好相反。
        double configuredRange = com.piranport.config.ModArtilleryConfig.ARTILLERY_MAX_RANGE.get();
        double optimalPitch;
        if (BallisticSolver.beyondMaxRange(horizontalDist, configuredRange)) {
            optimalPitch = BallisticSolver.truncatedAngle(velocity, drag, mcGravity, configuredRange,
                    BallisticSolver.UNRESTRICTED_MIN_ANGLE, maxElevation);
            player.displayClientMessage(
                    Component.translatable("hud.piranport.fire_control.out_of_range"), true);
        } else {
            // 经 BallisticDispatcher 分发：实验炮走网络，其余炮走 BallisticSolver。
            // 依据：docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md 4.2
            BallisticSolver.Result result = com.piranport.combat.neural.BallisticDispatcher.solve(
                    weapon, velocity, drag, mcGravity,
                    horizontalDist, verticalDist, 0.0,
                    BallisticSolver.UNRESTRICTED_MIN_ANGLE, maxElevation);
            optimalPitch = result.angle();
            if (result.outOfRange()) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.out_of_range"), true);
            }
        }

        // 将服务端解算统计发送给客户端
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.piranport.combat.BallisticSolverStats stats = com.piranport.combat.BallisticSolverStats.getInstance();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new com.piranport.network.SolverStatsPayload(
                            stats.getLastTernaryIters(),
                            stats.getLastNewtonIters(),
                            stats.getLastTotalUs(),
                            stats.getCombinedVerticalError(),
                            stats.getCombinedHorizontalError(),
                            Math.toDegrees(optimalPitch)));
        }

        // MC 偏航角：atan2(-dx, dz) 映射到 MC 坐标（yaw=0 = +Z）
        double yawRad = Math.atan2(-toTarget.x, toTarget.z);
        double pitchRad = optimalPitch; // 方向向量，向上为正

        double cosP = Math.cos(pitchRad);
        return new Vec3(
                -Math.sin(yawRad) * cosP,
                Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
        ).normalize();
    }

}
