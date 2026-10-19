package com.piranport.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 自动索敌必须复用共享敌对判据；声呐标记与自动攻击的过滤边界不能混用。 */
class AircraftCombatTargetingTest {
    private static final Path TARGETING =
            Path.of("src/main/java/com/piranport/aviation/combat/TargetResolution.java");
    private static final Path FLIGHT =
            Path.of("src/main/java/com/piranport/entity/AircraftFlight.java");
    private static final Path DEPTH_CHARGE =
            Path.of("src/main/java/com/piranport/aviation/combat/strategy/DepthChargeAttackStrategy.java");
    private static final Path GUN =
            Path.of("src/main/java/com/piranport/aviation/combat/strategy/GunAttackStrategy.java");

    @Test
    void fighterAutoSeekFiltersAircraftWithSharedHostilityRuleAfterManualLocks() throws Exception {
        String targeting = Files.readString(TARGETING);
        String method = methodBody(targeting, "public static Entity resolveFighterTarget(");
        int nextMethod = method.indexOf("public static Entity resolveAutonomousFighterTarget(");
        assertTrue(nextMethod > 0);
        method = method.substring(0, nextMethod);
        int locked = method.indexOf("resolveLocked(ctx, owner,");
        int automatic = method.indexOf("return autoSeek(ctx, com.piranport.entity.AircraftEntity.class");
        assertTrue(locked >= 0);
        assertTrue(automatic > locked, "手动火控锁定必须先于自动索敌处理");
        assertTrue(method.contains("CombatTargeting.isHostileTarget(owner, entity)"));
        assertFalse(method.contains("getOwnerUUID()"), "自动索敌不得自行按 owner UUID 另行判断敌我");

        String gun = Files.readString(GUN);
        assertTrue(gun.contains("TargetResolution.resolveFighterTarget(context, owner)"));
    }

    @Test
    void fighterCruisingGateUsesSameRuleForExistingHostileTypes() throws Exception {
        String flight = Files.readString(FLIGHT);
        String method = methodBody(flight, "private static boolean tryEngagePlayerTargets(");
        // 自动索敌分支以 boolean hasHostile 开头；前面的 usesBulletAttack 分支是火控锁定闸口。
        int fighterBranch = method.indexOf("boolean hasHostile");
        int aswBranchEnd = method.indexOf("} else {", fighterBranch);
        assertTrue(fighterBranch >= 0);
        assertTrue(aswBranchEnd > fighterBranch);
        String fighterGate = method.substring(fighterBranch, aswBranchEnd);
        assertTrue(fighterGate.contains("entity instanceof Monster"));
        assertTrue(fighterGate.contains("getEntitiesOfClass(AircraftEntity.class"));
        assertEquals(2, occurrences(fighterGate, "CombatTargeting.isHostileTarget(owner, entity)"),
                "Monster 与敌机候选都必须使用共享敌对判据");
        assertFalse(fighterGate.contains("getOwnerUUID()"));
    }

    @Test
    void fighterLockGateAcceptsEnemyAircraft() throws Exception {
        String flight = Files.readString(FLIGHT);
        String gate = methodBody(flight, "private static boolean tryEngagePlayerTargets(");
        assertTrue(gate.contains("hasAliveAirLockedTarget(craft, owner)"),
                "战斗机/火箭机的手动锁定闸口必须接受敌机（AircraftEntity）");

        String helper = methodBody(flight, "private static boolean hasAliveAirLockedTarget(");
        assertTrue(helper.contains("entity instanceof AircraftEntity aircraft && aircraft.isAlive()"),
                "敌机不继承 LivingEntity，必须单独判定");
    }

    @Test
    void aswAutoSeekUsesHostilityFilterWhileSonarDoesNot() throws Exception {
        String depthCharge = Files.readString(DEPTH_CHARGE);
        assertTrue(depthCharge.contains("AircraftAswRecon.isHostileAswTarget")
                || depthCharge.contains("CombatTargeting.isHostileTarget(owner, entity)"));

        String asw = Files.readString(Path.of(
                "src/main/java/com/piranport/entity/AircraftAswRecon.java"));
        String sonar = methodBody(asw, "public static void tickAswSonar(");
        assertTrue(sonar.contains("isAswTarget(living)"));
        assertFalse(sonar.contains("isHostileAswTarget"), "声呐标记必须保留所有水生生物");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法：" + signature);
        return source.substring(start);
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
