package com.piranport.network;

import com.piranport.debug.PiranPortDebug;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: toggle the debug cooldown override (clamps every cooldown to 5s). */
public class DebugCooldownOverridePayload {
    private final boolean enabled;

    public DebugCooldownOverridePayload(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(DebugCooldownOverridePayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static DebugCooldownOverridePayload decode(FriendlyByteBuf buf) {
        return new DebugCooldownOverridePayload(buf.readBoolean());
    }

    public static void handle(DebugCooldownOverridePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null && sp.hasPermissions(2)) {
                PiranPortDebug.setCooldownOverride(msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
