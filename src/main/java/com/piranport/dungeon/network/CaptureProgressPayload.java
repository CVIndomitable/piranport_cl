package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C：夺旗占领进度（0–100），负数表示当前节点没有夺旗进度条。 */
public record CaptureProgressPayload(float percent) implements CustomPacketPayload {

    public static final Type<CaptureProgressPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "capture_progress"));

    public static final StreamCodec<ByteBuf, CaptureProgressPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.FLOAT.encode(buf, p.percent()),
            buf -> new CaptureProgressPayload(ByteBufCodecs.FLOAT.decode(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CaptureProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.setCaptureProgress(payload.percent()));
    }
}
