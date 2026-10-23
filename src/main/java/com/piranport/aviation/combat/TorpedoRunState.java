package com.piranport.aviation.combat;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 鱼雷机的每架次运行时状态（进入 ATTACKING 时重置）。 */
public final class TorpedoRunState {
    /** 非 null 表示已进入低空平飞段。 */
    @Nullable public Vec3 runStart;
    public int runTicks;
    /** 平飞段每 tick 解算出的预测撞击点；接近段为 null。 */
    @Nullable public Vec3 aimPoint;
    /** 本架次是否已在接近中达到过平飞入口距离；未达到前若距离过近就先拉开。 */
    public boolean standoffReached;
    /** 正在背向拉开，直到距离与高度都满足平飞入口条件。 */
    public boolean egressing;
    @Nullable public Vec3 approachDirection;
    /**
     * 平滑后的预测点水面高度。水面搜索按方块取整，预测点每跨一格会跳 1.0，
     * 直接拿它做目标高度会让飞机在下降段一格一格地抽动；这里用一阶滤波抹平。
     */
    @Nullable public Double smoothedSurfaceY;
    /**
     * 本轮航线锁定的目标。多目标手动锁定/自动索敌在中途换目标时，
     * runStart、预瞄方向和已平滑的水面高度都属于旧目标，必须整体复位；
     * 否则飞机会拿旧航线去投新目标。
     */
    @Nullable public UUID activeTarget;

    public void reset() {
        runStart = null;
        runTicks = 0;
        aimPoint = null;
        standoffReached = false;
        egressing = false;
        approachDirection = null;
        smoothedSurfaceY = null;
        activeTarget = null;
    }
}
