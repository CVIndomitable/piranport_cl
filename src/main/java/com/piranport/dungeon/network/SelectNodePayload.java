package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Flagship selects a node to enter in the node map.
 */
public record SelectNodePayload(BlockPos lecternPos, int keySlot, String nodeId)
        implements CustomPacketPayload {

    public static final Type<SelectNodePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "select_node"));

    public static final StreamCodec<ByteBuf, SelectNodePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                ByteBufCodecs.VAR_INT.encode(buf, p.keySlot());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nodeId());
            },
            buf -> new SelectNodePayload(
                    BlockPos.of(buf.readLong()),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SelectNodePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Validate keySlot range
            int keySlot = payload.keySlot();
            if (keySlot < 0 || keySlot >= player.getInventory().getContainerSize()) return;

            // Validate nodeId length
            if (payload.nodeId().length() > 128) return;

            // Validate distance to lectern
            if (player.distanceToSqr(payload.lecternPos().getX() + 0.5,
                    payload.lecternPos().getY() + 0.5,
                    payload.lecternPos().getZ() + 0.5) > 64.0) return;

            // Validate key
            ItemStack keyStack = player.getInventory().getItem(keySlot);
            if (!(keyStack.getItem() instanceof DungeonKeyItem)) return;

            // Validate flagship permission
            DungeonLobbyManager.Lobby lobby =
                    DungeonLobbyManager.INSTANCE.getLobby(payload.lecternPos());
            if (lobby != null && !lobby.isFlagship(player.getUUID())) return;

            String stageId = DungeonKeyItem.getStageId(keyStack);
            StageData stage = DungeonRegistry.INSTANCE.getStage(stageId);
            if (stage == null) return;

            NodeData node = stage.nodes().get(payload.nodeId());
            if (node == null) return;

            // Get or create instance
            ServerLevel serverLevel = (ServerLevel) player.level();
            DungeonInstanceManager mgr = DungeonInstanceManager.get(serverLevel);
            java.util.UUID instanceId = DungeonKeyItem.getInstanceId(keyStack);
            DungeonInstance instance;

            if (instanceId != null) {
                instance = mgr.getInstance(instanceId);
                if (instance == null) return;
                if (instance.getState() == DungeonInstance.State.SUSPENDED) {
                    mgr.resumeInstance(instanceId);
                }

                // Validate node reachability: must not be already cleared,
                // and must be reachable from a cleared node via stage edges
                if (instance.getClearedNodes().contains(payload.nodeId())) return;
                boolean reachable = false;
                for (String cleared : instance.getClearedNodes()) {
                    if (stage.getReachableFrom(cleared).contains(payload.nodeId())) {
                        reachable = true;
                        break;
                    }
                }
                // If no nodes cleared yet, only start node is valid
                if (instance.getClearedNodes().isEmpty()) {
                    reachable = payload.nodeId().equals(stage.startNode());
                }
                if (!reachable) return;
            } else {
                // First node selection: only start node allowed
                if (!payload.nodeId().equals(stage.startNode())) return;

                // Create new instance
                instance = mgr.createInstance(stageId, player,
                        payload.lecternPos(),
                        player.level().dimension().location().toString());
                if (instance == null) return;
                DungeonKeyItem.setInstanceId(keyStack, instance.getInstanceId());
            }

            // Handle node by type
            DungeonEventHandler.enterNode(serverLevel, instance, node, stage,
                    player, keyStack, lobby);
        });
    }
}
