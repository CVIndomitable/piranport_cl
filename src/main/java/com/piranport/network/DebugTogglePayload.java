package com.piranport.network;

import com.piranport.debug.PiranPortDebug;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: toggle server-side debug logging on/off. */
public class DebugTogglePayload {
    private final boolean enabled;

    public DebugTogglePayload(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(DebugTogglePayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static DebugTogglePayload decode(FriendlyByteBuf buf) {
        return new DebugTogglePayload(buf.readBoolean());
    }

    public static void handle(DebugTogglePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null && sp.hasPermissions(2)) {
                PiranPortDebug.setServerEnabled(msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
