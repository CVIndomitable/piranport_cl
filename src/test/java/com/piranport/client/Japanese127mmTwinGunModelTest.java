package com.piranport.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Japanese127mmTwinGunModelTest {

    @Test
    void handheldAndMountedModelsKeepMinecraftScaleDiscipline() throws Exception {
        JsonObject mounted = load("japanese_127mm_twin_gun_mounted");
        JsonObject held = load("japanese_127mm_twin_gun_held");

        assertTrue(mounted.getAsJsonArray("elements").size() <= 12);
        assertTrue(held.getAsJsonArray("elements").size() <= 12);
        assertMinimumElementSize(mounted);
        assertMinimumElementSize(held);
        assertTrue(elementNames(mounted).anyMatch("training ring"::equals));
        assertTrue(elementNames(held).anyMatch("hand grip"::equals));
    }

    @Test
    void loadedModelsOnlySwapTheMuzzleTexture() throws Exception {
        assertEquals("piranport:item/japanese_127mm_twin_gun_mounted",
                load("japanese_127mm_twin_gun_mounted_loaded").get("parent").getAsString());
        assertEquals("piranport:item/japanese_127mm_twin_gun_held",
                load("japanese_127mm_twin_gun_held_loaded").get("parent").getAsString());
    }

    private static java.util.stream.Stream<String> elementNames(JsonObject model) {
        return model.getAsJsonArray("elements").asList().stream()
                .map(element -> element.getAsJsonObject().get("name").getAsString());
    }

    private static void assertMinimumElementSize(JsonObject model) {
        model.getAsJsonArray("elements").forEach(raw -> {
            JsonObject element = raw.getAsJsonObject();
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            for (int axis = 0; axis < 3; axis++) {
                double size = to.get(axis).getAsDouble() - from.get(axis).getAsDouble();
                assertTrue(size >= 1.0, element.get("name").getAsString() + " 含小于 1px 的几何体");
            }
        });
    }

    private static JsonObject load(String name) throws Exception {
        String path = "/assets/piranport/models/item/" + name + ".json";
        try (var stream = Japanese127mmTwinGunModelTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "缺少模型 " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
