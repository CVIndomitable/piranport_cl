package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.skin.ClientSkinData;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.netty.buffer.ByteBuf;

import java.util.UUID;

public record SkinSyncPayload(UUID playerUuid, int skinId) implements CustomPacketPayload {
    public static final Type<SkinSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "skin_sync"));

    public static final StreamCodec<ByteBuf, SkinSyncPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeLong(payload.playerUuid().getMostSignificantBits());
                        buf.writeLong(payload.playerUuid().getLeastSignificantBits());
                        ByteBufCodecs.VAR_INT.encode(buf, payload.skinId());
                    },
                    buf -> {
                        UUID uuid = new UUID(buf.readLong(), buf.readLong());
                        int skinId = ByteBufCodecs.VAR_INT.decode(buf);
                        return new SkinSyncPayload(uuid, skinId);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkinSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientSkinData.setActiveSkin(payload.playerUuid(), payload.skinId()));
    }
}
