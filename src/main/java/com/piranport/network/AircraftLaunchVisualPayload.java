package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.UUID;

/** 玩家放飞时冻结飞机、来源与动画参数；与实体生成包可任意顺序抵达。 */
public record AircraftLaunchVisualPayload(int launcherId, UUID launcherUuid, int aircraftId, UUID aircraftUuid,
        int sourceSlot, int socketId, String visualId, long snapshotFingerprint, long startTick,
        int deployTicks, int rollTicks, int flightTicks) implements CustomPacketPayload {
    public static final Type<AircraftLaunchVisualPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aircraft_launch_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AircraftLaunchVisualPayload> STREAM_CODEC =
            StreamCodec.ofMember((p, b) -> {
                b.writeVarInt(p.launcherId); b.writeUUID(p.launcherUuid);
                b.writeVarInt(p.aircraftId); b.writeUUID(p.aircraftUuid);
                b.writeVarInt(p.sourceSlot); b.writeVarInt(p.socketId); b.writeUtf(p.visualId);
                b.writeLong(p.snapshotFingerprint); b.writeLong(p.startTick); b.writeVarInt(p.deployTicks); b.writeVarInt(p.rollTicks); b.writeVarInt(p.flightTicks);
            }, b -> new AircraftLaunchVisualPayload(b.readVarInt(), b.readUUID(), b.readVarInt(), b.readUUID(),
                    b.readVarInt(), b.readVarInt(), b.readUtf(), b.readLong(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(AircraftLaunchVisualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.triggerAircraftLaunchVisual(payload));
    }
}
