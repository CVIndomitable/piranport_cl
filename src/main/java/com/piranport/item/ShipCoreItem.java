package com.piranport.item;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.entity.TorpedoEntity;

import com.piranport.network.ShakeEffectPayload;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import com.piranport.registry.ModSounds;
import net.minecraft.core.NonNullList;
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
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

import com.piranport.client.BallisticSolver;
import com.piranport.PiranPort;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ShipCoreItem extends Item {

    // Phase 12: 口径弹药标签 — 数据包可通过添加物品到这些标签来扩展兼容弹药
    private static final TagKey<Item> SMALL_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "small_shells"));
    private static final TagKey<Item> MEDIUM_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "medium_shells"));
    private static final TagKey<Item> LARGE_SHELLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "large_shells"));

    private final ShipType shipType;

    public ShipCoreItem(Properties properties, ShipType shipType) {
        super(properties);
        this.shipType = shipType;
    }

    public ShipType getShipType() {
        return shipType;
    }

    // ===== Fuel bar (durability-style) =====

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        return fuel != null && fuel.currentFuel() < fuel.maxFuel();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        if (fuel == null || fuel.maxFuel() == 0) return 0;
        return Math.round(13.0f * fuel.currentFuel() / fuel.maxFuel());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FuelData fuel = stack.get(ModDataComponents.SHIP_CORE_FUEL.get());
        if (fuel == null) return 0xFF00FF00;
        float fraction = fuel.getFraction();
        return net.minecraft.util.Mth.hsvToRgb(fraction / 3.0f, 1.0f, 1.0f);
    }

    /**
     * Validate and fix fuel data integrity. Returns corrected FuelData.
     */
    private FuelData validateAndFixFuelData(ItemStack stack) {
        FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, shipType.fuelCapacity));
        if (fuel.maxFuel() < 0 || fuel.currentFuel() < 0) {
            fuel = new FuelData(0, shipType.fuelCapacity);
            stack.set(ModDataComponents.SHIP_CORE_FUEL.get(), fuel);
        }
        return fuel;
    }

    /**
     * Bundle-like armor storage for no-GUI mode.
     * - Right-click an ArmorPlateItem onto the core in inventory → stores the plate inside the core.
     * - Right-click the core with an empty cursor → extracts the last stored plate back to cursor.
     * Capacity = shipType.enhancementSlots (2–4 plates).
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other,
            Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY) return false;

        // Fuel refueling: lava bucket → +1b fuel (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(net.minecraft.world.item.Items.LAVA_BUCKET)) {
            FuelData fuel = validateAndFixFuelData(stack);
            if (!fuel.isFull()) {
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + 1));
                access.set(new ItemStack(net.minecraft.world.item.Items.BUCKET));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            // Full — feedback but don't consume interaction (allow armor install etc.)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // Fuel refueling: coal/charcoal → +1 fuel per item (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && (other.is(net.minecraft.world.item.Items.COAL) || other.is(net.minecraft.world.item.Items.CHARCOAL))) {
            FuelData fuel = validateAndFixFuelData(stack);
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
                if (space <= 0) return false;
                int toAdd = Math.min(space, other.getCount());
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + toAdd));
                other.shrink(toAdd);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // Fuel refueling: blaze rod → +1 fuel per item (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(net.minecraft.world.item.Items.BLAZE_ROD)) {
            FuelData fuel = validateAndFixFuelData(stack);
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
                if (space <= 0) return false;
                int toAdd = Math.min(space, other.getCount());
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + toAdd));
                other.shrink(toAdd);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        // Fuel refueling: fuel item → batch fill (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(ModItems.FUEL.get())) {
            FuelData fuel = validateAndFixFuelData(stack);
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
                if (space <= 0) return false;
                int toAdd = Math.min(space, other.getCount());
                stack.set(ModDataComponents.SHIP_CORE_FUEL.get(),
                        fuel.withCurrentFuel(fuel.currentFuel() + toAdd));
                other.shrink(toAdd);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.PLAYERS, 0.5f, 1.0f);
                return true;
            }
            // Full — feedback but don't consume interaction
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.5f, 1.0f);
            return false;
        }

        int capacity = shipType.enhancementSlots;
        ItemContainerContents existing = stack.getOrDefault(
                ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> stored = NonNullList.withSize(capacity, ItemStack.EMPTY);
        existing.copyInto(stored);

        if (!other.isEmpty()) {
            // Insert: cursor has ArmorPlateItem/SonarItem/EngineItem/TorpedoReloadItem → store in first empty slot
            if (!(other.getItem() instanceof ArmorPlateItem) && !(other.getItem() instanceof SonarItem) && !(other.getItem() instanceof EngineItem) && !(other.getItem() instanceof TorpedoReloadItem)) return false;
            for (int i = 0; i < capacity; i++) {
                if (stored.get(i).isEmpty()) {
                    stored.set(i, other.copyWithCount(1));
                    other.shrink(1);
                    stack.set(ModDataComponents.SHIP_CORE_ARMOR.get(),
                            ItemContainerContents.fromItems(stored));
                    return true;
                }
            }
            // Full — play error sound as feedback
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
            return true;
        } else {
            // Extract: cursor is empty → pop last stored plate back to cursor
            for (int i = capacity - 1; i >= 0; i--) {
                if (!stored.get(i).isEmpty()) {
                    access.set(stored.get(i).copy());
                    stored.set(i, ItemStack.EMPTY);
                    stack.set(ModDataComponents.SHIP_CORE_ARMOR.get(),
                            ItemContainerContents.fromItems(stored));
                    return true;
                }
            }
            return false; // nothing stored
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        // Compute current load + engine bonus so the shift-details block can show
        // real-time current speed alongside empty/full-load speeds.
        int currentTotalLoad = 0;
        double currentEngineSpeedBonus = 0;
        boolean hasCurrentLoad = false;
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            net.minecraft.world.entity.player.Player cp =
                    net.minecraft.client.Minecraft.getInstance().player;
            if (cp != null) {
                currentTotalLoad = com.piranport.combat.TransformationManager
                        .getInventoryWeaponLoad(cp.getInventory())
                        + com.piranport.combat.TransformationManager.getCoreArmorLoad(stack);
                currentEngineSpeedBonus = com.piranport.combat.TransformationManager
                        .getCoreEngineSpeedBonus(stack);
                hasCurrentLoad = true;
            }
        }

        // No-GUI mode: only the offhand core is active.
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                net.minecraft.world.entity.player.Player clientPlayer =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (clientPlayer != null) {
                    Inventory inv = clientPlayer.getInventory();
                    ItemStack offhand = inv.offhand.get(0);
                    boolean isActive = offhand.getItem() instanceof ShipCoreItem
                            && offhand.getItem() == stack.getItem()
                            && ItemStack.isSameItemSameComponents(offhand, stack);
                    if (!isActive) {
                        tooltipComponents.add(Component.translatable("tooltip.piranport.core_inactive")
                                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    }
                    if (!isActive) {
                        tooltipComponents.add(Component.translatable("tooltip.piranport.core_inactive")
                                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    }
                    int weaponLoad = com.piranport.combat.TransformationManager
                            .getInventoryWeaponLoad(inv);
                    int armorLoad = com.piranport.combat.TransformationManager
                            .getCoreArmorLoad(stack);
                    tooltipComponents.add(Component.translatable(
                            "container.piranport.load", weaponLoad + armorLoad, shipType.maxLoad));
                    // Show stored armor plates
                    int capacity = shipType.enhancementSlots;
                    ItemContainerContents armorContents = stack.getOrDefault(
                            ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
                    NonNullList<ItemStack> storedArmor = NonNullList.withSize(capacity, ItemStack.EMPTY);
                    armorContents.copyInto(storedArmor);
                    int armorBonus = com.piranport.combat.TransformationManager.getCoreArmorBonus(stack);
                    int totalArmor = shipType.baseArmor + armorBonus;
                    if (armorBonus > 0) {
                        tooltipComponents.add(Component.translatable(
                                "tooltip.piranport.core_armor_with_bonus", totalArmor, armorBonus, capacity));
                    } else {
                        tooltipComponents.add(Component.translatable(
                                "tooltip.piranport.core_armor_slots", totalArmor, capacity));
                    }
                    for (ItemStack s : storedArmor) {
                        if (!s.isEmpty()) {
                            tooltipComponents.add(Component.literal("  • ").append(s.getHoverName()));
                        }
                    }
                }
            }
        // Fuel tank info (both modes)
        FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                new FuelData(0, shipType.fuelCapacity));
        tooltipComponents.add(Component.translatable(
                "tooltip.piranport.fuel_tank", fuel.currentFuel(), fuel.maxFuel()));
        // Shift: core stats
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                if (shipType.healthBonus != 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.health_bonus",
                            (shipType.healthBonus > 0 ? "+" : "") + shipType.healthBonus)
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.max_load", shipType.maxLoad)
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                int armorBonus = com.piranport.combat.TransformationManager.getCoreArmorBonus(stack);
                int totalArmor = shipType.baseArmor + armorBonus;
                if (armorBonus > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.armor_with_bonus",
                            totalArmor, armorBonus)
                            .withStyle(net.minecraft.ChatFormatting.BLUE));
                } else {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.armor", totalArmor)
                            .withStyle(net.minecraft.ChatFormatting.BLUE));
                }
                if (shipType.armorToughness > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.toughness", shipType.armorToughness)
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.empty_speed",
                        String.format("%.2f", shipType.emptySpeed)).withStyle(net.minecraft.ChatFormatting.GREEN));
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.full_speed",
                        String.format("%.2f", shipType.fullLoadSpeed)).withStyle(net.minecraft.ChatFormatting.YELLOW));
                if (currentEngineSpeedBonus > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.engine.speed_bonus",
                            String.format("%.0f", currentEngineSpeedBonus * 100)).withStyle(net.minecraft.ChatFormatting.GREEN));
                }
                if (hasCurrentLoad) {
                    double loadRatio = shipType.maxLoad > 0
                            ? (double) currentTotalLoad / shipType.maxLoad : 0;
                    double currentSpeed = shipType.emptySpeed
                            - (shipType.emptySpeed - shipType.fullLoadSpeed)
                                    * Math.min(loadRatio, 1.0)
                            + currentEngineSpeedBonus;
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.current_speed",
                                    String.format("%.2f", currentSpeed))
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
            } else {
                tooltipComponents.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isTransformed = TransformationManager.isTransformed(stack);

        if (player.isShiftKeyDown()) {
            // No-GUI mode: transformation is automatic (offhand-driven), manual toggle disabled
            return InteractionResultHolder.pass(stack);
        }

        // Right-click (no shift) → recall all airborne aircraft
        // Only when core is in main hand — in no-GUI mode the core sits in offhand and
        // the offhand use() fires after every weapon use(), which would immediately recall
        // any aircraft that was just launched by the main-hand weapon item.
        if (hand == InteractionHand.MAIN_HAND
                && !level.isClientSide && level instanceof ServerLevel sl) {
            int recalled = recallAllAircraft(sl, player);
            if (recalled > 0) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_recalled", recalled), true);
                return InteractionResultHolder.consume(stack);
            }
        }

        if (isTransformed) {
            // No-GUI mode: weapons fire via their own use(); return pass so offhand may trigger
            return InteractionResultHolder.pass(stack);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // ===== Inventory-mode firing =====

    /**
     * Entry point for weapon items' use() in no-GUI mode.
     * Scans inventory for an active (transformed) ship core, then fires the weapon at the given hand slot.
     * Returns true if firing was attempted (so the weapon can return CONSUME).
     */
    /**
     * 返回 true 表示本次"动作被消费"（走完了 fire 派发流程；各派发分支仍可能因弹药/装填原因失败并
     * 自行提示玩家）。返回 false 表示动作未消费（未找到变身核心 / 被冷却静默阻断），以便调用方返回
     * {@link InteractionResultHolder#pass} 而非 {@code consume}。
     */
    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) return false;
        // Phase 12: 旁观者模式不开火
        if (player.isSpectator()) return false;

        Inventory inv = player.getInventory();
        int weaponSlot = (hand == InteractionHand.MAIN_HAND) ? inv.selected : 40;

        // Find transformed core (skip the weapon's own slot)
        ItemStack coreStack = ItemStack.EMPTY;
        int coreInventorySlot = -1;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                coreStack = s;
                coreInventorySlot = i;
                break;
            }
        }
        if (coreStack.isEmpty() && weaponSlot != 40) {
            ItemStack offhand = inv.offhand.get(0);
            if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) {
                coreStack = offhand;
                coreInventorySlot = 40;
            }
        }
        if (coreStack.isEmpty()) return false;

        return fireWeaponAtSlot(level, player, coreStack, weaponSlot, coreInventorySlot);
    }

    /**
     * Fires the weapon at the given inventory slot using the ship core's ammo pool and cooldowns.
     * 返回 true 表示已派发 fire 分支（动作被消费）；false 表示被冷却阻断，调用方不应 consume 动作。
     */
    private static boolean fireWeaponAtSlot(Level level, Player player, ItemStack coreStack, int weaponSlot, int coreInventorySlot) {
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

        // Cannon — always auto-resupply (Phase 4: manual mode removed)
        int barrelCount = getBarrelCount(weapon);

        // Phase 9: 耐久检测
        if (weapon.isDamageableItem() && weapon.getDamageValue() >= weapon.getMaxDamage() - 1) {
            player.displayClientMessage(Component.translatable("message.piranport.cannon_damaged"), true);
            return true;
        }

        // Creative mode: find any matching ammo without consuming
        if (player.getAbilities().instabuild) {
            Item selectedAmmoType = findPreferredOrSufficientAmmo(inv, weapon, barrelCount,
                    coreInventorySlot, weaponSlot);

            if (selectedAmmoType == null) {
                selectedAmmoType = getDefaultAmmoForWeapon(weapon);
                if (selectedAmmoType == null) {
                    player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
                    return true;
                }
            }

            recordCurrentAmmoType(weapon, selectedAmmoType);

            ItemStack firstAmmo = findFirstAmmoStack(inv, selectedAmmoType, coreInventorySlot, weaponSlot);
            if (firstAmmo.isEmpty()) {
                firstAmmo = new ItemStack(selectedAmmoType);
            }

            boolean isType3 = isType3Shell(firstAmmo);
            boolean isVT = isVTShell(firstAmmo);
            boolean isHE = isHEShell(firstAmmo) || isVT;
            ItemStack shellForRender = firstAmmo.copyWithCount(1);

            int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, level.getGameTime()));
            weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), cooldownTicks));

            fireCannonSalvo(level, player, weapon, shellForRender, barrelCount, isType3, isVT, isHE);

            com.piranport.debug.PiranPortDebug.event(
                    "Fire (creative) | weapon={} ammo={} barrels={}",
                    BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                    BuiltInRegistries.ITEM.getKey(shellForRender.getItem()).getPath(),
                    barrelCount);

            float pitch = getSoundPitch(weapon);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
            return true;
        }

        // Survival mode: find sufficient ammo type and consume
        Item selectedAmmoType = findPreferredOrSufficientAmmo(inv, weapon, barrelCount,
                coreInventorySlot, weaponSlot);

        if (selectedAmmoType == null) {
            player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
            return true;
        }

        recordCurrentAmmoType(weapon, selectedAmmoType);

        // 只统计选定类型的弹药
        int totalAmmo = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreInventorySlot || i == weaponSlot) continue;
            ItemStack ammo = inv.items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() == selectedAmmoType) {
                totalAmmo += ammo.getCount();
            }
        }
        if (weaponSlot != 40 && coreInventorySlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == selectedAmmoType) {
                totalAmmo += oh.getCount();
            }
        }

        ItemStack firstAmmo = findFirstAmmoStack(inv, selectedAmmoType, coreInventorySlot, weaponSlot);

        boolean isType3 = isType3Shell(firstAmmo);
        boolean isVT = isVTShell(firstAmmo);
        boolean isHE = isHEShell(firstAmmo) || isVT;
        ItemStack shellForRender = firstAmmo.copyWithCount(1);

        // 只消耗选定类型的弹药
        int toConsume = barrelCount;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreInventorySlot || i == weaponSlot) continue;
            ItemStack ammo = inv.items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() == selectedAmmoType) {
                int take = Math.min(toConsume, ammo.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(ammo, take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreInventorySlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == selectedAmmoType) {
                int take = Math.min(toConsume, oh.getCount());
                com.piranport.debug.PiranPortDebug.consumeAmmo(oh, take);
                toConsume -= take;
            }
        }

        // Check if enough ammo remains for next salvo
        int remainingAmmo = totalAmmo - barrelCount;
        boolean hasNextRound = remainingAmmo >= barrelCount;

        if (hasNextRound) {
            int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, level.getGameTime()));
            weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), cooldownTicks));
        } else {
            int penaltyTicks = 10;
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
            weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    WeaponCooldown.of(level.getGameTime(), penaltyTicks));
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
        }

        fireCannonSalvo(level, player, weapon, shellForRender, barrelCount, isType3, isVT, isHE);

        com.piranport.debug.PiranPortDebug.event(
                "Fire | weapon={} ammo={} barrels={} remaining={}",
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                BuiltInRegistries.ITEM.getKey(shellForRender.getItem()).getPath(),
                barrelCount, remainingAmmo);

        float pitch = getSoundPitch(weapon);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
        return true;
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
                torpedo.setDamage(torpedoType.getDamage());
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

            int cooldown = launcher.getCooldownTicks();
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
        int cooldown = launcher.getCooldownTicks();
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
                torpedo.setDamage(loadedTorpedo.getDamage());
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
        int cooldown = launcher.getCooldownTicks();

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
        float torpedoSpeed = torpedoType.getSpeed();
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        TorpedoEntity primaryGuided = null;
        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setDamage(torpedoType.getDamage());
            torpedo.setSpeed(torpedoType.getSpeed());
            torpedo.setLifetime(torpedoType.getLifetimeTicks());
            if (magnetic) torpedo.setMagnetic(true);
            if (acousticHoming) torpedo.setAcoustic(true);
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
        int chargeCount = launcher.getChargeCount();
        int cooldown = launcher.getCooldownTicks();

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
                spawnDepthCharge(level, player, horizLook, 0.0, 0.6);
            }
            case FRONT_BACK -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7);   // far
                spawnDepthCharge(level, player, horizLook, 0.0, 0.4);   // near
            }
            case TRIANGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7);   // center far
                Vec3 left = rotateHorizontal(horizLook, Math.toRadians(-20));
                spawnDepthCharge(level, player, left, 0.0, 0.5);
                Vec3 right = rotateHorizontal(horizLook, Math.toRadians(20));
                spawnDepthCharge(level, player, right, 0.0, 0.5);
            }
        }

        // Damage launcher
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
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

    private static void spawnDepthCharge(Level level, Player player, Vec3 dir, double angleOffset, double speed) {
        DepthChargeEntity dc = new DepthChargeEntity(level, player, 14f, 3.0f);
        dc.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
        dc.setDeltaMovement(dir.x * speed, 0.3, dir.z * speed);
        level.addFreshEntity(dc);
    }

    // ===== Missile firing (no-GUI mode) =====

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

            spawnMissile(level, player, launcher, ammoId);
    
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
        spawnMissile(level, player, launcher, ammoId);

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
        spawnMissile(level, player, launcher, ammoId);

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
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        if (nextAvailable > 0) {
            int cd = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
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
    private static void spawnMissile(Level level, Player player,
                                      MissileLauncherItem launcher, String displayItemId) {
        spawnMissileWithDir(level, player, launcher, displayItemId, player.getLookAngle());
    }

    /**
     * 通用导弹生成：在玩家眼睛 + dir*0.5 处生成导弹，以 dir 方向按 initialSpeed 射出。
     * dir 预期为单位向量；非单位向量将被规整化。
     */
    private static void spawnMissileWithDir(Level level, Player player,
                                             MissileLauncherItem launcher, String displayItemId,
                                             Vec3 dir) {
        Vec3 d = dir.lengthSqr() > 1e-6 ? dir.normalize() : player.getLookAngle();
        MissileEntity missile = new MissileEntity(level, launcher.getMissileType(),
                launcher.getDamage(), launcher.getArmorPen(),
                launcher.getExplosionPower(), displayItemId);
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

        // In no-GUI mode, use FlightGroupData if present (indexed by inventory slot).
        // Since group slotIndices map to inventory slot numbers in this mode, look them up.
        FlightGroupData groupData = coreStack.getOrDefault(
                ModDataComponents.FLIGHT_GROUP_DATA.get(), FlightGroupData.empty());
        FlightGroupData.AttackMode attackMode = FlightGroupData.AttackMode.FOCUS;
        boolean hasBullets = true; // default: fighters use bullets
        String payloadType = "";
        for (FlightGroupData.FlightGroup group : groupData.groups()) {
            if (group.slotIndices().contains(weaponSlot)) {
                attackMode = group.attackMode();
                hasBullets = group.getSlotBullets(weaponSlot);
                payloadType = group.getSlotPayload(weaponSlot);
                break;
            }
        }

        // Apply default payload for bombers when no flight group configured
        if (payloadType.isEmpty() && launchInfo != null) {
            switch (launchInfo.aircraftType()) {
                case TORPEDO_BOMBER -> { payloadType = "piranport:aerial_torpedo"; hasBullets = false; }
                case DIVE_BOMBER, LEVEL_BOMBER -> { payloadType = "piranport:aerial_bomb"; hasBullets = false; }
                case ASW -> { payloadType = "piranport:depth_charge"; hasBullets = false; }
                default -> { }
            }
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
                    return;
                }
            }
        }

        AircraftEntity aircraft = AircraftEntity.create(level, player, weaponSlot, aircraftStack,
                attackMode, coreInventorySlot, hasBullets, payloadType);
        level.addFreshEntity(aircraft);
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

    // ===== Weapon cooldown tooltip (shared by all weapon item types) =====

    /**
     * Client-only helper. Appends "已装填" or "装填中: Xs" to a weapon item's tooltip,
     * based on the player's active ship core SlotCooldowns.
     * Only shows when the player is transformed and the weapon is physically in their inventory.
     */
    public static void appendWeaponCooldownTooltip(ItemStack stack, List<Component> tooltip) {
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        Inventory inv = mc.player.getInventory();

        // Find the active (transformed) ship core
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

        // Find weapon slot by object identity (only works when item is directly in player's inventory)
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
        // Auto-reload missiles (ANTI_AIR) consume ammo directly from inventory, never use LoadedAmmo
        boolean isAutoReloadMissile = stack.getItem() instanceof MissileLauncherItem ml0 && !ml0.isManualReload();
        // Manual-reload missiles (ROCKET/ANTI_SHIP) always need LoadedAmmo regardless of config
        boolean needsLoadedAmmo = !isAutoReloadMissile
                && ((isManualMode && !(stack.getItem() instanceof AircraftItem))
                    || (stack.getItem() instanceof MissileLauncherItem ml && ml.isManualReload()));

        if (onCooldown) {
            // Weapon on cooldown
            if (needsLoadedAmmo) {
                LoadedAmmo reloading = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
                if (reloading.hasAmmo()) {
                    tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
            } else {
                // Auto-reload weapon on cooldown = actively reloading
                tooltip.add(Component.translatable("tooltip.piranport.weapon_reloading")
                        .withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        } else if (needsLoadedAmmo) {
            // Manual mode or manual-reload missile: require LOADED_AMMO to be present
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            boolean hasAmmo;
            if (stack.getItem() instanceof TorpedoLauncherItem tl) {
                hasAmmo = loaded.count() >= tl.getTubeCount();
            } else if (stack.getItem() instanceof CannonItem ci) {
                hasAmmo = loaded.count() >= ci.getBarrelCount();
            } else if (stack.getItem() instanceof com.piranport.artillery.ArtilleryItem ai) {
                hasAmmo = loaded.count() >= ai.getBarrelCount();
            } else {
                hasAmmo = loaded.hasAmmo();
            }
            if (hasAmmo) {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        } else if (isAutoReloadMissile) {
            // Auto-reload missile: check inventory for matching ammo
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
                        .withStyle(net.minecraft.ChatFormatting.GREEN));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }

    // ===== Caliber matching =====
    // Phase 12: 已迁移至 piranport:small_shells / medium_shells / large_shells 物品标签

    /**
     * 查找背包中第一个数量足够的弹药类型
     * @return 数量足够的弹药类型，如果没有则返回 null
     */
    private static Item findSufficientAmmoType(Inventory inv, ItemStack weapon,
                                                int required, int coreSlot, int weaponSlot) {
        // 收集所有匹配口径的弹药类型
        Set<Item> candidateTypes = new HashSet<>();
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack ammo = inv.items.get(i);
            if (!ammo.isEmpty() && matchesCaliber(ammo, weapon)) {
                candidateTypes.add(ammo.getItem());
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && matchesCaliber(oh, weapon)) {
                candidateTypes.add(oh.getItem());
            }
        }

        // 对每种类型统计数量，返回第一个数量足够的
        for (Item candidateType : candidateTypes) {
            int count = 0;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreSlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.getItem() == candidateType) {
                    count += s.getCount();
                }
            }
            if (weaponSlot != 40 && coreSlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && oh.getItem() == candidateType) {
                    count += oh.getCount();
                }
            }

            if (count >= required) {
                return candidateType; // 找到第一个数量足够的类型
            }
        }

        return null; // 没有类型满足数量要求
    }

    /** Phase 12: 用物品标签匹配口径，替代硬编码物品列表。数据包可向标签添加物品来扩展。 */
    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get()) || weapon.is(ModItems.SINGLE_SMALL_GUN.get())) {
            return ammo.is(SMALL_SHELLS);
        } else if (weapon.is(ModItems.MEDIUM_GUN.get())) {
            return ammo.is(MEDIUM_SHELLS);
        } else if (weapon.is(ModItems.LARGE_GUN.get())) {
            return ammo.is(LARGE_SHELLS);
        }
        return false;
    }

    /**
     * 根据 SelectedAmmoType 查找偏好弹种（优先），
     * 偏好弹种不足时退回到 findSufficientAmmoType。
     */
    private static Item findPreferredOrSufficientAmmo(Inventory inv, ItemStack weapon,
                                                       int required, int coreSlot, int weaponSlot) {
        SelectedAmmoType preferred = weapon.getOrDefault(
                ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        if (preferred.hasSelection()) {
            ResourceLocation rl = ResourceLocation.tryParse(preferred.ammoItemId());
            if (rl != null) {
                Item preferredItem = BuiltInRegistries.ITEM.get(rl);
                if (preferredItem != null && preferredItem != net.minecraft.world.item.Items.AIR) {
                    // 统计偏好弹种数量
                    int count = 0;
                    for (int i = 0; i < inv.items.size(); i++) {
                        if (i == coreSlot || i == weaponSlot) continue;
                        ItemStack s = inv.items.get(i);
                        if (!s.isEmpty() && s.getItem() == preferredItem) {
                            count += s.getCount();
                        }
                    }
                    if (weaponSlot != 40 && coreSlot != 40) {
                        ItemStack oh = inv.offhand.get(0);
                        if (!oh.isEmpty() && oh.getItem() == preferredItem) {
                            count += oh.getCount();
                        }
                    }
                    if (count >= required) {
                        return preferredItem;
                    }
                }
            }
        }
        return findSufficientAmmoType(inv, weapon, required, coreSlot, weaponSlot);
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
                || stack.is(ModItems.LARGE_AP_SHELL.get());
    }

    static boolean isAPShell(String ammoItemId) {
        return ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.SMALL_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.MEDIUM_AP_SHELL.get()).toString())
                || ammoItemId.equals(BuiltInRegistries.ITEM.getKey(ModItems.LARGE_AP_SHELL.get()).toString());
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
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) return ai.getDamage();
        if (item instanceof CannonItem ci) return ci.getDamage();
        return 6f;
    }

    private static int getGunCooldown(ItemStack weapon) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) return ai.getCooldownTicks();
        if (item instanceof CannonItem ci) return ci.getCooldownTicks();
        return 30;
    }

    private static int getBarrelCount(ItemStack weapon) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) return ai.getBarrelCount();
        if (item instanceof CannonItem ci) return ci.getBarrelCount();
        return 1;
    }

    private static boolean isSmallCaliber(ItemStack weapon) {
        return weapon.is(ModItems.SMALL_GUN.get()) || weapon.is(ModItems.SINGLE_SMALL_GUN.get());
    }

    private static float getExplosionPower(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return 1.0f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.5f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 2.0f;
        return 1.0f;
    }

    private static float getProjectileVelocity(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return 2.0f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 2.5f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 3.0f;
        return 2.0f;
    }

    private static float getProjectileInaccuracy(ItemStack weapon) {
        if (isSmallCaliber(weapon)) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.0f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.5f;
        return 1.0f;
    }

    /** Phase 2: 从武器数据获取阻力系数。 */
    private static float getProjectileDrag(ItemStack weapon) {
        Item item = weapon.getItem();
        if (item instanceof com.piranport.artillery.ArtilleryItem ai) return ai.getDragCoeff();
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

    /** Phase 5: 弹道解算瞄准目标位，由 fireFromScope→tryFireFromInventory→fireCannonSalvo 传递。 */
    private static final ThreadLocal<Vec3> pendingAimTarget = new ThreadLocal<>();

    /** Phase 5: 从瞄准镜模式开火（由 ScopeFirePayload 调用）。复用 tryFireFromInventory 的弹药/冷却逻辑。 */
    public static void fireFromScope(Player player, ItemStack weapon, double tx, double ty, double tz) {
        Vec3 target = new Vec3(tx, ty, tz);
        pendingAimTarget.set(target);
        try {
            tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND);
        } finally {
            pendingAimTarget.remove();
        }
    }

    /** Fire a cannon salvo: barrelCount projectiles with natural inaccuracy spread. */
    private static void fireCannonSalvo(Level level, Player player, ItemStack weapon,
            ItemStack shellForRender, int barrelCount, boolean isType3, boolean isVT, boolean isHE) {
        // Phase 11: 全局炮弹上限检测
        if (isShellLimitReached(level)) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.max_projectiles"), true);
            return;
        }
        // Phase 9: 消耗耐久（创造模式不消耗）
        if (!player.getAbilities().instabuild && weapon.isDamageableItem()) {
            int curDamage = weapon.getDamageValue();
            if (curDamage < weapon.getMaxDamage() - 1) {
                weapon.setDamageValue(curDamage + 1);
            }
        }
        Vec3 aimTarget = pendingAimTarget.get(); // Phase 5: 非 null 时使用弹道解算
        for (int b = 0; b < barrelCount; b++) {
            if (isType3) {
                fireSanshikiSpread(level, player, weapon, shellForRender);
            } else {
                float damage = getGunDamage(weapon);
                float explosionPower = getExplosionPower(weapon);
                float velocity = getProjectileVelocity(weapon);
                float inaccuracy = getProjectileInaccuracy(weapon);

                CannonProjectileEntity projectile = new CannonProjectileEntity(
                        level, player, shellForRender, damage, isHE, explosionPower);
                if (isVT) projectile.setVT(true);
                projectile.setDragCoeff(getProjectileDrag(weapon));

                if (aimTarget != null && b == 0) {
                    // Phase 5: 弹道解算瞄准（仅第一发使用精确角度，后续仍用散布，保持齐射散布自然）
                    fireProjectileWithAim(projectile, player, weapon, velocity, aimTarget);
                } else {
                    projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
                }
                level.addFreshEntity(projectile);
            }

            // Phase 10: 只在第一个炮管播放音效 + 粒子 + 震动（避免齐射重复）
            if (b == 0) {
                // 发射音效
                SoundEvent fireSound = getFireSound(weapon);
                float pitch = getSoundPitch(weapon);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        fireSound, SoundSource.PLAYERS, 2.0f, pitch);

                // 炮口火焰粒子（服务端广播）
                if (level instanceof ServerLevel serverLevel) {
                    Vec3 look = player.getLookAngle();
                    double px = player.getX() + look.x * 1.5;
                    double py = player.getY() + player.getEyeHeight() + look.y * 0.5;
                    double pz = player.getZ() + look.z * 1.5;
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 8,
                            0.3, 0.3, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.CLOUD, px, py, pz, 6,
                            0.4, 0.2, 0.4, 0.01);
                    serverLevel.sendParticles(ParticleTypes.LAVA, px, py, pz, 2,
                            0.2, 0.2, 0.2, 0);
                }

                // 发射屏幕震动（S2C）
                float shakeIntensity = isSmallCaliber(weapon) ? 0.3f : 0.6f;
                if (player instanceof ServerPlayer serverPlayer) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                            new ShakeEffectPayload(shakeIntensity, 6));
                }
            }
        }
    }

    /** Phase 5: 弹道解算瞄准发射。计算从炮口到目标的发射仰角，使用 shootFromRotation 设置速度。 */
    private static void fireProjectileWithAim(CannonProjectileEntity projectile, Player player,
                                               ItemStack weapon, float velocity, Vec3 aimTarget) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 muzzlePos = eyePos.add(lookDir.scale(1.5));

        Vec3 toTarget = aimTarget.subtract(muzzlePos);
        double horizontalDist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        double verticalDist = toTarget.y;

        if (horizontalDist < 1.0) {
            // 近距目标：直接朝准星方向发射
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 0);
            return;
        }

        // 弹道解算最佳仰角
        float drag = getProjectileDrag(weapon);
        double optimalPitch = BallisticSolver.solve(velocity, drag, BallisticSolver.DEFAULT_GRAVITY,
                horizontalDist, verticalDist);

        // MC 偏航角：atan2(-dx, dz) 映射到 MC 坐标（yaw=0 = +Z）
        float yawDeg = (float) Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z));
        float pitchDeg = (float) Math.toDegrees(optimalPitch);

        // shootFromRotation 会自动计算速度向量并记录 initialSpeed
        projectile.shootFromRotation(player, -pitchDeg, yawDeg, 0.0f, velocity, 0);
    }

    // ===== Global projectile limit (Phase 11) =====

    /** 检查全局炮弹是否已达上限（CannonProjectileEntity 合计）。 */
    private static boolean isShellLimitReached(Level level) {
        int maxProjectiles = ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES.get();
        if (maxProjectiles <= 0) return false;
        int count = level.getEntitiesOfClass(
                CannonProjectileEntity.class,
                new net.minecraft.world.phys.AABB(-3e7, -64, -3e7, 3e7, 320, 3e7)).size();
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

    private static void fireSanshikiSpread(Level level, Player player, ItemStack weapon, ItemStack shellForRender) {
        float baseDamage = getGunDamage(weapon);
        float pelletDamage = baseDamage * 0.25f;
        float velocity = getProjectileVelocity(weapon);

        // 霰弹数量限制：统计玩家附近256格内的现有霰弹数，超过上限则不发射
        int limit = getShrapnelLimit();
        if (limit <= 0) return;
        int existing = level.getEntitiesOfClass(
                SanshikiPelletEntity.class,
                new AABB(player.blockPosition()).inflate(256))
                .size();
        int canSpawn = limit - existing;
        if (canSpawn <= 0) return;
        int pelletCount = Math.min(limit, canSpawn);

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
            pellet.shootFromRotation(player,
                    basePitch + pitchOffset,
                    baseYaw + yawOffset,
                    0.0f, velocity, 0.5f);
            level.addFreshEntity(pellet);
        }
    }

    private static float[] getSpreadAngles(int count) {
        return switch (count) {
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> new float[]{0f};
        };
    }

    // ===== Fuel refill =====

    /**
     * Recall all airborne aircraft owned by this player. Returns the count recalled.
     */
    private static int recallAllAircraft(ServerLevel level, Player player) {
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
     * Applies FlammableEffect if any aircraft ends up with fuel > 0.
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
        boolean hasFueled = false;

        for (ItemStack weapon : inv.items) {
            if (!(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() >= info.fuelCapacity()) {
                if (info != null && info.currentFuel() > 0) hasFueled = true;
                continue;
            }
            // Find aviation_fuel in inventory
            for (ItemStack ammo : inv.items) {
                if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                    com.piranport.debug.PiranPortDebug.consumeAmmo(ammo, 1);
                    weapon.set(ModDataComponents.AIRCRAFT_INFO.get(),
                            info.withCurrentFuel(info.fuelCapacity()));
                    hasFueled = true;
                    break;
                }
            }
        }

        if (hasFueled && com.piranport.config.ModCommonConfig.isFlammableEffectActive()) {
            player.addEffect(new MobEffectInstance(ModMobEffects.FLAMMABLE, 999999, 0, false, true));
        }
    }

    // ===== Auto-launch (Phase 36) =====

    /**
     * Server-side: refuel + launch the first available FIGHTER in weapon slots.
     * Called by the auto-launch tick when phantoms are detected nearby.
     * Returns true if a fighter was launched.
     */
    public static boolean tryAutoLaunchFighter(Level level, Player player, ItemStack coreStack, int coreSlot) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return false;
        if (level.isClientSide()) return false;

        // Refuel aircraft before checking (consumes aviation_fuel from ammo slots)
        refillAircraftFuel(player, coreStack);

        // Re-read contents after refuel
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        int totalSlots = sci.getShipType().totalSlots();
        NonNullList<ItemStack> items = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
        contents.copyInto(items);

        FlightGroupData groupData = coreStack.getOrDefault(
                ModDataComponents.FLIGHT_GROUP_DATA.get(), FlightGroupData.empty());

        for (int wi = 0; wi < sci.getShipType().weaponSlots; wi++) {
            ItemStack weapon = items.get(wi);
            if (weapon.isEmpty() || !(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() <= 0) continue;

            // 战斗机/火箭机默认有子弹（与手动发射一致）；非战斗机默认无子弹
            boolean isFighter = info.aircraftType() == AircraftInfo.AircraftType.FIGHTER
                    || info.aircraftType() == AircraftInfo.AircraftType.ROCKET_FIGHTER;
            boolean hasBullets = isFighter; // default: fighters use bullets
            for (FlightGroupData.FlightGroup group : groupData.groups()) {
                if (group.slotIndices().contains(wi)) {
                    hasBullets = group.getSlotBullets(wi);
                    break;
                }
            }
            if (!hasBullets) continue;

            // TODO: rewrite auto-launch for no-GUI mode
            return false;
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
            spawnMissileAutoAim(level, player, launcher, ammoId);

            // Apply cooldown
            int cd = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
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
    private static void spawnMissileAutoAim(Level level, Player player,
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

        spawnMissileWithDir(level, player, launcher, displayItemId, aimDir);
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
        Item weaponItem = weapon.getItem();
        if (weaponItem == ModItems.SMALL_GUN.get() || weaponItem == ModItems.SINGLE_SMALL_GUN.get()) {
            return ModItems.SMALL_AP_SHELL.get();
        } else if (weaponItem == ModItems.MEDIUM_GUN.get()) {
            return ModItems.MEDIUM_AP_SHELL.get();
        } else if (weaponItem == ModItems.LARGE_GUN.get()) {
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
}
