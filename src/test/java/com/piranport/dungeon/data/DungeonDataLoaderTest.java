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

    /**
     * 副本/20 §4.6 回归：terrain_type 写在关卡顶层，必须下发到该关每个节点。
     * 曾经的 bug 是加载器只读节点级字段，25 个关卡的顶层声明被静默丢弃，
     * 所有节点回退 T1_OCEAN——玩家打完七章只看得到同一种地形。
     */
    @Test
    void stageLevelTerrainTypePropagatesToEveryNode() {
        loader.apply(terrainResources("T5", ""), null, null);
        StageData stage = DungeonRegistry.INSTANCE.getStage("test");
        for (NodeData node : stage.nodes().values()) {
            assertEquals(TerrainType.T5_FORTRESS_REEF, node.terrainType(),
                    "节点 " + node.nodeId() + " 应继承关卡级地形");
        }
    }

    @Test
    void nodeLevelTerrainTypeOverridesStageLevel() {
        loader.apply(terrainResources("T3", ",\"terrain_type\":\"T2\""), null, null);
        StageData stage = DungeonRegistry.INSTANCE.getStage("test");
        assertEquals(TerrainType.T2_ISLAND_REEFS, stage.nodes().get("A").terrainType(),
                "节点级 terrain_type 应覆盖关卡级");
        assertEquals(TerrainType.T3_WRECKAGE, stage.nodes().get("B").terrainType(),
                "未写节点级 terrain_type 的节点仍继承关卡级");
    }

    /** 地形写错一个字不该静默变成大海：无法识别必须拒绝整批加载。 */
    @Test
    void unrecognizedStageTerrainTypeRejectsReload() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.apply(terrainResources("T7", ""), null, null));
        assertFalse(DungeonRegistry.INSTANCE.hasStage("test"));
    }

    private static Map<ResourceLocation, JsonElement> terrainResources(String stageTerrain, String nodeExtra) {
        Map<ResourceLocation, JsonElement> resources = new HashMap<>();
        resources.put(id("chapters/test"), JsonParser.parseString("""
                {"chapter_id":"test","display_name":"test","stages":["test"]}
                """));
        resources.put(id("stages/test"), JsonParser.parseString("""
                {"stage_id":"test","chapter":"test","display_name":"test","start_node":"A",
                 "terrain_type":"%s",
                 "nodes":{"A":{"type":"resource"%s},"B":{"type":"resource"}},
                 "edges":[{"from":"A","to":"B"}],"boss_nodes":[]}
                """.formatted(stageTerrain, nodeExtra)));
        return resources;
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
