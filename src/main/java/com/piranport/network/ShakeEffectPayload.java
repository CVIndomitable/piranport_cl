package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.client.CameraShakeHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C 震动效果包。
 * 服务端在火炮发射/爆炸时发送给客户端，触发相机震动。
 */
public record ShakeEffectPayload(float intensity, int durationTicks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShakeEffectPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "shake_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShakeEffectPayload> STREAM_CODEC =
            StreamCodec.ofMember(
                    (p, buf) -> {
                        buf.writeFloat(p.intensity);
                        buf.writeVarInt(p.durationTicks);
                    },
                    buf -> new ShakeEffectPayload(buf.readFloat(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShakeEffectPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CameraShakeHandler.trigger(payload.intensity(), payload.durationTicks()));
    }
}
