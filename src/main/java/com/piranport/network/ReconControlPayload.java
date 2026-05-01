package com.piranport.network;

import com.piranport.aviation.ReconManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** C2S: player sends WASD movement direction for recon aircraft control. */
public class ReconControlPayload {
    private final float dx;
    private final float dy;
    private final float dz;

    public ReconControlPayload(float dx, float dy, float dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    public static void encode(ReconControlPayload msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.dx);
        buf.writeFloat(msg.dy);
        buf.writeFloat(msg.dz);
    }

    public static ReconControlPayload decode(FriendlyByteBuf buf) {
        return new ReconControlPayload(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ReconControlPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() == null) return;
            // Check for NaN and Infinity before clamp (clamp converts NaN to min value, Infinity bypasses range check)
            if (!Float.isFinite(msg.dx) || !Float.isFinite(msg.dy) || !Float.isFinite(msg.dz)) return;
            float dx = Mth.clamp(msg.dx, -1.0f, 1.0f);
            float dy = Mth.clamp(msg.dy, -1.0f, 1.0f);
            float dz = Mth.clamp(msg.dz, -1.0f, 1.0f);
            ReconManager.handleControl(ctx.get().getSender().getUUID(), dx, dy, dz);
        });
        ctx.get().setPacketHandled(true);
    }
}
