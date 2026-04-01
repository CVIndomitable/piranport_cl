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
 * S2C: Syncs current dungeon state (stage name, node, timer) to client HUD.
 */
public record DungeonStatePayload(String stageName, String nodeId, long timerStartMillis)
        implements CustomPacketPayload {

    public static final Type<DungeonStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_state"));

    public static final StreamCodec<ByteBuf, DungeonStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.nodeId());
                buf.writeLong(p.timerStartMillis());
            },
            buf -> new DungeonStatePayload(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readLong())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DungeonStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DungeonHudLayer.setDungeonState(payload.stageName(), payload.nodeId(),
                    payload.timerStartMillis());
        });
    }
}
