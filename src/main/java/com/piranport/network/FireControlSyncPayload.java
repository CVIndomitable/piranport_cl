package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientFireControlData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FireControlSyncPayload(List<UUID> targetUUIDs) implements CustomPacketPayload {

    public static final Type<FireControlSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fire_control_sync"));

    public static final StreamCodec<ByteBuf, FireControlSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.targetUUIDs().size());
                for (UUID uuid : p.targetUUIDs()) {
                    buf.writeLong(uuid.getMostSignificantBits());
                    buf.writeLong(uuid.getLeastSignificantBits());
                }
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > 16) size = 0; // guard against oversized packets
                List<UUID> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(new UUID(buf.readLong(), buf.readLong()));
                }
                return new FireControlSyncPayload(List.copyOf(list));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    // ===== Client-side handler =====

    public static void handle(FireControlSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientFireControlData.setTargets(payload.targetUUIDs()));
    }
}
