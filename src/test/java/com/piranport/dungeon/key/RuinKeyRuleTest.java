package com.piranport.dungeon.key;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** 书台遗迹开箱选钥匙（副本/17 §二）：没进过副本 → 1-1，进过 → 金猫猫。 */
class RuinKeyRuleTest {

    @Test
    void newcomerGetsFirstStage() {
        assertEquals("1-1", RuinKeyRule.stageFor(false));
    }

    @Test
    void veteranGetsEventStage() {
        assertEquals("goldencatcat", RuinKeyRule.stageFor(true));
    }

    /** 战利品表必须与 RuinKeyRule 同口径：两个互斥池分别按标记给对应关卡钥匙。 */
    @Test
    void lootTableMatchesRule() throws Exception {
        JsonObject table = read("/data/piranport/loot_table/chests/portal_ruin.json");
        JsonArray pools = table.getAsJsonArray("pools");
        assertEquals(2, pools.size());
        for (JsonElement p : pools) {
            JsonObject pool = p.getAsJsonObject();
            JsonObject cond = pool.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("piranport:entered_dungeon", cond.get("condition").getAsString());
            boolean entered = cond.get("entered").getAsBoolean();
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            String stage = entry.getAsJsonArray("functions").get(0).getAsJsonObject()
                    .getAsJsonObject("components").get("piranport:dungeon_stage_id").getAsString();
            assertEquals(RuinKeyRule.stageFor(entered), stage);
        }
    }

    /** 补给站 / 前哨站 / 深海基地不再掉主线钥匙或传送门激活核心。 */
    @Test
    void otherRuinsDropNoMainlineKey() throws Exception {
        for (String name : new String[]{"supply_depot", "outpost", "abyssal_base"}) {
            try (InputStream in = getClass().getResourceAsStream("/data/piranport/loot_table/chests/" + name + ".json")) {
                if (in == null) continue;
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(text.contains("dungeon_key"), name + " 不应再掉副本钥匙");
                assertFalse(text.contains("portal_activation_core"), name + " 不应再掉传送门激活核心");
            }
        }
    }

    private JsonObject read(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertNotNull(in, path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
