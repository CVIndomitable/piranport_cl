package com.piranport.dungeon.block;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 书台纹路三态与撤钥匙锁定判定（副本/17 §3.2）。纯逻辑，不碰注册表。 */
class LecternPatternTest {

    @Test
    void noKeyIsWhite() {
        var s = LecternPattern.status(false, true, true, 0);
        assertEquals(LecternPattern.Status.EMPTY, s);
        assertEquals(LecternPattern.WHITE, LecternPattern.of(s));
    }

    @Test
    void buildingIsRedAndLocksKey() {
        var s = LecternPattern.status(true, true, false, 0);
        assertEquals(LecternPattern.Status.BUILDING, s);
        assertEquals(LecternPattern.RED, LecternPattern.of(s));
        assertFalse(LecternPattern.canWithdraw(s), "建造中钥匙必须锁定");
    }

    @Test
    void fullIsRedButKeyWithdrawable() {
        var s = LecternPattern.status(true, true, true, LecternPattern.MAX_PLAYERS);
        assertEquals(LecternPattern.Status.FULL, s);
        assertEquals(LecternPattern.RED, LecternPattern.of(s));
        assertTrue(LecternPattern.canWithdraw(s));
    }

    @Test
    void builtWithRoomIsGreen() {
        for (int present = 0; present < LecternPattern.MAX_PLAYERS; present++) {
            var s = LecternPattern.status(true, true, true, present);
            assertEquals(LecternPattern.Status.READY, s);
            assertEquals(LecternPattern.GREEN, LecternPattern.of(s));
            assertTrue(LecternPattern.canWithdraw(s));
        }
    }

    @Test
    void missingInstanceIsRedAndWithdrawable() {
        var s = LecternPattern.status(true, false, false, 0);
        assertEquals(LecternPattern.Status.INVALID, s);
        assertEquals(LecternPattern.RED, LecternPattern.of(s));
        assertTrue(LecternPattern.canWithdraw(s), "失效钥匙必须能撤下，否则书台被永久占用");
    }

    @Test
    void onlyGreenAllowsEntry() {
        for (var s : LecternPattern.Status.values()) {
            assertEquals(s == LecternPattern.Status.READY, LecternPattern.of(s) == LecternPattern.GREEN, s.name());
        }
    }

    @Test
    void chartFaceIsTintedForBlockColor() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(
                "/assets/piranport/models/block/dungeon_lectern.json")) {
            assertNotNull(in);
            JsonObject model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            boolean tinted = false;
            for (JsonElement e : model.getAsJsonArray("elements")) {
                for (var face : e.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                    JsonObject f = face.getValue().getAsJsonObject();
                    if ("#chart".equals(f.get("texture").getAsString()) && f.has("tintindex")) {
                        assertEquals(0, f.get("tintindex").getAsInt());
                        tinted = true;
                    }
                }
            }
            assertTrue(tinted, "纹路面必须带 tintindex 0，BlockColor 才能染白/红/绿");
        }
    }
}
