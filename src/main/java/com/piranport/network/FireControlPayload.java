package com.piranport.network;

import com.piranport.aviation.FireControlManager;
import com.piranport.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class FireControlPayload {
    private final FireAction action;
    private final UUID targetUUID;

    public enum FireAction {
        LOCK, ADD, CANCEL
    }

    /** Sentinel UUID used when action is CANCEL. */
    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    public FireControlPayload(FireAction action, UUID targetUUID) {
        this.action = action;
        this.targetUUID = targetUUID;
    }

    /** Factory for CANCEL action (no target needed). */
    public static FireControlPayload cancel() {
        return new FireControlPayload(FireAction.CANCEL, EMPTY_UUID);
    }

    public static void encode(FireControlPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.action.ordinal());
        buf.writeLong(msg.targetUUID.getMostSignificantBits());
        buf.writeLong(msg.targetUUID.getLeastSignificantBits());
    }

    public static FireControlPayload decode(FriendlyByteBuf buf) {
        int actionOrdinal = buf.readVarInt();
        FireAction action = actionOrdinal >= 0 && actionOrdinal < FireAction.values().length
                ? FireAction.values()[actionOrdinal]
                : FireAction.CANCEL;
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new FireControlPayload(action, uuid);
    }

    public static void handle(FireControlPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            UUID playerUUID = player.getUUID();

            if (msg.action == FireAction.CANCEL) {
                FireControlManager.clearTargets(playerUUID);
            } else {
                Entity entity = player.serverLevel().getEntity(msg.targetUUID);
                // Limit lock range to simulation distance (in blocks)
                int simDistBlocks = player.serverLevel().getServer().getPlayerList().getSimulationDistance() * 16;
                // 允许锁 LivingEntity（怪/玩家/动物）和 AircraftEntity（敌方飞机）；
                // AircraftEntity 不继承 LivingEntity，需单独放行，否则火控无法对空
                boolean validTarget = entity != null && entity.isAlive() && entity != player
                        && !(entity instanceof net.minecraft.world.Container)
                        && (entity instanceof LivingEntity || entity instanceof AircraftEntity);

                // Line-of-sight check: prevent locking targets through walls
                boolean hasLineOfSight = false;
                if (validTarget && player.distanceTo(entity) <= simDistBlocks) {
                    net.minecraft.world.level.ClipContext clipCtx = new net.minecraft.world.level.ClipContext(
                            player.getEyePosition(),
                            entity.position().add(0, entity.getBbHeight() * 0.5, 0),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,
                            net.minecraft.world.level.ClipContext.Fluid.NONE,
                            player
                    );
                    net.minecraft.world.phys.BlockHitResult hit = player.level().clip(clipCtx);
                    hasLineOfSight = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
                }

                if (validTarget && hasLineOfSight) {
                    if (msg.action == FireAction.LOCK) {
                        FireControlManager.lock(playerUUID, entity.getUUID());
                    } else {
                        FireControlManager.addTarget(playerUUID, entity.getUUID());
                    }
                }
            }

            List<UUID> targets = FireControlManager.getTargets(playerUUID);
            ModPackets.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new FireControlSyncPayload(targets));
        });
        ctx.get().setPacketHandled(true);
    }
}
