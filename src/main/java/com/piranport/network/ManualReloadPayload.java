package com.piranport.network;

import com.piranport.combat.TransformationManager;
import com.piranport.item.CannonItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ManualReloadPayload {

    public ManualReloadPayload() {
    }

    public static void encode(ManualReloadPayload msg, FriendlyByteBuf buf) {
        // No data to encode
    }

    public static ManualReloadPayload decode(FriendlyByteBuf buf) {
        return new ManualReloadPayload();
    }

    public static void handle(ManualReloadPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;
            if (!TransformationManager.isPlayerTransformed(player)) return;

            Inventory inv = player.getInventory();

            // Find a manually-reloading cannon the player is holding (main hand or off hand)
            // Small-caliber guns auto-reload and don't use R key
            ItemStack weapon = ItemStack.EMPTY;
            int weaponSlot = -1;
            ItemStack mainHand = player.getMainHandItem();
            if (mainHand.getItem() instanceof CannonItem
                    && !mainHand.is(ModItems.SMALL_GUN.get()) && !mainHand.is(ModItems.SINGLE_SMALL_GUN.get())) {
                weapon = mainHand;
                weaponSlot = inv.selected;
            } else {
                ItemStack offHand = inv.offhand.get(0);
                if (offHand.getItem() instanceof CannonItem
                        && !offHand.is(ModItems.SMALL_GUN.get()) && !offHand.is(ModItems.SINGLE_SMALL_GUN.get())) {
                    weapon = offHand;
                    weaponSlot = 40;
                }
            }
            if (weapon.isEmpty()) {
                // 玩家手持发射器（鱼雷/导弹）按 R 时给出明确反馈，避免静默无响应；
                // 鱼雷/导弹发射器须用装填设施重装，不支持随身 R 键
                ItemStack mh = player.getMainHandItem();
                ItemStack oh = inv.offhand.get(0);
                if (mh.getItem() instanceof TorpedoLauncherItem || oh.getItem() instanceof TorpedoLauncherItem
                        || mh.getItem() instanceof MissileLauncherItem || oh.getItem() instanceof MissileLauncherItem) {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.use_reload_facility"), true);
                }
                return;
            }

            CannonItem cannonItem = (CannonItem) weapon.getItem();
            int barrelCount = cannonItem.getBarrelCount();

            // 1.20.1: Use NBT instead of DataComponents
            CompoundTag tag = weapon.getOrCreateTag();

            // Already loaded or reloading
            int currentAmmo = tag.getInt("LoadedAmmoCount");
            if (currentAmmo >= barrelCount) {
                long cooldownEnd = tag.getLong("WeaponCooldownEnd");
                if (cooldownEnd > player.level().getGameTime()) {
                    player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
                }
                return;
            }

            // Find transformed core (need it for SLOT_COOLDOWNS)
            ItemStack coreStack = ItemStack.EMPTY;
            int coreSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreStack = s;
                    coreSlot = i;
                    break;
                }
            }
            if (coreStack.isEmpty() && weaponSlot != 40) {
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

            // Count total matching ammo in inventory
            int totalAmmo = 0;
            String ammoId = "";
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && ShipCoreItem.matchesCaliber(ammo, weapon)) {
                    totalAmmo += ammo.getCount();
                    if (ammoId.isEmpty()) {
                        ammoId = BuiltInRegistries.ITEM.getKey(ammo.getItem()).toString();
                    }
                }
            }

            if (totalAmmo < barrelCount) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            // Consume barrelCount ammo across inventory
            int toConsume = barrelCount;
            for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && ShipCoreItem.matchesCaliber(ammo, weapon)) {
                    int take = Math.min(toConsume, ammo.getCount());
                    ammo.shrink(take);
                    toConsume -= take;
                }
            }

            // 1.20.1: Store in NBT
            tag.putInt("LoadedAmmoCount", barrelCount);
            tag.putString("LoadedAmmoId", ammoId);

            // Set cooldown — reload time starts now, weapon ready when cooldown expires
            int baseCooldown = ((CannonItem) weapon.getItem()).getCooldownTicks();
            int cooldownTicks = TransformationManager.boostedCooldown(player, baseCooldown);
            long gameTime = player.level().getGameTime();
            tag.putLong("WeaponCooldownEnd", gameTime + cooldownTicks);

            // Store slot cooldown in core
            CompoundTag coreTag = coreStack.getOrCreateTag();
            CompoundTag slotCooldowns = coreTag.getCompound("SlotCooldowns");
            slotCooldowns.putLong("slot_" + weaponSlot, gameTime + cooldownTicks);
            coreTag.put("SlotCooldowns", slotCooldowns);

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
            player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
