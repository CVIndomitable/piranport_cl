package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.menu.ShipCoreEquipmentMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端到服务端的网络包，用于打开舰装核心装备界面。
 * 当玩家按下 K 键且背包中有核心时发送。
 */
public record OpenShipEquipmentPayload(int coreSlot) implements CustomPacketPayload {

    public static final Type<OpenShipEquipmentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "open_ship_equipment"));

    public static final StreamCodec<ByteBuf, OpenShipEquipmentPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(OpenShipEquipmentPayload::new, OpenShipEquipmentPayload::coreSlot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理逻辑
     */
    public static void handle(OpenShipEquipmentPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                int slot = payload.coreSlot();

                // 验证槽位索引有效性（0-40：背包36 + 副手1 + 盔甲4）
                if (slot < 0 || slot >= 41) return;

                // 获取核心物品
                ItemStack coreStack = serverPlayer.getInventory().getItem(slot);
                if (!(coreStack.getItem() instanceof com.piranport.item.ShipCoreItem)) return;

                // 检查配置：只有在 GUI 模式启用时才打开界面
                if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
                    serverPlayer.displayClientMessage(
                            Component.translatable("message.piranport.gui_mode_disabled"),
                            true);
                    return;
                }

                // 打开装备界面
                serverPlayer.openMenu(
                        new SimpleMenuProvider(
                                (id, inv, p) -> new ShipCoreEquipmentMenu(id, inv, slot),
                                Component.translatable("container.piranport.ship_equipment")
                        ),
                        buf -> buf.writeVarInt(slot)
                );
            }
        });
    }
}
