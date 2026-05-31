package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleFighterGroundAttackPayload() implements CustomPacketPayload {
    public static final Type<ToggleFighterGroundAttackPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "toggle_fighter_ground_attack"));

    public static final StreamCodec<ByteBuf, ToggleFighterGroundAttackPayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleFighterGroundAttackPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleFighterGroundAttackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!TransformationManager.isPlayerTransformed(player)) return;

            boolean groundEnabled = FireControlManager.toggleFighterGround(player.getUUID());
            player.displayClientMessage(Component.translatable(
                    groundEnabled ? "message.piranport.fighter_air_only_off"
                            : "message.piranport.fighter_air_only_on"), true);
        });
    }
}
