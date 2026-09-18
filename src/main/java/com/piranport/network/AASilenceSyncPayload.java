package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.registry.ModAttachmentTypes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 服务端同步静默截止 tick，让客户端 HUD 与防空判定读取同一状态。 */
public record AASilenceSyncPayload(long endsAt) implements CustomPacketPayload {
    public static final Type<AASilenceSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aa_silence_sync"));
    public static final StreamCodec<ByteBuf, AASilenceSyncPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_LONG.map(AASilenceSyncPayload::new, AASilenceSyncPayload::endsAt);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AASilenceSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().getData(ModAttachmentTypes.AA_SILENCE.get())
                .setEndsAt(payload.endsAt()));
    }
}
