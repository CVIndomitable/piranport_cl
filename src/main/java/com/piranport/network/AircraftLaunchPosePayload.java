package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C: short rigging pose pulse when a transformed player launches aircraft. */
public record AircraftLaunchPosePayload(int entityId, int skinId, int durationTicks)
        implements CustomPacketPayload {
    public static final Type<AircraftLaunchPosePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aircraft_launch_pose"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AircraftLaunchPosePayload> STREAM_CODEC =
            StreamCodec.ofMember(
                    (payload, buf) -> {
                        buf.writeVarInt(payload.entityId());
                        buf.writeVarInt(payload.skinId());
                        buf.writeVarInt(payload.durationTicks());
                    },
                    buf -> new AircraftLaunchPosePayload(
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AircraftLaunchPosePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientHooks.triggerAircraftLaunchPose(
                payload.entityId(), payload.skinId(), payload.durationTicks()));
    }
}
