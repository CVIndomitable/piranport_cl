package com.piranport.npc.deepocean;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定《深海/01》§六 鱼雷装填表：各舰种 JSON 的 torpedo_cooldown 必须与策划定稿一致，
 * 且不低于 90 秒（1800 tick）下限。改 JSON 时若误配，这里先红。
 */
class DeepOceanTorpedoReloadTableTest {

    private static final Path DIR = Path.of("src/main/resources/data/piranport/npc/deep_ocean");

    private static int cooldownOf(String file) throws IOException {
        JsonObject obj = JsonParser.parseString(Files.readString(DIR.resolve(file))).getAsJsonObject();
        return DeepOceanEntityData.fromJson(obj).torpedoCooldown();
    }

    @Test
    void torpedoReloadTableMatchesPolicy() throws IOException {
        assertEquals(1920, cooldownOf("destroyer.json"), "驱逐 1920");
        assertEquals(2160, cooldownOf("light_cruiser.json"), "轻巡 2160");
        assertEquals(2160, cooldownOf("heavy_cruiser.json"), "重巡 2160");
        assertEquals(2400, cooldownOf("submarine.json"), "潜艇低级 2400");
        assertEquals(1800, cooldownOf("torpedo_cruiser.json"), "雷巡每组 1800");
        assertEquals(2160, cooldownOf("boss.json"), "旗舰/Boss 2160");
    }

    @Test
    void everyTorpedoShipMeetsReloadFloor() throws IOException {
        String[] files = {"destroyer.json", "light_cruiser.json", "heavy_cruiser.json",
                "submarine.json", "torpedo_cruiser.json", "boss.json"};
        for (String f : files) {
            assertTrue(cooldownOf(f) >= AbstractDeepOceanEntity.TORPEDO_RELOAD_FLOOR_TICKS,
                    f + " 装填低于 90 秒下限");
        }
    }
}
