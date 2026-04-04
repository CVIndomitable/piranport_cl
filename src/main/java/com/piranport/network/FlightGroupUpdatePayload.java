package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.component.FlightGroupData;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FlightGroupUpdatePayload(int coreSlot, FlightGroupData data)
        implements CustomPacketPayload {

    public static final Type<FlightGroupUpdatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "flight_group_update"));

    public static final StreamCodec<ByteBuf, FlightGroupUpdatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, FlightGroupUpdatePayload::coreSlot,
                    FlightGroupData.STREAM_CODEC, FlightGroupUpdatePayload::data,
                    FlightGroupUpdatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final java.util.Set<String> VALID_PAYLOADS = java.util.Set.of(
            "", "piranport:aerial_torpedo", "piranport:aerial_bomb");

    public static void handle(FlightGroupUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            int slot = payload.coreSlot();
            if (slot < 0 || slot >= 41) return;
            ItemStack coreStack = player.getInventory().getItem(slot);
            if (coreStack.getItem() instanceof ShipCoreItem sci) {
                // Validate all slotIndices are within weapon slot range
                int weaponSlots = sci.getShipType().weaponSlots;
                for (var group : payload.data().groups()) {
                    for (int idx : group.slotIndices()) {
                        if (idx < 0 || idx >= weaponSlots) return;
                    }
                    // Validate slotPayload strings are whitelisted
                    for (String pl : group.slotPayload().values()) {
                        if (!VALID_PAYLOADS.contains(pl)) return;
                    }
                }
                coreStack.set(ModDataComponents.FLIGHT_GROUP_DATA.get(), payload.data());
            }
        });
    }
}
