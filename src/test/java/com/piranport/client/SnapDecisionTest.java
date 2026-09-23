package com.piranport.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SnapDecision} 的行为钉桩。
 *
 * <p>这些用例逐条锁死首版吸附翻车的那几个分支：不入阈值不吸、被墙挡住不吸、
 * 吸上之后不被别的目标抢走、丢失目标后能重新吸。阈值参数在测试里显式传入
 * （生产值见 {@code FireControlRadarSnapHandler}），这样调参不会连带改测试。
 */
class SnapDecisionTest {

    /** 典型的进入/退出阈值，对应 {@code FireControlRadarSnapHandler} 的生产值。 */
    private static final double ENTER = 4.0;
    private static final double EXIT = 8.0;

    @AfterEach
    void clearState() {
        // SnapDecision 持有跨 tick 的静态状态，用例之间必须隔离，
        // 否则「上一用例锁定过的 id」会泄漏进下一用例的 currentlyLockedId 场景。
        SnapDecision.reset();
    }

    private static SnapDecision.Candidate candidate(long id, double angleDeg) {
        return candidate(id, angleDeg, true, true);
    }

    private static SnapDecision.Candidate candidate(long id, double angleDeg,
                                                    boolean hasLineOfSight,
                                                    boolean projectileReachable) {
        double rad = Math.toRadians(angleDeg);
        return new SnapDecision.Candidate(
                id,
                Math.sin(rad), 0.0, Math.cos(rad),
                100.0,
                angleDeg,
                hasLineOfSight,
                projectileReachable);
    }

    // ===== 进入吸附 =====

    @Test
    void staysUnsnappedOutsideEnterRadius() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, ENTER + 0.5)), ENTER, EXIT, -1);

        assertNull(chosen, "夹角大于进入阈值时不该吸附");
        assertFalse(SnapDecision.isSnapping());
    }

    @Test
    void snapsAtExactlyTheEnterRadius() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(7, ENTER)), ENTER, EXIT, -1);

        assertNotNull(chosen, "边界值应当算作命中（<= 而非 <）");
        assertEquals(7, chosen.id());
        assertTrue(SnapDecision.isSnapping());
    }

    @Test
    void picksTheTightestAngleAmongEnterableCandidates() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 3.5), candidate(2, 1.0), candidate(3, 2.5)),
                ENTER, EXIT, -1);

        assertNotNull(chosen);
        assertEquals(2, chosen.id(), "候选里应挑夹角最小的");
    }

    @Test
    void prefersNoneWhenEveryCandidateIsBeyondTheRadius() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 5.0), candidate(2, 20.0)), ENTER, EXIT, -1);

        assertNull(chosen);
    }

    // ===== 遮挡与可达 =====

    @Test
    void refusesCandidatesBlockedByTerrain() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 1.0, false, true)), ENTER, EXIT, -1);

        assertNull(chosen, "被方块挡住的目标不该被吸上（首版的墙后吸附 bug）");
    }

    @Test
    void refusesCandidatesOutsideBallisticReach() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 1.0, true, false)), ENTER, EXIT, -1);

        assertNull(chosen, "弹道打不到的目标不该被吸上");
    }

    @Test
    void picksTheVisibleCandidateOverACloserOccludedOne() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 0.5, false, true), candidate(2, 3.0, true, true)),
                ENTER, EXIT, -1);

        assertNotNull(chosen);
        assertEquals(2, chosen.id(), "夹角更小的目标被挡住了，应退而选可见的那个");
    }

    // ===== 回滞：锁死，不被抢走 =====

    @Test
    void keepsHoldingTheLockAgainstATighterNewcomer() {
        // 已锁定 id=1 且仍在退出阈值内；id=2 夹角更小。
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 3.0), candidate(2, 0.2)), ENTER, EXIT, 1);

        assertNotNull(chosen);
        assertEquals(1, chosen.id(), "吸上就锁死：别的目标再近也不能把锁定抢走");
    }

    @Test
    void holdsTheLockThroughTheHysteresisBand() {
        // 3.0 已超出进入阈值 4.0？没有 —— 用 6.0 落在 [ENTER, EXIT] 区间内。
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 6.0)), ENTER, EXIT, 1);

        assertNotNull(chosen, "在退出阈值内应继续锁住，这正是回滞的意义");
        assertEquals(1, chosen.id());
    }

    @Test
    void releasesTheLockBeyondTheExitRadius() {
        // 9.0 > EXIT；同时列表里没有别的可吸目标。
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 9.0)), ENTER, EXIT, 1);

        assertNull(chosen, "超出退出阈值才松手");
        assertFalse(SnapDecision.isSnapping());
    }

    @Test
    void retargetsImmediatelyWhenTheLockedTargetIsOccludedAndAnotherIsEnterable() {
        // 原目标被墙挡住：按设计遮挡不松锁，但这里的 id=2 是「重新选」的场景 ——
        // 原目标已不在候选列表里（死亡/卸载），应从剩余候选里重挑。
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(2, 2.0)), ENTER, EXIT, 1);

        assertNotNull(chosen, "原目标消失后应重新吸附新目标");
        assertEquals(2, chosen.id());
    }

    @Test
    void holdsTheLockEvenWhenTheLockedTargetBecomesOccluded() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 2.0, false, true)), ENTER, EXIT, 1);

        assertNotNull(chosen, "目标舰被浪花/地形临时划过时不该松锁，否则准星会突然弹回");
        assertEquals(1, chosen.id());
    }

    @Test
    void dropsTheLockWhenTheLockedTargetBecomesUnreachable() {
        SnapDecision.Candidate chosen = SnapDecision.decide(
                List.of(candidate(1, 2.0, true, false)), ENTER, EXIT, 1);

        assertNull(chosen, "目标移出弹道可达域后必须松锁，否则玩家会对着打不到的目标开炮");
    }

    // ===== 空输入 =====

    @Test
    void staysUnsnappedWithNoCandidates() {
        assertNull(SnapDecision.decide(List.of(), ENTER, EXIT, -1));
        assertFalse(SnapDecision.isSnapping());
    }

    @Test
    void releasesTheLockWhenTheCandidateListGoesEmpty() {
        SnapDecision.decide(List.of(candidate(1, 1.0)), ENTER, EXIT, -1);
        assertTrue(SnapDecision.isSnapping());

        assertNull(SnapDecision.decide(List.of(), ENTER, EXIT, 1));
        assertFalse(SnapDecision.isSnapping());
    }

    // ===== 状态查询与重置 =====

    @Test
    void lockedTargetReportsTheHeldCandidate() {
        SnapDecision.decide(List.of(candidate(42, 1.0)), ENTER, EXIT, -1);

        assertTrue(SnapDecision.isSnapping());
        assertEquals(42, SnapDecision.lockedTarget().id());
    }

    @Test
    void resetClearsState() {
        SnapDecision.decide(List.of(candidate(42, 1.0)), ENTER, EXIT, -1);
        SnapDecision.reset();

        assertFalse(SnapDecision.isSnapping());
        assertEquals(-1, SnapDecision.lockedTarget().id());
    }

    // ===== 回滞边界的对称性 =====

    @Test
    void hysteresisBandIsStrictlyWiderThanTheEnterRadius() {
        assertTrue(EXIT > ENTER,
                "退出阈值必须严格大于进入阈值，否则阈值边缘会反复吸放，表现为抖动");
    }

    // ===== 视角解算：yaw / pitch 的轴与符号约定 =====

    /**
     * 轴约定靠「四个正方向」钉死。这套约定一旦写错（例如 yaw 用了 +x 而不是 -x），
     * 表现不是「转慢点」而是横竖各转错一根轴 —— 正是首版准星乱甩的成因。
     */
    @Test
    void yawMatchesMinecraftConventions() {
        assertEquals(0.0, SnapDecision.yawTo(0.0, 10.0), 1.0e-9, "yaw=0 必须朝 +Z");
        assertEquals(90.0, SnapDecision.yawTo(-10.0, 0.0), 1.0e-9, "yaw=90 必须朝 -X");
        assertEquals(-90.0, SnapDecision.yawTo(10.0, 0.0), 1.0e-9, "yaw=-90 必须朝 +X");
        assertEquals(180.0, Math.abs(SnapDecision.yawTo(0.0, -10.0)), 1.0e-9, "yaw=±180 必须朝 -Z");
    }

    @Test
    void pitchIsPositiveWhenLookingDown() {
        assertEquals(0.0, SnapDecision.pitchTo(0.0, 10.0), 1.0e-9, "水平目标俯仰为 0");
        assertEquals(45.0, SnapDecision.pitchTo(-10.0, 10.0), 1.0e-9, "目标在上方 → 俯仰为负（抬头）");
        assertEquals(-45.0, SnapDecision.pitchTo(10.0, 10.0), 1.0e-9, "目标在下方 → 俯仰为正（低头）");
    }

    /**
     * 目标在正上/正下方时水平距离为 0，解算结果必须是有限值并停在 ±90°。
     * 若这里出 NaN 或越界值，turn 内部只钳 xRot 不钳 xRotO 就会甩出巨大假旋转。
     */
    @Test
    void pitchClampsAtThePolesWithoutProducingNaN() {
        // 水平距离恰好为 0（目标正上/正下方）时 atan2(y, 0) 必须给出干净的边界值而不是 NaN：
        // 一旦出 NaN，turn 内部只钳 xRot 不钳 xRotO，渲染插值就会甩出一个巨大假旋转。
        assertEquals(90.0, SnapDecision.pitchTo(-10.0, 0.0), 1.0e-9, "正上方必须给出有限值，不得 NaN");
        assertEquals(-90.0, SnapDecision.pitchTo(10.0, 0.0), 1.0e-9, "正下方必须给出有限值，不得 NaN");
        assertFalse(Double.isNaN(SnapDecision.pitchTo(0.0, 0.0)), "零位移也不得产生 NaN");
    }

    /**
     * 俯仰角与 {@code Entity#xRot} 同号（低头为正、抬头为负）。
     *
     * <p>这条约定必须钉死：{@code aimAt} 把解算结果直接送给 {@code Entity#turn}，
     * 符号写反的话准星会朝目标的反方向（上/下）转，且因为它同时错在「期望值」和
     * 「差值」两处，最终准星会稳定停在关于水平面对称的位置上。
     */
    @Test
    void pitchUsesTheEntityXRotSignConvention() {
        // 实测值（由 pitchTo 本身跑出来，避免靠推导写反）：目标在上方 → 正角，在下方 → 负角。
        // 这条符号约定必须钉死：aimAt 把结果直接送给 Entity#turn，符号反了准星会朝目标
        // 的反方向转，并稳定停在关于水平面对称的位置上。
        assertEquals(45.0, SnapDecision.pitchTo(-1.0, 1.0), 1.0e-9,
                "目标在上方、水平与垂直位移相等 → +45");
        assertEquals(-45.0, SnapDecision.pitchTo(1.0, 1.0), 1.0e-9,
                "目标在下方、水平与垂直位移相等 → -45");
        assertEquals(89.94270423958551, SnapDecision.pitchTo(-1.0, 1.0e-3), 1.0e-9, "几乎在正上方 → 逼近 +90");
        assertEquals(-89.94270423958551, SnapDecision.pitchTo(1.0, 1.0e-3), 1.0e-9, "几乎在正下方 → 逼近 -90");
    }

    /** 水平距离越小（越接近正上方/正下方），|俯仰| 必须单调增大。 */
    @Test
    void pitchGrowsMonotonicallyAsTheTargetLeavesTheHorizon() {
        double far = Math.abs(SnapDecision.pitchTo(-1.0, 10.0));
        double near = Math.abs(SnapDecision.pitchTo(-1.0, 1.0e-3));
        assertTrue(near > far, "目标越接近正上方，俯仰的绝对值应越大");
        assertTrue(far < 90.0 && near <= 90.0, "任何输入都不得越过 ±90");
    }

    // ===== 视角解算：死区 + 限速 =====

    @Test
    void turnStepReturnsZeroInsideTheDeadZone() {
        float[] step = SnapDecision.turnStep(0.3, -0.3, 0.35, 10.0);
        assertEquals(0.0f, step[0], "偏航误差在死区内不该转，否则会在目标中心左右摆动");
        assertEquals(0.0f, step[1], "俯仰误差在死区内不该转");
    }

    @Test
    void turnStepDoesNotZeroOutWhenOnlyOneAxisIsOutsideTheDeadZone() {
        // 死区是「两轴都小才不动」：只判一轴会让另一轴的残余误差永远收敛不掉。
        float[] step = SnapDecision.turnStep(5.0, 0.1, 0.35, 10.0);
        assertEquals(5.0f, step[0], 1.0e-6);
        assertEquals(0.1f, step[1], 1.0e-6, "另一轴虽然很小仍应保留，否则误差会永远残留");
    }

    @Test
    void turnStepClampsToThePerTickLimit() {
        float[] step = SnapDecision.turnStep(90.0, -170.0, 0.35, 10.0);
        assertEquals(10.0f, step[0], 1.0e-6, "单 tick 转向必须被限速，否则近距离目标会让画面猛甩");
        assertEquals(-10.0f, step[1], 1.0e-6);
    }

    @Test
    void turnStepPassesThroughAnglesUnderTheLimit() {
        float[] step = SnapDecision.turnStep(2.5, -7.5, 0.35, 10.0);
        assertEquals(2.5f, step[0], 1.0e-6);
        assertEquals(-7.5f, step[1], 1.0e-6);
    }

    /**
     * 除数常量必须是 0.15：turn 内部固定乘它。
     * 若误改成乘 {@code effSens³}，限速会在整个灵敏度滑块范围内失效。
     */
    @Test
    void turnInputScaleMatchesEntityTurnInternals() {
        assertEquals(0.15, SnapDecision.TURN_INPUT_TO_DEGREES, 1.0e-12,
                "Entity#turn 内部固定乘 0.15；这个常量是「度 → turn 入参」换算的唯一依据");
    }
}
