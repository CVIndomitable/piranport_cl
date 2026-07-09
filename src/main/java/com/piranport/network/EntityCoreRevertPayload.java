package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.entitycore.EntityCoreState;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EntityCoreRevertPayload() implements CustomPacketPayload {
    public static final Type<EntityCoreRevertPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "entity_core_revert"));

    public static final StreamCodec<ByteBuf, EntityCoreRevertPayload> STREAM_CODEC =
            StreamCodec.unit(new EntityCoreRevertPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EntityCoreRevertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp) {
                int currentCore = EntityCoreState.getActiveEntityCore(sp);
                if (currentCore > 0) {
                    EntityCoreState.revertEntityCore(sp);
                    sp.displayClientMessage(
                            Component.translatable("message.piranport.entity_core_removed"), true);
                }
            }
        });
    }
}
