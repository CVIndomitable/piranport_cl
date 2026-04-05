package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Sync lobby member list to all members.
 */
public record LobbyUpdatePayload(List<String> memberNames, String flagshipName,
                                   String selectedStageId) implements CustomPacketPayload {

    public static final Type<LobbyUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "lobby_update"));

    public static final StreamCodec<ByteBuf, LobbyUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.memberNames().size());
                for (String name : p.memberNames()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, name);
                }
                ByteBufCodecs.STRING_UTF8.encode(buf, p.flagshipName());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.selectedStageId());
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > 64) size = 0;
                List<String> names = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    names.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                String flagship = ByteBufCodecs.STRING_UTF8.decode(buf);
                String stage = ByteBufCodecs.STRING_UTF8.decode(buf);
                return new LobbyUpdatePayload(List.copyOf(names), flagship, stage);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LobbyUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client stores lobby data for screen rendering
            ClientDungeonData.setLobbyMembers(payload.memberNames(),
                    payload.flagshipName(), payload.selectedStageId());
        });
    }
}
