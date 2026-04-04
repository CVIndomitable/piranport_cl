package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.menu.FlightGroupMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenFlightGroupPayload(int coreSlot) implements CustomPacketPayload {

    public static final Type<OpenFlightGroupPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "open_flight_group"));

    public static final StreamCodec<ByteBuf, OpenFlightGroupPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(OpenFlightGroupPayload::new, OpenFlightGroupPayload::coreSlot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenFlightGroupPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                int slot = payload.coreSlot();
                if (slot < 0 || slot >= 41) return;
                net.minecraft.world.item.ItemStack coreStack = serverPlayer.getInventory().getItem(slot);
                if (!(coreStack.getItem() instanceof com.piranport.item.ShipCoreItem)) return;

                // No-GUI mode: U key toggles fighter air-only instead of opening flight group menu
                if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
                    boolean airOnly = com.piranport.aviation.FireControlManager.toggleFighterAirOnly(
                            serverPlayer.getUUID());
                    serverPlayer.displayClientMessage(Component.translatable(
                            airOnly ? "message.piranport.fighter_air_only_on"
                                    : "message.piranport.fighter_air_only_off"), true);
                    return;
                }

                serverPlayer.openMenu(
                        new SimpleMenuProvider(
                                (id, inv, p) -> new FlightGroupMenu(id, inv, slot),
                                Component.translatable("container.piranport.flight_groups")
                        ),
                        buf -> buf.writeVarInt(slot)
                );
            }
        });
    }
}
