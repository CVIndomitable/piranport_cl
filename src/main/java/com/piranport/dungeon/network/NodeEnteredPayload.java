package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.client.DungeonHudLayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Notifies client that a new node has been entered.
 */
public record NodeEnteredPayload(String nodeId, String nodeType) implements CustomPacketPayload {

    public static final Type<NodeEnteredPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "node_entered"));

    public static final StreamCodec<ByteBuf, NodeEnteredPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, NodeEnteredPayload::nodeId,
                    ByteBufCodecs.STRING_UTF8, NodeEnteredPayload::nodeType,
                    NodeEnteredPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(NodeEnteredPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update HUD with current node
            // The full state will be synced via DungeonStatePayload,
            // this is just for immediate feedback
        });
    }
}
