package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** 冻结回收前实体位置与回收后的甲板快照，供客户端播放降落动画。 */
public record AircraftLandingVisualPayload(int launcherId, UUID launcherUuid, int aircraftId, UUID aircraftUuid,
        int targetSlot, int socketId, String visualId, double sourceX, double sourceY, double sourceZ,
        float sourceYaw, float sourcePitch, long snapshotFingerprint, long startTick,
        int approachTicks, int rollTicks, int settleTicks) implements CustomPacketPayload {
    public static final Type<AircraftLandingVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aircraft_landing_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AircraftLandingVisualPayload> STREAM_CODEC =
            StreamCodec.ofMember((p, b) -> {
                b.writeVarInt(p.launcherId); b.writeUUID(p.launcherUuid);
                b.writeVarInt(p.aircraftId); b.writeUUID(p.aircraftUuid);
                b.writeVarInt(p.targetSlot); b.writeVarInt(p.socketId); b.writeUtf(p.visualId);
                b.writeDouble(p.sourceX); b.writeDouble(p.sourceY); b.writeDouble(p.sourceZ);
                b.writeFloat(p.sourceYaw); b.writeFloat(p.sourcePitch);
                b.writeLong(p.snapshotFingerprint); b.writeLong(p.startTick);
                b.writeVarInt(p.approachTicks); b.writeVarInt(p.rollTicks); b.writeVarInt(p.settleTicks);
            }, b -> new AircraftLandingVisualPayload(b.readVarInt(), b.readUUID(), b.readVarInt(), b.readUUID(),
                    b.readVarInt(), b.readVarInt(), b.readUtf(), b.readDouble(), b.readDouble(), b.readDouble(),
                    b.readFloat(), b.readFloat(), b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AircraftLandingVisualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.triggerAircraftLandingVisual(payload));
    }
}
