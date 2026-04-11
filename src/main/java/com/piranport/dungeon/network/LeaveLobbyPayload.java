package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
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
            // Validate distance and block type (consistent with JoinLobbyPayload)
            net.minecraft.core.BlockPos pos = payload.lecternPos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            if (!(player.level().getBlockState(pos).getBlock()
                    instanceof com.piranport.dungeon.block.DungeonLecternBlock)) return;
            GlobalPos globalPos = GlobalPos.of(player.level().dimension(), pos);
            DungeonLobbyManager.INSTANCE.leaveLobby(globalPos, player.getUUID());
            DungeonLobbyManager.INSTANCE.broadcastLobbyUpdate(player.server, globalPos);
        });
    }
}
