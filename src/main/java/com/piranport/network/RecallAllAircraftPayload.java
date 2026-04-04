package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.entity.AircraftEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
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

    public static void handle(RecallAllAircraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sp)) return;
            if (sp.level().isClientSide()) return;

            // Only in no-GUI mode + transformed
            if (com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) return;
            if (!com.piranport.combat.TransformationManager.isPlayerTransformed(sp)) return;

            ServerLevel sl = (ServerLevel) sp.level();
            UUID ownerUUID = sp.getUUID();
            List<AircraftEntity> aircraft = sl.getEntitiesOfClass(
                    AircraftEntity.class,
                    new AABB(sp.getX() - 300, sp.getY() - 300, sp.getZ() - 300,
                             sp.getX() + 300, sp.getY() + 300, sp.getZ() + 300),
                    a -> ownerUUID.equals(a.getOwnerUUID()) && a.isAlive());

            for (AircraftEntity a : aircraft) {
                a.recallAndRemove();
            }

            if (!aircraft.isEmpty()) {
                ReconManager.endRecon(ownerUUID);
                FireControlManager.clearTargets(ownerUUID);
                sp.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_recalled", aircraft.size()), true);
            }
        });
    }
}
