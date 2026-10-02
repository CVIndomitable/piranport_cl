package com.piranport.aviation;

import com.piranport.component.AircraftInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出击准备判定：R 键装填读条的前置校验、到期结算与放飞校验共用这一份规则，
 * 三者必须给同一个答案（曾经"提示无需补给但油量为 0 被放飞拦下"就是这么来的）。
 */
class AircraftSortieReadinessTest {

    /** 组合必须合法（见 AircraftDefinition#validateCombination），否则构造直接抛异常。 */
    private static AircraftDefinition definition(AircraftInfo.AircraftType type,
                                                 AircraftDefinition.AttackProfile profile,
                                                 AircraftDefinition.PayloadType payload) {
        return new AircraftDefinition("piranport:aircraft/readiness_test", type,
                profile, payload, "test", 100, 4, 10.0F, 1.0F, 10,
                AircraftInfo.BombingMode.DIVE);
    }

    private static AircraftInfo info(AircraftInfo.AircraftType type, int fuel, boolean payloadLoaded) {
        return new AircraftInfo(type, 100, 4, fuel, 10.0F, 1.0F, 10,
                AircraftInfo.BombingMode.DIVE, payloadLoaded);
    }

    @Test
    void fighterNeedsFuelOnly() {
        AircraftDefinition fighter = definition(AircraftInfo.AircraftType.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, AircraftDefinition.PayloadType.NONE);
        ResolvedAircraftStats stats = AircraftStatsService.resolve(fighter);

        assertFalse(AircraftSortieReadiness.isReady(info(AircraftInfo.AircraftType.FIGHTER, 0, false), fighter, stats));
        assertTrue(AircraftSortieReadiness.needsFuel(info(AircraftInfo.AircraftType.FIGHTER, 0, false), stats));
        assertFalse(AircraftSortieReadiness.needsPayload(fighter, info(AircraftInfo.AircraftType.FIGHTER, 0, false)));

        // 战斗机没有对海挂载：只有油也算准备完毕
        assertTrue(AircraftSortieReadiness.isReady(info(AircraftInfo.AircraftType.FIGHTER, 100, false), fighter, stats));
    }

    @Test
    void attackerNeedsFuelAndPayload() {
        AircraftDefinition attacker = definition(AircraftInfo.AircraftType.TORPEDO_BOMBER,
                AircraftDefinition.AttackProfile.TORPEDO, AircraftDefinition.PayloadType.AERIAL_TORPEDO);
        ResolvedAircraftStats stats = AircraftStatsService.resolve(attacker);

        // 有油无弹 → 未准备，且缺口是挂载
        AircraftInfo fueledOnly = info(AircraftInfo.AircraftType.TORPEDO_BOMBER, 100, false);
        assertFalse(AircraftSortieReadiness.isReady(fueledOnly, attacker, stats));
        assertFalse(AircraftSortieReadiness.needsFuel(fueledOnly, stats));
        assertTrue(AircraftSortieReadiness.needsPayload(attacker, fueledOnly));

        // 有弹无油 → 未准备，且缺口是燃油
        AircraftInfo payloadOnly = info(AircraftInfo.AircraftType.TORPEDO_BOMBER, 0, true);
        assertFalse(AircraftSortieReadiness.isReady(payloadOnly, attacker, stats));
        assertTrue(AircraftSortieReadiness.needsFuel(payloadOnly, stats));
        assertFalse(AircraftSortieReadiness.needsPayload(attacker, payloadOnly));

        // 两项齐备才算准备完毕
        assertTrue(AircraftSortieReadiness.isReady(
                info(AircraftInfo.AircraftType.TORPEDO_BOMBER, 100, true), attacker, stats));
    }

    @Test
    void partialFuelIsNotReady() {
        AircraftDefinition fighter = definition(AircraftInfo.AircraftType.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, AircraftDefinition.PayloadType.NONE);
        ResolvedAircraftStats stats = AircraftStatsService.resolve(fighter);
        // 读条必须把油加满：半箱油仍算未准备，否则读条后的状态与放飞校验不一致
        assertFalse(AircraftSortieReadiness.isReady(info(AircraftInfo.AircraftType.FIGHTER, 99, false), fighter, stats));
        assertTrue(AircraftSortieReadiness.isReady(info(AircraftInfo.AircraftType.FIGHTER, 100, false), fighter, stats));
    }
}
