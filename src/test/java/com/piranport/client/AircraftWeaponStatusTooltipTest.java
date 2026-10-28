package com.piranport.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 舰载机只能有一条状态行（未装填 → 装填中 → 已装填），图标谓词必须与 tooltip 同源。
 *
 * <p>回归背景：图标用「油 > 0」、tooltip 用 {@code AircraftSortieReadiness}，出现
 * 「图标已装填、文字未装填」；同时 {@code appendWeaponCooldownTooltip} 又追加一行，
 * 让同一架飞机同时显示「未装填」和「已装填」。
 */
class AircraftWeaponStatusTooltipTest {

    private static final Path CLIENT_ITEM_HOOKS =
            Path.of("src/main/java/com/piranport/client/ClientItemHooks.java");
    private static final Path AIRCRAFT_ITEM =
            Path.of("src/main/java/com/piranport/item/AircraftItem.java");
    private static final Path CLIENT_MOD_EVENTS =
            Path.of("src/main/java/com/piranport/client/ClientModEvents.java");
    private static final Path RESUPPLY =
            Path.of("src/main/java/com/piranport/entity/AircraftResupply.java");

    @Test
    void aircraftTooltipHasSingleStatusLine() throws Exception {
        String hooks = methodBody(Files.readString(CLIENT_ITEM_HOOKS),
                "public void appendWeaponCooldownTooltip(");
        int aircraftReturn = hooks.indexOf(
                "if (stack.getItem() instanceof com.piranport.item.AircraftItem) return;");
        int firstLineOutput = hooks.indexOf("int weaponSlot = -1;");
        assertTrue(aircraftReturn >= 0,
                "appendWeaponCooldownTooltip 必须对舰载机提前放行，否则会追加第二条状态行");
        assertTrue(firstLineOutput > aircraftReturn,
                "提前放行必须发生在读取槽位/输出状态之前");

        String tooltip = methodBody(Files.readString(AIRCRAFT_ITEM), "public void appendHoverText(");
        assertTrue(tooltip.contains("AircraftSortieReadiness.isReady(state, definition, stats)"),
                "状态行必须按出击准备判定，而不是只看油量");
        assertTrue(tooltip.contains("cooldown.isOnCooldown(level.getGameTime())")
                        && tooltip.contains("tooltip.piranport.weapon_reloading"),
                "读条期间应显示「装填中」，且仍只占一行");
        assertFalse(tooltip.contains("ShipCoreCombat.appendWeaponCooldownTooltip("),
                "状态行只能由 AircraftItem 输出一次，不能再调用通用装填提示追加第二行");
    }

    @Test
    void aircraftIconUsesSameReadinessAsTooltip() throws Exception {
        String predicate = methodBody(Files.readString(CLIENT_MOD_EVENTS),
                "if (entry.get() instanceof com.piranport.item.AircraftItem) {");
        assertTrue(predicate.contains("AircraftSortieReadiness.isReady("),
                "图标两态必须与 tooltip 共用 AircraftSortieReadiness");
        assertFalse(predicate.contains("currentFuel() > 0"),
                "只看油 > 0 会把「满油但没挂载的战斗机/攻击机」显示成可出击");
    }

    @Test
    void recallReturnsExactlyOneUnfueledAircraft() throws Exception {
        String resupply = Files.readString(RESUPPLY);
        String recall = methodBody(resupply, "public static void recallAndRemove(");
        assertTrue(recall.contains("if (craft.isRemoved()) return;"),
                "返航到达与主动召回同 tick 各调一次时，实体已移除就不能再返还第二份物品");

        String build = methodBody(resupply, "static ItemStack buildReturnStack(");
        assertTrue(build.contains("withCurrentFuel(0)"),
                "召回返还的飞机必须燃油归零（下一次出击仍要按 R 装填）");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法：" + signature);
        return source.substring(start);
    }
}
