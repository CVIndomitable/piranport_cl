package com.piranport.item;

import com.piranport.aviation.AircraftFireStrategy;
import com.piranport.combat.TransformationManager;
import com.piranport.component.SlotCooldowns;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.piranport.combat.depthcharge.DepthChargeFireStrategy;
import com.piranport.combat.missile.MissileFireStrategy;
import com.piranport.combat.torpedo.TorpedoFireStrategy;
import com.piranport.platform.ClientHooks;
import org.jetbrains.annotations.Nullable;
import com.piranport.combat.cannon.CannonAim;
import com.piranport.combat.cannon.CannonAim.*;
import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.combat.cannon.CannonReloading;
import com.piranport.combat.cannon.CannonInventory;
import com.piranport.combat.cannon.CannonSalvos;
import static com.piranport.combat.cannon.CannonInventory.findCoreSlotIndex;
import static com.piranport.combat.cannon.CannonFiring.fireLoadedCannon;

/**
 * 舰装战斗入口：定位核心和武器后，派发到各武器策略。
 * 火炮装填、弹药、弹道与齐射规则由 combat.cannon 各组件负责。
 * 所有战斗状态修改仅在服务端主线程执行。
 */
public final class ShipCoreCombat {
    private ShipCoreCombat() {}

    public static final int ARTILLERY_AIM_NONE = CannonAim.NONE;
    public static final int ARTILLERY_AIM_TARGET = CannonAim.TARGET;
    public static final int ARTILLERY_AIM_MAX_RANGE = CannonAim.MAX_RANGE;
    public static final int ARTILLERY_AIM_DIRECT_TARGET = CannonAim.DIRECT_TARGET;

    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        return tryFireFromInventory(level, player, hand, new NoAim());
    }

    private static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand, CannonAim aim) {
        if (level.isClientSide) return false;
        // Phase 12: 旁观者模式不开火
        if (!player.isAlive() || player.isSpectator()) return false;

        Inventory inv = player.getInventory();
        int weaponSlot = (hand == InteractionHand.MAIN_HAND) ? inv.selected : 40;
        ItemStack weapon = (weaponSlot == 40) ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Find transformed core
        int coreInventorySlot = findCoreSlotIndex(inv, player, weaponSlot);
        if (coreInventorySlot == -1) return false;
        ItemStack coreStack;
        if (coreInventorySlot == 40) {
            coreStack = inv.offhand.get(0);
        } else if (coreInventorySlot == -2) {
            coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        } else {
            coreStack = inv.items.get(coreInventorySlot);
        }
        if (coreStack.isEmpty()) return false;

        // 火炮单击优化：手持冷却时自动找同类型可用炮
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            int[] best = findBestArtillerySlot(player, weapon.getItem(), coreStack);
            if (best != null) {
                weaponSlot = best[0];
                coreInventorySlot = best[1];
            }
        }

        return fireWeaponAtSlot(level, player, coreStack, weaponSlot, coreInventorySlot, aim);
    }

    private static boolean fireWeaponAtSlot(Level level, Player player, ItemStack coreStack, int weaponSlot, int coreInventorySlot, CannonAim aim) {
        Inventory inv = player.getInventory();
        ItemStack weapon = (weaponSlot == 40) ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Per-slot cooldown (keyed by inventory slot index)
        SlotCooldowns cooldowns = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)
                && cooldowns.isOnCooldown(weaponSlot, level.getGameTime())) {
            com.piranport.debug.PiranPortDebug.event(
                    "Fire BLOCKED by cooldown | slot={} weapon={} remainTicks={}",
                    weaponSlot,
                    BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                    cooldowns.endTick().getOrDefault(weaponSlot, 0L) - level.getGameTime());
            return false;
        }

        // Torpedo launcher — requires 鱼雷再装填 enhancement for auto-reload
        if (weapon.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                TorpedoFireStrategy.fireTorpedosInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, torpedoLauncher, cooldowns);
            } else {
                TorpedoFireStrategy.fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, torpedoLauncher, cooldowns);
            }
            return true;
        }

        // Depth charge launcher
        if (weapon.getItem() instanceof DepthChargeLauncherItem dcLauncher) {
            DepthChargeFireStrategy.fireDepthCharges(level, player, coreStack, inv, weaponSlot, coreInventorySlot, dcLauncher, cooldowns);
            return true;
        }

        // Missile launcher
        if (weapon.getItem() instanceof MissileLauncherItem missileLauncher) {
            MissileFireStrategy.fireMissiles(level, player, coreStack, inv, weaponSlot, coreInventorySlot, missileLauncher, cooldowns);
            return true;
        }

        // Aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            AircraftFireStrategy.launchAircraftInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, cooldowns);
            return true;
        }

        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            return fireLoadedCannon(level, player, coreStack, inv, weaponSlot, coreInventorySlot,
                    weapon, aim);
        }

        return false;
    }

    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        ClientHooks.appendWeaponCooldownTooltip(stack, tooltip);
    }

    public static void fireFromScope(Player player, ItemStack weapon, double tx, double ty, double tz) {
        Vec3 target = new Vec3(tx, ty, tz);
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new Aimed(target));
    }

    public static void fireDirectAt(Player player, ItemStack weapon, double tx, double ty, double tz) {
        Vec3 target = new Vec3(tx, ty, tz);
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new DirectAim(target));
    }

    public static void fireMaxRange(Player player, ItemStack weapon) {
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new MaxRange());
    }

    public static void fireMaxRange(ServerPlayer player, ItemStack weapon) {
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new MaxRange());
    }

    public static int recallAllAircraft(ServerLevel level, Player player) {
        return AircraftFireStrategy.recallAllAircraft(level, player);
    }

    public static void refillAircraftFuel(Player player, ItemStack coreStack) {
        AircraftFireStrategy.refillAircraftFuel(player, coreStack);
    }

    public static boolean tryAutoLaunchFighter(Level level, Player player, ItemStack coreStack, int coreSlot) {
        return AircraftFireStrategy.tryAutoLaunchFighter(level, player, coreStack, coreSlot);
    }

    public static boolean tryAutoFireAntiAirMissile(Level level, Player player, ItemStack coreStack, int coreSlot) {
        return MissileFireStrategy.tryAutoFireAntiAirMissile(level, player, coreStack, coreSlot);
    }

    public static void tickCannonAutoReload(Player player, ItemStack coreStack) {
        CannonReloading.tickCannonAutoReload(player, coreStack);
    }

    public static void tryManualCannonReload(Player player, ItemStack coreStack, int coreSlot, ItemStack weapon) {
        CannonReloading.tryManualCannonReload(player, coreStack, coreSlot, weapon);
    }

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        return CannonAmmoRules.matchesCaliber(ammo, weapon, null);
    }

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon, @Nullable Level level) {
        return CannonAmmoRules.matchesCaliber(ammo, weapon, level);
    }

    @Nullable
    public static int[] findBestArtillerySlot(Player player, Item weaponType, ItemStack coreStack) {
        return CannonInventory.findBestArtillerySlot(player, weaponType, coreStack);
    }

    public static List<int[]> findMatchingArtillerySlots(Player player, Item weaponType, ItemStack coreStack) {
        return CannonInventory.findMatchingArtillerySlots(player, weaponType, coreStack);
    }

    public static void syncAmmoToSiblingGuns(Player player) {
        CannonReloading.syncAmmoToSiblingGuns(player);
    }

    public static void beginSalvo(ServerPlayer player, Item weaponType,
            int aimMode, double ax, double ay, double az) {
        CannonSalvos.beginSalvo(player, weaponType, aimMode, ax, ay, az);
    }

    public static void executeSalvoFire(ServerLevel level, ServerPlayer player,
            int weaponSlot, int coreSlot, Item expectedWeaponType,
            int aimMode, double ax, double ay, double az) {
        CannonSalvos.executeSalvoFire(level, player, weaponSlot, coreSlot, expectedWeaponType,
                aimMode, ax, ay, az);
    }
}
