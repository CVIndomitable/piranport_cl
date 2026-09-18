package com.piranport.dungeon.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DungeonDataLoaderTest {
    private final DungeonDataLoader loader = new DungeonDataLoader();

    @AfterEach
    void clearRegistry() {
        DungeonRegistry.INSTANCE.load(Map.of(), Map.of(), Map.of());
    }

    @Test
    void bundledDungeonDataFormsAValidCompleteSnapshot() throws Exception {
        Path root = Path.of("src/main/resources/data/piranport/dungeon");
        Map<ResourceLocation, JsonElement> resources = new HashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                String id = root.relativize(path).toString().replace(".json", "");
                resources.put(ResourceLocation.fromNamespaceAndPath("piranport", id),
                        JsonParser.parseString(Files.readString(path)));
            }
        }
        loader.apply(resources, null, null);
        assertTrue(DungeonRegistry.INSTANCE.hasStage("1-1"));
        assertFalse(DungeonRegistry.INSTANCE.getSortedChapters().isEmpty());
    }

    @Test
    void invalidReferencePreservesPreviousSnapshotAndIndexes() {
        loader.apply(validResources(), null, null);
        StageData original = DungeonRegistry.INSTANCE.getStage("test");
        assertTrue(original.getReachableFrom("A").contains("B"));
        Map<ResourceLocation, JsonElement> invalid = validResources();
        invalid.get(id("stages/test")).getAsJsonObject().addProperty("start_node", "missing");

        assertThrows(IllegalArgumentException.class, () -> loader.apply(invalid, null, null));

        assertSame(original, DungeonRegistry.INSTANCE.getStage("test"));
        assertTrue(original.getReachableFrom("A").contains("B"));
    }

    @Test
    void malformedNestedRewardRejectsEntireReload() {
        loader.apply(validResources(), null, null);
        StageData original = DungeonRegistry.INSTANCE.getStage("test");
        Map<ResourceLocation, JsonElement> invalid = validResources();
        invalid.get(id("stages/test")).getAsJsonObject().add("first_clear_rewards",
                JsonParser.parseString("[{\"item\":\"minecraft:diamond\"}]"));

        assertThrows(IllegalArgumentException.class, () -> loader.apply(invalid, null, null));
        assertSame(original, DungeonRegistry.INSTANCE.getStage("test"));
    }

    @Test
    void duplicateLogicalIdsAreRejectedInsteadOfDependingOnIterationOrder() {
        Map<ResourceLocation, JsonElement> invalid = validResources();
        invalid.put(id("stages/duplicate"), invalid.get(id("stages/test")).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> loader.apply(invalid, null, null));
        assertFalse(DungeonRegistry.INSTANCE.hasStage("test"));
    }

    @Test
    void successfulReloadReplacesTopologyCache() {
        loader.apply(validResources(), null, null);
        DungeonRegistry.INSTANCE.getStage("test").getReachableFrom("A");
        Map<ResourceLocation, JsonElement> changed = validResources();
        changed.get(id("stages/test")).getAsJsonObject().add("edges", JsonParser.parseString("[]"));

        loader.apply(changed, null, null);

        assertTrue(DungeonRegistry.INSTANCE.getStage("test").getReachableFrom("A").isEmpty());
    }

    private static Map<ResourceLocation, JsonElement> validResources() {
        Map<ResourceLocation, JsonElement> resources = new HashMap<>();
        resources.put(id("chapters/test"), JsonParser.parseString("""
                {"chapter_id":"test","display_name":"test","stages":["test"]}
                """));
        resources.put(id("stages/test"), JsonParser.parseString("""
                {"stage_id":"test","chapter":"test","display_name":"test","start_node":"A",
                 "nodes":{"A":{"type":"resource"},"B":{"type":"resource"}},
                 "edges":[{"from":"A","to":"B"}],"boss_nodes":[]}
                """));
        return resources;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("piranport", path);
    }
}
