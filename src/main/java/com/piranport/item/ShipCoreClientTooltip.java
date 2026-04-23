package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

// Holds all client-only tooltip logic for ShipCoreItem. Kept in its own class so
// ShipCoreItem's bytecode never references @OnlyIn(CLIENT) types like Minecraft/LocalPlayer,
// which RuntimeDistCleaner blocks on a dedicated server during class loading.
public final class ShipCoreClientTooltip {
    private ShipCoreClientTooltip() {}

    public record LoadInfo(int totalLoad, double engineSpeedBonus) {}

    public static LoadInfo currentLoadAndBonus(ItemStack stack) {
        Player cp = Minecraft.getInstance().player;
        if (cp == null) return null;
        int total = TransformationManager.getInventoryWeaponLoad(cp.getInventory())
                + TransformationManager.getCoreArmorLoad(stack);
        double bonus = TransformationManager.getCoreEngineSpeedBonus(stack);
        return new LoadInfo(total, bonus);
    }

    public static void appendNoGuiLoadTooltip(ItemStack stack, ShipCoreItem.ShipType shipType,
                                              List<Component> tooltipComponents) {
        Player cp = Minecraft.getInstance().player;
        if (cp == null) return;
        Inventory inv = cp.getInventory();
        int bestMaxLoad = 0;
        for (ItemStack s : inv.items) {
            if (s.getItem() instanceof ShipCoreItem sci2) {
                bestMaxLoad = Math.max(bestMaxLoad, sci2.getShipType().maxLoad);
            }
        }
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() instanceof ShipCoreItem sci2) {
            bestMaxLoad = Math.max(bestMaxLoad, sci2.getShipType().maxLoad);
        }
        boolean isActive = shipType.maxLoad >= bestMaxLoad;
        if (!isActive) {
            tooltipComponents.add(Component.translatable("tooltip.piranport.core_inactive")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        int weaponLoad = TransformationManager.getInventoryWeaponLoad(inv);
        int armorLoad = TransformationManager.getCoreArmorLoad(stack);
        tooltipComponents.add(Component.translatable(
                "container.piranport.load", weaponLoad + armorLoad,
                isActive ? bestMaxLoad : shipType.maxLoad));
        int capacity = shipType.enhancementSlots;
        ItemContainerContents armorContents = stack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> storedArmor = NonNullList.withSize(capacity, ItemStack.EMPTY);
        armorContents.copyInto(storedArmor);
        int armorBonus = TransformationManager.getCoreArmorBonus(stack);
        tooltipComponents.add(Component.translatable(
                "tooltip.piranport.core_armor_slots", armorBonus, capacity));
        for (ItemStack s : storedArmor) {
            if (!s.isEmpty()) {
                tooltipComponents.add(Component.literal("  • ").append(s.getHoverName()));
            }
        }
    }

    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Inventory inv = mc.player.getInventory();

        ItemStack coreStack = ItemStack.EMPTY;
        for (ItemStack s : inv.items) {
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                coreStack = s;
                break;
            }
        }
        if (coreStack.isEmpty()) {
            ItemStack offhand = inv.offhand.get(0);
            if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) {
                coreStack = offhand;
            }
        }
        if (coreStack.isEmpty()) return;

        int weaponSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == stack) { weaponSlot = i; break; }
        }
        if (weaponSlot == -1 && inv.offhand.get(0) == stack) weaponSlot = 40;
        if (weaponSlot == -1) return;

        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = mc.level.getGameTime();

        boolean onCooldown = cooldowns.isOnCooldown(weaponSlot, gameTime);
        boolean isManualMode = !com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get();
        boolean isAutoReloadMissile = stack.getItem() instanceof MissileLauncherItem ml0 && !ml0.isManualReload();
        boolean needsLoadedAmmo = !isAutoReloadMissile
                && ((isManualMode && !(stack.getItem() instanceof AircraftItem))
                    || (stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload()));

        if (onCooldown) {
            if (needsLoadedAmmo) {
                LoadedAmmo reloading = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
                if (reloading.hasAmmo()) {
                    tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                            .withStyle(ChatFormatting.RED));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } else if (needsLoadedAmmo) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            boolean hasAmmo;
            if (stack.getItem() instanceof TorpedoLauncherItem tl) {
                hasAmmo = loaded.count() >= tl.getTubeCount();
            } else if (stack.getItem() instanceof CannonItem ci) {
                hasAmmo = loaded.count() >= ci.getBarrelCount();
            } else {
                hasAmmo = loaded.hasAmmo();
            }
            if (hasAmmo) {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                        .withStyle(ChatFormatting.RED));
            }
        } else if (isAutoReloadMissile) {
            MissileLauncherItem mlCheck = (MissileLauncherItem) stack.getItem();
            Item ammoItem = mlCheck.getAmmoItem();
            boolean hasAmmoInInventory = false;
            for (ItemStack s : inv.items) {
                if (!s.isEmpty() && s.is(ammoItem)) { hasAmmoInInventory = true; break; }
            }
            if (!hasAmmoInInventory) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) hasAmmoInInventory = true;
            }
            if (hasAmmoInInventory) {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                        .withStyle(ChatFormatting.RED));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
