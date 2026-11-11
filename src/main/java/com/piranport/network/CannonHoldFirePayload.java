package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.cannon.CannonHoldFireManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Press, held heartbeat and release; the server chooses weapons and firing cadence. */
public record CannonHoldFirePayload(byte action, boolean aimed, double x, double y, double z)
        implements CustomPacketPayload {
    public static final byte START = 0, UPDATE = 1, STOP = 2;
    public static final Type<CannonHoldFirePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "cannon_hold_fire"));
    public static final StreamCodec<ByteBuf, CannonHoldFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.action());
                buf.writeBoolean(p.aimed());
                buf.writeDouble(p.x());
                buf.writeDouble(p.y());
                buf.writeDouble(p.z());
            },
            buf -> new CannonHoldFirePayload(buf.readByte(), buf.readBoolean(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static CannonHoldFirePayload of(byte action, Vec3 target) {
        return target == null ? new CannonHoldFirePayload(action, false, 0, 0, 0)
                : new CannonHoldFirePayload(action, true, target.x, target.y, target.z);
    }

    public static void handle(CannonHoldFirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (payload.action == STOP) {
                CannonHoldFireManager.stop(player);
                return;
            }
            if (payload.action != START && payload.action != UPDATE) return;
            Vec3 target = null;
            if (payload.aimed) {
                if (!Double.isFinite(payload.x) || !Double.isFinite(payload.y) || !Double.isFinite(payload.z)) return;
                Vec3 candidate = new Vec3(payload.x, payload.y, payload.z);
                double range = Math.max(500.0, player.server.getPlayerList().getSimulationDistance() * 16.0);
                if (candidate.distanceToSqr(player.position()) <= range * range) target = candidate;
            }
            if (payload.action == START) CannonHoldFireManager.start(player, target);
            else CannonHoldFireManager.update(player, target);
        });
    }
}
