package com.piranport.dungeon.network;

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
 * S2C：出口传送门的"下界门式传送演出"开关。
 *
 * <p><b>为什么由服务端主动下发开/关，而不是客户端自己计时：</b>演出时长由服务端倒数决定
 * （走进门面后 20 tick 才真正传送）。玩家中途走出门范围时传送会被取消，客户端必须同步
 * 取消演出，否则会出现"演出放完了却没被传走"的错位。客户端只负责表现，不自己判断结束。</p>
 */
public record PortalTransitPayload(boolean active) implements CustomPacketPayload {
    public static final Type<PortalTransitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "portal_transit"));

    public static final StreamCodec<ByteBuf, PortalTransitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.BOOL.encode(buf, p.active()),
            buf -> new PortalTransitPayload(ByteBufCodecs.BOOL.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void sendStart(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PortalTransitPayload(true));
    }

    public static void sendStop(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new PortalTransitPayload(false));
    }

    public static void handle(PortalTransitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.setPortalTransit(payload.active()));
    }
}
