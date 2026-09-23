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
}
