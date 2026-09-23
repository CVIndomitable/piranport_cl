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
 * <p><b>这个值只在本次会话内有效，但它不会「自动重新下发」</b>：唯一的发送点是
 * {@code ToggleFcRadarPayload#handle}，也就是玩家按下 0 键那一刻。重连、换维度、死亡重生
 * 都不会补发，而 {@code SHIP_FC_RADAR_ON} 是持久化组件、会跟着核心物品跨存档同步 ——
 * 于是「上个存档里雷达是开着的、这次进新世界没再按 0」的玩家，客户端缓存会停在上一局的
 * 残留值上。为此客户端在断开连接时会把缓存复位为 0（见
 * {@code FireControlRadarSnapHandler#reset}），并且必须等到玩家重新按 0 才会再次有值。
 * 不要在客户端把这个值持久化，也不要把「有没有值」当成「雷达开没开」的判据。
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
