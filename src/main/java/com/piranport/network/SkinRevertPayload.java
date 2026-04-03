package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.skin.SkinManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import io.netty.buffer.ByteBuf;

public record SkinRevertPayload() implements CustomPacketPayload {
    public static final Type<SkinRevertPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "skin_revert"));

    public static final StreamCodec<ByteBuf, SkinRevertPayload> STREAM_CODEC =
            StreamCodec.unit(new SkinRevertPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkinRevertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                int currentSkin = SkinManager.getActiveSkin(sp);
                if (currentSkin > 0) {
                    SkinManager.revertSkin(sp);
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.skin_removed"), true);
                }
            }
        });
    }
}
