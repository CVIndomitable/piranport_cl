package com.piranport.dungeon.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.piranport.PiranPort;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    protected Map<ResourceLocation, JsonElement> prepare(ResourceManager resourceManager,
                                                          ProfilerFiller profiler) {
        // 原版扫描器会记录语法错误后跳过文件；这里必须拒绝整批重载，防止旧定义悄悄消失。
        FileToIdConverter converter = FileToIdConverter.json("dungeon");
        Map<ResourceLocation, JsonElement> prepared = new HashMap<>();
        for (var entry : converter.listMatchingResources(resourceManager).entrySet()) {
            ResourceLocation id = converter.fileToId(entry.getKey());
            String path = id.getPath();
            if (!path.startsWith("chapters/") && !path.startsWith("stages/")
                    && !path.startsWith("enemy_sets/")) continue;
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonElement json = GsonHelper.fromJson(GSON, reader, JsonElement.class);
                if (json == null || !json.isJsonObject()) {
                    throw new IllegalArgumentException("配置根节点必须是对象");
                }
                prepared.put(id, json);
            } catch (IOException | RuntimeException e) {
                throw new IllegalArgumentException("无法读取副本配置 " + entry.getKey(), e);
            }
        }
        return prepared;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        List<String> errors = new ArrayList<>();
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
                    putUnique(chapters, chapter.chapterId(), chapter);
                } else if (path.startsWith("stages/")) {
                    StageData stage = parseStage(json.getAsJsonObject());
                    putUnique(stages, stage.stageId(), stage);
                } else if (path.startsWith("enemy_sets/")) {
                    EnemySetData enemySet = parseEnemySet(json.getAsJsonObject());
                    putUnique(enemySets, enemySet.enemySetId(), enemySet);
                }
            } catch (Exception e) {
                errors.add(id + ": " + e.getMessage());
            }
        }

        errors.addAll(DungeonDataValidator.validate(chapters, stages, enemySets));
        if (!errors.isEmpty()) {
            // 在提交注册表之前拒绝整批数据，重载失败时上一份有效配置仍可继续使用。
            throw new IllegalArgumentException("副本配置校验失败：\n" + String.join("\n", errors));
        }

        DungeonRegistry.INSTANCE.load(chapters, stages, enemySets);
        PiranPort.LOGGER.info("Loaded dungeon data: {} chapters, {} stages, {} enemy sets",
                chapters.size(), stages.size(), enemySets.size());
    }

    private static <T> void putUnique(Map<String, T> entries, String id, T value) {
        if (id.isBlank()) throw new IllegalArgumentException("配置 ID 不能为空");
        if (entries.putIfAbsent(id, value) != null) {
            throw new IllegalArgumentException("重复配置 ID: " + id);
        }
    }

    private void requireField(JsonObject json, String field, String context) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in " + context);
        }
    }

    private ChapterData parseChapter(JsonObject json) {
        requireField(json, "chapter_id", "chapter");
        requireField(json, "display_name", "chapter");
        requireField(json, "stages", "chapter");
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
        requireField(json, "stage_id", "stage");
        requireField(json, "chapter", "stage");
        requireField(json, "display_name", "stage");
        requireField(json, "start_node", "stage");
        requireField(json, "nodes", "stage");
        requireField(json, "edges", "stage");
        requireField(json, "boss_nodes", "stage");
        String stageId = json.get("stage_id").getAsString();
        String chapter = json.get("chapter").getAsString();
        String displayName = json.get("display_name").getAsString();
        String startNode = json.get("start_node").getAsString();

        // 副本/20 §4.6：terrain_type 是**关卡级**属性，写在同一关所有节点的公共字段位置上。
        // 节点级 terrain_type 保留为可选覆盖（个别节点换地形时用），未写则继承关卡值。
        // 历史 bug：这里曾只读节点级，导致 25 个关卡写在顶层的 terrain_type 全部被忽略，
        // 所有节点回退 T1_OCEAN——玩家打完七章看到的是同一种地形。
        TerrainType stageTerrain = TerrainType.T1_OCEAN;
        if (json.has("terrain_type") && !json.get("terrain_type").isJsonNull()) {
            stageTerrain = TerrainType.parseStrict(json.get("terrain_type").getAsString());
            if (stageTerrain == null) {
                throw new IllegalArgumentException("未知地形类型 '"
                        + json.get("terrain_type").getAsString() + "'，关卡 " + stageId);
            }
        }

        // Parse nodes
        Map<String, NodeData> nodes = new HashMap<>();
        JsonObject nodesObj = json.getAsJsonObject("nodes");
        for (var nodeEntry : nodesObj.entrySet()) {
            String nodeId = nodeEntry.getKey();
            JsonObject nodeJson = nodeEntry.getValue().getAsJsonObject();
            nodes.put(nodeId, parseNode(nodeId, nodeJson, stageTerrain));
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

        // Parse checkpoints (整合版 §3.2：每关 1-2 个、Boss 节点前必设)
        List<CheckpointData> checkpoints = new ArrayList<>();
        if (json.has("checkpoints")) {
            for (JsonElement ce : json.getAsJsonArray("checkpoints")) {
                JsonObject cpObj = ce.getAsJsonObject();
                if (!cpObj.has("id") || cpObj.get("id").isJsonNull()
                        || !cpObj.has("node_id") || cpObj.get("node_id").isJsonNull()) {
                    throw new IllegalArgumentException("记录点缺少 id/node_id，关卡 " + stageId);
                }
                int posX = cpObj.has("pos_x") ? cpObj.get("pos_x").getAsInt() : 0;
                int posY = cpObj.has("pos_y") ? cpObj.get("pos_y").getAsInt() : 64;
                int posZ = cpObj.has("pos_z") ? cpObj.get("pos_z").getAsInt() : 0;
                String facing = cpObj.has("facing") ? cpObj.get("facing").getAsString() : "south";
                String unlock = cpObj.has("unlock_condition")
                        ? cpObj.get("unlock_condition").getAsString()
                        : CheckpointData.UNLOCK_ANY;
                checkpoints.add(new CheckpointData(
                        cpObj.get("id").getAsString(),
                        cpObj.get("node_id").getAsString(),
                        posX, posY, posZ, facing, unlock));
            }
        }

        // Parse victory conditions (整合版 §2.4 关卡公式 7×5×5)
        Set<VictoryCondition> victoryConditions = new HashSet<>();
        if (json.has("victory_conditions")) {
            for (JsonElement vce : json.getAsJsonArray("victory_conditions")) {
                String v = vce.getAsString();
                VictoryCondition cond = VictoryCondition.fromString(v);
                if (cond == null) {
                    // 现有关卡包含尚未映射到枚举的扩展任务名，沿用警告以保持当前关卡兼容。
                    PiranPort.LOGGER.warn("Unknown victory_condition '{}' in stage {}", v, stageId);
                } else {
                    victoryConditions.add(cond);
                }
            }
        }

        // 决策/副本/12：关卡级默认值（若未指定则取所有节点的并集）
        SceneData stageScene = aggregateStageScene(nodes);
        Set<CombatRestriction> stageRestrictions = aggregateStageRestrictions(nodes);

        // 决策/副本/12：业务判定字段（顶层可选）
        StageData.VictoryObjectives objectives = parseVictoryObjectives(json);

        return new StageData(stageId, chapter, displayName,
                Map.copyOf(nodes), List.copyOf(edges), startNode,
                List.copyOf(bossNodes), List.copyOf(firstClearRewards),
                List.copyOf(checkpoints),
                Set.copyOf(victoryConditions),
                stageScene, Set.copyOf(stageRestrictions), objectives);
    }

    /**
     * 关卡级场景默认值：若 JSON 顶层未指定 sceneData，按节点列表首个有效场景作为默认值。
     */
    private SceneData aggregateStageScene(Map<String, NodeData> nodes) {
        for (NodeData n : nodes.values()) {
            if (n.scene() != null) return n.scene();
        }
        return SceneData.DAY;
    }

    /**
     * 关卡级战斗限制默认值：聚合所有节点的限制并集。
     */
    private Set<CombatRestriction> aggregateStageRestrictions(Map<String, NodeData> nodes) {
        Set<CombatRestriction> all = new HashSet<>();
        for (NodeData n : nodes.values()) {
            all.addAll(n.restrictions());
        }
        return all;
    }

    /**
     * 解析关卡业务判定字段（决策 §副本/12）：
     * 运输目标点偏移、护航目标、夺旗半径/计时等。
     */
    private StageData.VictoryObjectives parseVictoryObjectives(JsonObject json) {
        if (!json.has("victory_objectives")) {
            return StageData.VictoryObjectives.EMPTY;
        }
        JsonObject obj = json.getAsJsonObject("victory_objectives");
        boolean requireAll = obj.has("require_all_nodes_cleared")
                && obj.get("require_all_nodes_cleared").getAsBoolean();
        int surviveSec = obj.has("survive_seconds") ? obj.get("survive_seconds").getAsInt() : 0;
        String escortKey = obj.has("escort_entity") && !obj.get("escort_entity").isJsonNull()
                ? obj.get("escort_entity").getAsString() : null;
        int[] reachOffset = null;
        if (obj.has("reach_point_offset") && obj.get("reach_point_offset").isJsonArray()) {
            JsonArray arr = obj.getAsJsonArray("reach_point_offset");
            if (arr.size() == 3) {
                reachOffset = new int[]{
                        arr.get(0).getAsInt(),
                        arr.get(1).getAsInt(),
                        arr.get(2).getAsInt()
                };
            }
        }
        int capRadius = obj.has("capture_radius") ? obj.get("capture_radius").getAsInt() : 0;
        int capHold = obj.has("capture_hold_seconds") ? obj.get("capture_hold_seconds").getAsInt() : 0;
        return new StageData.VictoryObjectives(requireAll, surviveSec,
                escortKey, reachOffset, capRadius, capHold);
    }

    private NodeData parseNode(String nodeId, JsonObject json, TerrainType stageTerrain) {
        requireField(json, "type", "node " + nodeId);
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
        // 节点级 terrain_type 是可选覆盖；未写时继承关卡级（见 parseStage）。
        TerrainType terrainType = stageTerrain;
        if (json.has("terrain_type") && !json.get("terrain_type").isJsonNull()) {
            terrainType = TerrainType.parseStrict(json.get("terrain_type").getAsString());
            if (terrainType == null) {
                throw new IllegalArgumentException("未知地形类型 '"
                        + json.get("terrain_type").getAsString() + "'，节点 " + nodeId);
            }
        }

        // 整合版 §2.4 战斗限制（5 种）
        Set<CombatRestriction> restrictions = new HashSet<>();
        if (json.has("restrictions")) {
            for (JsonElement re : json.getAsJsonArray("restrictions")) {
                String r = re.getAsString();
                CombatRestriction cr = CombatRestriction.fromString(r);
                if (cr == null) {
                    throw new IllegalArgumentException("未知战斗限制 " + r + "，节点 " + nodeId);
                } else {
                    restrictions.add(cr);
                }
            }
        }

        // 整合版 §2.4 场景（5 种：白天/夜战/雷雨/大雾/烈日）
        SceneData scene = SceneData.DAY;
        if (json.has("scene")) {
            SceneData parsed = SceneData.fromString(json.get("scene").getAsString());
            if (parsed != null) {
                scene = parsed;
            } else {
                throw new IllegalArgumentException("未知场景，节点 " + nodeId);
            }
        }

        return new NodeData(nodeId, type, enemies, List.copyOf(rewards),
                List.copyOf(cost), costMessage, displayX, displayY, script,
                terrainType, Set.copyOf(restrictions), scene);
    }

    private List<NodeData.RewardEntry> parseRewards(JsonArray arr) {
        List<NodeData.RewardEntry> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            JsonObject obj = e.getAsJsonObject();
            // 奖励缺字段必须拒绝重载，避免通关后才发现奖励丢失。
            if (!obj.has("item") || obj.get("item").isJsonNull()
                    || !obj.has("count") || obj.get("count").isJsonNull()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            String item = obj.get("item").getAsString();
            int count = obj.get("count").getAsInt();
            float chance = obj.has("chance") ? obj.get("chance").getAsFloat() : 1.0f;
            list.add(new NodeData.RewardEntry(item, count, chance));
        }
        return list;
    }

    private List<NodeData.CostEntry> parseCosts(JsonArray arr) {
        List<NodeData.CostEntry> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonElement e = arr.get(i);
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            JsonObject obj = e.getAsJsonObject();
            if (!obj.has("item") || obj.get("item").isJsonNull()
                    || !obj.has("count") || obj.get("count").isJsonNull()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            list.add(new NodeData.CostEntry(
                    obj.get("item").getAsString(),
                    obj.get("count").getAsInt()));
        }
        return list;
    }

    private EnemySetData parseEnemySet(JsonObject json) {
        requireField(json, "enemy_set_id", "enemy_set");
        requireField(json, "spawn_list", "enemy_set");
        String id = json.get("enemy_set_id").getAsString();
        List<EnemySetData.SpawnEntry> spawnList = new ArrayList<>();
        for (int i = 0; i < json.getAsJsonArray("spawn_list").size(); i++) {
            JsonElement e = json.getAsJsonArray("spawn_list").get(i);
            if (!e.isJsonObject()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            JsonObject obj = e.getAsJsonObject();
            if (!obj.has("entity") || obj.get("entity").isJsonNull()
                    || !obj.has("count") || obj.get("count").isJsonNull()) {
                throw new IllegalArgumentException("嵌套条目缺少必要字段，序号 " + i);
            }
            spawnList.add(new EnemySetData.SpawnEntry(
                    obj.get("entity").getAsString(),
                    obj.get("count").getAsInt()));
        }
        EnemySetData.SpawnEntry flagship = null;
        if (json.has("flagship") && !json.get("flagship").isJsonNull()) {
            JsonObject fObj = json.getAsJsonObject("flagship");
            if (fObj.has("entity") && !fObj.get("entity").isJsonNull()
                    && fObj.has("count") && !fObj.get("count").isJsonNull()) {
                flagship = new EnemySetData.SpawnEntry(
                        fObj.get("entity").getAsString(),
                        fObj.get("count").getAsInt());
            } else {
                throw new IllegalArgumentException("旗舰缺少 entity/count，敌人组 " + id);
            }
        }
        String formation = json.has("formation") && !json.get("formation").isJsonNull()
                ? json.get("formation").getAsString() : null;
        return new EnemySetData(id, List.copyOf(spawnList), flagship, formation);
    }
}
