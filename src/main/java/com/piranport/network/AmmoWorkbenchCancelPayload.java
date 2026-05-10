package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.block.entity.AmmoWorkbenchBlockEntity;
import com.piranport.menu.AmmoWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AmmoWorkbenchCancelPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<AmmoWorkbenchCancelPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ammo_workbench_cancel"));

    public static final StreamCodec<FriendlyByteBuf, AmmoWorkbenchCancelPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeBlockPos(p.pos),
                    buf -> new AmmoWorkbenchCancelPayload(buf.readBlockPos())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AmmoWorkbenchCancelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            BlockPos pos = payload.pos;
            if (!player.level().isLoaded(pos)) return;
            int minY = player.level().getMinBuildHeight();
            int maxY = player.level().getMaxBuildHeight();
            if (pos.getY() < minY || pos.getY() >= maxY) return;
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64.0) return;

            // Must have the corresponding menu open and bound to this position.
            if (!(player.containerMenu instanceof AmmoWorkbenchMenu menu)) return;
            if (!menu.getBlockPos().equals(pos)) return;
            if (!menu.stillValid(player)) return;

            if (!(player.level().getBlockEntity(pos) instanceof AmmoWorkbenchBlockEntity be)) return;
            if (!be.isCrafting()) return;

            // Cancel crafting and refund materials
            be.cancelCrafting();
        });
    }
}
