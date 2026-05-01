package com.piranport.network;

import com.piranport.aviation.ClientReconData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: server notifies client to switch camera to recon entity. */
public class ReconStartPayload {
    private final int entityId;

    public ReconStartPayload(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ReconStartPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static ReconStartPayload decode(FriendlyByteBuf buf) {
        return new ReconStartPayload(buf.readVarInt());
    }

    public static void handle(ReconStartPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientReconData.handleReconStart(msg.entityId)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
