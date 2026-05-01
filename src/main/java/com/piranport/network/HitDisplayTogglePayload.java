package com.piranport.network;

import com.piranport.combat.HitNotifier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: toggle hit/kill/miss chat notifications for this player. */
public class HitDisplayTogglePayload {
    private final boolean enabled;

    public HitDisplayTogglePayload(boolean enabled) {
        this.enabled = enabled;
    }

    public static void encode(HitDisplayTogglePayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.enabled);
    }

    public static HitDisplayTogglePayload decode(FriendlyByteBuf buf) {
        return new HitDisplayTogglePayload(buf.readBoolean());
    }

    public static void handle(HitDisplayTogglePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) {
                HitNotifier.setEnabled(sp.getUUID(), msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
