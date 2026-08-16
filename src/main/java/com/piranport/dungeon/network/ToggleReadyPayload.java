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
 * C2S: 玩家切换自己的"准备"状态（策划 §10.2 大厅成员列表）。
 *
 * <p>仅切换发送者自身；server 端校验玩家属于对应 lectern 的 lobby 后更新并广播。
 */
public record ToggleReadyPayload(BlockPos lecternPos) implements CustomPacketPayload {

    public static final Type<ToggleReadyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "toggle_ready"));

    public static final StreamCodec<ByteBuf, ToggleReadyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeLong(p.lecternPos().asLong()),
            buf -> new ToggleReadyPayload(BlockPos.of(buf.readLong()))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ToggleReadyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            BlockPos pos = payload.lecternPos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
            if (!(player.level().getBlockState(pos).getBlock() instanceof DungeonLecternBlock)) return;

            GlobalPos gp = GlobalPos.of(player.level().dimension(), pos);
            DungeonLobbyManager.Lobby lobby = DungeonLobbyManager.INSTANCE.getLobby(gp);
            if (lobby == null) return;

            lobby.toggleReady(player.getUUID());
            DungeonLobbyManager.INSTANCE.broadcastLobbyUpdate(player.server, gp);
        });
    }
}