package com.piranport.combat;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

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

    /** 屏幕投影结果（GUI 缩放坐标）。{@code onScreen=false} 表示目标在视野外，坐标已贴边钳制。 */
    public record ScreenPoint(float x, float y, boolean onScreen) {}

    /**
     * 把世界坐标投影到屏幕（GUI 缩放坐标）。
     *
     * <p>预瞄点画在 HUD 而不是世界里，是因为世界空间的图形按透视缩小，远距离落点会糊成一点，
     * 而远距离恰恰最需要落点提示；HUD 标记固定像素尺寸，屏幕位置仍传达"偏左/偏右"。
     *
     * <p>用相机朝向四元数的共轭把世界位移转到视图空间（相机朝 -Z），再过真实投影矩阵。
     * 传入真实投影矩阵而不是自算 FOV，是为了让开镜缩放自动生效。
     *
     * @return 相机背后或 w<=0（投影退化）时返回 {@code null}，调用方应整帧丢弃而不是钳制 ——
     *         相机背后的点投影出来是镜像假点，钳到边缘会指向完全相反的方向。
     */
    public static ScreenPoint projectToScreen(Vec3 world, Vec3 cameraPos, Quaternionf cameraRotation,
                                              Matrix4f projection, int guiWidth, int guiHeight,
                                              int margin) {
        if (!isFinite(world) || !isFinite(cameraPos)) return null;
        Vector3f view = new Vector3f(
                (float) (world.x - cameraPos.x),
                (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z));
        new Quaternionf(cameraRotation).conjugate().transform(view);
        if (view.z >= -1.0e-4f) return null;

        Vector4f clip = new Vector4f(view.x, view.y, view.z, 1.0f);
        projection.transform(clip);
        if (clip.w <= 1.0e-6f) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        boolean onScreen = ndcX >= -1.0f && ndcX <= 1.0f && ndcY >= -1.0f && ndcY <= 1.0f;
        float x = (ndcX * 0.5f + 0.5f) * guiWidth;
        float y = (0.5f - ndcY * 0.5f) * guiHeight;
        int m = Math.max(0, Math.min(margin, Math.min(guiWidth, guiHeight) / 2));
        float clampedX = Math.min(Math.max(x, m), Math.max(m, guiWidth - m));
        float clampedY = Math.min(Math.max(y, m), Math.max(m, guiHeight - m));
        return new ScreenPoint(clampedX, clampedY, onScreen);
    }
}
