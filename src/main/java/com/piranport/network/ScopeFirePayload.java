package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.item.ShipCoreItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S：玩家从瞄准镜模式开火。useBallisticAim=false 时为快速点击（正常方向开火），true 时使用弹道解算。 */
public record ScopeFirePayload(boolean useBallisticAim, double targetX, double targetY, double targetZ) implements CustomPacketPayload {

    public static final Type<ScopeFirePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "scope_fire"));

    public static final StreamCodec<ByteBuf, ScopeFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.useBallisticAim());
                buf.writeDouble(p.targetX());
                buf.writeDouble(p.targetY());
                buf.writeDouble(p.targetZ());
            },
            buf -> {
                boolean useBallistic = buf.readBoolean();
                double tx = buf.readDouble();
                double ty = buf.readDouble();
                double tz = buf.readDouble();
                return new ScopeFirePayload(useBallistic, tx, ty, tz);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** 快捷工厂：快速点击（正常方向开火） */
    public static ScopeFirePayload quickFire() {
        return new ScopeFirePayload(false, 0, 0, 0);
    }

    /** 快捷工厂：瞄准开火（弹道解算） */
    public static ScopeFirePayload aimedFire(double x, double y, double z) {
        return new ScopeFirePayload(true, x, y, z);
    }

    public static void handle(ScopeFirePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (weapon.isEmpty()) return;

            if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
                return;
            }

            if (payload.useBallisticAim()) {
                double dx = payload.targetX() - player.getX();
                double dy = payload.targetY() - player.getY();
                double dz = payload.targetZ() - player.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq > 300.0 * 300.0) {
                    PiranPort.LOGGER.warn("ScopeFirePayload target too far ({}m), ignored", Math.sqrt(distSq));
                    return;
                }
                ShipCoreItem.fireFromScope(player, weapon,
                        payload.targetX(), payload.targetY(), payload.targetZ());
            } else {
                ShipCoreItem.tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND);
            }
        });
    }
}
