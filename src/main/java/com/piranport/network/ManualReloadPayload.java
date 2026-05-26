package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.ReloadHelper;
import com.piranport.combat.TransformationManager;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ManualReloadPayload() implements CustomPacketPayload {
    public static final Type<ManualReloadPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "manual_reload"));

    public static final StreamCodec<ByteBuf, ManualReloadPayload> STREAM_CODEC =
            StreamCodec.unit(new ManualReloadPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManualReloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;
            if (!TransformationManager.isPlayerTransformed(player)) return;

            Inventory inv = player.getInventory();

            // 查找已变身的舰装核心
            ItemStack coreStack = ItemStack.EMPTY;
            int coreSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreStack = s;
                    coreSlot = i;
                    break;
                }
            }
            if (coreStack.isEmpty()) {
                ItemStack offhand = inv.offhand.get(0);
                if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) {
                    coreStack = offhand;
                    coreSlot = 40;
                }
            }
            if (coreStack.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.piranport.no_core"), true);
                return;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = inv.offhand.get(0);

            // 鱼雷发射器：需要装备"鱼雷再装填"增强
            if (mainHand.getItem() instanceof TorpedoLauncherItem) {
                if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                    ReloadHelper.reloadTorpedoLauncher(player, inv, mainHand, inv.selected, coreStack, coreSlot);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.use_reload_facility"), true);
                }
                return;
            } else if (offHand.getItem() instanceof TorpedoLauncherItem) {
                if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                    ReloadHelper.reloadTorpedoLauncher(player, inv, offHand, 40, coreStack, coreSlot);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.use_reload_facility"), true);
                }
                return;
            }

            // 导弹发射器：必须使用装填设施
            if (mainHand.getItem() instanceof MissileLauncherItem || offHand.getItem() instanceof MissileLauncherItem) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.use_reload_facility"), true);
                return;
            }

            // 火炮在 Phase 4 自动补给，R 键无效
            if (mainHand.getItem() instanceof com.piranport.artillery.ArtilleryItem
                    || offHand.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
                return;
            }
        });
    }
}

