package com.piranport.combat;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 火控可视化预测的纯函数（策划决策/火控/06）。
 *
 * <p>只做数学，不碰渲染与 MC 注册表，便于单测：
 * 采样窗口速度、鱼雷水平拦截点、准星是否进入落点球。
 */
public final class FireControlPrediction {
    private FireControlPrediction() {}

    /** 采样窗口长度（tick），文档 §4。 */
    public static final int SAMPLE_WINDOW = 5;
    /** 失步/瞬移时的速度上限（格/tick），避免预测点被甩出地图。 */
    public static final double MAX_SAMPLED_SPEED = 4.0;

    /**
     * 位置环形队列：保留最近 {@link #SAMPLE_WINDOW} 个 tick 的位置，
     * 速度 = (最新 − 最旧) / (样本数 − 1)。
     */
    public static final class PositionSampler {
        private final Deque<Vec3> samples = new ArrayDeque<>(SAMPLE_WINDOW);

        public void push(Vec3 position) {
            if (!isFinite(position)) return;
            if (samples.size() >= SAMPLE_WINDOW) samples.removeFirst();
            samples.addLast(position);
        }

        public void clear() { samples.clear(); }

        public int size() { return samples.size(); }

        /** 样本不足 2 个时返回零向量。 */
        public Vec3 velocity() {
            if (samples.size() < 2) return Vec3.ZERO;
            Vec3 v = samples.peekLast().subtract(samples.peekFirst()).scale(1.0 / (samples.size() - 1));
            if (!isFinite(v)) return Vec3.ZERO;
            if (v.lengthSqr() > MAX_SAMPLED_SPEED * MAX_SAMPLED_SPEED) v = v.normalize().scale(MAX_SAMPLED_SPEED);
            return v;
        }
    }

    /**
     * 鱼雷水平匀速拦截：求最小正 t 使 |P + V·t − O|_xz = s·t。
     *
     * @return 命中点（y 取目标当前高度），无解返回 null
     */
    public static Vec3 torpedoIntercept(Vec3 origin, Vec3 targetPos, Vec3 targetVelocity, double torpedoSpeed) {
        if (!(torpedoSpeed > 1.0e-6) || !isFinite(origin) || !isFinite(targetPos) || !isFinite(targetVelocity)) return null;
        double dx = targetPos.x - origin.x;
        double dz = targetPos.z - origin.z;
        double vx = targetVelocity.x;
        double vz = targetVelocity.z;
        // (vx²+vz²−s²)t² + 2(dx·vx+dz·vz)t + (dx²+dz²) = 0
        double a = vx * vx + vz * vz - torpedoSpeed * torpedoSpeed;
        double b = 2.0 * (dx * vx + dz * vz);
        double c = dx * dx + dz * dz;
        double t;
        if (Math.abs(a) < 1.0e-9) {
            if (Math.abs(b) < 1.0e-12) return c < 1.0e-12 ? targetPos : null;
            t = -c / b;
        } else {
            double disc = b * b - 4.0 * a * c;
            if (disc < 0) return null;
            double sq = Math.sqrt(disc);
            double t1 = (-b - sq) / (2.0 * a);
            double t2 = (-b + sq) / (2.0 * a);
            double lo = Math.min(t1, t2);
            double hi = Math.max(t1, t2);
            t = lo > 0 ? lo : hi;
        }
        if (!(t > 0) || !Double.isFinite(t)) return null;
        return new Vec3(targetPos.x + vx * t, targetPos.y, targetPos.z + vz * t);
    }

    /**
     * 准星射线是否进入落点球：射线（只看前方）到球心的最近距离 &lt; radius。
     */
    public static boolean rayHitsSphere(Vec3 eye, Vec3 lookDir, Vec3 center, double radius) {
        if (!isFinite(eye) || !isFinite(lookDir) || !isFinite(center)) return false;
        double len = lookDir.length();
        if (len < 1.0e-9) return false;
        Vec3 dir = lookDir.scale(1.0 / len);
        Vec3 toCenter = center.subtract(eye);
        double along = toCenter.dot(dir);
        if (along < 0) return toCenter.length() < radius;
        Vec3 closest = eye.add(dir.scale(along));
        return closest.distanceToSqr(center) < radius * radius;
    }

    /** 扇形圆心角（度）= 2 × max|散布角|。 */
    public static double fanAngleDegrees(float[] spreadAngles) {
        if (spreadAngles == null) return 0;
        double max = 0;
        for (float a : spreadAngles) max = Math.max(max, Math.abs(a));
        return max * 2.0;
    }

    private static boolean isFinite(Vec3 v) {
        return v != null && Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }
}
