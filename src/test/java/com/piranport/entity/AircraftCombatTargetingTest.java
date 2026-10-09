package com.piranport.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 战斗机自动筛选必须复用敌对判据，手动火控锁定仍优先。 */
class AircraftCombatTargetingTest {
    private static final Path COMBAT = Path.of("src/main/java/com/piranport/entity/AircraftCombat.java");
    private static final Path AIRCRAFT = Path.of("src/main/java/com/piranport/entity/AircraftEntity.java");

    @Test
    void fighterAutoSeekFiltersAircraftWithSharedHostilityRuleAfterManualLocks() throws Exception {
        String source = Files.readString(COMBAT);
        String method = methodBody(source, "public static Entity resolveFighterTarget(", "\n    }\n");
        int locks = method.indexOf("if (!locks.isEmpty())");
        int automaticScan = method.indexOf("// Auto-seek: only enemy aircraft");

        assertTrue(locks >= 0);
        assertTrue(automaticScan > locks, "手动火控锁定必须先于自动过滤处理");
        assertTrue(method.contains("getEntitiesOfClass(AircraftEntity.class"), "机枪自动索敌仍只找飞机");
        assertTrue(method.contains("CombatTargeting.isHostileTarget(owner, e)"));
        assertFalse(method.contains("getOwnerUUID()"), "自动索敌不得自行按 owner UUID 另行判断敌我");
    }

    @Test
    void fighterCruisingGateUsesSameRuleForExistingHostileTypes() throws Exception {
        String source = Files.readString(AIRCRAFT);
        String method = methodBody(source, "private void tickCruising(Player owner)", "\n    }\n");
        int fighterBranch = method.indexOf("aircraftType == AircraftInfo.AircraftType.FIGHTER");
        int attackTransition = method.indexOf("setState(FlightState.ATTACKING)", fighterBranch);
        assertTrue(fighterBranch >= 0);
        assertTrue(attackTransition > fighterBranch);

        String fighterGate = method.substring(fighterBranch, attackTransition);
        assertTrue(fighterGate.contains("e instanceof Monster"), "保留原有战斗机对地敌对生物范围");
        assertTrue(fighterGate.contains("getEntitiesOfClass(AircraftEntity.class"));
        assertEquals(2, occurrences(fighterGate, "CombatTargeting.isHostileTarget(owner, e)"),
                "Monster 与敌机候选都必须使用共享敌对判据");
        assertFalse(fighterGate.contains("getOwnerUUID()"));
    }

    private static String methodBody(String source, String signature, String endMarker) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法：" + signature);
        int end = source.indexOf(endMarker, start);
        assertTrue(end > start, "找不到方法结束位置：" + signature);
        return source.substring(start, end);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int from = 0;
        while ((from = value.indexOf(needle, from)) >= 0) {
            count++;
            from += needle.length();
        }
        return count;
    }
}
