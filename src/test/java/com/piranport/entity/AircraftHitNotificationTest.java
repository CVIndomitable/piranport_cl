package com.piranport.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 命中/击杀/未命中提示的武器名必须是载机物品名（如「飞行圆饼」），不能落到实体类型名（「战斗机」）。
 */
class AircraftHitNotificationTest {

    private static final Path AIRCRAFT_ENTITY =
            Path.of("src/main/java/com/piranport/entity/AircraftEntity.java");
    private static final Path SELF_DEFENSE =
            Path.of("src/main/java/com/piranport/entity/AircraftSelfDefense.java");
    private static final Path GUN =
            Path.of("src/main/java/com/piranport/aviation/combat/strategy/GunAttackStrategy.java");
    private static final Path TORPEDO =
            Path.of("src/main/java/com/piranport/aviation/combat/strategy/TorpedoAttackStrategy.java");

    @Test
    void displayNamePrefersAircraftItemName() throws Exception {
        String method = methodBody(Files.readString(AIRCRAFT_ENTITY), "public Component displayName()");
        int itemName = method.indexOf("originalStack.getHoverName()");
        int typeFallback = method.indexOf("return getDisplayName();");
        assertTrue(itemName >= 0,
                "displayName 必须优先返回载机物品名（含自定义名），供命中提示使用");
        assertTrue(typeFallback > itemName,
                "实体类型名只能作为旧存档兜底，不能优先于物品名");
    }

    @Test
    void allAircraftWeaponSourcesUseDisplayName() throws Exception {
        assertTrue(Files.readString(GUN).contains(
                        "bullet.setSourceAircraftName(context.displayName())"),
                "机枪命中的武器名必须取载机物品名");
        assertTrue(Files.readString(TORPEDO).contains(
                        "torpedo.setSourceAircraftName(context.displayName())"),
                "鱼雷命中的武器名必须取载机物品名");
        assertTrue(Files.readString(SELF_DEFENSE).contains(
                        "bullet.setSourceAircraftName(craft.displayName())"),
                "自卫机枪命中同样不能用实体类型名");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到方法：" + signature);
        return source.substring(start);
    }
}
