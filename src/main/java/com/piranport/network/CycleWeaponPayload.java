package com.piranport.network;

import com.piranport.combat.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CycleWeaponPayload {

    public CycleWeaponPayload() {
    }

    public static void encode(CycleWeaponPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static CycleWeaponPayload decode(FriendlyByteBuf buf) {
        return new CycleWeaponPayload();
    }

    public static void handle(CycleWeaponPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender() != null) {
                TransformationManager.cycleWeapon(ctx.get().getSender());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
