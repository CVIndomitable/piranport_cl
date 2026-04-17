package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.entity.AircraftEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Set;
import java.util.UUID;

/**
 * C2S: no-GUI mode empty-hand right-click → recall all airborne aircraft.
 */
public record RecallAllAircraftPayload() implements CustomPacketPayload {
    public static final Type<RecallAllAircraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "recall_all_aircraft"));

    public static final StreamCodec<ByteBuf, RecallAllAircraftPayload> STREAM_CODEC =
            StreamCodec.unit(new RecallAllAircraftPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Per-player cooldown: minimum 20 ticks between recall attempts. */
    private static final java.util.Map<UUID, Long> lastRecallTick = new java.util.concurrent.ConcurrentHashMap<>();

    public static void handle(RecallAllAircraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) return;
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
    }
}
