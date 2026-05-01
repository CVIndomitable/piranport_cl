package com.piranport.network;

import com.piranport.aviation.ClientReconData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C: server notifies client to restore camera to the player. */
public class ReconEndPayload {

    public ReconEndPayload() {
    }

    public static void encode(ReconEndPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static ReconEndPayload decode(FriendlyByteBuf buf) {
        return new ReconEndPayload();
    }

    public static void handle(ReconEndPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Client-side only
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClientReconData.handleReconEnd()
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
