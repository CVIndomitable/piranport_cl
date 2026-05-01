package com.piranport.network;

import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.entity.AircraftEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * C2S: no-GUI mode empty-hand right-click → recall all airborne aircraft.
 */
public class RecallAllAircraftPayload {

    public RecallAllAircraftPayload() {
    }

    public static void encode(RecallAllAircraftPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static RecallAllAircraftPayload decode(FriendlyByteBuf buf) {
        return new RecallAllAircraftPayload();
    }

    /** Per-player cooldown: minimum 20 ticks between recall attempts. */
    private static final java.util.Map<UUID, Long> lastRecallTick = new java.util.concurrent.ConcurrentHashMap<>();

    /** 玩家退出时清理 rate-limit 记录，防止长生命周期服务器内存泄漏。
     *  由 GameEvents.onPlayerLogout 调用。 */
    public static void onPlayerDisconnect(UUID playerUUID) {
        lastRecallTick.remove(playerUUID);
    }

    public static void handle(RecallAllAircraftPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            if (sp.level().isClientSide()) return;

            // Only in no-GUI mode + transformed
            if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) return;
            if (!com.piranport.combat.TransformationManager.isPlayerTransformed(sp)) return;

            // Rate limit: 20 ticks between recalls
            long now = sp.level().getGameTime();
            Long last = lastRecallTick.get(sp.getUUID());
            if (last != null && now - last < 20) return;
            lastRecallTick.put(sp.getUUID(), now);

            UUID ownerUUID = sp.getUUID();
            Set<AircraftEntity> aircraft = AircraftIndex.snapshot(ownerUUID);
            int recalled = 0;
            for (AircraftEntity a : aircraft) {
                if (!a.isAlive()) continue;
                a.startReturning("recall_all_key");
                recalled++;
            }

            if (recalled > 0) {
                ReconManager.endRecon(ownerUUID);
                FireControlManager.clearTargets(ownerUUID);
                sp.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_recalled", recalled), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
