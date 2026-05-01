package com.piranport.network;

import com.piranport.aviation.ClientAswSonarData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: server sends the list of entity IDs detected by an ASW aircraft's sonar.
 * Each ASW aircraft sends its own scan results independently.
 */
public class AswSonarSyncPayload {
    private final int aircraftEntityId;
    private final List<Integer> detectedEntityIds;

    public AswSonarSyncPayload(int aircraftEntityId, List<Integer> detectedEntityIds) {
        this.aircraftEntityId = aircraftEntityId;
        this.detectedEntityIds = detectedEntityIds;
    }

    public static void encode(AswSonarSyncPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.aircraftEntityId);
        buf.writeVarInt(msg.detectedEntityIds.size());
        for (int id : msg.detectedEntityIds) {
            buf.writeVarInt(id);
        }
    }

    public static AswSonarSyncPayload decode(FriendlyByteBuf buf) {
        int aircraftId = buf.readVarInt();
        int size = buf.readVarInt();
        if (size < 0 || size > 128) size = 0;
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readVarInt());
        }
        return new AswSonarSyncPayload(aircraftId, List.copyOf(ids));
    }

    public static void handle(AswSonarSyncPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientAswSonarData.update(msg.aircraftEntityId, msg.detectedEntityIds)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
