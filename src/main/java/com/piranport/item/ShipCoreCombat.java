package com.piranport.item;

import com.piranport.aviation.AircraftFireStrategy;
import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.data.AmmoInventory;
import com.piranport.combat.data.WeaponState;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.network.ShakeEffectPayload;
import com.piranport.registry.ModDataComponents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModSounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Mob;
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
import com.piranport.combat.depthcharge.DepthChargeFireStrategy;
import com.piranport.combat.missile.MissileFireStrategy;
import com.piranport.combat.torpedo.TorpedoFireStrategy;
import com.piranport.platform.ClientHooks;
import com.piranport.PiranPort;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipCoreCombat.class);

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

        WeaponState ws = new WeaponState(weapon);
        LoadedAmmo loaded = ws.getLoadedAmmo();
        boolean isAutoLoading = weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai && ai.isAutoLoading();
        if (!isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level)) {
            ws.clearLoadedAmmo();
            // 策划决策/武器/07-火炮装填双模式.md：自动模式空炮时自动启动装填读条；
            // 手动模式按 R 才启动，未装填时射击什么都不做（只提示）
            if (isAutoLoading) {
                startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
            }
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        ItemStack shellForRender = createAmmoStack(loaded.ammoItemId());
        if (shellForRender.isEmpty()) {
            ws.clearLoadedAmmo();
            player.displayClientMessage(Component.translatable("message.piranport.weapon_not_loaded"), true);
            return true;
        }

        boolean isType3 = isType3Shell(loaded.ammoItemId());
        boolean isVT = isVTShell(loaded.ammoItemId());
        boolean isHE = isHEShell(loaded.ammoItemId()) || isVT;
        ws.clearLoadedAmmo();
        new AmmoInventory(inv, coreSlot, weaponSlot).recordAmmoType(weapon, shellForRender.getItem());

        boolean fired = fireCannonSalvo(level, player, weapon, shellForRender, barrelCount,
                isType3, isVT, isHE, aim);
        if (!fired) {
            ws.setLoadedAmmo(loaded.count(), loaded.ammoItemId());
            return true;
        }

        // 决策/数值/05 §定稿修订 #3：大口径主炮开火触发 5 秒防空静默窗口
        com.piranport.combat.AASilenceManager.onCannonFire(player, shellForRender);

        // 策划决策/武器/07-火炮装填双模式.md
        // 开火后自动模式立即启动下一轮装填读条；手动模式按 R 才启动
        if (isAutoLoading) {
            startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
        }

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
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai)) return false;
        if (coreSlot == -1) return false;

        long now = player.level().getGameTime();
        int barrelCount = getBarrelCount(weapon, player.level());
        WeaponState ws = new WeaponState(weapon);
        LoadedAmmo loaded = ws.getLoadedAmmo();
        if (isLoadedCannonAmmoValid(loaded, weapon, barrelCount, player.level())) {
            return false;
        }
        if (ws.hasLoadedAmmo()) {
            ws.clearLoadedAmmo();
        }

        if (cooldowns.isOnCooldown(weaponSlot, now)) {
            return false;
        }

        // 策划决策/武器/07-火炮装填双模式.md
        // 读条结束 → 调用 completeCannonReload（手动+自动都执行，否则手动模式的 R 键读条永不完成）
        if (ws.getCooldown() != null && ws.getCooldown().endTick() > 0 && ws.getCooldown().endTick() <= now) {
            return completeCannonReload(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
        }

        // 启动新读条：仅自动模式（R 键路径已单独处理手动模式）
        if (ai.isAutoLoading()) {
            return startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
        }
        return false;
    }

    private static boolean startCannonReloadIfPossible(Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns) {
        LOGGER.info("[ShipCoreCombat] startCannonReloadIfPossible - weapon: {}, weaponSlot: {}, coreSlot: {}",
            weapon.getItem(), weaponSlot, coreSlot);
        if (coreSlot == -1) {
            LOGGER.info("[ShipCoreCombat] coreSlot == -1, returning false");
            return false;
        }
        int barrelCount = getBarrelCount(weapon, player.level());
        LOGGER.info("[ShipCoreCombat] barrelCount: {}, creative: {}", barrelCount, player.getAbilities().instabuild);
        AmmoInventory ammoInv = new AmmoInventory(inv, coreSlot, weaponSlot);
        Item ammoType = ammoInv.chooseReloadAmmo(weapon, barrelCount, player.getAbilities().instabuild, player.level());
        LOGGER.info("[ShipCoreCombat] chooseReloadAmmo returned: {}", ammoType);
        if (ammoType == null && !player.getAbilities().instabuild) {
            LOGGER.info("[ShipCoreCombat] No ammo found and not creative, clearing reload state");
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        if (ammoType != null) {
            ammoInv.recordAmmoType(weapon, ammoType);
        }

        int reloadTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon, player.level()));
        LOGGER.info("[ShipCoreCombat] Setting cooldown - reloadTicks: {}, weaponSlot: {}", reloadTicks, weaponSlot);
        long now = player.level().getGameTime();
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, reloadTicks, now));
        new WeaponState(weapon).setCooldown(now, reloadTicks);
        playCannonReloadStartSound(player, weapon);
        LOGGER.info("[ShipCoreCombat] Reload started successfully");
        return true;
    }

    /**
     * 手动模式火炮 R 键启动装填读条（策划决策/武器/07-火炮装填双模式.md）
     * <p>仅在武器 MANUAL 装填模式时空炮且未在读条才启动；自动模式应走自动 tick 装填。</p>
     */
    public static void tryManualCannonReload(Player player, ItemStack coreStack, int coreSlot, ItemStack weapon) {
        LOGGER.info("[tryManualCannonReload] Called - weapon: {}, core: {}, coreSlot: {}", weapon.getItem(), coreStack.getItem(), coreSlot);
        if (player.level().isClientSide()) return;
        if (coreStack.isEmpty() || weapon.isEmpty()) return;
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem ai)) return;

        LOGGER.info("[tryManualCannonReload] ArtilleryItem check passed, isAutoLoading: {}", ai.isAutoLoading());
        // 策划决策/武器/09：仅 MANUAL 模式武器允许 R 键手动装填；
        // 自动模式武器按 R 不应启动读条（避免与自动 tick 装填冲突）。
        if (ai.isAutoLoading()) {
            player.displayClientMessage(Component.translatable("message.piranport.weapon_auto_reload"), true);
            return;
        }

        // 已装弹或已在读条 → 提示
        WeaponState ws = new WeaponState(weapon);
        LOGGER.info("[tryManualCannonReload] WeaponState check - hasLoadedAmmo: {}", ws.hasLoadedAmmo());
        if (ws.hasLoadedAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.weapon_already_loaded"), true);
            return;
        }
        long now = player.level().getGameTime();
        LOGGER.info("[tryManualCannonReload] Cooldown check - isOnCooldown: {}", ws.isOnCooldown(now));
        if (ws.isOnCooldown(now)) {
            player.displayClientMessage(Component.translatable("message.piranport.weapon_reloading"), true);
            return;
        }

        LOGGER.info("[tryManualCannonReload] All checks passed, starting reload");
        Inventory inv = player.getInventory();
        int weaponSlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (inv.items.get(i) == weapon) { weaponSlot = i; break; }
        }
        if (weaponSlot < 0) weaponSlot = inv.selected;
        LOGGER.info("[tryManualCannonReload] weaponSlot: {}, coreSlot: {}", weaponSlot, coreSlot);
        SlotCooldowns cooldowns = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        startCannonReloadIfPossible(player, coreStack, inv, weaponSlot, coreSlot, weapon, cooldowns);
    }

    private static boolean completeCannonReload(Player player, ItemStack coreStack, Inventory inv,
            int weaponSlot, int coreSlot, ItemStack weapon, SlotCooldowns cooldowns) {
        int barrelCount = getBarrelCount(weapon, player.level());
        AmmoInventory ammoInv = new AmmoInventory(inv, coreSlot, weaponSlot);
        Item ammoType = ammoInv.chooseReloadAmmo(weapon, barrelCount, player.getAbilities().instabuild, player.level());
        if (ammoType == null) {
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        if (!player.getAbilities().instabuild
                && !ammoInv.consumeAmmo(ammoType, barrelCount)) {
            return clearCannonReloadState(coreStack, weapon, weaponSlot, cooldowns);
        }

        String ammoId = BuiltInRegistries.ITEM.getKey(ammoType).toString();
        WeaponState ws = new WeaponState(weapon);
        ws.setLoadedAmmo(barrelCount, ammoId);
        ws.clearCooldown();
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(), cooldowns.withoutSlotCooldown(weaponSlot));
        ammoInv.recordAmmoType(weapon, ammoType);
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
        WeaponState ws = new WeaponState(weapon);
        if (ws.getCooldown() != null) {
            ws.clearCooldown();
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
        WeaponState ws = new WeaponState(weapon);
        LoadedAmmo loaded = ws.getLoadedAmmo();
        return isLoadedCannonAmmoValid(loaded, weapon, barrelCount, level);
    }

    private static ItemStack createAmmoStack(String ammoItemId) {
        ResourceLocation rl = ResourceLocation.tryParse(ammoItemId);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item);
    }

    private static void fireTorpedosInventoryMode(Level level, Player player, ItemStack coreStack,
                                            Inventory inv, int weaponSlot, int coreSlot,
                                            TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        TorpedoFireStrategy.fireTorpedosInventoryMode(level, player, coreStack, inv, weaponSlot, coreSlot, launcher, cooldowns);
    }

    private static void fireDepthCharges(Level level, Player player, ItemStack coreStack,
                                          Inventory inv, int weaponSlot, int coreSlot,
                                          DepthChargeLauncherItem launcher, SlotCooldowns cooldowns) {
        DepthChargeFireStrategy.fireDepthCharges(level, player, coreStack, inv, weaponSlot, coreSlot, launcher, cooldowns);
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

    /** 偏好弹种不足时,按背包槽位顺序查找第一种同口径且足量的弹药类型。 */
    // This entire method is now replaced by AmmoInventory.findFirstSufficientAmmoByInventoryOrder()

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

    // This entire method is now replaced by AmmoInventory.chooseReloadAmmo()

    // This entire method is now replaced by AmmoInventory.recordAmmoType()

    // This entire method is now replaced by AmmoInventory.findFirstAmmoStack()

    // This entire method is now replaced by AmmoInventory.countAmmo()

    // This entire method is now replaced by AmmoInventory.consumeAmmo()

    static boolean isHEShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get());
    }

    static boolean isHEShell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_HE_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.MEDIUM_HE_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.LARGE_HE_SHELL.get()).toString())
                // MK23 视作 HE 走原版爆炸管线（副本/08 决策 §威力写死查表）
                || isMK23Shell(ammoItemId);
    }

    /** MK23 核炮弹识别 — 仅大型火炮可装填（参见 large_shells.json）。 */
    static boolean isMK23Shell(ItemStack stack) {
        return stack.is(ModItems.MK23_NUCLEAR_SHELL.get());
    }

    static boolean isMK23Shell(String ammoItemId) {
        return ammoItemId.equals(
                BuiltInRegistries.ITEM.getKey(ModItems.MK23_NUCLEAR_SHELL.get()).toString());
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
                // 副本/08 决策：MK23 核炮弹威力 = HE 表值 ×10（写死查表，不走运行时系数）。
                // 仅作用于 LARGE_SHELLS 火炮；isMK23Shell 已在 large_shells 标签上保证。
                if (isMK23Shell(shellForRender)) {
                    explosionPower = explosionPower * 10f;
                }
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

    // ====================================================================
    // 飞机系统 — 起飞/召回/燃料/自动战斗
    // ====================================================================

    // ===== Fuel refill =====

    /**
     * Recall all airborne aircraft owned by this player. Returns the count recalled.
     */
    public static int recallAllAircraft(ServerLevel level, Player player) {
        return AircraftFireStrategy.recallAllAircraft(level, player);
    }

    /**
     * On transformation: consume aviation_fuel to fill aircraft.
     * One aviation_fuel item fills one aircraft to full fuelCapacity.
     */
    public static void refillAircraftFuel(Player player, ItemStack coreStack) {
        AircraftFireStrategy.refillAircraftFuel(player, coreStack);
    }

    /**
     * Inventory mode fuel refill: scan inventory for AircraftItem stacks and aviation_fuel,
     * consuming one fuel per aircraft that needs it.
     */
    private static void refillAircraftFuelInventoryMode(Player player) {
        AircraftFireStrategy.refillAircraftFuelInventoryMode(player);
    }

    // ===== Auto-launch (Phase 36) =====

    /**
     * Server-side: refuel + launch the first available FIGHTER in weapon slots.
     * Called by the auto-launch tick when phantoms are detected nearby.
     * Returns true if a fighter was launched.
     */
    public static boolean tryAutoLaunchFighter(Level level, Player player, ItemStack coreStack, int coreSlot) {
        return AircraftFireStrategy.tryAutoLaunchFighter(level, player, coreStack, coreSlot);
    }

    /**
     * Auto-fire anti-air missiles from the player's hotbar when auto-launch is active.
     * Scans hotbar for ANTI_AIR MissileLauncherItems, checks cooldown and ammo, fires one missile.
     * The missile is aimed toward the fire control target (or upward if no lock).
     */
    public static boolean tryAutoFireAntiAirMissile(Level level, Player player, ItemStack coreStack, int coreSlot) {
        return MissileFireStrategy.tryAutoFireAntiAirMissile(level, player, coreStack, coreSlot);
    }

    /** Spawn a missile aimed toward the fire control target, nearest hostile, or upward. */
    private static void spawnMissileAutoAim(Level level, Player player, ItemStack launcherStack,
                                             MissileLauncherItem launcher, String displayItemId) {
        MissileFireStrategy.spawnMissileAutoAim(level, player, launcherStack, launcher, displayItemId);
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
        WeaponState heldWs = new WeaponState(held);
        SelectedAmmoType selected = heldWs.getSelectedAmmoType();

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.items.size(); i++) {
            ItemStack stack = inv.items.get(i);
            if (stack != held && stack.getItem() == heldType) {
                String ammoId = selected.hasSelection() ? selected.ammoItemId() : null;
                if (ammoId != null) new WeaponState(stack).setSelectedAmmoType(ammoId);
            }
        }
        ItemStack offhand = inv.offhand.get(0);
        if (offhand.getItem() == heldType) {
            String ammoId = selected.hasSelection() ? selected.ammoItemId() : null;
            if (ammoId != null) new WeaponState(offhand).setSelectedAmmoType(ammoId);
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
