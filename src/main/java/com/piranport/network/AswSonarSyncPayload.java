package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.ClientAswSonarData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: server sends the list of entity IDs detected by an ASW aircraft's sonar.
 * Each ASW aircraft sends its own scan results independently.
 */
public record AswSonarSyncPayload(int aircraftEntityId, List<Integer> detectedEntityIds)
        implements CustomPacketPayload {

    public static final Type<AswSonarSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "asw_sonar_sync"));

    public static final StreamCodec<ByteBuf, AswSonarSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.aircraftEntityId());
                ByteBufCodecs.VAR_INT.encode(buf, p.detectedEntityIds().size());
                for (int id : p.detectedEntityIds()) {
                    ByteBufCodecs.VAR_INT.encode(buf, id);
                }
            },
            buf -> {
                int aircraftId = ByteBufCodecs.VAR_INT.decode(buf);
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > 128) size = 0;
                List<Integer> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ids.add(ByteBufCodecs.VAR_INT.decode(buf));
                }
                return new AswSonarSyncPayload(aircraftId, List.copyOf(ids));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AswSonarSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAswSonarData.update(payload.aircraftEntityId(), payload.detectedEntityIds()));
    }
}
