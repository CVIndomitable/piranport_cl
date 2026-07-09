package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.entitycore.ClientEntityCoreData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record EntityCoreSyncPayload(UUID playerUuid, int coreId) implements CustomPacketPayload {
    public static final Type<EntityCoreSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "entity_core_sync"));

    public static final StreamCodec<ByteBuf, EntityCoreSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(payload.playerUuid().getMostSignificantBits());
                        buf.writeLong(payload.playerUuid().getLeastSignificantBits());
                        ByteBufCodecs.VAR_INT.encode(buf, payload.coreId());
                    },
                    buf -> {
                        UUID uuid = new UUID(buf.readLong(), buf.readLong());
                        int coreId = ByteBufCodecs.VAR_INT.decode(buf);
                        return new EntityCoreSyncPayload(uuid, coreId);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EntityCoreSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientEntityCoreData.setActiveEntityCore(payload.playerUuid(), payload.coreId()));
    }
}
