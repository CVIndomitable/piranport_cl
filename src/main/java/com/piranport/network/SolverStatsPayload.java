package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C 弹道解算统计包。
 * 服务端在火炮解算后发送给客户端，携带实际迭代次数等信息。
 */
public record SolverStatsPayload(int ternaryIters, int newtonIters,
                                 long totalUs, double verticalError,
                                 double horizontalError, double angleDeg) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SolverStatsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "solver_stats"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SolverStatsPayload> STREAM_CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeVarInt(p.ternaryIters);
                        buf.writeVarInt(p.newtonIters);
                        buf.writeVarLong(p.totalUs);
                        buf.writeDouble(p.verticalError);
                        buf.writeDouble(p.horizontalError);
                        buf.writeDouble(p.angleDeg);
                    },
                    buf -> new SolverStatsPayload(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarLong(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SolverStatsPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHooks.setServerSolverStats(
                payload.ternaryIters(), payload.newtonIters(), payload.totalUs(),
                payload.verticalError(), payload.horizontalError(), payload.angleDeg()));
    }
}
