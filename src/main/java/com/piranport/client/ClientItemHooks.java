package com.piranport.client;

import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.dungeon.client.DungeonHudLayer;
import com.piranport.dungeon.client.DungeonResultScreen;
import com.piranport.dungeon.client.DungeonReviveScreen;
import com.piranport.dungeon.client.TownScrollScreen;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.AircraftItem;
import com.piranport.item.MissileLauncherItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.network.CannonImpactEffectPayload;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class ClientItemHooks {
    private static final int FRIENDLY_BLUE = 0x3399FF;
    private static final int HOSTILE_RED = 0xFF3333;
    private static final int FC_TARGET_RED = 0xFF0000;
    private static final int ALLY_GREEN = 0x33FF33;

    private ClientItemHooks() {}

    public static boolean hasShiftDown() {
        return Screen.hasShiftDown();
    }

    public static Player getClientPlayer() {
        return Minecraft.getInstance().player;
    }

    public static long getClientGameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : -1L;
    }

    public static boolean toggleArtilleryScope(Player player, ItemStack stack) {
        if (ClientScopeHandler.isScoping()) {
            ClientScopeHandler.exitScope();
        } else {
            ClientScopeHandler.enterScope(player, stack);
        }
        return true;
    }

    public static void handleTorpedoGuidanceState(boolean active, int entityId) {
        if (active) {
            ClientTorpedoGuidance.handleStart(entityId);
        } else {
            ClientTorpedoGuidance.handleEnd();
        }
    }

    public static void handleReconState(boolean active, int entityId) {
        if (active) {
            com.piranport.aviation.ClientReconData.handleReconStart(entityId);
        } else {
            com.piranport.aviation.ClientReconData.handleReconEnd();
        }
    }

    public static void triggerCameraShake(float intensity, int durationTicks) {
        CameraShakeHandler.trigger(intensity, durationTicks);
    }

    public static void spawnCannonImpactEffect(Object payload) {
        if (payload instanceof CannonImpactEffectPayload impact) {
            CannonImpactEffects.spawn(impact);
        }
    }

    public static void updateAswSonar(int aircraftEntityId, List<Integer> detectedEntityIds) {
        com.piranport.aviation.ClientAswSonarData.update(aircraftEntityId, detectedEntityIds);
    }

    public static void setFireControlTargets(List<UUID> targetUUIDs) {
        com.piranport.aviation.ClientFireControlData.setTargets(targetUUIDs);
    }

    public static void setServerSolverStats(int ternaryIters, int newtonIters) {
        ClientScopeHandler.setServerSolverStats(ternaryIters, newtonIters);
    }

    public static boolean isReconEntity(int entityId) {
        return com.piranport.aviation.ClientReconData.isInReconMode()
                && com.piranport.aviation.ClientReconData.getReconEntityId() == entityId;
    }

    public static boolean isInReconMode() {
        return com.piranport.aviation.ClientReconData.isInReconMode();
    }

    public static void openTownScrollScreen() {
        Minecraft.getInstance().setScreen(new TownScrollScreen());
    }

    public static void openDungeonResultScreen(String stageName, long timeMillis,
                                                boolean isFirstClear, List<String> rewardNames) {
        DungeonHudLayer.clearDungeonState();
        Minecraft.getInstance().setScreen(new DungeonResultScreen(
                stageName, timeMillis, isFirstClear, rewardNames));
    }

    public static void openDungeonReviveScreen() {
        Minecraft.getInstance().setScreen(new DungeonReviveScreen());
    }

    public static void updateDungeonNode(String nodeId) {
        DungeonHudLayer.updateNode(nodeId);
    }

    public static void setDungeonState(String stageName, String nodeId, long timerStartMillis) {
        DungeonHudLayer.setDungeonState(stageName, nodeId, timerStartMillis);
    }

    public static boolean shouldAircraftGlow(AircraftEntity aircraft) {
        if (isFireControlTarget(aircraft)) {
            return true;
        }
        if (!com.piranport.ClientTickHandler.isHighlightEnabled()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return aircraft.getOwnerUUID() != null;
    }

    public static int getAircraftGlowColor(AircraftEntity aircraft, int fallbackColor) {
        if (isFireControlTarget(aircraft)) {
            return FC_TARGET_RED;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return fallbackColor;
        }
        if (aircraft.isOwnedByPlayer(mc.player)) {
            return FRIENDLY_BLUE;
        }
        java.util.UUID ownerUUID = aircraft.getOwnerUUID();
        if (ownerUUID != null && ownerUUID.equals(mc.player.getUUID())) {
            return FRIENDLY_BLUE;
        }
        if (aircraft.getFlightState() == AircraftEntity.FlightState.ATTACKING
                && (ownerUUID == null || !ownerUUID.equals(mc.player.getUUID()))) {
            return HOSTILE_RED;
        }
        return ALLY_GREEN;
    }

    private static boolean isFireControlTarget(AircraftEntity aircraft) {
        return com.piranport.aviation.ClientFireControlData.getTargets().contains(aircraft.getUUID());
    }

    /**
     * Appends current reload readiness for weapons that are directly in the
     * local player's inventory.
     */
    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Inventory inv = mc.player.getInventory();
        ItemStack coreStack = TransformationManager.findTransformedCore(mc.player);
        if (coreStack.isEmpty()) return;

        int weaponSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == stack) {
                weaponSlot = i;
                break;
            }
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
                tooltip.add(Component.translatable(reloading.hasAmmo()
                                ? "tooltip.piranport.weapon_reloading"
                                : "tooltip.piranport.weapon_not_loaded")
                        .withStyle(reloading.hasAmmo() ? ChatFormatting.YELLOW : ChatFormatting.RED));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(ChatFormatting.YELLOW));
            }
        } else if (needsLoadedAmmo) {
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            boolean hasAmmo;
            if (stack.getItem() instanceof TorpedoLauncherItem tl) {
                hasAmmo = loaded.count() >= tl.getTubeCount();
            } else if (stack.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
                hasAmmo = loaded.count() >= ai.getEffectiveData(mc.level).barrels();
            } else {
                hasAmmo = loaded.hasAmmo();
            }
            tooltip.add(Component.translatable(hasAmmo
                            ? "tooltip.piranport.weapon_ready"
                            : "tooltip.piranport.weapon_not_loaded")
                    .withStyle(hasAmmo ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else if (isAutoReloadMissile) {
            MissileLauncherItem mlCheck = (MissileLauncherItem) stack.getItem();
            Item ammoItem = mlCheck.getAmmoItem();
            boolean hasAmmoInInventory = false;
            for (ItemStack s : inv.items) {
                if (!s.isEmpty() && s.is(ammoItem)) {
                    hasAmmoInInventory = true;
                    break;
                }
            }
            if (!hasAmmoInInventory) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) hasAmmoInInventory = true;
            }
            tooltip.add(Component.translatable(hasAmmoInInventory
                            ? "tooltip.piranport.weapon_ready"
                            : "tooltip.piranport.weapon_not_loaded")
                    .withStyle(hasAmmoInInventory ? ChatFormatting.GREEN : ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
