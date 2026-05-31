package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.block.entity.ShipCoreModifierBlockEntity;
import com.piranport.menu.ShipCoreModifierMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：应用改装配置到舰装核心
 */
public record ApplyModificationPayload(BlockPos pos, int weaponSlots, int enhancementSlots) implements CustomPacketPayload {
    public static final Type<ApplyModificationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "apply_modification"));

    public static final StreamCodec<ByteBuf, ApplyModificationPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ApplyModificationPayload::pos,
            ByteBufCodecs.INT, ApplyModificationPayload::weaponSlots,
            ByteBufCodecs.INT, ApplyModificationPayload::enhancementSlots,
            ApplyModificationPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ApplyModificationPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!(player.containerMenu instanceof ShipCoreModifierMenu menu)) return;
            if (!menu.getBlockPos().equals(payload.pos)) return;
            if (!menu.stillValid(player)) return;

            BlockEntity be = player.level().getBlockEntity(payload.pos);
            if (!(be instanceof ShipCoreModifierBlockEntity modifier)) return;
            if (menu.getBlockEntity() != modifier) return;

            // 设置配置
            modifier.setWeaponSlots(payload.weaponSlots);
            modifier.setEnhancementSlots(payload.enhancementSlots);

            // 应用改装
            boolean success = modifier.applyModification(player);
            if (success) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.modification_applied"), true);
            } else {
                player.displayClientMessage(
                        Component.translatable("message.piranport.modification_failed"), true);
            }
        });
    }
}
