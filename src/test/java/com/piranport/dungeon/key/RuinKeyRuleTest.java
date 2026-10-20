package com.piranport.dungeon.key;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 书台遗迹开箱选钥匙（副本/17 §二）：没进过副本 → 1-1，进过 → 金猫猫。 */
class RuinKeyRuleTest {

    private static final Pattern STAGE_ID = Pattern.compile("\"piranport:dungeon_stage_id\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void newcomerGetsFirstStage() {
        assertEquals("1-1", RuinKeyRule.stageFor(false));
    }

    @Test
    void veteranGetsEventStage() {
        assertEquals("goldencatcat", RuinKeyRule.stageFor(true));
    }

    /**
     * 战利品表必须与 RuinKeyRule 同口径：仅"进过副本"条件分流的钥匙池，按标记给对应关卡钥匙。
     * 空白书台池是无条件池，不参与分流，因此按条件过滤后再断言。
     */
    @Test
    void lootTableMatchesRule() throws Exception {
        JsonObject table = read("/data/piranport/loot_table/chests/portal_ruin.json");
        JsonArray pools = table.getAsJsonArray("pools");
        int keyPools = 0;
        for (JsonElement p : pools) {
            JsonObject pool = p.getAsJsonObject();
            if (!pool.has("conditions")) continue;
            JsonObject cond = pool.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("piranport:entered_dungeon", cond.get("condition").getAsString());
            boolean entered = cond.get("entered").getAsBoolean();
            JsonObject entry = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
            String stage = entry.getAsJsonArray("functions").get(0).getAsJsonObject()
                    .getAsJsonObject("components").get("piranport:dungeon_stage_id").getAsString();
            assertEquals(RuinKeyRule.stageFor(entered), stage);
            keyPools++;
        }
        assertEquals(2, keyPools);
    }

    /**
     * 补给站 / 前哨站 / 深海基地不掉主线钥匙（dungeon_key_ch*）与传送门激活核心。
     * 深海基地的活动钥匙（delusion_1 / hell_1）属于活动关卡，《海图/01》与《副本/17》允许掉落，
     * 因此只校验其关卡标识仅为这两个活动关，不把整个 dungeon_key 物品名一刀切。
     */
    @Test
    void otherRuinsDropNoMainlineKey() throws Exception {
        Map<String, Set<String>> allowedStages = Map.of(
                "supply_depot", Set.of(),
                "outpost", Set.of(),
                "abyssal_base", Set.of(),
                "abyssal_base_rare", Set.of("delusion_1", "hell_1"));
        for (Map.Entry<String, Set<String>> e : allowedStages.entrySet()) {
            String name = e.getKey();
            try (InputStream in = getClass().getResourceAsStream("/data/piranport/loot_table/chests/" + name + ".json")) {
                assertNotNull(in, name);
                String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(text.contains("dungeon_key_ch"), name + " 不应再掉主线钥匙");
                assertFalse(text.contains("portal_activation_core"), name + " 不应再掉传送门激活核心");
                Matcher m = STAGE_ID.matcher(text);
                while (m.find()) {
                    assertTrue(e.getValue().contains(m.group(1)),
                            name + " 掉了不允许的关卡钥匙: " + m.group(1));
                }
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
