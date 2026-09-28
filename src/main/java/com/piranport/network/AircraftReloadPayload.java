package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.item.AircraftItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Starts or cancels the R-key aircraft loading use action. */
public record AircraftReloadPayload(boolean start, int handIndex) implements CustomPacketPayload {
    public static final Type<AircraftReloadPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "aircraft_reload"));
    public static final StreamCodec<ByteBuf, AircraftReloadPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AircraftReloadPayload::start,
            ByteBufCodecs.VAR_INT, AircraftReloadPayload::handIndex,
            AircraftReloadPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AircraftReloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null || !TransformationManager.isPlayerTransformed(player)) return;
            InteractionHand hand = payload.handIndex() == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!(player.getItemInHand(hand).getItem() instanceof AircraftItem)) return;
            if (payload.start()) {
                if (!player.isUsingItem()) player.startUsingItem(hand);
            } else if (player.isUsingItem() && player.getUsedItemHand() == hand) {
                player.stopUsingItem();
            }
        });
    }
}
