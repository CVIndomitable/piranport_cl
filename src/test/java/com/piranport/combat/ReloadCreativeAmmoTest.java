package com.piranport.combat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 创造弹药库装填鱼雷：开始读条与到期结算都必须认无限弹，
 * 不能因为背包里实体鱼雷不足而报「同类弹药不足」。
 */
class ReloadCreativeAmmoTest {

    private static final Path RELOAD_HELPER =
            Path.of("src/main/java/com/piranport/combat/ReloadHelper.java");
    private static final Path LIFECYCLE =
            Path.of("src/main/java/com/piranport/combat/data/WeaponReloadLifecycle.java");

    @Test
    void startReloadFallsBackToCreativeCacheWhenPhysicalAmmoIsShort() throws Exception {
        String source = Files.readString(RELOAD_HELPER);
        String method = source.substring(source.indexOf("public static void reloadTorpedoLauncher("));
        int countCheck = method.indexOf("if (!infinite && available < needed)");
        int cacheFallback = method.indexOf("CreativeAmmoCache.findInfiniteTorpedo(player, caliber)");
        assertTrue(cacheFallback > 0, "实体鱼雷不足时必须再查创造弹药库");
        assertTrue(cacheFallback < countCheck,
                "创造弹药库回退必须发生在最终「同类弹药不足」判定之前");
    }

    @Test
    void settlementDoesNotConsumeWhenCacheSelected() throws Exception {
        String source = Files.readString(LIFECYCLE);
        String method = source.substring(source.indexOf("private static void finishOneTorpedoReload("));
        assertTrue(method.contains("CreativeAmmoCache.isInfinite(player, ammo)"),
                "到期结算必须重新检查创造弹药库，不能只靠开始读条时的判断");
        int infiniteCheck = method.indexOf("boolean infinite");
        int insufficient = method.indexOf("insufficient_same_ammo");
        assertTrue(infiniteCheck > 0 && insufficient > infiniteCheck,
                "无限弹判断必须排在实体弹药不足提示之前");
        assertTrue(method.contains("if (!infinite) {"),
                "无限弹时不得扣实体鱼雷");
    }
}
