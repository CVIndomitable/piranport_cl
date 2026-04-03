package com.piranport.dungeon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads dungeon configuration JSON files from data packs.
 * Scans data/<namespace>/dungeon/chapters/, stages/, enemy_sets/.
 */
public class DungeonDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();

    public DungeonDataLoader() {
        super(GSON, "dungeon");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<String, ChapterData> chapters = new HashMap<>();
        Map<String, StageData> stages = new HashMap<>();
        Map<String, EnemySetData> enemySets = new HashMap<>();

        for (var entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement json = entry.getValue();
            String path = id.getPath(); // e.g. "chapters/chapter_1", "stages/1-1", "enemy_sets/enemy_set_1a"

            try {
                if (path.startsWith("chapters/")) {
                    ChapterData chapter = parseChapter(json.getAsJsonObject());
                    chapters.put(chapter.chapterId(), chapter);
                } else if (path.startsWith("stages/")) {
                    StageData stage = parseStage(json.getAsJsonObject());
                    stages.put(stage.stageId(), stage);
                } else if (path.startsWith("enemy_sets/")) {
                    EnemySetData enemySet = parseEnemySet(json.getAsJsonObject());
                    enemySets.put(enemySet.enemySetId(), enemySet);
                }
            } catch (Exception e) {
                PiranPort.LOGGER.warn("Failed to parse dungeon config {}: {}", id, e.getMessage());
            }
        }

        // Validate references
        for (var stage : stages.values()) {
            // Validate edges reference existing nodes
            for (var edge : stage.edges()) {
                if (!stage.nodes().containsKey(edge.from()) || !stage.nodes().containsKey(edge.to())) {
                    PiranPort.LOGGER.warn("Stage {} has edge referencing missing node: {} -> {}",
                            stage.stageId(), edge.from(), edge.to());
                }
            }
            // Validate enemy set references
            for (var node : stage.nodes().values()) {
                if ((node.type() == NodeData.NodeType.BATTLE || node.type() == NodeData.NodeType.BOSS)
                        && node.enemies() != null && !enemySets.containsKey(node.enemies())) {
                    PiranPort.LOGGER.warn("Stage {} node {} references missing enemy_set: {}",
                            stage.stageId(), node.nodeId(), node.enemies());
                }
            }
            // Validate start node exists
            if (!stage.nodes().containsKey(stage.startNode())) {
                PiranPort.LOGGER.warn("Stage {} start_node '{}' does not exist in nodes",
                        stage.stageId(), stage.startNode());
            }
        }

        DungeonRegistry.INSTANCE.load(chapters, stages, enemySets);
        PiranPort.LOGGER.info("Loaded dungeon data: {} chapters, {} stages, {} enemy sets",
                chapters.size(), stages.size(), enemySets.size());
    }

    private ChapterData parseChapter(JsonObject json) {
        String chapterId = json.get("chapter_id").getAsString();
        String displayName = json.get("display_name").getAsString();
        int sortOrder = json.has("sort_order") ? json.get("sort_order").getAsInt() : 0;
        List<String> stageList = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("stages")) {
            stageList.add(e.getAsString());
        }
        return new ChapterData(chapterId, displayName, sortOrder, List.copyOf(stageList));
    }

    private StageData parseStage(JsonObject json) {
        String stageId = json.get("stage_id").getAsString();
        String chapter = json.get("chapter").getAsString();
        String displayName = json.get("display_name").getAsString();
        String startNode = json.get("start_node").getAsString();

        // Parse nodes
        Map<String, NodeData> nodes = new HashMap<>();
        JsonObject nodesObj = json.getAsJsonObject("nodes");
        for (var nodeEntry : nodesObj.entrySet()) {
            String nodeId = nodeEntry.getKey();
            JsonObject nodeJson = nodeEntry.getValue().getAsJsonObject();
            nodes.put(nodeId, parseNode(nodeId, nodeJson));
        }

        // Parse edges
        List<StageData.EdgeData> edges = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("edges")) {
            JsonObject edgeJson = e.getAsJsonObject();
            edges.add(new StageData.EdgeData(
                    edgeJson.get("from").getAsString(),
                    edgeJson.get("to").getAsString()));
        }

        // Parse boss nodes
        List<String> bossNodes = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("boss_nodes")) {
            bossNodes.add(e.getAsString());
        }

        // Parse first clear rewards
        List<NodeData.RewardEntry> firstClearRewards = new ArrayList<>();
        if (json.has("first_clear_rewards")) {
            firstClearRewards = parseRewards(json.getAsJsonArray("first_clear_rewards"));
        }

        return new StageData(stageId, chapter, displayName,
                Map.copyOf(nodes), List.copyOf(edges), startNode,
                List.copyOf(bossNodes), List.copyOf(firstClearRewards));
    }

    private NodeData parseNode(String nodeId, JsonObject json) {
        NodeData.NodeType type = NodeData.NodeType.fromString(json.get("type").getAsString());
        String enemies = json.has("enemies") ? json.get("enemies").getAsString() : null;
        List<NodeData.RewardEntry> rewards = json.has("rewards")
                ? parseRewards(json.getAsJsonArray("rewards")) : List.of();
        List<NodeData.CostEntry> cost = json.has("cost")
                ? parseCosts(json.getAsJsonArray("cost")) : List.of();
        String costMessage = json.has("cost_message") ? json.get("cost_message").getAsString() : "";
        int displayX = json.has("display_x") ? json.get("display_x").getAsInt() : 0;
        int displayY = json.has("display_y") ? json.get("display_y").getAsInt() : 0;
        String script = json.has("script") ? json.get("script").getAsString() : null;

        return new NodeData(nodeId, type, enemies, List.copyOf(rewards),
                List.copyOf(cost), costMessage, displayX, displayY, script);
    }

    private List<NodeData.RewardEntry> parseRewards(JsonArray arr) {
        List<NodeData.RewardEntry> list = new ArrayList<>();
        for (JsonElement e : arr) {
            JsonObject obj = e.getAsJsonObject();
            String item = obj.get("item").getAsString();
            int count = obj.get("count").getAsInt();
            float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0f;
            list.add(new NodeData.RewardEntry(item, count, chance));
        }
        return list;
    }

    private List<NodeData.CostEntry> parseCosts(JsonArray arr) {
        List<NodeData.CostEntry> list = new ArrayList<>();
        for (JsonElement e : arr) {
            JsonObject obj = e.getAsJsonObject();
            list.add(new NodeData.CostEntry(
                    obj.get("item").getAsString(),
                    obj.get("count").getAsInt()));
        }
        return list;
    }

    private EnemySetData parseEnemySet(JsonObject json) {
        String id = json.get("enemy_set_id").getAsString();
        List<EnemySetData.SpawnEntry> spawnList = new ArrayList<>();
        for (JsonElement e : json.getAsJsonArray("spawn_list")) {
            JsonObject obj = e.getAsJsonObject();
            spawnList.add(new EnemySetData.SpawnEntry(
                    obj.get("entity").getAsString(),
                    obj.get("count").getAsInt()));
        }
        EnemySetData.SpawnEntry flagship = null;
        if (json.has("flagship")) {
            JsonObject fObj = json.getAsJsonObject("flagship");
            flagship = new EnemySetData.SpawnEntry(
                    fObj.get("entity").getAsString(),
                    fObj.get("count").getAsInt());
        }
        return new EnemySetData(id, List.copyOf(spawnList), flagship);
    }
}
