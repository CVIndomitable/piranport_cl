package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.item.CannonItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

            // Find the medium/large cannon the player is holding (main hand or off hand)
            ItemStack weapon = ItemStack.EMPTY;
            int weaponSlot = -1;
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof CannonItem
                    && (mainHand.is(ModItems.MEDIUM_GUN.get()) || mainHand.is(ModItems.LARGE_GUN.get()))) {
                weapon = mainHand;
                weaponSlot = inv.selected;
            } else {
                ItemStack offHand = inv.offhand.get(0);
                if (offHand.getItem() instanceof CannonItem
                        && (offHand.is(ModItems.MEDIUM_GUN.get()) || offHand.is(ModItems.LARGE_GUN.get()))) {
                    weapon = offHand;
                    weaponSlot = 40;
                }
            }
            if (weapon.isEmpty()) return;

            // Already loaded
            LoadedAmmo current = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (current.hasAmmo()) {
                player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
                return;
            }

            // Find transformed core slot (to skip it during ammo scan)
            int coreSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreSlot = i;
                    break;
                }
            }
            if (coreSlot == -1 && weaponSlot != 40) {
                ItemStack offhand = inv.offhand.get(0);
                if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) {
                    coreSlot = 40;
                }
            }

            // Scan inventory for matching caliber ammo (same logic as auto-reload)
            int ammoSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && ShipCoreItem.matchesCaliber(ammo, weapon)) {
                    ammoSlot = i;
                    break;
                }
            }

            if (ammoSlot == -1) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            // Load the ammo
            ItemStack ammoStack = inv.items.get(ammoSlot);
            String ammoId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem()).toString();
            weapon.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(1, ammoId));
            ammoStack.shrink(1);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
            player.displayClientMessage(Component.translatable("message.piranport.reload_complete"), true);
        });
    }
}
