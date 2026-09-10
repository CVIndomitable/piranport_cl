package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.ReloadHelper;
import com.piranport.combat.TransformationManager;
import com.piranport.item.MissileLauncherItem;
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
            ItemStack coreStack = TransformationManager.findTransformedCore(player);
            if (coreStack.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.piranport.no_core"), true);
                return;
            }
            // 核心所在槽位：头盔模式传 -2（不在背包中，不参与背包扫描排除）
            int coreSlot = 40;
            {
                boolean found = false;
                for (int i = 0; i < inv.items.size(); i++) {
                    if (inv.items.get(i) == coreStack) { coreSlot = i; found = true; break; }
                }
                if (!found && inv.offhand.get(0) == coreStack) { coreSlot = 40; found = true; }
                if (!found) coreSlot = -2;
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

            // 火炮：依据策划决策/武器/07-火炮装填双模式.md
            // 自动模式 = 开火后冷却（FPS 风格），不需要 R 键；手动模式 = 必须按 R 键启动读条
            if (mainHand.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
                if (!ai.isAutoLoading()) {
                    // 手动模式：仅在空炮且无读条时启动读条；已装弹或已在读条时提示
                    com.piranport.item.ShipCoreCombat.tryManualCannonReload(player, coreStack, mainHand);
                }
                return;
            } else if (offHand.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
                if (!ai.isAutoLoading()) {
                    com.piranport.item.ShipCoreCombat.tryManualCannonReload(player, coreStack, offHand);
                }
                return;
            }
        });
    }
}

