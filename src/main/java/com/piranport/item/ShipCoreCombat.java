package com.piranport.item;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftAttackMode;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.network.AircraftLaunchPosePayload;
import com.piranport.network.ShakeEffectPayload;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModSounds;
import com.piranport.skin.SkinManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;
import com.piranport.combat.BallisticSolver;
import com.piranport.platform.ClientHooks;
import com.piranport.PiranPort;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 舰装核心战斗系统 — 从 {@link ShipCoreItem} 提取的静态武器/弹药/飞机/辅助方法。
 *
 * <p><b>线程模型</b>: 服务端主线程。
 * <p><b>包级访问</b>: 通过 {@code ShipCoreItem.SMALL_SHELLS} 访问标签字段（同包可见）。
 * <p><b>设计</b>: 纯静态方法集合，无实例状态。
 */
public class ShipCoreCombat {

    private ShipCoreCombat() {}

    // ===== 开火指令：在弹药/冷却管道中传递瞄准信息 =====
    private sealed interface AimInstruction permits NoAim, Aimed, DirectAim, MaxRange {}
    private record NoAim() implements AimInstruction {}
    private record Aimed(Vec3 target) implements AimInstruction {}
    private record DirectAim(Vec3 target) implements AimInstruction {}
    private record MaxRange() implements AimInstruction {}

    // 齐射 aim 模式常量（对外公开，供 SalvoManager 延迟射击用）
    public static final int ARTILLERY_AIM_NONE = 0;
    public static final int ARTILLERY_AIM_TARGET = 1;
    public static final int ARTILLERY_AIM_MAX_RANGE = 2;
    public static final int ARTILLERY_AIM_DIRECT_TARGET = 3;

    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        return tryFireFromInventory(level, player, hand, new NoAim());
    }

    private static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand, AimInstruction aim) {
        if (level.isClientSide) return false;
        // Phase 12: 旁观者模式不开火
        if (player.isSpectator()) return false;

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

    /**
     * Fires the weapon at the given inventory slot using the ship core's ammo pool and cooldowns.
     * 返回 true 表示已派发 fire 分支（动作被消费）；false 表示被冷却阻断，调用方不应 consume 动作。
     */
    private static boolean fireWeaponAtSlot(Level level, Player player, ItemStack coreStack, int weaponSlot, int coreInventorySlot, AimInstruction aim) {
        Inventory inv = player.getInventory();
        ItemStack weapon = (weaponSlot == 40) ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Per-slot cooldown (keyed by inventory slot index)
        SlotCooldowns cooldowns = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        if (cooldowns.isOnCooldown(weaponSlot, level.getGameTime())) {
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
                fireTorpedosInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, torpedoLauncher, cooldowns);
            } else {
                fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, torpedoLauncher, cooldowns);
            }
            return true;
        }

        // Depth charge launcher
        if (weapon.getItem() instanceof DepthChargeLauncherItem dcLauncher) {
            fireDepthCharges(level, player, coreStack, inv, weaponSlot, coreInventorySlot, dcLauncher, cooldowns);
            return true;
        }

        // Missile launcher
        if (weapon.getItem() instanceof MissileLauncherItem missileLauncher) {
            fireMissiles(level, player, coreStack, inv, weaponSlot, coreInventorySlot, missileLauncher, cooldowns);
            return true;
        }

        // Aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            launchAircraftInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, cooldowns);
            return true;
        }

        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
            return fireLoadedCannon(level, player, coreStack, inv, weaponSlot, coreInventorySlot,
                    weapon, cooldowns, aim);
        }

        return false;
    }

    /** 火炮按策划采用弩式时序：先有已装填弹药才能开火，开火后开始装填下一轮。 */
    private static boolean fireLoadedCannon(Level level, Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns, AimInstruction aim) {
        int barrelCount = getBarrelCount(weapon, level);

        if (isCannonDamaged(weapon, level)) {
            player.displayClientMessage(Component.translatable("message.piranport.cannon_damaged"), true);
            return true;
        }

        LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level)) {
            weapon.remove(ModDataComponents.LOADED_AMMO.get());
            startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        ItemStack shellForRender = createAmmoStack(loaded.ammoItemId());
        if (shellForRender.isEmpty()) {
            weapon.remove(ModDataComponents.LOADED_AMMO.get());
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        boolean isType3 = isType3Shell(loaded.ammoItemId());
        boolean isVT = isVTShell(loaded.ammoItemId());
        boolean isHE = isHEShell(loaded.ammoItemId()) || isVT;
        weapon.remove(ModDataComponents.LOADED_AMMO.get());
        recordCurrentAmmoType(weapon, shellForRender.getItem());

        boolean fired = fireCannonSalvo(level, player, weapon, shellForRender, barrelCount,
                isType3, isVT, isHE, aim);
        if (!fired) {
            weapon.set(ModDataComponents.LOADED_AMMO.get(), loaded);
            return true;
        }

        startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);

        com.piranport.debug.PiranPortDebug.event(
                "Fire cannon | weapon={} ammo={} barrels={}",
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                loaded.ammoItemId(),
                barrelCount);
        return true;
    }

    /**
     * 服务端 tick 驱动火炮自动装填：空炮无读条则开始装填，读条完成才消耗弹药并写入 LOADED_AMMO。
     */
    public static void tickCannonAutoReload(Player player, ItemStack coreStack) {
        if (player.level().isClientSide()) return;
        if (coreStack.isEmpty()) return;

        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        SlotCooldowns updated = cooldowns;
        boolean changed = false;

        for (int i = 0; i < inv.items.size(); i++) {
            if (tickCannonAutoReloadSlot(player, coreStack, inv, i, findCoreSlotIndex(inv, player, i),
                    inv.items.get(i), updated)) {
                updated = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
                changed = true;
            }
        }

        ItemStack offhand = inv.offhand.get(0);
        if (tickCannonAutoReloadSlot(player, coreStack, inv, 40, findCoreSlotIndex(inv, player, 40),
                offhand, updated)) {
            changed = true;
        }

        if (changed) {
            TransformationManager.writeCoreToConfiguredSlot(player, coreStack);
        }
    }

    private static boolean tickCannonAutoReloadSlot(Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns) {
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) return false;
        if (coreSlot == -1) return false;

        long now = player.level().getGameTime();
        int barrelCount = getBarrelCount(weapon, player.level());
        LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (isLoadedCannonAmmoValid(loaded, weapon, barrelCount, player.level())) {
            return false;
        }
        if (loaded.hasAmmo()) {
            weapon.remove(ModDataComponents.LOADED_AMMO.get());
        }

        if (cooldowns.isOnCooldown(weaponSlot, now)) {
            return false;
        }

        WeaponCooldown itemCooldown = weapon.get(ModDataComponents.WEAPON_COOLDOWN.get());
        if (itemCooldown != null && itemCooldown.endTick() > 0 && itemCooldown.endTick() <= now) {
            return completeCannonReload(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
        }

        return startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
    }

    private static boolean startCannonReloadIfPossible(Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns) {
        if (coreSlot == -1) return false;
        int barrelCount = getBarrelCount(weapon, player.level());
        Item ammoType = chooseCannonReloadAmmo(inv, weapon, barrelCount, coreSlot, weaponSlot,
                player.level(), player.getAbilities().instabuild);
        if (ammoType == null && !player.getAbilities().instabuild) {
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        if (ammoType != null) {
            recordCurrentAmmoType(weapon, ammoType);
        }

        int reloadTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon, player.level()));
        long now = player.level().getGameTime();
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, reloadTicks, now));
        weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.of(now, reloadTicks));
        playCannonReloadStartSound(player, weapon);
        return true;
    }

    private static boolean completeCannonReload(Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns) {
        int barrelCount = getBarrelCount(weapon, player.level());
        Item ammoType = chooseCannonReloadAmmo(inv, weapon, barrelCount, coreSlot, weaponSlot,
                player.level(), player.getAbilities().instabuild);
        if (ammoType == null) {
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        if (!player.getAbilities().instabuild
                && !consumeCannonAmmo(inv, ammoType, barrelCount, coreSlot, weaponSlot)) {
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        String ammoId = BuiltInRegistries.ITEM.getKey(ammoType).toString();
        weapon.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(barrelCount, ammoId));
        weapon.remove(ModDataComponents.WEAPON_COOLDOWN.get());
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(), cooldowns.withoutSlotCooldown(weaponSlot));
        recordCurrentAmmoType(weapon, ammoType);
        playCannonReloadCompleteSound(player, weapon);

        com.piranport.debug.PiranPortDebug.event(
                "Cannon reload complete | slot={} weapon={} ammo={} barrels={}",
                weaponSlot,
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                ammoId,
                barrelCount);
        return true;
    }

    private static boolean clearCannonReloadState(ItemStack coreStack, ItemStack weapon,
            int weaponSlot, SlotCooldowns cooldowns) {
        boolean changed = false;
        if (weapon.get(ModDataComponents.WEAPON_COOLDOWN.get()) != null) {
            weapon.remove(ModDataComponents.WEAPON_COOLDOWN.get());
            changed = true;
        }
        if (cooldowns.endTick().containsKey(weaponSlot) || cooldowns.totalTick().containsKey(weaponSlot)) {
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(), cooldowns.withoutSlotCooldown(weaponSlot));
            changed = true;
        }
        return changed;
    }

    private static boolean isLoadedCannonAmmoValid(LoadedAmmo loaded, ItemStack weapon,
            int barrelCount, @Nullable Level level) {
        if (!loaded.hasAmmo() || loaded.count() < barrelCount) return false;
        ItemStack ammo = createAmmoStack(loaded.ammoItemId());
        return !ammo.isEmpty() && matchesCaliber(ammo, weapon, level);
    }

    private static boolean isCannonReadyToFire(ItemStack weapon, @Nullable Level level) {
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) return false;
        int barrelCount = getBarrelCount(weapon, level);
        LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        return isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level);
    }

    private static ItemStack createAmmoStack(String ammoItemId) {
        ResourceLocation rl = ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    /** Manual-mode torpedo: consume LOADED_AMMO component on the launcher item. */
    private static void fireTorpedosManualMode(Level level, Player player, ItemStack coreStack,
            Inventory inv, int weaponSlot, TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        int tubeCount = launcher.getTubeCount();
        int caliber = launcher.getCaliber();

        // Creative mode: auto-find torpedo from inventory
        if (player.getAbilities().instabuild) {
            TorpedoItem torpedoType = null;
            int coreSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreSlot = i;
                }
                if (torpedoType == null && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                    torpedoType = ti;
                }
            }
            if (torpedoType == null && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                    torpedoType = ti;
                }
            }

            if (torpedoType == null) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            String ammoId = BuiltInRegistries.ITEM.getKey(torpedoType).toString();
            boolean magnetic = isMagneticTorpedo(ammoId);
            boolean wireGuided = isWireGuidedTorpedo(ammoId);
            boolean acousticHoming = isAcousticTorpedo(ammoId);
            float torpedoSpeed = torpedoType.getSpeed();
            float[] angles = getSpreadAngles(tubeCount);
            Vec3 look = player.getLookAngle();

            TorpedoEntity primaryGuided = null;
            for (float angle : angles) {
                Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
                TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
                torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, torpedoType.getDamage()));
                torpedo.setSpeed(torpedoType.getSpeed());
                torpedo.setLifetime(torpedoType.getLifetimeTicks());
                if (magnetic) torpedo.setMagnetic(true);
                if (wireGuided) torpedo.setWireGuided(true);
                if (acousticHoming) torpedo.setAcoustic(true);
                torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
                torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
                level.addFreshEntity(torpedo);
                if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
            }
            if (primaryGuided != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
                com.piranport.combat.TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
            }

            int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
            int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), boostedCooldown));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
            return;
        }

        // Survival mode: use LOADED_AMMO component
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!loaded.hasAmmo() || loaded.count() < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Use caliber from method start
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
        boolean magnetic = isMagneticTorpedo(loaded.ammoItemId());
        boolean wireGuided = isWireGuidedTorpedo(loaded.ammoItemId());
        boolean acousticHoming = isAcousticTorpedo(loaded.ammoItemId());
        // Resolve torpedo item to read per-item stats
        Item loadedItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(loaded.ammoItemId()));
        TorpedoItem loadedTorpedo = loadedItem instanceof TorpedoItem ti ? ti : null;
        float torpedoSpeed = loadedTorpedo != null ? loadedTorpedo.getSpeed() : (caliber == 610 ? 1.0f : 1.2f);
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            if (loadedTorpedo != null) {
                torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, loadedTorpedo.getDamage()));
                torpedo.setSpeed(loadedTorpedo.getSpeed());
                torpedo.setLifetime(loadedTorpedo.getLifetimeTicks());
            }
            if (magnetic) torpedo.setMagnetic(true);
            if (wireGuided) torpedo.setWireGuided(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
            if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
        }
        if (primaryGuided != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.piranport.combat.TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
        }

        // Consume all loaded torpedoes
        launcherStack.remove(ModDataComponents.LOADED_AMMO.get());

        // Damage launcher
        boolean launcherBroken = false;
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                launcherBroken = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        if (!launcherBroken) {
            int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), boostedCooldown));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    private static void fireTorpedosInventoryMode(Level level, Player player, ItemStack coreStack,
                                            Inventory inv, int weaponSlot, int coreSlot,
                                            TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);

        // 优先使用已装填弹药（装填设施装的），用完后才自动从背包装填
        if (loaded.hasAmmo() && loaded.count() >= launcher.getTubeCount()) {
            fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, launcher, cooldowns);
            return;
        }

        int caliber = launcher.getCaliber();
        int tubeCount = launcher.getTubeCount();
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());

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

        // 创造模式：如果没有鱼雷，使用默认鱼雷类型
        if (torpedoType == null) {
            if (!player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
            // 创造模式：使用默认鱼雷
            torpedoType = getDefaultTorpedoForCaliber(caliber);
            if (torpedoType == null) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
        }

        // Creative mode: skip ammo consumption
        if (!player.getAbilities().instabuild) {
            // Count available ammo of the same type
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

            if (available < tubeCount) {
                player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
                return;
            }

            // Consume ammo before spawning entities (prevent TOCTOU)
            int toConsume = tubeCount;
            for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.getItem() == torpedoType) {
                    int take = Math.min(toConsume, s.getCount());
                    com.piranport.debug.PiranPortDebug.consumeAmmo(s, take);
                    toConsume -= take;
                }
            }
            if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                    int take = Math.min(toConsume, oh.getCount());
                    com.piranport.debug.PiranPortDebug.consumeAmmo(oh, take);
                    toConsume -= take;
                }
            }
        }

        boolean magnetic = torpedoType.isMagnetic();
        boolean acousticHoming = torpedoType.isAcoustic();
        boolean wireGuided = torpedoType.isWireGuided();
        boolean oxygen = torpedoType.isOxygen();
        float torpedoSpeed = torpedoType.getSpeed();
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setDamage(ExperienceShellItem.applyDamageBonus(launcherStack, torpedoType.getDamage()));
            torpedo.setSpeed(torpedoType.getSpeed());
            torpedo.setLifetime(torpedoType.getLifetimeTicks());
            if (magnetic) torpedo.setMagnetic(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            if (oxygen) torpedo.setOxygen(true);
            if (wireGuided) torpedo.setWireGuided(true);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
            if (wireGuided && primaryGuided == null) primaryGuided = torpedo;
        }
        if (primaryGuided != null && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.piranport.combat.TorpedoGuidanceManager.startGuidance(sp, primaryGuided);
        }

        // Damage launcher in-inventory
        boolean launcherBroken = false;
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                launcherBroken = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        // Check if enough torpedoes remain for next salvo
        int nextAvailable = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                nextAvailable += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                nextAvailable += oh.getCount();
            }
        }

        if (!launcherBroken) {
            if (nextAvailable >= tubeCount) {
                int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
                launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        WeaponCooldown.of(level.getGameTime(), boostedCooldown));
            } else {
                // 背包弹药不足，设置短冷却提示玩家需要补充弹药
                int penaltyTicks = 10;
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
                launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        WeaponCooldown.of(level.getGameTime(), penaltyTicks));
            }
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    private static void fireDepthCharges(Level level, Player player, ItemStack coreStack,
                                          Inventory inv, int weaponSlot, int coreSlot,
                                          DepthChargeLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        int chargeCount = launcher.getChargeCount();
        int cooldown = ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks());
        float damage = ExperienceShellItem.applyDamageBonus(launcherStack, 14f);
        float explosionPower = ExperienceShellItem.applyExplosionBonus(launcherStack, 3.0f);

        // Creative mode: skip ammo check and consumption
        if (!player.getAbilities().instabuild) {
            // Count available depth charge ammo in inventory
            int available = 0;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ModItems.DEPTH_CHARGE.get())) {
                    available += s.getCount();
                }
            }
            if (weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ModItems.DEPTH_CHARGE.get())) {
                    available += oh.getCount();
                }
            }

            if (available < chargeCount) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            // Consume ammo
            int toConsume = chargeCount;
            for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ModItems.DEPTH_CHARGE.get())) {
                    int take = Math.min(toConsume, s.getCount());
                    com.piranport.debug.PiranPortDebug.consumeAmmo(s, take);
                    toConsume -= take;
                }
            }
            if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ModItems.DEPTH_CHARGE.get())) {
                    int take = Math.min(toConsume, oh.getCount());
                    com.piranport.debug.PiranPortDebug.consumeAmmo(oh, take);
                    toConsume -= take;
                }
            }
        } else {
            // 创造模式：即使没有深弹也允许发射（使用默认深弹）
            // 无需额外检查，直接发射
        }

        // Spawn depth charges based on spread pattern
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        switch (launcher.getSpreadPattern()) {
            case SINGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.6, damage, explosionPower);
            }
            case FRONT_BACK -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7, damage, explosionPower);   // far
                spawnDepthCharge(level, player, horizLook, 0.0, 0.4, damage, explosionPower);   // near
            }
            case TRIANGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7, damage, explosionPower);   // center far
                Vec3 left = rotateHorizontal(horizLook, Math.toRadians(-20));
                spawnDepthCharge(level, player, left, 0.0, 0.5, damage, explosionPower);
                Vec3 right = rotateHorizontal(horizLook, Math.toRadians(20));
                spawnDepthCharge(level, player, right, 0.0, 0.5, damage, explosionPower);
            }
        }

        // Damage launcher
        boolean launcherBroken = false;
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                launcherBroken = true;
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        if (!launcherBroken) {
            int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), boostedCooldown));
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    private static void spawnDepthCharge(Level level, Player player, Vec3 dir, double angleOffset,
                                         double speed, float damage, float explosionPower) {
        DepthChargeEntity dc = new DepthChargeEntity(level, player, damage, explosionPower);
        dc.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
        dc.setDeltaMovement(dir.x * speed, 0.3, dir.z * speed);
        level.addFreshEntity(dc);
    }

    // ===== Missile firing =====

    private static void fireMissiles(Level level, Player player, ItemStack coreStack,
                                      Inventory inv, int weaponSlot, int coreSlot,
                                      MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        if (launcher.isManualReload()) {
            // 反舰导弹/火箭弹：仅手动装填（装填设施），不受鱼雷再装填强化影响
            fireMissileManual(level, player, coreStack, inv, weaponSlot, launcher, cooldowns);
        } else {
            // 防空导弹：自动从背包装填
            fireMissileAutoReload(level, player, coreStack, inv, weaponSlot, coreSlot, launcher, cooldowns);
        }
    }

    /** 反舰导弹/火箭弹：消耗 LOADED_AMMO，无冷却，仅装填设施装弹。 */
    private static void fireMissileManual(Level level, Player player, ItemStack coreStack,
                                           Inventory inv, int weaponSlot,
                                           MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Creative mode: auto-find missile from inventory
        if (player.getAbilities().instabuild) {
            Item ammoItem = launcher.getAmmoItem();
            int coreSlot = -1;
            int ammoSlot = -1;

            for (int i = 0; i < inv.items.size(); i++) {
                if (i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    coreSlot = i;
                }
                if (ammoSlot == -1 && !s.isEmpty() && s.is(ammoItem)) {
                    ammoSlot = i;
                }
            }
            if (ammoSlot == -1 && weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) {
                    ammoSlot = 40;
                }
            }

            String ammoId;
            if (ammoSlot == -1) {
                // 创造模式：使用默认弹药
                ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            } else {
                ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            }

            spawnMissile(level, player, launcherStack, launcher, ammoId);
    
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            return;
        }

        // Survival mode: use LOADED_AMMO component
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!loaded.hasAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // 发射1枚导弹
        String ammoId = loaded.ammoItemId();
        spawnMissile(level, player, launcherStack, launcher, ammoId);

        // 消耗1枚
        int remaining = loaded.count() - 1;
        if (remaining <= 0) {
            launcherStack.remove(ModDataComponents.LOADED_AMMO.get());
        } else {
            launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(remaining, ammoId));
        }

        // 无冷却 — 反舰/火箭可连续发射


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 导弹自动装填：从背包消耗弹药，发射后进入冷却。防空导弹专用。 */
    private static void fireMissileAutoReload(Level level, Player player, ItemStack coreStack,
                                               Inventory inv, int weaponSlot, int coreSlot,
                                               MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        Item ammoItem = launcher.getAmmoItem();

        // 查找弹药
        int ammoSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.is(ammoItem)) {
                ammoSlot = i;
                break;
            }
        }
        if (ammoSlot == -1 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.is(ammoItem)) {
                ammoSlot = 40;
            }
        }

        // 创造模式：如果没有弹药，使用默认弹药ID
        String ammoId;
        if (ammoSlot == -1) {
            if (!player.getAbilities().instabuild) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
            // 创造模式：使用默认弹药
            ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
        } else {
            // 有弹药：使用物品栏中的弹药类型
            ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            // 创造模式：不消耗弹药
            if (!player.getAbilities().instabuild) {
                com.piranport.debug.PiranPortDebug.consumeAmmo(
                        ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot), 1);
            }
        }

        // 发射
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        spawnMissile(level, player, launcherStack, launcher, ammoId);

        // 检查剩余弹药（避免冷却后才发现无弹药）
        int nextAvailable = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.is(ammoItem)) {
                nextAvailable += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.is(ammoItem)) {
                nextAvailable += oh.getCount();
            }
        }

        // Creative mode: always has next round
        if (player.getAbilities().instabuild) {
            nextAvailable = 1;
        }

        // 应用冷却
        if (nextAvailable > 0) {
            int cd = TransformationManager.boostedCooldown(player,
                    ExperienceShellItem.applyCooldownReduction(launcherStack, launcher.getCooldownTicks()));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cd, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), cd));
        } else {
            int penaltyTicks = 10;
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), penaltyTicks));
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
        }


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 生成导弹实体：玩家前方0.5格处，沿视线方向发射。 */
    private static void spawnMissile(Level level, Player player, ItemStack launcherStack,
                                      MissileLauncherItem launcher, String displayItemId) {
        spawnMissileWithDir(level, player, launcherStack, launcher, displayItemId, player.getLookAngle());
    }

    /**
     * 通用导弹生成：在玩家眼睛 + dir*0.5 处生成导弹，以 dir 方向按 initialSpeed 射出。
     * dir 预期为单位向量；非单位向量将被规整化。
     */
    private static void spawnMissileWithDir(Level level, Player player, ItemStack launcherStack,
                                             MissileLauncherItem launcher, String displayItemId,
                                             Vec3 dir) {
        Vec3 d = dir.lengthSqr() > 1e-6 ? dir.normalize() : player.getLookAngle();
        MissileEntity missile = new MissileEntity(level, launcher.getMissileType(),
                ExperienceShellItem.applyDamageBonus(launcherStack, launcher.getDamage()),
                launcher.getArmorPen(),
                ExperienceShellItem.applyExplosionBonus(launcherStack, launcher.getExplosionPower()),
                displayItemId);
        missile.setOwner(player);
        // Y 跟随 dir.y 偏移，避免抬头/俯冲时导弹从胸前喷出
        missile.setPos(
                player.getX() + d.x * 0.5,
                player.getEyeY() - 0.1 + d.y * 0.5,
                player.getZ() + d.z * 0.5);
        float initSpeed = launcher.getMissileType().initialSpeed;
        missile.setDeltaMovement(d.x * initSpeed, d.y * initSpeed, d.z * initSpeed);
        level.addFreshEntity(missile);
    }

    private static void launchAircraftInventoryMode(Level level, Player player, ItemStack coreStack,
                                              Inventory inv, int weaponSlot, int coreInventorySlot,
                                              SlotCooldowns cooldowns) {
        ItemStack aircraftStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Fuel check — refuse launch if currentFuel == 0
        AircraftInfo launchInfo = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (launchInfo == null || launchInfo.currentFuel() <= 0) {
            player.displayClientMessage(Component.translatable("message.piranport.no_fuel"), true);
            return;
        }

        AircraftAttackMode attackMode = AircraftAttackMode.FOCUS;
        boolean hasBullets = launchInfo.aircraftType() == AircraftInfo.AircraftType.FIGHTER
                || launchInfo.aircraftType() == AircraftInfo.AircraftType.ROCKET_FIGHTER;
        String payloadType = "";

        switch (launchInfo.aircraftType()) {
            case TORPEDO_BOMBER -> { payloadType = "piranport:aerial_torpedo"; hasBullets = false; }
            case DIVE_BOMBER, LEVEL_BOMBER -> { payloadType = "piranport:aerial_bomb"; hasBullets = false; }
            case ASW -> { payloadType = "piranport:depth_charge"; hasBullets = false; }
            default -> { }
        }

        // Consume payload from inventory if needed
        if (!payloadType.isEmpty() && !hasBullets) {
            net.minecraft.world.item.Item payloadItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(payloadType));

            // 创造模式：即使没有挂载物也允许发射（使用默认挂载物）
            // 生存模式：消耗挂载物
            if (!player.getAbilities().instabuild) {
                // Survival mode: consume payload
                boolean consumed = false;
                for (int i = 0; i < inv.items.size(); i++) {
                    if (i == coreInventorySlot || i == weaponSlot) continue;
                    ItemStack s = inv.items.get(i);
                    if (!s.isEmpty() && s.getItem() == payloadItem) {
                        com.piranport.debug.PiranPortDebug.consumeAmmo(s, 1);
                        consumed = true;
                        break;
                    }
                }
                if (!consumed && weaponSlot != 40 && coreInventorySlot != 40) {
                    ItemStack oh = inv.offhand.get(0);
                    if (!oh.isEmpty() && oh.getItem() == payloadItem) {
                        com.piranport.debug.PiranPortDebug.consumeAmmo(oh, 1);
                        consumed = true;
                    }
                }
                if (!consumed) {
                    player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                    // P0-3: 起飞失败埋点（弹药不足）
                    com.piranport.debug.PiranPortDebug.aircraftLaunchFailed(
                            player, weaponSlot, aircraftStack, "NO_AMMO");
                    return;
                }
            }
        }

        AircraftEntity aircraft = AircraftEntity.create(level, player, weaponSlot, aircraftStack,
                attackMode, coreInventorySlot, hasBullets, payloadType);
        level.addFreshEntity(aircraft);
        spawnAircraftLaunchEffect(level, player, launchInfo.aircraftType());
        // P0-3: 起飞成功埋点（带玩家短UUID、槽位、物品hash、payload、mode、entityId）
        com.piranport.debug.PiranPortDebug.aircraftLaunched(
                player, weaponSlot, aircraftStack, payloadType, attackMode.name(), aircraft.getId());
        // 旧版事件保留，便于历史脚本兼容
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft LAUNCH | type={} entityId={} payload={} mode={}",
                aircraft.getAircraftType().name(), aircraft.getId(), payloadType, attackMode.name());

        // Clear the aircraft from inventory
        if (weaponSlot == 40) {
            inv.offhand.set(0, ItemStack.EMPTY);
        } else {
            inv.items.set(weaponSlot, ItemStack.EMPTY);
        }

        int launchCooldown = TransformationManager.boostedCooldown(player, 20);
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, launchCooldown, level.getGameTime()));


        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        player.displayClientMessage(
                Component.translatable("message.piranport.aircraft_launched", aircraftStack.getHoverName()), true);
    }

    private enum LaunchStyle {
        J_DECK_BOW,
        J_SNOW_BOW,
        J_CRUISER_BOW,
        J_RIBBON_BOW,
        C_SIGNAL,
        C_MISSILE_RAIL,
        G_MECHANICAL,
        G_SUBMARINE,
        I_CATAPULT,
        I_AERIAL_FRAME,
        E_LONGBOW,
        E_DECK_LONGBOW,
        U_MUSKET,
        U_CARRIER_CATAPULT,
        F_RAPIER,
        DEFAULT
    }

    private record LaunchProfile(LaunchStyle style, ParticleOptions accentParticle, SoundEvent sound,
                                 float volume, float pitch, double width, double lift, int accentBonus) {}

    private static void spawnAircraftLaunchEffect(Level level, Player player, AircraftInfo.AircraftType aircraftType) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0, look.z);
        if (horizontal.lengthSqr() < 1.0e-5) {
            horizontal = new Vec3(0, 0, 1);
        } else {
            horizontal = horizontal.normalize();
        }
        Vec3 right = new Vec3(-horizontal.z, 0, horizontal.x);
        Vec3 origin = player.position().add(0, 1.1, 0).add(horizontal.scale(0.35));
        int skinId = SkinManager.getActiveSkin(player);
        LaunchProfile profile = resolveLaunchProfile(skinId);
        double poseRadius = serverLevel.getServer().getPlayerList().getSimulationDistance() * 16.0;
        PacketDistributor.sendToPlayersNear(
                serverLevel,
                null,
                player.getX(), player.getY(), player.getZ(),
                Math.max(48.0, poseRadius),
                new AircraftLaunchPosePayload(player.getId(), skinId, 18));

        for (int i = 0; i < 9; i++) {
            double t = (i - 4) / 4.0;
            Vec3 p = origin.add(right.scale(t * profile.width()))
                    .add(horizontal.scale(Math.abs(t) * 0.15))
                    .add(0, profile.lift(), 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z,
                    2, 0.04, 0.03, 0.04, 0.01);
        }

        spawnSkinLaunchGesture(serverLevel, origin, horizontal, right, profile, aircraftType);

        int accentCount = aircraftType == AircraftInfo.AircraftType.FIGHTER
                || aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER ? 14 : 10;
        serverLevel.sendParticles(profile.accentParticle(),
                origin.x + horizontal.x * 0.6,
                origin.y + 0.1 + profile.lift(),
                origin.z + horizontal.z * 0.6,
                accentCount + profile.accentBonus(),
                0.35, 0.18, 0.35, 0.04);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                profile.sound(), SoundSource.PLAYERS, profile.volume(), profile.pitch());
    }

    private static LaunchProfile resolveLaunchProfile(int skinId) {
        return switch (skinId) {
            case 4 -> new LaunchProfile(LaunchStyle.J_DECK_BOW, ParticleTypes.CRIT,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.12f, 0.72, 0.04, 4);
            case 5 -> new LaunchProfile(LaunchStyle.C_MISSILE_RAIL, ParticleTypes.ENCHANT,
                    SoundEvents.CROSSBOW_SHOOT, 0.56f, 1.35f, 0.52, 0.02, 3);
            case 6 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.54f, 1.08f, 0.46, 0.02, 1);
            case 7 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.54f, 1.28f, 0.58, 0.05, 2);
            case 8 -> new LaunchProfile(LaunchStyle.J_SNOW_BOW, ParticleTypes.SNOWFLAKE,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.18f, 0.56, 0.03, 2);
            case 9 -> new LaunchProfile(LaunchStyle.J_SNOW_BOW, ParticleTypes.SNOWFLAKE,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.38f, 0.66, 0.06, 4);
            case 10 -> new LaunchProfile(LaunchStyle.I_CATAPULT, ParticleTypes.CRIT,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.58f, 1.04f, 0.62, 0.02, 3);
            case 11 -> new LaunchProfile(LaunchStyle.J_CRUISER_BOW, ParticleTypes.CRIT,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.30f, 0.50, 0.04, 2);
            case 12 -> new LaunchProfile(LaunchStyle.G_MECHANICAL, ParticleTypes.WITCH,
                    SoundEvents.CROSSBOW_SHOOT, 0.56f, 0.95f, 0.54, 0.02, 2);
            case 13 -> new LaunchProfile(LaunchStyle.J_RIBBON_BOW, ParticleTypes.HEART,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.42f, 0.62, 0.07, 5);
            case 14 -> new LaunchProfile(LaunchStyle.C_SIGNAL, ParticleTypes.ENCHANT,
                    SoundEvents.AMETHYST_BLOCK_CHIME, 0.56f, 0.92f, 0.64, 0.04, 4);
            case 15 -> new LaunchProfile(LaunchStyle.C_MISSILE_RAIL, ParticleTypes.ENCHANT,
                    SoundEvents.CROSSBOW_SHOOT, 0.58f, 1.48f, 0.60, 0.04, 5);
            case 16 -> new LaunchProfile(LaunchStyle.I_AERIAL_FRAME, ParticleTypes.CRIT,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.56f, 1.22f, 0.50, 0.03, 2);
            case 17 -> new LaunchProfile(LaunchStyle.G_SUBMARINE, ParticleTypes.BUBBLE,
                    SoundEvents.CROSSBOW_SHOOT, 0.48f, 0.78f, 0.42, -0.03, 1);
            case 18 -> new LaunchProfile(LaunchStyle.E_DECK_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.06f, 0.68, 0.07, 4);
            case 19 -> new LaunchProfile(LaunchStyle.F_RAPIER, ParticleTypes.CRIT,
                    SoundEvents.CROSSBOW_SHOOT, 0.54f, 1.26f, 0.48, 0.04, 3);
            case 20 -> new LaunchProfile(LaunchStyle.U_CARRIER_CATAPULT, ParticleTypes.END_ROD,
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.62f, 0.98f, 0.76, 0.02, 5);
            case 21 -> new LaunchProfile(LaunchStyle.U_MUSKET, ParticleTypes.POOF,
                    SoundEvents.FIREWORK_ROCKET_BLAST, 0.58f, 1.16f, 0.50, 0.02, 3);
            case 22 -> new LaunchProfile(LaunchStyle.E_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.56f, 1.34f, 0.56, 0.08, 3);
            case 23 -> new LaunchProfile(LaunchStyle.E_LONGBOW, ParticleTypes.END_ROD,
                    SoundEvents.ARROW_SHOOT, 0.58f, 1.30f, 0.58, 0.08, 3);
            default -> new LaunchProfile(LaunchStyle.DEFAULT, ParticleTypes.FIREWORK,
                    SoundEvents.CROSSBOW_SHOOT, 0.55f, 1.25f, 0.55, 0.0, 0);
        };
    }

    private static void spawnSkinLaunchGesture(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                               LaunchProfile profile, AircraftInfo.AircraftType aircraftType) {
        boolean fighter = aircraftType == AircraftInfo.AircraftType.FIGHTER
                || aircraftType == AircraftInfo.AircraftType.ROCKET_FIGHTER;
        switch (profile.style()) {
            case J_DECK_BOW -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 7, 0.34, 0.02);
                spawnBowArc(level, origin.add(0, 0.08, 0), forward, right, ParticleTypes.CRIT, 0.58, 0.24);
                Vec3 arrow = origin.add(forward.scale(fighter ? 1.05 : 0.82)).add(0, 0.24, 0);
                level.sendParticles(ParticleTypes.END_ROD, arrow.x, arrow.y, arrow.z,
                        fighter ? 12 : 8, 0.08, 0.05, 0.08, 0.02);
            }
            case J_SNOW_BOW -> {
                spawnBowArc(level, origin, forward, right, ParticleTypes.SNOWFLAKE, 0.52, 0.22);
                for (int i = 0; i < 8; i++) {
                    Vec3 p = origin.add(forward.scale(0.16 * i)).add(0, 0.08 + i * 0.018, 0);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z,
                            2, 0.04, 0.04, 0.04, 0.0);
                }
            }
            case J_CRUISER_BOW -> {
                spawnBowArc(level, origin.add(0, 0.05, 0), forward, right, ParticleTypes.CRIT, 0.44, 0.20);
                for (int i = -1; i <= 1; i++) {
                    Vec3 p = origin.add(right.scale(i * 0.18)).add(forward.scale(0.22)).add(0, 0.12, 0);
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                            fighter ? 4 : 3, 0.02, 0.02, 0.02, 0.01);
                }
            }
            case J_RIBBON_BOW -> {
                spawnBowArc(level, origin.add(0, 0.06, 0), forward, right, ParticleTypes.CRIT, 0.56, 0.20);
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI * 2.0 / 6.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * 0.32))
                            .add(forward.scale(Math.sin(angle) * 0.12))
                            .add(0, 0.18, 0);
                    level.sendParticles(ParticleTypes.HEART, p.x, p.y, p.z,
                            1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            case C_SIGNAL -> {
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI * 2.0 / 8.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * profile.width() * 0.75))
                            .add(forward.scale(Math.sin(angle) * 0.20))
                            .add(0, 0.12 + profile.lift(), 0);
                    level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.0);
                }
                if (profile.accentBonus() >= 4) {
                    spawnSignalBars(level, origin, right, ParticleTypes.ENCHANT);
                }
            }
            case C_MISSILE_RAIL -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.ENCHANT, 9, 0.22, profile.lift());
                for (int side = -1; side <= 1; side += 2) {
                    Vec3 rail = origin.add(right.scale(side * profile.width() * 0.55)).add(forward.scale(0.18));
                    level.sendParticles(ParticleTypes.ENCHANT, rail.x, rail.y + 0.1, rail.z,
                            fighter ? 7 : 5, 0.03, 0.04, 0.03, 0.02);
                }
            }
            case G_MECHANICAL -> {
                for (int i = 0; i < 6; i++) {
                    double angle = i * Math.PI * 2.0 / 6.0;
                    Vec3 p = origin.add(right.scale(Math.cos(angle) * 0.34))
                            .add(forward.scale(Math.sin(angle) * 0.16))
                            .add(0, 0.10, 0);
                    level.sendParticles(ParticleTypes.WITCH, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.01);
                }
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 5, 0.18, 0.03);
            }
            case G_SUBMARINE -> {
                for (int i = 0; i < 9; i++) {
                    Vec3 p = origin.add(forward.scale(i * 0.10)).add(0, -0.08 + i * 0.012, 0)
                            .add(right.scale(Math.sin(i * 0.8) * 0.10));
                    level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z,
                            3, 0.03, 0.02, 0.03, 0.01);
                }
                level.sendParticles(ParticleTypes.WITCH,
                        origin.x, origin.y + 0.05, origin.z,
                        fighter ? 8 : 5, 0.16, 0.05, 0.16, 0.01);
            }
            case I_CATAPULT -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.CRIT, 9, 0.30, 0.02);
                Vec3 exhaust = origin.add(forward.scale(-0.10)).add(0, 0.05, 0);
                level.sendParticles(ParticleTypes.POOF, exhaust.x, exhaust.y, exhaust.z,
                        fighter ? 10 : 7, 0.16, 0.05, 0.16, 0.02);
            }
            case I_AERIAL_FRAME -> {
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 4; i++) {
                        Vec3 p = origin.add(right.scale(side * (0.18 + i * 0.06)))
                                .add(forward.scale(0.10 + i * 0.05))
                                .add(0, 0.08 + i * 0.018, 0);
                        level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                                2, 0.02, 0.02, 0.02, 0.01);
                    }
                }
            }
            case E_LONGBOW, E_DECK_LONGBOW -> {
                double height = profile.style() == LaunchStyle.E_DECK_LONGBOW ? 0.52 : 0.44;
                for (int i = 0; i < 13; i++) {
                    double t = (i - 6) / 6.0;
                    Vec3 p = origin.add(right.scale(t * 0.30))
                            .add(forward.scale(Math.abs(t) * 0.10))
                            .add(0, height - Math.abs(t) * 0.34, 0);
                    level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z,
                            1, 0.01, 0.01, 0.01, 0.0);
                }
                if (profile.style() == LaunchStyle.E_DECK_LONGBOW) {
                    spawnRunwayStreak(level, origin, forward, right, ParticleTypes.END_ROD, 7, 0.30, 0.04);
                }
                Vec3 arrow = origin.add(forward.scale(fighter ? 1.05 : 0.82)).add(0, height * 0.75, 0);
                level.sendParticles(ParticleTypes.END_ROD, arrow.x, arrow.y, arrow.z,
                        fighter ? 12 : 8, 0.08, 0.05, 0.08, 0.02);
            }
            case U_MUSKET -> {
                Vec3 muzzle = origin.add(forward.scale(0.64)).add(0, 0.12, 0);
                level.sendParticles(ParticleTypes.POOF, muzzle.x, muzzle.y, muzzle.z,
                        fighter ? 12 : 8, 0.12, 0.04, 0.12, 0.02);
                level.sendParticles(ParticleTypes.CRIT, muzzle.x + forward.x * 0.15, muzzle.y, muzzle.z + forward.z * 0.15,
                        fighter ? 9 : 6, 0.04, 0.02, 0.04, 0.04);
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.POOF, 5, 0.16, 0.00);
            }
            case U_CARRIER_CATAPULT -> {
                spawnRunwayStreak(level, origin, forward, right, ParticleTypes.END_ROD, 11, 0.36, 0.02);
                Vec3 exhaust = origin.add(forward.scale(-0.12));
                level.sendParticles(ParticleTypes.POOF, exhaust.x, exhaust.y, exhaust.z,
                        fighter ? 12 : 8, 0.20, 0.06, 0.20, 0.025);
            }
            case F_RAPIER -> {
                for (int i = 0; i < 8; i++) {
                    Vec3 p = origin.add(forward.scale(i * 0.13))
                            .add(right.scale((i - 3.5) * 0.035))
                            .add(0, 0.12 + i * 0.012, 0);
                    level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z,
                            2, 0.02, 0.02, 0.02, 0.02);
                }
                Vec3 flourish = origin.add(forward.scale(0.34)).add(right.scale(0.22)).add(0, 0.22, 0);
                level.sendParticles(ParticleTypes.ENCHANT, flourish.x, flourish.y, flourish.z,
                        7, 0.06, 0.05, 0.06, 0.01);
            }
            case DEFAULT -> {
                Vec3 p = origin.add(forward.scale(0.45)).add(0, 0.12, 0);
                level.sendParticles(ParticleTypes.FIREWORK, p.x, p.y, p.z,
                        fighter ? 8 : 5, 0.18, 0.08, 0.18, 0.03);
            }
        }
    }

    private static void spawnBowArc(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                    ParticleOptions particle, double width, double height) {
        for (int i = 0; i < 13; i++) {
            double t = (i - 6) / 6.0;
            Vec3 p = origin.add(right.scale(t * width))
                    .add(forward.scale(Math.abs(t) * 0.08))
                    .add(0, height - Math.abs(t) * height * 0.72, 0);
            level.sendParticles(particle, p.x, p.y, p.z,
                    1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static void spawnRunwayStreak(ServerLevel level, Vec3 origin, Vec3 forward, Vec3 right,
                                          ParticleOptions particle, int points, double halfWidth, double lift) {
        for (int i = 0; i < points; i++) {
            double t = points <= 1 ? 0.0 : i / (double) (points - 1);
            double side = i % 2 == 0 ? -halfWidth : halfWidth;
            Vec3 p = origin.add(forward.scale(t * 0.95))
                    .add(right.scale(side * (0.28 + 0.72 * t)))
                    .add(0, 0.04 + lift + t * 0.10, 0);
            level.sendParticles(particle, p.x, p.y, p.z,
                    2, 0.02, 0.02, 0.02, 0.01);
        }
    }

    private static void spawnSignalBars(ServerLevel level, Vec3 origin, Vec3 right, ParticleOptions particle) {
        for (int bar = 0; bar < 3; bar++) {
            for (int i = -2; i <= 2; i++) {
                Vec3 p = origin.add(right.scale(i * 0.08)).add(0, 0.10 + bar * 0.08, 0);
                level.sendParticles(particle, p.x, p.y, p.z,
                        1, 0.01, 0.01, 0.01, 0.0);
            }
        }
    }

    // ===== Weapon cooldown tooltip (shared by all weapon item types) =====

    /**
     * Client-only helper. Appends "已装填" or "装填中: Xs" to a weapon item's tooltip,
     * based on the player's active ship core SlotCooldowns.
     * Only shows when the player is transformed and the weapon is physically in their inventory.
     */
    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        ClientHooks.appendWeaponCooldownTooltip(stack, tooltip);
    }

    // ===== Caliber matching =====
    // Phase 12: 已迁移至 piranport:small_shells / medium_shells / large_shells 物品标签

    /** 偏好弹种不足时，按背包槽位顺序查找第一种同口径且足量的弹药类型。 */
    private static Item findFirstSufficientAmmoTypeByInventoryOrder(Inventory inv, ItemStack weapon,
                                                                    int required, int coreSlot, int weaponSlot,
                                                                    @Nullable Level level) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack ammo = inv.items.get(i);
            if (!ammo.isEmpty()
                    && matchesCaliber(ammo, weapon, level)
                    && countAmmo(inv, ammo.getItem(), coreSlot, weaponSlot) >= required) {
                return ammo.getItem();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack offhand = inv.offhand.get(0);
            if (!offhand.isEmpty()
                    && matchesCaliber(offhand, weapon, level)
                    && countAmmo(inv, offhand.getItem(), coreSlot, weaponSlot) >= required) {
                return offhand.getItem();
            }
        }
        return null;
    }

    /** Phase 12: 用物品标签匹配口径，替代硬编码物品列表。数据包可向标签添加物品来扩展。 */
    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        return matchesCaliber(ammo, weapon, null);
    }

    /** Phase 12: 用物品标签匹配有效口径，替代硬编码物品列表。数据包可向标签添加物品来扩展。 */
    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ammo.is(ShipCoreItem.SMALL_SHELLS);
            if (caliber <= 8) return ammo.is(ShipCoreItem.MEDIUM_SHELLS);
            return ammo.is(ShipCoreItem.LARGE_SHELLS);
        }
        return false;
    }

    /** 根据策划的装填优先级选择弹药：上次/指定弹种优先，否则背包从左到右。 */
    private static Item chooseCannonReloadAmmo(Inventory inv, ItemStack weapon,
                                               int required, int coreSlot, int weaponSlot,
                                               @Nullable Level level, boolean creative) {
        SelectedAmmoType preferred = weapon.getOrDefault(
                ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        if (preferred.hasSelection()) {
            ResourceLocation rl = ResourceLocation.tryParse(preferred.ammoItemId());
            if (rl != null) {
                Item preferredItem = BuiltInRegistries.ITEM.get(rl);
                if (preferredItem != null && preferredItem != net.minecraft.world.item.Items.AIR) {
                    ItemStack ammoStack = new ItemStack(preferredItem);
                    if (matchesCaliber(ammoStack, weapon, level)
                            && (creative || countAmmo(inv, preferredItem, coreSlot, weaponSlot) >= required)) {
                        return preferredItem;
                    }
                }
            }
        }
        Item fallback = findFirstSufficientAmmoTypeByInventoryOrder(inv, weapon, required,
                coreSlot, weaponSlot, level);
        if (fallback == null && creative) {
            return getDefaultAmmoForWeapon(weapon, level);
        }
        return fallback;
    }

    /** 将当前消耗的弹种写入武器的 SelectedAmmoType DataComponent。 */
    private static void recordCurrentAmmoType(ItemStack weapon, Item ammoItem) {
        String id = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
        weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), new SelectedAmmoType(id));
    }

    /** 在背包中查找选定类型的第一个弹药堆叠。 */
    private static ItemStack findFirstAmmoStack(Inventory inv, Item ammoType,
                                                  int coreSlot, int weaponSlot) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack ammo = inv.items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() == ammoType) {
                return ammo;
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == ammoType) {
                return oh;
            }
        }
        return ItemStack.EMPTY;
    }

    private static int countAmmo(Inventory inv, Item ammoType, int coreSlot, int weaponSlot) {
        int count = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack stack = inv.items.get(i);
            if (!stack.isEmpty() && stack.getItem() == ammoType) {
                count += stack.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack offhand = inv.offhand.get(0);
            if (!offhand.isEmpty() && offhand.getItem() == ammoType) {
                count += offhand.getCount();
            }
        }
        return count;
    }

    private static boolean consumeCannonAmmo(Inventory inv, Item ammoType, int required,
                                             int coreSlot, int weaponSlot) {
        if (countAmmo(inv, ammoType, coreSlot, weaponSlot) < required) return false;
        int toConsume = required;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack stack = inv.items.get(i);
            if (!stack.isEmpty() && stack.getItem() == ammoType) {
                int take = Math.min(toConsume, stack.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(stack, take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack offhand = inv.offhand.get(0);
            if (!offhand.isEmpty() && offhand.getItem() == ammoType) {
                int take = Math.min(toConsume, offhand.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(offhand, take);
                toConsume -= take;
            }
        }
        return toConsume <= 0;
    }

    static boolean isHEShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get());
    }

    static boolean isHEShell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_HE_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.MEDIUM_HE_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.LARGE_HE_SHELL.get()).toString());
    }

    static boolean isVTShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_VT_SHELL.get());
    }

    static boolean isVTShell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_VT_SHELL.get()).toString());
    }

    static boolean isType3Shell(ItemStack stack) {
        return stack.is(ModItems.SMALL_TYPE3_SHELL.get())
                || stack.is(ModItems.MEDIUM_TYPE3_SHELL.get())
                || stack.is(ModItems.LARGE_TYPE3_SHELL.get());
    }

    static boolean isType3Shell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_TYPE3_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.MEDIUM_TYPE3_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.LARGE_TYPE3_SHELL.get()).toString());
    }

    static boolean isAPShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_AP_SHELL.get())
                || stack.is(ModItems.MEDIUM_AP_SHELL.get())
                || stack.is(ModItems.LARGE_AP_SHELL.get())
                // 依据：弹药-AP弹穿甲设计.md —— 91 式 / 一式 / 超重弹均为 AP 子类
                || stack.is(ModItems.TYPE_91_AP_SHELL.get())
                || stack.is(ModItems.TYPE_1_AP_SHELL.get())
                || stack.is(ModItems.SUPER_HEAVY_AP_SHELL.get());
    }

    static boolean isAPShell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.MEDIUM_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.LARGE_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.TYPE_91_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.TYPE_1_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SUPER_HEAVY_AP_SHELL.get()).toString());
    }

    static boolean isMagneticTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isMagnetic();
    }

    static boolean isMagneticTorpedo(String ammoItemId) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return false;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item instanceof TorpedoItem ti && ti.isMagnetic();
    }

    static boolean isWireGuidedTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isWireGuided();
    }

    static boolean isWireGuidedTorpedo(String ammoItemId) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return false;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item instanceof TorpedoItem ti && ti.isWireGuided();
    }

    static boolean isAcousticTorpedo(ItemStack stack) {
        return stack.getItem() instanceof TorpedoItem ti && ti.isAcoustic();
    }

    static boolean isAcousticTorpedo(String ammoItemId) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return false;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item instanceof TorpedoItem ti && ti.isAcoustic();
    }

    /** Look up TorpedoItem from a loaded ammo item ID string. */
    static TorpedoItem lookupTorpedoItem(String ammoItemId) {
        var rl = net.minecraft.resources.ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return null;
        Item item = BuiltInRegistries.ITEM.get(rl);
        return item instanceof TorpedoItem ti ? ti : null;
    }

    // ===== Gun stats =====

    private static float getGunDamage(ItemStack weapon) {
        return getGunDamage(weapon, null);
    }
    
    private static float getGunDamage(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            float baseDamage = level != null ? ai.getEffectiveData(level).damage() : ai.getDamage();
            return ExperienceShellItem.applyDamageBonus(weapon, baseDamage);
        }
        return 6f;
    }

    private static int getGunCooldown(ItemStack weapon) {
        return getGunCooldown(weapon, null);
    }
    
    private static int getGunCooldown(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            com.piranport.artillery.config.ArtilleryCannonData data =
                    level != null ? ai.getEffectiveData(level) : ai.getData();
            return ExperienceShellItem.applyCooldownReduction(
                    weapon, Math.max(data.reloadTime(), data.fireCooldown()));
        }
        return 30;
    }

    private static int getBarrelCount(ItemStack weapon) {
        return getBarrelCount(weapon, null);
    }
    
    private static int getBarrelCount(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            com.piranport.artillery.config.ArtilleryCannonData data =
                    level != null ? ai.getEffectiveData(level) : ai.getData();
            return Math.max(data.barrels(), data.salvoCount());
        }
        return 1;
    }

    private static int getSalvoIntervalTicks(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            com.piranport.artillery.config.ArtilleryCannonData data =
                    level != null ? ai.getEffectiveData(level) : ai.getData();
            return Math.max(1, Math.round(data.salvoInterval()));
        }
        return 2;
    }

    private static int getCannonDurability(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).durability() : ai.getData().durability();
        }
        return weapon.getMaxDamage();
    }

    private static boolean isCannonDamaged(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (!weapon.isDamageableItem()) return false;
        int effectiveDurability = getCannonDurability(weapon, level);
        return weapon.getDamageValue() >= effectiveDurability - 1;
    }

    private static boolean isSmallCaliber(ItemStack weapon) {
        return weapon.is(ModItems.SMALL_GUN.get()) || weapon.is(ModItems.SINGLE_SMALL_GUN.get());
    }

    private static float getExplosionPower(ItemStack weapon) {
        return getExplosionPower(weapon, null);
    }
    
    private static float getExplosionPower(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            float baseExplosion = level != null ? ai.getEffectiveData(level).explosionPower() : ai.getExplosionPower();
            return ExperienceShellItem.applyExplosionBonus(weapon, baseExplosion);
        }
        return 1.0f;
    }

    private static float getProjectileVelocity(ItemStack weapon) {
        return getProjectileVelocity(weapon, null);
    }
    
    private static float getProjectileVelocity(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).initialSpeed() : ai.getInitialSpeed();
        }
        return 2.0f;
    }

    private static float getProjectileInaccuracy(ItemStack weapon) {
        return getProjectileInaccuracy(weapon, null);
    }
    
    private static float getProjectileInaccuracy(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).dispersion() : ai.getDispersionAngle();
        }
        if (isSmallCaliber(weapon)) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.0f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.5f;
        return 1.0f;
    }

    private static float getVerticalSpread(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).verticalSpread() : ai.getData().verticalSpread();
        }
        return getProjectileInaccuracy(weapon, level);
    }

    private static float getHorizontalSpread(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).horizontalSpread() : ai.getData().horizontalSpread();
        }
        return getProjectileInaccuracy(weapon, level);
    }

    private static double getMinElevationRadians(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            float value = level != null ? ai.getEffectiveData(level).minElevation() : ai.getData().minElevation();
            return Math.toRadians(value);
        }
        return Math.toRadians(-89.0);
    }

    private static double getMaxElevationRadians(ItemStack weapon, net.minecraft.world.level.Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            float value = level != null ? ai.getEffectiveData(level).maxElevation() : ai.getData().maxElevation();
            return Math.toRadians(value);
        }
        return Math.toRadians(89.0);
    }

    /** 从武器数据获取自定义重力（真实比例，0=使用默认）。 */
    private static float getProjectileGravity(ItemStack weapon) {
        return getProjectileGravity(weapon, null);
    }
    
    private static float getProjectileGravity(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).gravity() : ai.getCustomGravity();
        }
        return 0f;
    }

    /** Phase 2: 从武器数据获取每 tick 速度减小量。 */
    private static float getProjectileDrag(ItemStack weapon) {
        return getProjectileDrag(weapon, null);
    }
    
    private static float getProjectileDrag(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            return level != null ? ai.getEffectiveData(level).dragCoeff() : ai.getDragCoeff();
        }
        if (isSmallCaliber(weapon)) return 0.015f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 0.01f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.008f;
        return 0.01f;
    }

    private static float getSoundPitch(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.2f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.8f;
        return 1.0f;
    }

    /** Phase 10: 根据武器获取对应口径的发射音效。 */
    private static SoundEvent getFireSound(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return ModSounds.CANNON_FIRE_SMALL.get();
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return ModSounds.CANNON_FIRE_MEDIUM.get();
        return ModSounds.CANNON_FIRE_LARGE.get();
    }

    private static SoundEvent getFireTailSound(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return ModSounds.CANNON_FIRE_SMALL_TAIL.get();
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return ModSounds.CANNON_FIRE_MEDIUM_TAIL.get();
        return ModSounds.CANNON_FIRE_LARGE_TAIL.get();
    }

    @Nullable
    private static SoundEvent getDistantFireSound(ItemStack weapon) {
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return ModSounds.CANNON_FIRE_MEDIUM_DISTANT.get();
        if (weapon.is(ModItems.LARGE_GUN.get())) return ModSounds.CANNON_FIRE_LARGE_DISTANT.get();
        return null;
    }

    private static void playCannonFireSound(Level level, Player player, ItemStack weapon) {
        float pitch = getSoundPitch(weapon);
        SoundEvent fireSound = getFireSound(weapon);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                fireSound, SoundSource.PLAYERS, 2.0f, pitch);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                getFireTailSound(weapon), SoundSource.PLAYERS, 1.15f, Math.max(0.55f, pitch * 0.82f));
        SoundEvent distantFireSound = getDistantFireSound(weapon);
        if (distantFireSound != null) {
            float distantVolume = weapon.is(ModItems.LARGE_GUN.get()) ? 1.65f : 1.25f;
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    distantFireSound, SoundSource.PLAYERS, distantVolume, Math.max(0.5f, pitch * 0.62f));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.95f, pitch * 0.75f);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.65f, pitch * 0.55f);
    }

    private static void playCannonReloadStartSound(Player player, ItemStack weapon) {
        float pitch = Math.max(0.55f, getSoundPitch(weapon) * 0.75f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD.get(), SoundSource.PLAYERS, 0.45f, pitch);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD_BREECH.get(), SoundSource.PLAYERS, 0.32f, Math.max(0.5f, pitch * 0.88f));
    }

    private static void playCannonReloadCompleteSound(Player player, ItemStack weapon) {
        float pitch = Math.max(0.65f, getSoundPitch(weapon));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD.get(), SoundSource.PLAYERS, 0.65f, pitch);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.CANNON_RELOAD_BREECH.get(), SoundSource.PLAYERS, 0.42f, Math.max(0.55f, pitch * 0.78f));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.35f, pitch + 0.25f);
    }

    /** Phase 5: 从瞄准镜模式开火（由 ScopeFirePayload 调用）。复用 tryFireFromInventory 的弹药/冷却逻辑。 */
    public static void fireFromScope(Player player, ItemStack weapon, double tx, double ty, double tz) {
        Vec3 target = new Vec3(tx, ty, tz);
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new Aimed(target));
    }

    /** 非弹道解算的直射目标开火：用于快速点击时应用炮塔转速滞后。 */
    public static void fireDirectAt(Player player, ItemStack weapon, double tx, double ty, double tz) {
        Vec3 target = new Vec3(tx, ty, tz);
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new DirectAim(target));
    }

    /** Phase 5: 最大射程开火（由 ScopeFirePayload 调用）。使用最大射程仰角发射。 */
    public static void fireMaxRange(Player player, ItemStack weapon) {
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new MaxRange());
    }

    /** Phase 5: 目标超出射程时回退为最大射程射击。 */
    public static void fireMaxRange(ServerPlayer player, ItemStack weapon) {
        tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND, new MaxRange());
    }

    /** 获取武器的炮口位置列表。如果武器是 ArtilleryItem，从配置读取；否则返回默认单炮口。 */
    private static java.util.List<com.piranport.artillery.config.MuzzlePos> getMuzzlePositions(ItemStack weapon, net.minecraft.world.level.Level level) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) {
            java.util.List<com.piranport.artillery.config.MuzzlePos> muzzles =
                    level != null ? ai.getEffectiveData(level).muzzles() : ai.getData().muzzles();
            if (!muzzles.isEmpty()) {
                return muzzles;
            }
        }
        // 默认单炮口位置（向前1.5格）
        return java.util.List.of(new com.piranport.artillery.config.MuzzlePos(0, 0, 1.5));
    }

    /** 将炮口位置从武器本地坐标系旋转到玩家视角坐标系。 */
    private static Vec3 rotateMuzzleByPlayerView(Player player, com.piranport.artillery.config.MuzzlePos muzzle) {
        // 炮口位置定义：x=左右，y=上下，z=前后（相对武器）
        Vec3 localOffset = new Vec3(muzzle.x(), muzzle.y(), muzzle.z());

        // 获取玩家视角方向
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        // 转换为弧度
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // 构建旋转矩阵（先俯仰后偏航）
        double cosYaw = Math.cos(yawRad);
        double sinYaw = Math.sin(yawRad);
        double cosPitch = Math.cos(pitchRad);
        double sinPitch = Math.sin(pitchRad);

        // 应用旋转变换
        double x = localOffset.x * cosYaw - localOffset.z * sinYaw;
        double y = localOffset.y * cosPitch + localOffset.z * sinPitch;
        double z = localOffset.x * sinYaw + localOffset.z * cosYaw * cosPitch;

        return new Vec3(x, y, z);
    }

    /** Fire a cannon salvo: barrelCount projectiles with natural inaccuracy spread. */
    private static boolean fireCannonSalvo(Level level, Player player, ItemStack weapon,
            ItemStack shellForRender, int barrelCount, boolean isType3, boolean isVT,
            boolean isHE, AimInstruction aim) {
        // Phase 11: 全局炮弹上限检测
        if (isShellLimitReached(level, player)) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.max_projectiles"), true);
            return false;
        }
        // Phase 9: 消耗耐久（创造模式不消耗）
        if (!player.getAbilities().instabuild && weapon.isDamageableItem()) {
            int curDamage = weapon.getDamageValue();
            int effectiveDurability = getCannonDurability(weapon, level);
            if (curDamage < effectiveDurability - 1) {
                weapon.setDamageValue(curDamage + 1);
            }
        }

        // 获取炮口位置数据
        java.util.List<com.piranport.artillery.config.MuzzlePos> muzzles = getMuzzlePositions(weapon, level);
        Vec3 referenceMuzzleOffset = rotateMuzzleByPlayerView(player, muzzles.get((muzzles.size() - 1) / 2));
        Vec3 aimOrigin = player.getEyePosition().add(referenceMuzzleOffset);

        float verticalSpreadDeg = getVerticalSpread(weapon, level);
        float horizontalSpreadDeg = getHorizontalSpread(weapon, level);
        for (int b = 0; b < barrelCount; b++) {
            // 计算当前炮管的炮口位置
            com.piranport.artillery.config.MuzzlePos muzzle = muzzles.get(b % muzzles.size());
            Vec3 muzzleOffset = rotateMuzzleByPlayerView(player, muzzle);
            Vec3 spawnPos = player.getEyePosition().add(muzzleOffset);

            if (isType3) {
                boolean spawned = fireSanshikiSpread(level, player, weapon, shellForRender, spawnPos);
                if (!spawned && b == 0) {
                    return false;
                }
            } else {
                float damage = getGunDamage(weapon, level);
                float explosionPower = getExplosionPower(weapon, level);
                float velocity = getProjectileVelocity(weapon, level);
                float drag = getProjectileDrag(weapon, level);
                float gravity = getProjectileGravity(weapon, level);

                CannonProjectileEntity projectile = new CannonProjectileEntity(
                        level, player, shellForRender, damage, isHE, explosionPower);
                if (isVT) projectile.setVT(true);
                projectile.setDragCoeff(drag);
                projectile.setCustomGravity(gravity);
                // 依据：策划决策/数值/05-船型职能分化修订.md（AP 大口径对小型船过穿）
                if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem artilleryItem) {
                    projectile.setSourceCaliber(artilleryItem.getEffectiveData(level).caliber());
                }

                Vec3 direction;
                if (aim instanceof Aimed(Vec3 aimTarget)) {
                    // 弹道解算瞄准：以中间炮口作为整门炮的火控基准。
                    direction = computeAimDirection(player, weapon, velocity, aimTarget, aimOrigin);
                } else if (aim instanceof DirectAim(Vec3 aimTarget)) {
                    // 快速点击直射：目标点来自客户端炮塔转速滞后的射线，不做抛物线解算。
                    direction = computeDirectAimDirection(player, aimTarget, spawnPos);
                } else if (aim instanceof MaxRange) {
                    // 最大射程：使用玩家 yaw + 最大射程仰角
                    double mcGravity = gravity > 0f ? gravity / 196.0 : BallisticSolver.DEFAULT_GRAVITY;
                    double pitch = BallisticSolver.calculateMaxRangeAngle(velocity, drag, mcGravity,
                            getMinElevationRadians(weapon, level), getMaxElevationRadians(weapon, level));
                    float yaw = player.getYRot();
                    double yawRad = Math.toRadians(yaw);
                    double cosP = Math.cos(pitch);
                    direction = new Vec3(-Math.sin(yawRad) * cosP, Math.sin(pitch), Math.cos(yawRad) * cosP).normalize();
                } else {
                    direction = player.getLookAngle();
                }
                // 高斯散布：按配置的角度标准差偏转初速度方向
                Vec3 velocityVec = direction.scale(velocity);
                velocityVec = com.piranport.artillery.ArtilleryItem.applyDispersion(
                        velocityVec, level.random, horizontalSpreadDeg, verticalSpreadDeg);

                // 设置炮弹生成位置和速度
                projectile.setPos(spawnPos);
                projectile.shoot(velocityVec.x, velocityVec.y, velocityVec.z, (float) velocityVec.length(), 0f);
                level.addFreshEntity(projectile);
            }

            // 炮口火焰粒子（服务端广播）— 使用实际炮口位置
            if (level instanceof ServerLevel serverLevel) {
                Vec3 look = player.getLookAngle();
                double px = spawnPos.x + look.x * 0.3;
                double py = spawnPos.y + look.y * 0.3;
                double pz = spawnPos.z + look.z * 0.3;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 3,
                        0.2, 0.2, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 2,
                        0.3, 0.15, 0.3, 0.01);
                serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 1,
                        0.1, 0.1, 0.1, 0);
            }

            // Phase 10: 只在第一个炮管播放音效 + 震动（避免齐射重复）
            if (b == 0) {
                playCannonFireSound(level, player, weapon);

                // 发射屏幕震动（S2C）
                float shakeIntensity = isSmallCaliber(weapon) ? 0.3f : 0.6f;
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new ShakeEffectPayload(shakeIntensity, 6));
                }
            }
        }
        return true;
    }

    /** Phase 5: 弹道解算瞄准。计算从炮口到目标的最优发射方向向量。 */
    private static Vec3 computeAimDirection(Player player, ItemStack weapon,
                                             float velocity, Vec3 aimTarget, Vec3 muzzlePos) {
        Vec3 toTarget = aimTarget.subtract(muzzlePos);
        double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double verticalDist = toTarget.y;

        if (horizontalDist < 1.0) {
            return player.getLookAngle();
        }

        float drag = getProjectileDrag(weapon, player.level());
        float gravity = getProjectileGravity(weapon, player.level());
        double mcGravity = gravity > 0f ? gravity / 196.0 : BallisticSolver.DEFAULT_GRAVITY;
        BallisticSolver.Result result = BallisticSolver.solve(velocity, drag, mcGravity,
                horizontalDist, verticalDist, 0.0,
                getMinElevationRadians(weapon, player.level()),
                getMaxElevationRadians(weapon, player.level()));
        double optimalPitch = result.angle();

        if (result.outOfRange()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.out_of_range"), true);
        }

        // 将服务端解算统计发送给客户端
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.piranport.combat.BallisticSolverStats stats = com.piranport.combat.BallisticSolverStats.getInstance();
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(serverPlayer,
                    new com.piranport.network.SolverStatsPayload(
                            stats.getLastTernaryIters(),
                            stats.getLastNewtonIters(),
                            stats.getLastTotalUs(),
                            stats.getCombinedVerticalError(),
                            stats.getCombinedHorizontalError(),
                            Math.toDegrees(optimalPitch)));
        }

        // MC 偏航角：atan2(-dx, dz) 映射到 MC 坐标（yaw=0 = +Z）
        double yawRad = Math.atan2(-toTarget.x, toTarget.z);
        double pitchRad = optimalPitch; // 方向向量，向上为正

        double cosP = Math.cos(pitchRad);
        return new Vec3(
                -Math.sin(yawRad) * cosP,
                Math.sin(pitchRad),
                Math.cos(yawRad) * cosP
        ).normalize();
    }

    private static Vec3 computeDirectAimDirection(Player player, Vec3 aimTarget, Vec3 muzzlePos) {
        Vec3 direction = aimTarget.subtract(muzzlePos);
        if (direction.lengthSqr() < 1.0e-6) {
            return player.getLookAngle();
        }
        return direction.normalize();
    }

    // ===== Global projectile limit (Phase 11) =====

    /** 检查全局炮弹是否已达上限（CannonProjectileEntity 合计）。 */
    private static boolean isShellLimitReached(Level level, Player player) {
        int maxProjectiles = ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES.get();
        if (maxProjectiles <= 0) return false;
        // 仅在玩家附近搜索（模拟距离范围），避免使用全图(-3e7~3e7) AABB 遍历
        int count = level.getEntitiesOfClass(
                CannonProjectileEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(256)).size();
        return count >= maxProjectiles;
    }

    // ===== Type 3 (Sanshiki) spread firing =====

    /**
     * Spawns pellets in a circular spread pattern (sunflower/golden-angle distribution).
     * Each pellet deals 1/4 of the same-caliber HE base damage.
     * Pellet count is controlled by ModArtilleryConfig.PERF_SHRAPNEL_LIMIT.
     */
    /** 单次开火最多同时存在的三式弹霰弹数量（从 ModArtilleryConfig 读取）。 */
    private static int getShrapnelLimit() {
        return ModArtilleryConfig.PERF_SHRAPNEL_LIMIT.get();
    }

    private static boolean fireSanshikiSpread(Level level, Player player, ItemStack weapon, ItemStack shellForRender, Vec3 spawnPos) {
        float baseDamage = getGunDamage(weapon, level);
        float pelletDamage = baseDamage * 0.25f;
        float velocity = getProjectileVelocity(weapon, level);

        // 霰弹数量限制：统计玩家附近256格内的现有霰弹数，超过上限则不发射
        int targetCount = 64; // 三式弹固定发射64枚霰弹（设计文档要求）
        int limit = getShrapnelLimit();
        if (limit <= 0) return false;
        int existing = level.getEntitiesOfClass(
                SanshikiPelletEntity.class,
                new AABB(player.blockPosition()).inflate(256))
                .size();
        int canSpawn = limit - existing;
        if (canSpawn < targetCount) {
            // 霰弹数量不足，拒绝发射
            player.displayClientMessage(
                    Component.translatable("message.piranport.shrapnel_limit"), true);
            return false;
        }
        int pelletCount = targetCount;

        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();
        float maxRadius = 7.0f; // degrees, same spread range as before
        float goldenAngle = 2.39996323f; // ~137.508 degrees in radians

        for (int i = 0; i < pelletCount; i++) {
            float r = maxRadius * (float) Math.sqrt((i + 0.5f) / pelletCount);
            float theta = i * goldenAngle;
            float yawOffset = r * (float) Math.cos(theta);
            float pitchOffset = r * (float) Math.sin(theta);

            SanshikiPelletEntity pellet = new SanshikiPelletEntity(
                    level, player, pelletDamage, shellForRender);
            pellet.setPos(spawnPos);
            pellet.shootFromRotation(player,
                    basePitch + pitchOffset,
                    baseYaw + yawOffset,
                    0.0f, velocity, 0.5f);
            level.addFreshEntity(pellet);
        }
        return true;
    }

    private static float[] getSpreadAngles(int count) {
        return switch (count) {
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> new float[]{0f};
        };
    }

    // ====================================================================
    // 飞机系统 — 起飞/召回/燃料/自动战斗
    // ====================================================================

    // ===== Fuel refill =====

    /**
     * Recall all airborne aircraft owned by this player. Returns the count recalled.
     */
    public static int recallAllAircraft(ServerLevel level, Player player) {
        java.util.UUID ownerUUID = player.getUUID();
        // Reduced recall range from 300 to 128 blocks for better performance
        java.util.List<AircraftEntity> aircraft = level.getEntitiesOfClass(
                AircraftEntity.class,
                new AABB(player.getX() - 128, player.getY() - 64, player.getZ() - 128,
                         player.getX() + 128, player.getY() + 64, player.getZ() + 128),
                a -> ownerUUID.equals(a.getOwnerUUID()) && a.isAlive());
        for (AircraftEntity a : aircraft) {
            a.startReturning("core_recall");
        }
        // End recon mode if active
        if (!aircraft.isEmpty()) {
            com.piranport.aviation.ReconManager.endRecon(ownerUUID);
            com.piranport.aviation.FireControlManager.clearTargets(ownerUUID);
        }
        return aircraft.size();
    }

    /**
     * On transformation: consume aviation_fuel to fill aircraft.
     * One aviation_fuel item fills one aircraft to full fuelCapacity.
     */
    public static void refillAircraftFuel(Player player, ItemStack coreStack) {
        if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return; // manual mode: no auto fuel
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        refillAircraftFuelInventoryMode(player);
    }

    /**
     * Inventory mode fuel refill: scan inventory for AircraftItem stacks and aviation_fuel,
     * consuming one fuel per aircraft that needs it.
     */
    private static void refillAircraftFuelInventoryMode(Player player) {
        Inventory inv = player.getInventory();

        for (ItemStack weapon : inv.items) {
            if (!(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() >= info.fuelCapacity()) {
                continue;
            }
            // Find aviation_fuel in inventory
            for (ItemStack ammo : inv.items) {
                if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                    com.piranport.debug.PiranPortDebug.consumeAmmo(ammo, 1);
                    weapon.set(ModDataComponents.AIRCRAFT_INFO.get(),
                            info.withCurrentFuel(info.fuelCapacity()));
                    break;
                }
            }
        }
    }

    // ===== Auto-launch (Phase 36) =====

    /**
     * Server-side: refuel + launch the first available FIGHTER in weapon slots.
     * Called by the auto-launch tick when phantoms are detected nearby.
     * Returns true if a fighter was launched.
     */
    public static boolean tryAutoLaunchFighter(Level level, Player player, ItemStack coreStack, int coreSlot) {
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return false;
        if (level.isClientSide()) return false;

        // Refuel aircraft before checking.
        refillAircraftFuel(player, coreStack);

        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = level.getGameTime();

        for (int wi = 0; wi < 9; wi++) {
            if (wi == coreSlot) continue;
            if (cooldowns.isOnCooldown(wi, gameTime)) continue;

            ItemStack weapon = inv.items.get(wi);
            if (weapon.isEmpty() || !(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() <= 0) continue;

            boolean isFighter = info.aircraftType() == AircraftInfo.AircraftType.FIGHTER
                    || info.aircraftType() == AircraftInfo.AircraftType.ROCKET_FIGHTER;
            if (!isFighter) continue;

            launchAircraftInventoryMode(level, player, coreStack, inv, wi, coreSlot, cooldowns);
            com.piranport.debug.PiranPortDebug.event(
                    "Auto fighter launch | slot={} aircraft={}",
                    wi, BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath());
            return true;
        }
        return false;
    }

    /**
     * Auto-fire anti-air missiles from the player's hotbar when auto-launch is active.
     * Scans hotbar for ANTI_AIR MissileLauncherItems, checks cooldown and ammo, fires one missile.
     * The missile is aimed toward the fire control target (or upward if no lock).
     */
    public static boolean tryAutoFireAntiAirMissile(Level level, Player player, ItemStack coreStack, int coreSlot) {
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return false;
        if (level.isClientSide()) return false;

        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long gameTime = level.getGameTime();

        // Scan hotbar (slots 0-8) for anti-air missile launchers
        for (int slot = 0; slot < 9; slot++) {
            if (slot == coreSlot) continue;
            ItemStack stack = inv.items.get(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof MissileLauncherItem launcher)) continue;
            if (launcher.getMissileType() != MissileEntity.MissileType.ANTI_AIR) continue;

            // Check cooldown
            if (cooldowns.isOnCooldown(slot, gameTime)) continue;

            // Find ammo in inventory
            Item ammoItem = launcher.getAmmoItem();
            int ammoSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == slot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.is(ammoItem)) {
                    ammoSlot = i;
                    break;
                }
            }
            if (ammoSlot == -1 && slot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.is(ammoItem)) {
                    ammoSlot = 40;
                }
            }
            if (ammoSlot == -1) continue; // No ammo for this launcher, try next

            // Consume 1 ammo
            String ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            com.piranport.debug.PiranPortDebug.consumeAmmo(
                    ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot), 1);

            // Spawn missile aimed at fire control target
            spawnMissileAutoAim(level, player, stack, launcher, ammoId);

            // Apply cooldown
            int cd = TransformationManager.boostedCooldown(player,
                    ExperienceShellItem.applyCooldownReduction(stack, launcher.getCooldownTicks()));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(slot, cd, gameTime));
            stack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(gameTime, cd));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        }
        return false;
    }

    /** Spawn a missile aimed toward the fire control target, nearest hostile, or upward. */
    private static void spawnMissileAutoAim(Level level, Player player, ItemStack launcherStack,
                                             MissileLauncherItem launcher, String displayItemId) {
        Vec3 aimDir = null;
        if (level instanceof ServerLevel sl) {
            // 1. Fire control target
            List<UUID> fcTargets = FireControlManager.getTargets(player.getUUID());
            for (UUID targetUUID : fcTargets) {
                net.minecraft.world.entity.Entity target = sl.getEntity(targetUUID);
                if (target != null && target.isAlive() && !target.isUnderWater()
                        && !(target instanceof net.minecraft.world.Container)) {
                    Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                            .subtract(player.getEyePosition());
                    if (toTarget.lengthSqr() > 0.01) {
                        aimDir = toTarget.normalize();
                    }
                    break;
                }
            }
            // 2. No fire control lock — find nearest hostile mob (Enemy interface covers Phantom/Vex/Monster)
            //    防空导弹自动瞄准仅限飞行目标（离地至少2格的空中目标）
            final boolean antiAirOnly = launcher.getMissileType() == MissileEntity.MissileType.ANTI_AIR;
            if (aimDir == null) {
                LivingEntity nearest = null;
                double bestDist = 32.0;
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(32.0),
                        e -> e.isAlive() && e.isPickable() && e instanceof Enemy && !e.isUnderWater())) {
                    if (antiAirOnly) {
                        // Anti-air missiles only target airborne enemies (at least 2 blocks above ground)
                        net.minecraft.core.BlockPos below = mob.blockPosition().below(2);
                        if (mob.onGround() || level.getBlockState(below).isSolid()) {
                            continue;
                        }
                    }
                    double d = player.distanceTo(mob);
                    if (d < bestDist) {
                        bestDist = d;
                        nearest = mob;
                    }
                }
                if (nearest != null) {
                    Vec3 toTarget = nearest.position().add(0, nearest.getBbHeight() * 0.5, 0)
                            .subtract(player.getEyePosition());
                    if (toTarget.lengthSqr() > 0.01) {
                        aimDir = toTarget.normalize();
                    }
                }
            }
        }
        // 3. Fallback: upward launch (avoid hitting ground)
        if (aimDir == null) {
            Vec3 look = player.getLookAngle();
            aimDir = new Vec3(look.x, Math.max(look.y, 0.5), look.z).normalize();
        }

        spawnMissileWithDir(level, player, launcherStack, launcher, displayItemId, aimDir);
    }

    /** Rotate a look vector around the Y axis by angleRad, project to horizontal plane, normalize. */
    private static Vec3 rotateHorizontal(Vec3 look, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double nx = look.x * cos - look.z * sin;
        double nz = look.x * sin + look.z * cos;
        Vec3 result = new Vec3(nx, 0, nz);
        return result.lengthSqr() > 0 ? result.normalize() : new Vec3(1, 0, 0);
    }

    /**
     * 获取指定武器的默认弹药类型（创造模式使用）
     */
    private static Item getDefaultAmmoForWeapon(ItemStack weapon) {
        return getDefaultAmmoForWeapon(weapon, null);
    }

    private static Item getDefaultAmmoForWeapon(ItemStack weapon, @Nullable Level level) {
        if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
            int caliber = level != null ? ai.getEffectiveData(level).caliber() : ai.getCaliber();
            if (caliber <= 4) return ModItems.SMALL_AP_SHELL.get();
            if (caliber <= 8) return ModItems.MEDIUM_AP_SHELL.get();
            return ModItems.LARGE_AP_SHELL.get();
        }
        return null;
    }

    /**
     * 获取指定口径的默认鱼雷类型（创造模式使用）
     */
    private static TorpedoItem getDefaultTorpedoForCaliber(int caliber) {
        return switch (caliber) {
            case 533 -> (TorpedoItem) ModItems.TORPEDO_533MM.get();
            case 610 -> (TorpedoItem) ModItems.TORPEDO_610MM.get();
            default -> null;
        };
    }

    // ===== 齐射系统 =====

    /** 提取核心查找逻辑。返回核心所在格子（-2=头盔/config槽，-1=未找到）。 */
    private static int findCoreSlotIndex(Inventory inv, Player player, int weaponSlot) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                return i;
            }
        }
        if (weaponSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (oh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(oh)) {
                return 40;
            }
        }
        ItemStack configCore = TransformationManager.getCoreFromConfiguredSlot(player);
        if (configCore.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(configCore)) {
            return -2;
        }
        return -1;
    }

    /**
     * 单击最佳槽位：优先手持炮（同类型+未冷却），否则从左到右扫第一门同型可用炮。
     * @return {weaponSlot, coreSlot} 或 null
     */
    @Nullable
    public static int[] findBestArtillerySlot(Player player, Item weaponType, ItemStack coreStack) {
        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long now = player.level().getGameTime();

        // 优先手持
        int heldSlot = inv.selected;
        ItemStack held = inv.items.get(heldSlot);
        if (held.getItem() == weaponType
                && !cooldowns.isOnCooldown(heldSlot, now)
                && isCannonReadyToFire(held, player.level())) {
            int coreSlot = findCoreSlotIndex(inv, player, heldSlot);
            return new int[]{heldSlot, coreSlot};
        }

        // 从左到右扫描
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == heldSlot) continue;
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() == weaponType
                    && !cooldowns.isOnCooldown(i, now)
                    && isCannonReadyToFire(stack, player.level())) {
                int coreSlot = findCoreSlotIndex(inv, player, i);
                return new int[]{i, coreSlot};
            }
        }

        // 副手
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() == weaponType
                && !cooldowns.isOnCooldown(40, now)
                && isCannonReadyToFire(offhand, player.level())) {
            int coreSlot = findCoreSlotIndex(inv, player, 40);
            return new int[]{40, coreSlot};
        }

        return null;
    }

    /**
     * 收集物品栏中所有同类型且未冷却的火炮槽位。
     * @return 非空 {weaponSlot, coreSlot} 对列表
     */
    public static List<int[]> findMatchingArtillerySlots(Player player, Item weaponType, ItemStack coreStack) {
        List<int[]> result = new ArrayList<>();
        Inventory inv = player.getInventory();
        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        long now = player.level().getGameTime();

        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack.getItem() == weaponType
                    && !cooldowns.isOnCooldown(i, now)
                    && isCannonReadyToFire(stack, player.level())) {
                int coreSlot = findCoreSlotIndex(inv, player, i);
                result.add(new int[]{i, coreSlot});
            }
        }

        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() == weaponType
                && !cooldowns.isOnCooldown(40, now)
                && isCannonReadyToFire(offhand, player.level())) {
            int coreSlot = findCoreSlotIndex(inv, player, 40);
            result.add(new int[]{40, coreSlot});
        }

        return result;
    }

    /** 将手持火炮的 SELECTED_AMMO_TYPE 同步到物品栏中所有同类型火炮。 */
    public static void syncAmmoToSiblingGuns(Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof com.piranport.artillery.ArtilleryItem)) return;

        Item heldType = held.getItem();
        SelectedAmmoType selected = held.getOrDefault(
                ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack != held && stack.getItem() == heldType) {
                stack.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), selected);
            }
        }
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() == heldType) {
            offhand.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), selected);
        }
    }

    /**
     * 齐射入口（由 SalvoFirePayload.handle 的 enqueueWork 调用）。
     * 1) 弹种同步 2) 收集可发射槽位 3) 第一个立即发射 4) 剩余 → SalvoManager 延迟。
     */
    public static void beginSalvo(ServerPlayer player, Item weaponType,
                                   int aimMode, double ax, double ay, double az) {
        Inventory inv = player.getInventory();
        int weaponSlot = inv.selected;
        int coreSlot = findCoreSlotIndex(inv, player, weaponSlot);
        if (coreSlot == -1) return;

        ItemStack coreStack;
        if (coreSlot == 40) {
            coreStack = inv.offhand.get(0);
        } else if (coreSlot == -2) {
            coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        } else {
            coreStack = inv.items.get(coreSlot);
        }
        if (coreStack.isEmpty()) return;

        // 弹种同步
        syncAmmoToSiblingGuns(player);

        // 收集同型可用槽位
        List<int[]> allSlots = findMatchingArtillerySlots(player, weaponType, coreStack);
        if (allSlots.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.no_ready_gun"), true);
            return;
        }

        // 第一个立即发射
        int[] first = allSlots.remove(0);
        ItemStack firstWeapon = first[0] == 40 ? inv.offhand.get(0) : inv.items.get(first[0]);
        int salvoIntervalTicks = getSalvoIntervalTicks(firstWeapon, player.level());
        AimInstruction aim = decodeAim(aimMode, ax, ay, az);
        fireWeaponAtSlot(player.level(), player, coreStack, first[0], first[1], aim);

        // 剩余 → 延迟调度
        if (!allSlots.isEmpty()) {
            com.piranport.combat.SalvoManager.schedule(player, weaponType, allSlots,
                    aimMode, ax, ay, az, salvoIntervalTicks);
        }
    }

    /**
     * 由 SalvoManager 逐 tick 调用，执行单门炮的延迟射击。
     * 执行前严格校验：玩家存活、未跨维度、武器仍在且类型匹配、核心仍在。
     */
    public static void executeSalvoFire(ServerLevel level, ServerPlayer player,
                                         int weaponSlot, int coreSlot, Item expectedWeaponType,
                                         int aimMode, double ax, double ay, double az) {
        if (!player.isAlive()) return;
        if (player.level() != level) return;

        Inventory inv = player.getInventory();
        ItemStack weapon = (weaponSlot == 40) ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        if (weapon.isEmpty() || weapon.getItem() != expectedWeaponType) return;

        ItemStack coreStack;
        if (coreSlot == 40) {
            coreStack = inv.offhand.get(0);
        } else if (coreSlot == -2) {
            coreStack = TransformationManager.getCoreFromConfiguredSlot(player);
        } else {
            coreStack = inv.items.get(coreSlot);
        }
        if (coreStack.isEmpty() || !(coreStack.getItem() instanceof ShipCoreItem)
                || !TransformationManager.isTransformed(coreStack)) return;

        SlotCooldowns cooldowns = coreStack.getOrDefault(
                ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        if (cooldowns.isOnCooldown(weaponSlot, level.getGameTime())) return;

        AimInstruction aim = decodeAim(aimMode, ax, ay, az);
        fireWeaponAtSlot(level, player, coreStack, weaponSlot, coreSlot, aim);
    }

    /** 将 aim 模式 int 常量转为 sealed AimInstruction。 */
    private static AimInstruction decodeAim(int aimMode, double ax, double ay, double az) {
        return switch (aimMode) {
            case ARTILLERY_AIM_TARGET -> new Aimed(new Vec3(ax, ay, az));
            case ARTILLERY_AIM_MAX_RANGE -> new MaxRange();
            case ARTILLERY_AIM_DIRECT_TARGET -> new DirectAim(new Vec3(ax, ay, az));
            default -> new NoAim();
        };
    }
}
