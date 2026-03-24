package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.netty.buffer.ByteBuf;

public record CycleWeaponPayload() implements CustomPacketPayload {
    public static final Type<CycleWeaponPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "cycle_weapon"));

    public static final StreamCodec<ByteBuf, CycleWeaponPayload> STREAM_CODEC =
            StreamCodec.unit(new CycleWeaponPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CycleWeaponPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TransformationManager.cycleWeapon(context.player()));
    }
}
