package com.piranport.aviation;

import com.piranport.component.AircraftState;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 出击准备判定：R 键装填读条的前置校验、到期结算与放飞校验共用这一份规则。
 */
class AircraftSortieReadinessTest {

    private static AircraftDefinition definition(AircraftClass type,
                                                 AircraftDefinition.AttackProfile profile,
                                                 String payloadPath, String visualId) {
        return new AircraftDefinition(
                ResourceLocation.parse("piranport:readiness_test"),
                type, profile,
                payloadPath == null ? null : ResourceLocation.parse(payloadPath),
                visualId, 100, 4, 10.0F, 1.0F, 10, 15, 40, null);
    }

    private static AircraftState state(int fuel, boolean payloadLoaded) {
        return new AircraftState(fuel, payloadLoaded);
    }

    @Test
    void fighterNeedsFuelOnly() {
        AircraftDefinition fighter = definition(AircraftClass.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, null, "fighter");
        ResolvedAircraftStats stats = AircraftStatsService.resolve(fighter);

        assertFalse(AircraftSortieReadiness.isReady(state(0, false), fighter, stats));
        assertTrue(AircraftSortieReadiness.needsFuel(state(0, false), stats));
        assertFalse(AircraftSortieReadiness.needsPayload(fighter, state(0, false)));
        assertTrue(AircraftSortieReadiness.isReady(state(100, false), fighter, stats));
    }

    @Test
    void attackerNeedsFuelAndPayload() {
        AircraftDefinition attacker = definition(AircraftClass.TORPEDO_BOMBER,
                AircraftDefinition.AttackProfile.TORPEDO,
                "piranport:aerial_torpedo", "torpedo_bomber");
        ResolvedAircraftStats stats = AircraftStatsService.resolve(attacker);

        AircraftState fueledOnly = state(100, false);
        assertFalse(AircraftSortieReadiness.isReady(fueledOnly, attacker, stats));
        assertFalse(AircraftSortieReadiness.needsFuel(fueledOnly, stats));
        assertTrue(AircraftSortieReadiness.needsPayload(attacker, fueledOnly));

        AircraftState payloadOnly = state(0, true);
        assertFalse(AircraftSortieReadiness.isReady(payloadOnly, attacker, stats));
        assertTrue(AircraftSortieReadiness.needsFuel(payloadOnly, stats));
        assertFalse(AircraftSortieReadiness.needsPayload(attacker, payloadOnly));

        assertTrue(AircraftSortieReadiness.isReady(state(100, true), attacker, stats));
    }

    @Test
    void partialFuelIsNotReady() {
        AircraftDefinition fighter = definition(AircraftClass.FIGHTER,
                AircraftDefinition.AttackProfile.GUN, null, "fighter");
        ResolvedAircraftStats stats = AircraftStatsService.resolve(fighter);
        assertFalse(AircraftSortieReadiness.isReady(state(99, false), fighter, stats));
        assertTrue(AircraftSortieReadiness.isReady(state(100, false), fighter, stats));
    }
}
