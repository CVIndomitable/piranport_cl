package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

public record FireControlPayload(FireAction action, UUID targetUUID) implements CustomPacketPayload {

    public enum FireAction {
        LOCK, ADD, CANCEL;

        static final StreamCodec<ByteBuf, FireAction> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(i -> {
                    var vals = values();
                    return i >= 0 && i < vals.length ? vals[i] : CANCEL;
                }, Enum::ordinal);
    }

    /** Sentinel UUID used when action is CANCEL. */
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public static final Type<FireControlPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "fire_control"));

    public static final StreamCodec<ByteBuf, FireControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                FireAction.STREAM_CODEC.encode(buf, p.action());
                buf.writeLong(p.targetUUID().getMostSignificantBits());
                buf.writeLong(p.targetUUID().getLeastSignificantBits());
            },
            buf -> {
                FireAction action = FireAction.STREAM_CODEC.decode(buf);
                UUID uuid = new UUID(buf.readLong(), buf.readLong());
                return new FireControlPayload(action, uuid);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** Factory for CANCEL action (no target needed). */
    public static FireControlPayload cancel() {
        return new FireControlPayload(FireAction.CANCEL, EMPTY_UUID);
    }

    // ===== Server-side handler =====

    public static void handle(FireControlPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            UUID playerUUID = player.getUUID();

            if (payload.action() == FireAction.CANCEL) {
                FireControlManager.clearTargets(playerUUID);
            } else {
                Entity entity = player.serverLevel().getEntity(payload.targetUUID());
                // Limit lock range to simulation distance (in blocks)
                int simDistBlocks = player.serverLevel().getServer().getPlayerList().getSimulationDistance() * 16;
                if (entity instanceof LivingEntity le && le.isAlive() && entity != player
                        && !(entity instanceof net.minecraft.world.Container)
                        && player.distanceTo(entity) <= simDistBlocks) {
                    if (payload.action() == FireAction.LOCK) {
                        FireControlManager.lock(playerUUID, entity.getUUID());
                    } else {
                        FireControlManager.addTarget(playerUUID, entity.getUUID());
                    }
                }
            }

            List<UUID> targets = FireControlManager.getTargets(playerUUID);
            PacketDistributor.sendToPlayer(player, new FireControlSyncPayload(targets));
        });
    }
}
