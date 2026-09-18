package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.AutoModeState;
import com.piranport.combat.TransformationManager;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：切换自动模式总开关，近防炮、导弹、战斗机同时启用或停用。
 *
 * <p>服务端在舰装核心上写入 {@link ModDataComponents#SHIP_AUTO_MODE}；
 * 客户端通过 DataComponent 网络同步自动感知变更。</p>
 */
public record ToggleAutoModePayload() implements CustomPacketPayload {

    public static final Type<ToggleAutoModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "toggle_auto_mode"));

    public static final StreamCodec<ByteBuf, ToggleAutoModePayload> STREAM_CODEC =
            StreamCodec.unit(new ToggleAutoModePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleAutoModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack coreStack = TransformationManager.findTransformedCore(player);
            if (coreStack.isEmpty()) return;

            AutoModeState current = AutoModeState.fromStack(coreStack);
            AutoModeState next = current.next();
            next.writeToStack(coreStack);
            TransformationManager.writeCoreToConfiguredSlot(player, coreStack);

            player.displayClientMessage(Component.translatable(
                    switch (next) {
                        case OFF -> "message.piranport.auto_mode_off";
                        case ON -> "message.piranport.auto_mode_on";
                    }
            ), true);
        });
    }
}
