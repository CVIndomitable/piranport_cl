package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Player leaves a dungeon lobby.
 */
public record LeaveLobbyPayload(BlockPos lecternPos) implements CustomPacketPayload {

    public static final Type<LeaveLobbyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "leave_lobby"));

    public static final StreamCodec<ByteBuf, LeaveLobbyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new LeaveLobbyPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LeaveLobbyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            DungeonLobbyManager.INSTANCE.leaveLobby(payload.lecternPos(), player.getUUID());
            DungeonLobbyManager.INSTANCE.broadcastLobbyUpdate(player.server, payload.lecternPos());
        });
    }
}
