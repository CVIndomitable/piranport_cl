package com.piranport.dungeon.network;

import com.google.gson.*;
import com.piranport.PiranPort;
import com.piranport.dungeon.data.*;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

/**
 * S2C: Syncs dungeon registry (chapters + stages) to the client on login.
 * Uses JSON string encoding to avoid complex nested StreamCodec.
 */
public record DungeonRegistrySyncPayload(String jsonData) implements CustomPacketPayload {

    public static final Type<DungeonRegistrySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_registry_sync"));

    public static final StreamCodec<ByteBuf, DungeonRegistrySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.STRING_UTF8.encode(buf, p.jsonData()),
            buf -> new DungeonRegistrySyncPayload(ByteBufCodecs.STRING_UTF8.decode(buf))
    );

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
        context.enqueueWork(() -> {
            try {
                JsonObject root = JsonParser.parseString(payload.jsonData()).getAsJsonObject();

                Map<String, ChapterData> chapters = new HashMap<>();
                Map<String, StageData> stages = new HashMap<>();

                // Parse chapters
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

                // Parse stages
                JsonObject stagesObj = root.getAsJsonObject("stages");
                for (var entry : stagesObj.entrySet()) {
                    JsonObject sObj = entry.getValue().getAsJsonObject();
                    String stageId = sObj.get("stageId").getAsString();

                    // Nodes
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

                    // Edges
                    List<StageData.EdgeData> edges = new ArrayList<>();
                    for (JsonElement e : sObj.getAsJsonArray("edges")) {
                        JsonObject eObj = e.getAsJsonObject();
                        edges.add(new StageData.EdgeData(
                                eObj.get("from").getAsString(),
                                eObj.get("to").getAsString()));
                    }

                    // Boss nodes
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
                            List.of())); // firstClearRewards not needed client-side
                }

                ClientDungeonData.setRegistryData(chapters, stages);
            } catch (Exception e) {
                PiranPort.LOGGER.warn("Failed to parse dungeon registry sync: {}", e.getMessage());
            }
        });
    }
}
