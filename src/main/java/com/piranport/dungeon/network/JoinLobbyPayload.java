package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlock;
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
 * C2S: Player joins a dungeon lobby at a lectern.
 */
public record JoinLobbyPayload(BlockPos lecternPos) implements CustomPacketPayload {

    public static final Type<JoinLobbyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "join_lobby"));

    public static final StreamCodec<ByteBuf, JoinLobbyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new JoinLobbyPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(JoinLobbyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Validate distance to lectern
            BlockPos pos = payload.lecternPos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

            // Validate lectern block exists at position
            if (!(player.level().getBlockState(pos).getBlock() instanceof DungeonLecternBlock)) return;

            GlobalPos globalPos = GlobalPos.of(player.level().dimension(), pos);
            DungeonLobbyManager.INSTANCE.joinLobby(globalPos, player);
            DungeonLobbyManager.INSTANCE.broadcastLobbyUpdate(player.server, globalPos);
        });
    }
}
