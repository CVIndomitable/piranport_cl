package com.piranport.dungeon.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.piranport.PiranPort;
import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    nodes.put(ne.getKey(), new NodeData(
                            nObj.get("nodeId").getAsString(),
                            NodeData.NodeType.valueOf(nObj.get("type").getAsString()),
                            nObj.has("enemies") ? nObj.get("enemies").getAsString() : null,
                            List.copyOf(rewards),
                            List.copyOf(costs),
                            nObj.has("costMessage") ? nObj.get("costMessage").getAsString() : "",
                            nObj.get("displayX").getAsInt(),
                            nObj.get("displayY").getAsInt(),
                            nObj.has("script") ? nObj.get("script").getAsString() : null));
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
                stages.put(stageId, new StageData(
                        stageId,
                        sObj.get("chapter").getAsString(),
                        sObj.get("displayName").getAsString(),
                        Map.copyOf(nodes),
                        List.copyOf(edges),
                        sObj.get("startNode").getAsString(),
                        List.copyOf(bossNodes),
                        List.of()));
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

            stagesObj.add(entry.getKey(), sObj);
        }
        root.add("stages", stagesObj);

        return new DungeonRegistrySyncPayload(gson.toJson(root));
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
