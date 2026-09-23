package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C：把「服务端模拟距离换算出的吸附上限（格）」下发给客户端。
 *
 * <p>WHY 要有这个包：准星吸附必须跑在客户端 —— 它每 tick 都要改视角，走服务端
 * 一个来回的延迟会让吸附明显「追不上」鼠标。但吸附范围又要受服务端模拟距离钳制，
 * 否则玩家能吸到客户端根本没加载的实体上，射线求交打空，表现为随机失效。
 * 模拟距离只有服务端知道（{@code MinecraftServer#getPlayerList}），而
 * {@code FireControlRadarSnapHandler} 是客户端类，直接去取会违反
 * ArchitectureTest 的「client 不得依赖 server」规则。所以换算搬到服务端，
 * 客户端只收结果。
 *
 * <p>取值范围只有效于本次会话：模拟距离是存档/服务器配置，重连后会重新下发，
 * 客户端不需要持久化。
 */
public record FcRangeSyncPayload(double limitBlocks) implements CustomPacketPayload {

    public static final Type<FcRangeSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fc_range_sync"));

    public static final StreamCodec<ByteBuf, FcRangeSyncPayload> STREAM_CODEC =
            ByteBufCodecs.DOUBLE.map(FcRangeSyncPayload::new, FcRangeSyncPayload::limitBlocks);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 服务端下发吸附上限（格）。 */
    public static void send(ServerPlayer player, double limitBlocks) {
        PacketDistributor.sendToPlayer(player, new FcRangeSyncPayload(limitBlocks));
    }

    public static void handle(FcRangeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.setFcRadarSnapLimit(payload.limitBlocks()));
    }
}
