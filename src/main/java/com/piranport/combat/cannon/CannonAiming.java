package com.piranport.combat.cannon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import com.piranport.combat.BallisticSolver;
import net.neoforged.neoforge.network.PacketDistributor;
import static com.piranport.combat.cannon.CannonStats.getMinElevationRadians;
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
            double pitch = BallisticSolver.calculateMaxRangeAngle(velocity,
                    getProjectileDrag(weapon, player.level()), mcGravity,
                    getMinElevationRadians(weapon, player.level()), getMaxElevationRadians(weapon, player.level()));
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
        // 经 BallisticDispatcher 分发：实验炮走网络，其余炮走 BallisticSolver。
        // 依据：docs/策划决策/武器/火炮-神经网络弹道解算实验方案.md 4.2
        BallisticSolver.Result result = com.piranport.combat.neural.BallisticDispatcher.solve(
                weapon, velocity, drag, mcGravity,
                horizontalDist, verticalDist, 0.0,
                getMinElevationRadians(weapon, player.level()),
                getMaxElevationRadians(weapon, player.level()));
        double optimalPitch = result.angle();

        if (result.outOfRange()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.out_of_range"), true);
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
