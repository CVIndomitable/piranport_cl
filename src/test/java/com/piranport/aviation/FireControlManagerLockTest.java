package com.piranport.aviation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 手动火控锁定必须能覆盖内部友伤防呆（玩家锁自己的飞机/女仆时仍允许命中）。 */
class FireControlManagerLockTest {

    @AfterEach
    void clearState() {
        FireControlManager.clearAll();
    }

    @Test
    void isLockedReflectsLockAndAddTarget() {
        UUID player = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        assertFalse(FireControlManager.isLocked(player, target));

        FireControlManager.lock(player, target);
        assertTrue(FireControlManager.isLocked(player, target));
        assertFalse(FireControlManager.isLocked(player, UUID.randomUUID()));
        assertFalse(FireControlManager.isLocked(UUID.randomUUID(), target));

        UUID second = UUID.randomUUID();
        FireControlManager.addTarget(player, second);
        assertTrue(FireControlManager.isLocked(player, second));

        FireControlManager.clearTargets(player);
        assertFalse(FireControlManager.isLocked(player, target));
    }

    @Test
    void removeDeadTargetsDropsOnlyTheDeadOnes() {
        UUID player = UUID.randomUUID();
        UUID deadBoss = UUID.randomUUID();
        UUID liveEscort = UUID.randomUUID();
        FireControlManager.lock(player, deadBoss);
        FireControlManager.addTarget(player, liveEscort);

        assertTrue(FireControlManager.removeDeadTargets(player, uuid -> uuid.equals(deadBoss)));
        assertEquals(List.of(liveEscort), FireControlManager.getTargets(player));

        // 没有死目标可删时返回 false，调用方据此避免无意义地发包
        assertFalse(FireControlManager.removeDeadTargets(player, uuid -> false));
    }

    @Test
    void removeDeadTargetsClearsPlayerEntryWhenListBecomesEmpty() {
        UUID player = UUID.randomUUID();
        FireControlManager.lock(player, UUID.randomUUID());

        assertTrue(FireControlManager.removeDeadTargets(player, uuid -> true));
        assertTrue(FireControlManager.getTargets(player).isEmpty());
        assertFalse(FireControlManager.removeDeadTargets(player, uuid -> true),
                "条目已被移除，再次清理不应报告变更");
    }

    @Test
    void friendlyFireHelperChecksManualLockBeforeProtectionRules() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/piranport/combat/FriendlyFireHelper.java"));
        int method = source.indexOf("public static boolean shouldBlockHit(");
        assertTrue(method >= 0);
        int manualLock = source.indexOf("FireControlManager.isLocked(", method);
        int firstProtection = source.indexOf("AbstractDeepOceanEntity", method);
        assertTrue(manualLock > method, "shouldBlockHit 必须检查手动锁定");
        assertTrue(firstProtection > manualLock,
                "手动锁定判断必须排在第一条内部防呆规则之前，否则锁友方仍会被拦");
    }
}
