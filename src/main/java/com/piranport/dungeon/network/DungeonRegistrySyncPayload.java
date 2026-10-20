package com.piranport.dungeon.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.piranport.PiranPort;
import com.piranport.dungeon.data.BranchRule;
import com.piranport.dungeon.data.CheckpointData;
import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.CombatRestriction;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.TerrainType;
import com.piranport.dungeon.data.SceneData;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * S2C: Syncs dungeon registry (chapters + stages) to the client on login.
 * Uses JSON string encoding to avoid complex nested StreamCodec.
 *
 * <p>Sender-side: the JSON is generated once by {@link #fromRegistry()} and stored on
 * the payload as a string. The receiving side eagerly parses the string on the netty
 * IO thread (inside {@link #STREAM_CODEC}'s decoder) so the heavy Gson parse never
 * blocks the client game thread when {@code handle} runs.</p>
 */
public record DungeonRegistrySyncPayload(String jsonData,
                                          Map<String, ChapterData> parsedChapters,
                                          Map<String, StageData> parsedStages)
        implements CustomPacketPayload {

    /** Hard cap on the JSON payload size — protects against an over-sized configured datapack. */
    private static final int MAX_JSON_BYTES = 4 * 1024 * 1024; // 4 MiB

    public static final Type<DungeonRegistrySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_registry_sync"));

    public DungeonRegistrySyncPayload(String jsonData) {
        this(jsonData, null, null);
    }

    public static final StreamCodec<ByteBuf, DungeonRegistrySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                byte[] bytes = p.jsonData().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                if (bytes.length > MAX_JSON_BYTES) {
                    throw new io.netty.handler.codec.EncoderException(
                            "Dungeon registry payload too large: " + bytes.length + " bytes");
                }
                ByteBufCodecs.VAR_INT.encode(buf, bytes.length);
                buf.writeBytes(bytes);
            },
            buf -> {
                int len = ByteBufCodecs.VAR_INT.decode(buf);
                if (len < 0 || len > MAX_JSON_BYTES) {
                    throw new io.netty.handler.codec.DecoderException(
                            "Dungeon registry payload size out of range: " + len);
                }
                byte[] bytes = new byte[len];
                buf.readBytes(bytes);
                String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                // Parse on the netty IO thread to keep the client game thread responsive.
                ParsedRegistry parsed = parseJson(json);
                return new DungeonRegistrySyncPayload(json, parsed.chapters, parsed.stages);
            }
    );

    private record ParsedRegistry(Map<String, ChapterData> chapters, Map<String, StageData> stages) {}

    private static ParsedRegistry parseJson(String json) {
        Map<String, ChapterData> chapters = new HashMap<>();
        Map<String, StageData> stages = new HashMap<>();
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("chapters")) {
                JsonObject obj = el.getAsJsonObject();
                String chapterId = obj.get("chapterId").getAsString();
                List<String> stageList = new ArrayList<>();
                for (JsonElement s : obj.getAsJsonArray("stages")) stageList.add(s.getAsString());
                chapters.put(chapterId, new ChapterData(
                        chapterId,
                        obj.get("displayName").getAsString(),
                        obj.get("sortOrder").getAsInt(),
                        List.copyOf(stageList)));
            }
            JsonObject stagesObj = root.getAsJsonObject("stages");
            for (var entry : stagesObj.entrySet()) {
                JsonObject sObj = entry.getValue().getAsJsonObject();
                String stageId = sObj.get("stageId").getAsString();
                Map<String, NodeData> nodes = new HashMap<>();
                JsonObject nodesObj = sObj.getAsJsonObject("nodes");
                for (var ne : nodesObj.entrySet()) {
                    JsonObject nObj = ne.getValue().getAsJsonObject();
                    List<NodeData.RewardEntry> rewards = new ArrayList<>();
                    if (nObj.has("rewards")) {
                        for (JsonElement r : nObj.getAsJsonArray("rewards")) {
                            JsonObject rObj = r.getAsJsonObject();
                            rewards.add(new NodeData.RewardEntry(
                                    rObj.get("item").getAsString(),
                                    rObj.get("count").getAsInt(),
                                    rObj.get("chance").getAsFloat()));
                        }
                    }
                    List<NodeData.CostEntry> costs = new ArrayList<>();
                    if (nObj.has("cost")) {
                        for (JsonElement c : nObj.getAsJsonArray("cost")) {
                            JsonObject cObj = c.getAsJsonObject();
                            costs.add(new NodeData.CostEntry(
                                    cObj.get("item").getAsString(),
                                    cObj.get("count").getAsInt()));
                        }
                    }
                    nodes.put(ne.getKey(), readNode(nObj, rewards, costs));
                }
                List<StageData.EdgeData> edges = new ArrayList<>();
                for (JsonElement e : sObj.getAsJsonArray("edges")) {
                    JsonObject eObj = e.getAsJsonObject();
                    edges.add(new StageData.EdgeData(
                            eObj.get("from").getAsString(),
                            eObj.get("to").getAsString()));
                }
                List<String> bossNodes = new ArrayList<>();
                for (JsonElement b : sObj.getAsJsonArray("bossNodes")) {
                    bossNodes.add(b.getAsString());
                }
                // 整合版 §3.2：还原记录点
                List<CheckpointData> checkpoints = new ArrayList<>();
                if (sObj.has("checkpoints")) {
                    for (JsonElement ce : sObj.getAsJsonArray("checkpoints")) {
                        JsonObject cpObj = ce.getAsJsonObject();
                        checkpoints.add(new CheckpointData(
                                cpObj.get("id").getAsString(),
                                cpObj.get("nodeId").getAsString(),
                                cpObj.get("posX").getAsInt(),
                                cpObj.get("posY").getAsInt(),
                                cpObj.get("posZ").getAsInt(),
                                cpObj.has("facing") ? cpObj.get("facing").getAsString() : "south",
                                cpObj.has("unlockCondition")
                                        ? cpObj.get("unlockCondition").getAsString()
                                        : CheckpointData.UNLOCK_ANY));
                    }
                }
                stages.put(stageId, new StageData(
                        stageId,
                        sObj.get("chapter").getAsString(),
                        sObj.get("displayName").getAsString(),
                        Map.copyOf(nodes),
                        List.copyOf(edges),
                        sObj.get("startNode").getAsString(),
                        List.copyOf(bossNodes),
                        List.of(),
                        List.copyOf(checkpoints),
                        Set.of(),
                        com.piranport.dungeon.data.SceneData.DAY,
                        Set.of(),
                        com.piranport.dungeon.data.StageData.VictoryObjectives.EMPTY,
                        sObj.has("guidance") ? sObj.get("guidance").getAsString() : "沿节点图前进，完成当前节点后继续向前。",
                        sObj.has("objective") ? sObj.get("objective").getAsString() : "完成关卡目标并击破首领。",
                        sObj.has("ending") ? sObj.get("ending").getAsString() : "战区肃清，航路恢复，舰队返回港口。",
                        sObj.has("difficultyScale") ? sObj.get("difficultyScale").getAsFloat() : 1.0f));
            }
        } catch (Exception e) {
            PiranPort.LOGGER.warn("Failed to parse dungeon registry sync: {}", e.getMessage());
        }
        return new ParsedRegistry(chapters, stages);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /**
     * Build payload from current DungeonRegistry state.
     */
    public static DungeonRegistrySyncPayload fromRegistry() {
        DungeonRegistry reg = DungeonRegistry.INSTANCE;
        Gson gson = new Gson();
        JsonObject root = new JsonObject();

        // Serialize chapters
        JsonArray chaptersArr = new JsonArray();
        for (ChapterData ch : reg.getSortedChapters()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("chapterId", ch.chapterId());
            obj.addProperty("displayName", ch.displayName());
            obj.addProperty("sortOrder", ch.sortOrder());
            JsonArray stagesArr = new JsonArray();
            for (String s : ch.stages()) stagesArr.add(s);
            obj.add("stages", stagesArr);
            chaptersArr.add(obj);
        }
        root.add("chapters", chaptersArr);

        // Serialize stages
        JsonObject stagesObj = new JsonObject();
        for (var entry : reg.getAllStages().entrySet()) {
            StageData stage = entry.getValue();
            JsonObject sObj = new JsonObject();
            sObj.addProperty("stageId", stage.stageId());
            sObj.addProperty("chapter", stage.chapter());
            sObj.addProperty("displayName", stage.displayName());
            sObj.addProperty("startNode", stage.startNode());
            sObj.addProperty("guidance", stage.guidance());
            sObj.addProperty("objective", stage.objective());
            sObj.addProperty("ending", stage.ending());
            sObj.addProperty("difficultyScale", stage.difficultyScale());

            // Nodes
            JsonObject nodesObj = new JsonObject();
            for (var ne : stage.nodes().entrySet()) {
                NodeData node = ne.getValue();
                JsonObject nObj = new JsonObject();
                nObj.addProperty("nodeId", node.nodeId());
                nObj.addProperty("type", node.type().name());
                if (node.enemies() != null) nObj.addProperty("enemies", node.enemies());
                nObj.addProperty("displayX", node.displayX());
                nObj.addProperty("displayY", node.displayY());
                if (node.costMessage() != null && !node.costMessage().isEmpty())
                    nObj.addProperty("costMessage", node.costMessage());
                if (node.script() != null) nObj.addProperty("script", node.script());

                // Rewards (for display)
                if (!node.rewards().isEmpty()) {
                    JsonArray rArr = new JsonArray();
                    for (NodeData.RewardEntry r : node.rewards()) {
                        JsonObject rObj = new JsonObject();
                        rObj.addProperty("item", r.item());
                        rObj.addProperty("count", r.count());
                        rObj.addProperty("chance", r.chance());
                        rArr.add(rObj);
                    }
                    nObj.add("rewards", rArr);
                }
                // Costs (for display)
                if (!node.cost().isEmpty()) {
                    JsonArray cArr = new JsonArray();
                    for (NodeData.CostEntry c : node.cost()) {
                        JsonObject cObj = new JsonObject();
                        cObj.addProperty("item", c.item());
                        cObj.addProperty("count", c.count());
                        cArr.add(cObj);
                    }
                    nObj.add("cost", cArr);
                }

                nodesObj.add(ne.getKey(), nObj);
                writeNodeConfig(nObj, node);
            }
            sObj.add("nodes", nodesObj);

            // Edges
            JsonArray edgesArr = new JsonArray();
            for (StageData.EdgeData e : stage.edges()) {
                JsonObject eObj = new JsonObject();
                eObj.addProperty("from", e.from());
                eObj.addProperty("to", e.to());
                edgesArr.add(eObj);
            }
            sObj.add("edges", edgesArr);

            // Boss nodes
            JsonArray bossArr = new JsonArray();
            for (String b : stage.bossNodes()) bossArr.add(b);
            sObj.add("bossNodes", bossArr);

            // Checkpoints (整合版 §3.2)
            if (!stage.checkpoints().isEmpty()) {
                JsonArray cpArr = new JsonArray();
                for (CheckpointData cp : stage.checkpoints()) {
                    JsonObject cpObj = new JsonObject();
                    cpObj.addProperty("id", cp.id());
                    cpObj.addProperty("nodeId", cp.nodeId());
                    cpObj.addProperty("posX", cp.posX());
                    cpObj.addProperty("posY", cp.posY());
                    cpObj.addProperty("posZ", cp.posZ());
                    cpObj.addProperty("facing", cp.facing());
                    cpObj.addProperty("unlockCondition", cp.unlockCondition());
                    cpArr.add(cpObj);
                }
                sObj.add("checkpoints", cpArr);
            }

            stagesObj.add(entry.getKey(), sObj);
        }
        root.add("stages", stagesObj);

        return new DungeonRegistrySyncPayload(gson.toJson(root));
    }

    /** 节点的战斗与胜利配置；与 {@link #readNode} 成对，漏写任一字段客户端就丢配置。 */
    private static void writeNodeConfig(JsonObject nObj, NodeData node) {
        nObj.addProperty("terrainType", node.terrainType().name());
        JsonArray restrictions = new JsonArray();
        for (CombatRestriction r : node.restrictions()) restrictions.add(r.name());
        nObj.add("restrictions", restrictions);
        nObj.addProperty("scene", node.scene().name());
        nObj.addProperty("difficultyScale", node.difficultyScale());
        nObj.addProperty("waves", node.waves());
        JsonArray branches = new JsonArray();
        for (BranchRule b : node.branches()) {
            JsonObject when = new JsonObject();
            if (b.hull() != null) when.addProperty("hull", b.hull());
            if (b.escorts() != null) when.addProperty("escorts", b.escorts());
            if (b.role() != null) when.addProperty("role", b.role());
            if (b.carry() != null) when.addProperty("carry", b.carry());
            if (b.chance() != null) when.addProperty("chance", b.chance());
            JsonObject bObj = new JsonObject();
            bObj.add("when", when);
            bObj.addProperty("to", b.to());
            branches.add(bObj);
        }
        nObj.add("branches", branches);
        if (node.branchDefault() != null) nObj.addProperty("branchDefault", node.branchDefault());
        if (node.displayName() != null) nObj.addProperty("displayName", node.displayName());
        if (node.escort() != null) {
            JsonObject eObj = new JsonObject();
            eObj.addProperty("entity", node.escort().entityId());
            eObj.add("targetOffset", intArray(node.escort().targetOffset()));
            nObj.add("escort", eObj);
        }
        if (node.detour() != null) {
            JsonObject dObj = new JsonObject();
            dObj.add("targetOffset", intArray(node.detour().targetOffset()));
            nObj.add("detour", dObj);
        }
    }

    private static JsonArray intArray(int[] values) {
        JsonArray arr = new JsonArray();
        for (int v : values) arr.add(v);
        return arr;
    }

    private static int[] readIntArray(JsonArray arr) {
        int[] values = new int[arr.size()];
        for (int i = 0; i < values.length; i++) values[i] = arr.get(i).getAsInt();
        return values;
    }

    private static NodeData readNode(JsonObject nObj, List<NodeData.RewardEntry> rewards,
                                     List<NodeData.CostEntry> costs) {
        Set<CombatRestriction> restrictions = EnumSet.noneOf(CombatRestriction.class);
        if (nObj.has("restrictions")) {
            for (JsonElement r : nObj.getAsJsonArray("restrictions")) {
                restrictions.add(CombatRestriction.valueOf(r.getAsString()));
            }
        }
        List<BranchRule> branches = new ArrayList<>();
        if (nObj.has("branches")) {
            for (JsonElement be : nObj.getAsJsonArray("branches")) {
                JsonObject bObj = be.getAsJsonObject();
                JsonObject when = bObj.getAsJsonObject("when");
                Double chance = when.has("chance") ? when.get("chance").getAsDouble() : null;
                branches.add(new BranchRule(
                        optString(when, "hull"), optString(when, "escorts"),
                        optString(when, "role"), optString(when, "carry"),
                        chance, bObj.get("to").getAsString()));
            }
        }
        NodeData.EscortSpec escort = null;
        if (nObj.has("escort")) {
            JsonObject eObj = nObj.getAsJsonObject("escort");
            escort = new NodeData.EscortSpec(eObj.get("entity").getAsString(),
                    readIntArray(eObj.getAsJsonArray("targetOffset")));
        }
        NodeData.DetourSpec detour = nObj.has("detour")
                ? new NodeData.DetourSpec(readIntArray(nObj.getAsJsonObject("detour").getAsJsonArray("targetOffset")))
                : null;
        return new NodeData(
                nObj.get("nodeId").getAsString(),
                NodeData.NodeType.valueOf(nObj.get("type").getAsString()),
                optString(nObj, "enemies"),
                List.copyOf(rewards),
                List.copyOf(costs),
                nObj.has("costMessage") ? nObj.get("costMessage").getAsString() : "",
                nObj.get("displayX").getAsInt(),
                nObj.get("displayY").getAsInt(),
                optString(nObj, "script"),
                TerrainType.valueOf(nObj.get("terrainType").getAsString()),
                Set.copyOf(restrictions),
                SceneData.valueOf(nObj.get("scene").getAsString()),
                nObj.get("difficultyScale").getAsDouble(),
                nObj.get("waves").getAsInt(),
                List.copyOf(branches),
                optString(nObj, "branchDefault"),
                optString(nObj, "displayName"),
                escort,
                detour);
    }

    private static String optString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    public static void handle(DungeonRegistrySyncPayload payload, IPayloadContext context) {
        // Parsing already happened on the netty IO thread inside STREAM_CODEC.
        // Here we only need a quick swap on the client thread.
        Map<String, ChapterData> chapters = payload.parsedChapters();
        Map<String, StageData> stages = payload.parsedStages();
        if (chapters == null || stages == null) {
            // Fallback: payload was constructed without going through the codec
            // (e.g. a self-test path). Parse synchronously.
            ParsedRegistry parsed = parseJson(payload.jsonData());
            chapters = parsed.chapters;
            stages = parsed.stages;
        }
        final Map<String, ChapterData> finalChapters = chapters;
        final Map<String, StageData> finalStages = stages;
        context.enqueueWork(() -> ClientDungeonData.setRegistryData(finalChapters, finalStages));
    }
}
