package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.CannonItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoItem;
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

            // Find transformed core first (needed for torpedo reload check)
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

            // Find weapon in main hand or off hand
            ItemStack weapon = ItemStack.EMPTY;
            int weaponSlot = -1;
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = inv.offhand.get(0);

            // Check for torpedo launcher with 鱼雷再装填 enhancement
            if (mainHand.getItem() instanceof TorpedoLauncherItem) {
                if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                    reloadTorpedoLauncher(player, inv, mainHand, inv.selected, coreStack, coreSlot);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.use_reload_facility"), true);
                }
                return;
            } else if (offHand.getItem() instanceof TorpedoLauncherItem) {
                if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                    reloadTorpedoLauncher(player, inv, offHand, 40, coreStack, coreSlot);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.use_reload_facility"), true);
                }
                return;
            }

            // Check for missile launcher (always requires reload facility)
            if (mainHand.getItem() instanceof MissileLauncherItem || offHand.getItem() instanceof MissileLauncherItem) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.use_reload_facility"), true);
                return;
            }

            // Find a manually-reloading cannon (small-caliber guns auto-reload and don't use R key)
            if (mainHand.getItem() instanceof CannonItem
                    && !mainHand.is(ModItems.SMALL_GUN.get()) && !mainHand.is(ModItems.SINGLE_SMALL_GUN.get())) {
                weapon = mainHand;
                weaponSlot = inv.selected;
            } else if (offHand.getItem() instanceof CannonItem
                    && !offHand.is(ModItems.SMALL_GUN.get()) && !offHand.is(ModItems.SINGLE_SMALL_GUN.get())) {
                weapon = offHand;
                weaponSlot = 40;
            }
            if (weapon.isEmpty()) {
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

    /**
     * Reload torpedo launcher from inventory when 鱼雷再装填 enhancement is equipped.
     */
    private static void reloadTorpedoLauncher(Player player, Inventory inv, ItemStack launcherStack,
                                               int weaponSlot, ItemStack coreStack, int coreSlot) {
        if (!(launcherStack.getItem() instanceof TorpedoLauncherItem launcher)) return;

        int tubeCount = launcher.getTubeCount();
        int caliber = launcher.getCaliber();

        // Check if already fully loaded
        LoadedAmmo current = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (current.hasAmmo() && current.count() >= tubeCount) {
            WeaponCooldown wc = launcherStack.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
            if (wc.isOnCooldown(player.level().getGameTime())) {
                player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
            }
            return;
        }

        // Find first matching torpedo to determine type (strict: only consume same item type)
        TorpedoItem torpedoType = null;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                torpedoType = ti;
                break;
            }
        }
        if (torpedoType == null && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                torpedoType = ti;
            }
        }
        if (torpedoType == null) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Count available ammo of the same type
        int needed = tubeCount - (current.hasAmmo() ? current.count() : 0);
        int available = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() == torpedoType) {
                available += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                available += oh.getCount();
            }
        }

        if (available < needed) {
            player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
            return;
        }

        // Consume needed ammo across inventory
        int toConsume = needed;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() == torpedoType) {
                int take = Math.min(toConsume, s.getCount());
                s.shrink(take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                int take = Math.min(toConsume, oh.getCount());
                oh.shrink(take);
                toConsume -= take;
            }
        }

        // Set loaded ammo
        String ammoId = BuiltInRegistries.ITEM.getKey(torpedoType).toString();
        launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(tubeCount, ammoId));

        // Set cooldown — reload time starts now, weapon ready when cooldown expires
        int cooldownTicks = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
        long gameTime = player.level().getGameTime();
        launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                WeaponCooldown.of(gameTime, cooldownTicks));
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, gameTime));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
    }
}
