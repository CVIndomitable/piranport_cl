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
        int automatic = method.indexOf("return autoSeek(ctx, Entity.class");
        assertTrue(locked >= 0);
        assertTrue(automatic > locked, "手动火控锁定必须先于自动索敌处理");
        assertTrue(method.contains("CombatTargeting.isHostileTarget(owner, entity)"));
        assertTrue(method.contains("isAirborneTarget(entity)"));
        assertFalse(method.contains("getOwnerUUID()"), "自动索敌不得自行按 owner UUID 另行判断敌我");

        String gun = Files.readString(GUN);
        assertTrue(gun.contains("TargetResolution.resolveFighterTarget(context, owner)"));
    }

    @Test
    void fighterCruisingGateIncludesVanillaAndModdedFlyingMobs() throws Exception {
        String flight = Files.readString(FLIGHT);
        String method = methodBody(flight, "private static boolean tryEngagePlayerTargets(");
        int fighterBranch = method.indexOf("TargetResolution.isAirborneTarget(entity)");
        assertTrue(fighterBranch >= 0);
        assertTrue(method.contains("entity instanceof net.minecraft.world.entity.FlyingMob"));
        assertTrue(method.contains("CombatTargeting.isHostileTarget(owner, entity)"));
    }

    @Test
    void fighterLockGateAcceptsEnemyAircraft() throws Exception {
        String flight = Files.readString(FLIGHT);
        String gate = methodBody(flight, "private static boolean tryEngagePlayerTargets(");
        assertTrue(gate.contains("hasAliveAirLockedTarget(craft, owner)"),
                "战斗机/火箭机的手动锁定闸口必须接受敌机（AircraftEntity）");

        String helper = methodBody(flight, "private static boolean hasAliveAirLockedTarget(");
        assertTrue(helper.contains("TargetResolution.isAirborneTarget(entity)"),
                "手动火控可锁定原版和模组 FlyingMob");
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

    @Test
    void autonomousFighterBulletsSpareOwnerPlayers() throws Exception {
        String bullet = Files.readString(Path.of(
                "src/main/java/com/piranport/entity/BulletEntity.java"));
        String canHit = methodBody(bullet, "protected boolean canHitEntity(");
        assertTrue(canHit.contains("sparesPlayers && target instanceof Player"),
                "深海自主战斗机子弹不得命中玩家本体");

        String gun = Files.readString(GUN);
        assertTrue(gun.contains("bullet.setSparesPlayers(owner == null)"),
                "只有无主人（自主）子弹才启用玩家豁免，玩家飞机子弹保持原行为");
    }

    @Test
    void autonomousFighterPreyExcludesPlayerCompanions() throws Exception {
        String targeting = Files.readString(TARGETING);
        String prey = methodBody(targeting, "private static boolean isAutonomousFighterPrey(");
        assertTrue(prey.contains("ShipGirlEntity"),
                "随从舰娘是普通 Mob 且非 Enemy，必须显式排除，否则会被当成中立生物误打");
        assertFalse(prey.contains("Player"), "玩家本体不得进入自主战斗机猎物集合");
    }

    @Test
    void antiAirAutoAimIncludesHostileAircraftLikeMissileHoming() throws Exception {
        String fire = Files.readString(Path.of(
                "src/main/java/com/piranport/combat/missile/MissileFireStrategy.java"));
        String aim = methodBody(fire, "public static void spawnMissileAutoAim(");
        assertTrue(aim.contains("e instanceof AircraftEntity"),
                "防空导弹发射瞄准需与 MissileEntity#findTarget 一样纳入敌机");
        assertTrue(aim.contains("CombatTargeting.isHostileTarget(player, e)"),
                "敌机口径必须复用共享敌对判据");
    }

    @Test
    void airborneTargetCoversModdedFlyingMobsAndAutoLaunchUsesSameRule() throws Exception {
        String targeting = Files.readString(TARGETING);
        String method = methodBody(targeting, "public static boolean isAirborneTarget(");
        assertTrue(method.contains("FlyingAnimal"),
                "女仆野生妖精（EntityFairy）实现 FlyingAnimal 而非 FlyingMob，必须纳入空中目标");
        assertTrue(method.contains("FlyingPathNavigation"),
                "其他模组飞行怪用飞行导航时也要视为空中目标");
        assertTrue(method.contains("entity instanceof Vex"),
                "Vex 只继承 Monster，不实现 FlyingMob");

        String handler = Files.readString(Path.of(
                "src/main/java/com/piranport/handler/PlayerTickHandler.java"));
        String autoLaunch = methodBody(handler, "private static void tickAutoLaunchFighters(");
        assertTrue(autoLaunch.contains("TargetResolution.isAirborneTarget(e)"),
                "自动放飞战斗机的扫描必须复用同一套空中目标判定，否则会漏掉幻翼/模组飞行怪");
    }

    @Test
    void torpedoBomberAimsAtWaterSurfaceAndNeverLaunchesBehind() throws Exception {
        String strategy = Files.readString(Path.of(
                "src/main/java/com/piranport/aviation/combat/strategy/TorpedoAttackStrategy.java"));
        assertTrue(strategy.contains("setTorpedoAimPoint(new Vec3(predicted.x, smoothedSurfaceY, predicted.z))"),
                "鱼雷机预瞄点必须是预测撞击点的水面投影，而不是目标眼睛/几何中心");
        assertTrue(strategy.contains("boolean predictedAhead = horizontalVelocity.lengthSqr()"),
                "投雷前必须校验预测点在机头前方，避免目标被超车后鱼雷朝后发射");
        assertTrue(strategy.contains("abortTorpedoRun(state)"),
                "预测点落到身后时应中断航线重新进入，而不是继续投雷");
        assertTrue(strategy.contains("TORPEDO_BOMBER_TORPEDO_LIFETIME"),
                "航空鱼雷必须设置独立寿命，未命中时不能按默认 1200 tick 长时间乱跑");
        String entity = Files.readString(Path.of(
                "src/main/java/com/piranport/entity/AircraftEntity.java"));
        assertTrue(entity.contains("setTorpedoAimPoint(null)") && entity.contains("else {"),
                "离开攻击态要清空预瞄点，返航时不能残留上一轮的白线");
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
