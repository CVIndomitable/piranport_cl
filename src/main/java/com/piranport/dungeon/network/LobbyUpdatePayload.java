package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Sync lobby member list + per-member ready state to all members.
 *
 * <p>Phase 27：每个成员的"准备"状态以 parallel {@code readyStates} 列表发送，索引与 {@code memberNames} 一一对应。
 */
public record LobbyUpdatePayload(List<String> memberNames, String flagshipName,
                                   String selectedStageId, List<Boolean> readyStates)
        implements CustomPacketPayload {

    private static final int MAX_MEMBERS = 64;
    private static final int MAX_MEMBER_NAME_LENGTH = 32;
    private static final int MAX_NAME_OR_STAGE_LENGTH = 128;

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
                ByteBufCodecs.VAR_INT.encode(buf, p.readyStates().size());
                for (Boolean ready : p.readyStates()) {
                    buf.writeBoolean(ready);
                }
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > MAX_MEMBERS) {
                    throw new DecoderException(
                            "Lobby member count out of range: " + size);
                }
                List<String> names = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    String name = ByteBufCodecs.stringUtf8(MAX_MEMBER_NAME_LENGTH).decode(buf);
                    names.add(name);
                }
                String flagship = ByteBufCodecs.stringUtf8(MAX_NAME_OR_STAGE_LENGTH).decode(buf);
                String stage = ByteBufCodecs.stringUtf8(MAX_NAME_OR_STAGE_LENGTH).decode(buf);
                int rsize = ByteBufCodecs.VAR_INT.decode(buf);
                if (rsize < 0 || rsize > MAX_MEMBERS || rsize != size) {
                    throw new DecoderException(
                            "LobbyUpdatePayload readyStates size mismatch: " + rsize);
                }
                List<Boolean> ready = new ArrayList<>(rsize);
                for (int i = 0; i < rsize; i++) {
                    ready.add(buf.readBoolean());
                }
                return new LobbyUpdatePayload(List.copyOf(names), flagship, stage, List.copyOf(ready));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LobbyUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client stores lobby data for screen rendering
            ClientDungeonData.setLobbyMembers(payload.memberNames(),
                    payload.flagshipName(), payload.selectedStageId(), payload.readyStates());
        });
    }
}