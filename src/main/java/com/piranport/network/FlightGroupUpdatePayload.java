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

    public static void handle(FlightGroupUpdatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack coreStack = player.getInventory().getItem(payload.coreSlot());
            if (coreStack.getItem() instanceof ShipCoreItem) {
                coreStack.set(ModDataComponents.FLIGHT_GROUP_DATA.get(), payload.data());
            }
        });
    }
}
