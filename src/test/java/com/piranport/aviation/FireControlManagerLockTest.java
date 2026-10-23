package com.piranport.aviation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
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
