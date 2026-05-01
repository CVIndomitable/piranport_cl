package com.piranport.network;

import com.piranport.debug.PiranPortDebug;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: request a state snapshot to be written to the debug log. */
public class SnapshotRequestPayload {

    public SnapshotRequestPayload() {
    }

    public static void encode(SnapshotRequestPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static SnapshotRequestPayload decode(FriendlyByteBuf buf) {
        return new SnapshotRequestPayload();
    }

    public static void handle(SnapshotRequestPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null && sp.hasPermissions(2)) {
                PiranPortDebug.snapshot(sp);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
