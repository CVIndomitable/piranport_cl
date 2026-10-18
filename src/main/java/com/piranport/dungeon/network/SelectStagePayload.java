package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S 章节钥匙换关请求；服务端校验讲台状态与距离后切换钥匙所绑定的关卡。 */
public record SelectStagePayload(BlockPos lecternPos, String stageId) implements CustomPacketPayload {

    private static final int MAX_ID_LENGTH = 128;

    public static final Type<SelectStagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "select_stage"));

    public static final StreamCodec<ByteBuf, SelectStagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageId());
            },
            buf -> new SelectStagePayload(
                    BlockPos.of(buf.readLong()),
                    ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SelectStagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!player.isAlive() || player.isSpectator()) return;
            if (player.distanceToSqr(payload.lecternPos().getX() + 0.5, payload.lecternPos().getY() + 0.5,
                    payload.lecternPos().getZ() + 0.5) > 64.0) return;
            if (player.level().getBlockEntity(payload.lecternPos()) instanceof DungeonLecternBlockEntity lectern) {
                lectern.switchStage(player, payload.stageId());
            }
        });
    }
}
