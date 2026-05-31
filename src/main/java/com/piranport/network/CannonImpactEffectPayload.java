package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.client.CannonImpactEffects;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: 远距炮弹命中特效。 */
public record CannonImpactEffectPayload(double x, double y, double z, float power, Kind kind)
        implements CustomPacketPayload {

    public enum Kind {
        HE,
        AP,
        VT
    }

    public static final Type<CannonImpactEffectPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "cannon_impact_effect"));

    public static final StreamCodec<ByteBuf, CannonImpactEffectPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x());
                buf.writeDouble(p.y());
                buf.writeDouble(p.z());
                buf.writeFloat(p.power());
                buf.writeByte(p.kind().ordinal());
            },
            buf -> {
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                float power = buf.readFloat();
                int kindIndex = Byte.toUnsignedInt(buf.readByte());
                Kind[] values = Kind.values();
                Kind kind = values[Math.min(kindIndex, values.length - 1)];
                return new CannonImpactEffectPayload(x, y, z, power, kind);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CannonImpactEffectPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> CannonImpactEffects.spawn(payload));
    }
}
