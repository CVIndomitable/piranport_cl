package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ArtilleryConfigOverrideSavedData;
import com.piranport.config.ConfigToolPermissions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Reset one artillery config override entry back to datapack defaults.
 */
public record ResetSingleConfigPayload(String category, String key) implements CustomPacketPayload {

    public static final Type<ResetSingleConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "reset_single_config"));

    private static final int MAX_STRING_LENGTH = 256;

    public static final StreamCodec<ByteBuf, ResetSingleConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), ResetSingleConfigPayload::category,
            ByteBufCodecs.stringUtf8(MAX_STRING_LENGTH), ResetSingleConfigPayload::key,
            ResetSingleConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResetSingleConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }

            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                PiranPort.LOGGER.warn("Player {} tried to reset config entry without admin permission",
                        serverPlayer.getName().getString());
                return;
            }

            ServerLevel level = serverPlayer.serverLevel();
            ArtilleryConfigOverrideSavedData data = ArtilleryConfigOverrideSavedData.get(level);

            if ("cannon".equals(payload.category())) {
                data.removeAllCannonOverrides(payload.key());
            } else if ("projectile".equals(payload.category())) {
                data.removeProjectileOverride(payload.key());
            } else {
                PiranPort.LOGGER.warn("Player {} sent invalid config reset category {}",
                        serverPlayer.getName().getString(), payload.category());
                return;
            }

            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.piranport.config_item_reset", payload.key())
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            PiranPort.LOGGER.info("Player {} reset {} config override {}",
                    serverPlayer.getName().getString(), payload.category(), payload.key());
        });
    }
}
