package com.piranport.dungeon.escort;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 运输船的航线：一串路径点加上它们的弧长表，供航行、进度和刷怪锚点共用。
 *
 * <p>航线可变：{@link #currentWaypoint} 推进的是游标，{@link #advanceClock} 记录何时该重新规划。
 * 两者都只在服务端运输船上调用，不做线程安全处理。</p>
 */
public final class EscortRoute {

    static final int REPLAN_INTERVAL_TICKS = 100;
    static final int STALL_TICKS = 60;
    private static final double STALL_DISTANCE = 1.0;
    /** 到达路径点的水平半径；取小于 4 格单元的一半，保证转弯时船身不会切进陆地单元。 */
    private static final double REACH_RADIUS = 1.5;
    private static final double EPS = 1e-6;

    private final List<Vec3> points;
    private final double[] arc;
    private final boolean unreachable;
    private int cursor;
    private int ticksSincePlan;
    private double stallX;
    private double stallZ;
    private int stallTicks;

    private EscortRoute(List<Vec3> points, boolean unreachable) {
        this.points = List.copyOf(points);
        this.arc = new double[points.size()];
        for (int i = 1; i < arc.length; i++) {
            arc[i] = arc[i - 1] + horizontalDistance(points.get(i - 1), points.get(i));
        }
        this.unreachable = unreachable;
        this.stallX = points.get(0).x;
        this.stallZ = points.get(0).z;
    }

    public static EscortRoute planned(List<Vec3> points) {
        return new EscortRoute(points, false);
    }

    /** 无可通行路径时的退路：直线驶向目标，同时标记为不可达以便只报一次。 */
    public static EscortRoute unreachable(Vec3 from, Vec3 to) {
        return new EscortRoute(new ArrayList<>(List.of(from, to)), true);
    }

    /** 直接给定两端的直线航线，不标记为不可达；刷怪锚点在无航线时用它。 */
    public static EscortRoute straight(Vec3 from, Vec3 to) {
        return new EscortRoute(new ArrayList<>(List.of(from, to)), false);
    }

    public boolean unreachable() {
        return unreachable;
    }

    public Vec3 end() {
        return points.get(points.size() - 1);
    }

    public double length() {
        return arc[arc.length - 1];
    }

    /** 船当前应驶向的路径点；船进入某点的 {@link #REACH_RADIUS} 内即推进到下一点，终点保持不动。 */
    public Vec3 currentWaypoint(double x, double z) {
        while (cursor < points.size() - 1 && horizontalDistance(points.get(cursor), x, z) <= REACH_RADIUS) {
            cursor++;
        }
        return points.get(cursor);
    }

    /** 船在航线上的弧长占比，取到航线最近点的投影，结果钳制到 [0, 1]。 */
    public double progress(double x, double z) {
        double total = length();
        if (total < EPS) return 1.0;
        double bestDistance = Double.POSITIVE_INFINITY;
        double bestAlong = 0.0;
        for (int seg = 0; seg < points.size() - 1; seg++) {
            Vec3 a = points.get(seg);
            Vec3 b = points.get(seg + 1);
            double dx = b.x - a.x;
            double dz = b.z - a.z;
            double segLength2 = dx * dx + dz * dz;
            double t = segLength2 < EPS ? 0.0
                    : Math.max(0.0, Math.min(1.0, ((x - a.x) * dx + (z - a.z) * dz) / segLength2));
            double distance = Math.hypot(x - (a.x + dx * t), z - (a.z + dz * t));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestAlong = arc[seg] + t * (arc[seg + 1] - arc[seg]);
            }
        }
        return Math.max(0.0, Math.min(1.0, bestAlong / total));
    }

    /** 沿航线弧长 {@code s} 处的点，超出两端时钳制到端点。 */
    public Vec3 pointAt(double s) {
        if (points.size() < 2) return points.get(0);
        double clamped = Math.max(0.0, Math.min(length(), s));
        int seg = segmentAt(clamped);
        double segLength = arc[seg + 1] - arc[seg];
        double t = segLength < EPS ? 0.0 : (clamped - arc[seg]) / segLength;
        return points.get(seg).lerp(points.get(seg + 1), t);
    }

    /** 弧长 {@code s} 处的单位水平航向（y 恒为 0）；退化航线返回零向量。 */
    public Vec3 directionAt(double s) {
        if (points.size() < 2) return Vec3.ZERO;
        int seg = segmentAt(Math.max(0.0, Math.min(length(), s)));
        Vec3 a = points.get(seg);
        Vec3 b = points.get(seg + 1);
        double dx = b.x - a.x;
        double dz = b.z - a.z;
        double len = Math.hypot(dx, dz);
        if (len < EPS) return Vec3.ZERO;
        return new Vec3(dx / len, 0.0, dz / len);
    }

    /**
     * 每服务端 tick 调用一次：累计本航线已用时长与停滞计数，返回是否需要重新规划。
     * 停滞 = 连续 {@link #STALL_TICKS} tick 内水平位移不到 1 格（卡在礁石或被碰撞推挤）。
     */
    public boolean advanceClock(double x, double z) {
        ticksSincePlan++;
        if (Math.hypot(x - stallX, z - stallZ) >= STALL_DISTANCE) {
            stallX = x;
            stallZ = z;
            stallTicks = 0;
        } else {
            stallTicks++;
        }
        return ticksSincePlan >= REPLAN_INTERVAL_TICKS || stallTicks >= STALL_TICKS;
    }

    private int segmentAt(double s) {
        int seg = 0;
        while (seg < points.size() - 2 && arc[seg + 1] < s) seg++;
        return seg;
    }

    private static double horizontalDistance(Vec3 a, Vec3 b) {
        return Math.hypot(b.x - a.x, b.z - a.z);
    }

    private static double horizontalDistance(Vec3 a, double x, double z) {
        return Math.hypot(x - a.x, z - a.z);
    }
}
