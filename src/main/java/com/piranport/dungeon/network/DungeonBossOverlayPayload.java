package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端同步 Boss 铭牌、舰种、章节及分段血条；客户端不参与战斗判定。 */
public record DungeonBossOverlayPayload(String bossName, String shipType, String chapter,
                                        int segment, float health, float maxHealth,
                                        boolean visible, boolean quietBattlefield)
        implements CustomPacketPayload {
    public static final Type<DungeonBossOverlayPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_boss_overlay"));
    public static final StreamCodec<ByteBuf, DungeonBossOverlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.bossName());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.shipType());
                ByteBufCodecs.STRING_UTF8.encode(buf, payload.chapter());
                ByteBufCodecs.VAR_INT.encode(buf, payload.segment());
                buf.writeFloat(payload.health());
                buf.writeFloat(payload.maxHealth());
                ByteBufCodecs.BOOL.encode(buf, payload.visible());
                ByteBufCodecs.BOOL.encode(buf, payload.quietBattlefield());
            },
            buf -> new DungeonBossOverlayPayload(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    Math.max(0, Math.min(4, ByteBufCodecs.VAR_INT.decode(buf))),
                    buf.readFloat(), buf.readFloat(),
                    ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.BOOL.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DungeonBossOverlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.updateDungeonBossOverlay(
                payload.bossName(), payload.shipType(), payload.chapter(), payload.segment(),
                payload.health(), payload.maxHealth(), payload.visible(), payload.quietBattlefield()));
    }
}
