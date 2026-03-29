package com.piranport.network;

import com.piranport.PiranPort;
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

public record AutoLaunchTogglePayload(int coreSlot) implements CustomPacketPayload {

    public static final Type<AutoLaunchTogglePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "auto_launch_toggle"));

    public static final StreamCodec<ByteBuf, AutoLaunchTogglePayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(AutoLaunchTogglePayload::new, AutoLaunchTogglePayload::coreSlot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutoLaunchTogglePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            int slot = payload.coreSlot();
            if (slot < 0 || slot >= 41) return;
            ItemStack coreStack = player.getInventory().getItem(slot);
            if (coreStack.getItem() instanceof ShipCoreItem) {
                boolean current = coreStack.getOrDefault(ModDataComponents.SHIP_AUTO_LAUNCH.get(), false);
                coreStack.set(ModDataComponents.SHIP_AUTO_LAUNCH.get(), !current);
            }
        });
    }
}
