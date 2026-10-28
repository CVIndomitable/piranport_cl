package com.piranport.combat;

import com.piranport.combat.torpedo.TorpedoFireStrategy;
import com.piranport.component.LoadedAmmo;
import com.piranport.item.TorpedoLauncherItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 2026-10-26 鱼雷装填/开火门禁：
 * <ul>
 *   <li>生存与创造都必须装备「鱼雷再装填」部件，缺部件不启动读条；</li>
 *   <li>创造模式不是弹药来源，必须有实体鱼雷或创造弹药库；</li>
 *   <li>没有满膛鱼雷不得发射，装填中提示「装填中」；</li>
 *   <li>0/负数/离谱时长回落到合法值，避免超长读条。</li>
 * </ul>
 *
 * <p>纯逻辑用静态方法直接断言；跨类门禁用源码契约断言（与
 * {@link ReloadCreativeAmmoTest} 相同的测试风格，不需要启动注册表）。
 */
class TorpedoReloadGateTest {

    private static final Path MAIN = Path.of("src/main/java/com/piranport");

    // ===== 纯逻辑 =====

    @Test
    void reloadTicksFallBackToLegalDefault() {
        assertEquals(ReloadHelper.TORPEDO_RELOAD_TICKS, ReloadHelper.sanitizeTorpedoReloadTicks(0));
        assertEquals(ReloadHelper.TORPEDO_RELOAD_TICKS, ReloadHelper.sanitizeTorpedoReloadTicks(-1));
        assertEquals(ReloadHelper.TORPEDO_RELOAD_TICKS, ReloadHelper.sanitizeTorpedoReloadTicks(400));
        assertEquals(ReloadHelper.MAX_REASONABLE_TICKS,
                ReloadHelper.sanitizeTorpedoReloadTicks(ReloadHelper.MAX_REASONABLE_TICKS));
        assertEquals(ReloadHelper.TORPEDO_RELOAD_TICKS,
                ReloadHelper.sanitizeTorpedoReloadTicks(ReloadHelper.MAX_REASONABLE_TICKS + 1));
    }

    @Test
    void creativeAloneIsNotAnAmmoSource() {
        assertFalse(ReloadHelper.hasTorpedoAmmoSource(false, 4, 5), "创造模式本身不能凑数");
        assertTrue(ReloadHelper.hasTorpedoAmmoSource(false, 5, 5), "实体鱼雷够整组");
        assertTrue(ReloadHelper.hasTorpedoAmmoSource(true, 0, 5), "创造弹药库可以替代实体弹药");
    }

    @Test
    void launcherCooldownIsSanitized() {
        assertEquals(TorpedoLauncherItem.FALLBACK_COOLDOWN_TICKS,
                TorpedoLauncherItem.sanitizeCooldownTicks(0));
        assertEquals(TorpedoLauncherItem.FALLBACK_COOLDOWN_TICKS,
                TorpedoLauncherItem.sanitizeCooldownTicks(-7));
        assertEquals(TorpedoLauncherItem.FALLBACK_COOLDOWN_TICKS,
                TorpedoLauncherItem.sanitizeCooldownTicks(
                        TorpedoLauncherItem.MAX_REASONABLE_COOLDOWN_TICKS + 1));
        assertEquals(140, TorpedoLauncherItem.sanitizeCooldownTicks(140));
    }

    @Test
    void fireRequiresFullChamber() {
        assertTrue(TorpedoFireStrategy.hasFullSalvo(new LoadedAmmo(5, "piranport:torpedo_533mm"), 5));
        assertFalse(TorpedoFireStrategy.hasFullSalvo(new LoadedAmmo(4, "piranport:torpedo_533mm"), 5),
                "不满管不得发射");
        assertFalse(TorpedoFireStrategy.hasFullSalvo(LoadedAmmo.EMPTY, 5), "空膛不得发射");
    }

    // ===== 源码契约 =====

    @Test
    void reloadRequiresPartBeforeAnyStateWrite() throws Exception {
        String body = methodBody(read("combat/ReloadHelper.java"),
                "public static void reloadTorpedoLauncher(");
        assertTrue(body.contains("hasTorpedoReloadEquipped(player, coreStack)"),
                "缺部件必须直接拒绝装填");
        assertTrue(body.contains("message.piranport.need_torpedo_reload"),
                "缺部件只给动作栏提示");
        int gate = body.indexOf("hasTorpedoReloadEquipped(player, coreStack)");
        int pendingWrite = body.indexOf("PENDING_TORPEDO_RELOAD.get(),");
        assertTrue(gate > 0 && pendingWrite > gate,
                "缺部件检查必须早于 PENDING / 冷却写入");
        assertFalse(body.contains("getAbilities().instabuild"),
                "创造模式不能绕过真实弹药校验");
        assertTrue(body.contains("hasTorpedoAmmoSource("),
                "最终校验必须走真实弹药来源判断");
    }

    @Test
    void manualReloadPayloadNoLongerUsesNoPart3000Ticks() throws Exception {
        String source = read("network/ManualReloadPayload.java");
        assertTrue(source.contains("ReloadHelper.TORPEDO_RELOAD_TICKS"),
                "R 键只使用有部件的合法读条时长");
        assertFalse(source.contains("3000,"), "无部件 3000 tick 长读条分支必须移除");
        assertFalse(source.contains("? 400 : 3000"), "不再按部件是否存在切换读条时长");
    }

    @Test
    void playerFirePathIsChamberOnly() throws Exception {
        String ship = read("item/ShipCoreCombat.java");
        assertTrue(ship.contains("fireTorpedosManualMode"));
        assertFalse(ship.contains("fireTorpedosInventoryMode"),
                "不再按强化分流到背包取弹发射");

        String fireSource = read("combat/torpedo/TorpedoFireStrategy.java");
        String body = methodBody(fireSource, "public static void fireTorpedosManualMode(");
        assertTrue(body.contains("hasFullSalvo(loaded, tubeCount)"),
                "发射前必须检查满膛");
        assertTrue(body.contains("PENDING_TORPEDO_RELOAD.get()"),
                "装填中必须拒绝发射");
        assertFalse(body.contains("instabuild"), "创造模式不能绕过装填状态");
        assertFalse(body.contains("findInfiniteTorpedo"), "发射不再从弹药库/背包凭空取弹");
        assertFalse(fireSource.contains("fireTorpedosInventoryMode"));
    }

    @Test
    void hudOnlyShowsTorpedoBarWithReloadPart() throws Exception {
        // 只锚方法签名、不断言可见性：hasReloadBar 现在是包内可见，
        // 供 WeaponReloadDecorator 复用同一套"是否该画装填条"判断。
        String body = methodBody(read("client/ReloadProgressHudLayer.java"),
                "boolean hasReloadBar(");
        assertTrue(body.contains("hasTorpedoReloadEquipped(player, core)"),
                "没有再装填部件时不画鱼雷读条");
    }

    @Test
    void settlementDoesNotDoubleConsumeWhenAlreadyFull() throws Exception {
        String body = methodBody(read("combat/data/WeaponReloadLifecycle.java"),
                "private static void finishOneTorpedoReload(");
        int fullChamberClear = body.indexOf("loaded.hasAmmo() && loaded.count() >= launcher.getTubeCount()");
        int consume = body.indexOf("s.shrink(take)");
        assertTrue(fullChamberClear > 0, "满膛时必须丢弃 pending");
        assertTrue(consume > fullChamberClear, "丢弃 pending 必须发生在扣弹之前");
    }

    @Test
    void facilityOutputClearsPendingState() throws Exception {
        String body = methodBody(read("block/entity/ReloadFacilityBlockEntity.java"),
                "public static void serverTick(");
        assertTrue(body.contains("result.remove(ModDataComponents.PENDING_TORPEDO_RELOAD.get())"),
                "设施产出不能带出 R 读条 pending");
        assertTrue(body.contains("result.remove(ModDataComponents.WEAPON_COOLDOWN.get())"),
                "设施产出不能带出旧冷却");
    }

    // ===== 工具 =====

    private static String read(String relative) throws Exception {
        return Files.readString(MAIN.resolve(relative));
    }

    /** 用大括号配对截出方法体，避免整文件误匹配。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "missing " + signature);
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "missing body for " + signature);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return source.substring(start, i + 1);
            }
        }
        fail("unclosed method " + signature);
        return "";
    }
}
