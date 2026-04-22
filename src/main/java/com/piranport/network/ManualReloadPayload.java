package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.CannonItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
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

            // Already loaded or reloading
            LoadedAmmo current = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (current.hasAmmo() && current.count() >= barrelCount) {
                WeaponCooldown wc = weapon.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
                if (wc.isOnCooldown(player.level().getGameTime())) {
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
            weapon.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(barrelCount, ammoId));

            // Set cooldown — reload time starts now, weapon ready when cooldown expires
            int baseCooldown = ((CannonItem) weapon.getItem()).getCooldownTicks();
            int cooldownTicks = TransformationManager.boostedCooldown(player, baseCooldown);
            long gameTime = player.level().getGameTime();
            weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(gameTime, cooldownTicks));
            SlotCooldowns cooldowns = coreStack.getOrDefault(
                    ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, gameTime));

            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
            player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
        });
    }
}
