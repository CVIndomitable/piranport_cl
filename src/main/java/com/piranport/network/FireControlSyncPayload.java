package com.piranport.network;

import com.piranport.aviation.ClientFireControlData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class FireControlSyncPayload {
    private final List<UUID> targetUUIDs;

    public FireControlSyncPayload(List<UUID> targetUUIDs) {
        this.targetUUIDs = targetUUIDs;
    }

    public static void encode(FireControlSyncPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.targetUUIDs.size());
        for (UUID uuid : msg.targetUUIDs) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    public static FireControlSyncPayload decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > 16) size = 0; // guard against oversized packets
        List<UUID> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new UUID(buf.readLong(), buf.readLong()));
        }
        return new FireControlSyncPayload(List.copyOf(list));
    }

    public static void handle(FireControlSyncPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientFireControlData.setTargets(msg.targetUUIDs)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
