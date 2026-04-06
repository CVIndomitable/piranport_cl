package com.piranport.item;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.component.FuelData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.SanshikiPelletEntity;
import com.piranport.entity.DepthChargeEntity;
import com.piranport.entity.MissileEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.menu.ShipCoreMenu;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.AABB;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class ShipCoreItem extends Item {

    public enum ShipType {
        //                  hp   cost  wpn ammo enh fuel dist  满载  空载  护甲 韧性
        SMALL(  0,  40, 4, 4, 2, 10, 100.0, 1.15, 1.4,   8,  4),
        MEDIUM(10,  64, 5, 4, 3, 20,  70.0, 1.0,  1.2,  12,  8),
        LARGE( 20, 112, 6, 4, 4, 30,  50.0, 0.85, 1.0,  16, 12),
        SUBMARINE(-8, 32, 4, 4, 2, 10, 100.0, 0.7, 0.8,  4,  0);

        public final int healthBonus;
        public final int maxLoad;
        public final int weaponSlots;
        public final int ammoSlots;
        public final int enhancementSlots;
        /** Fuel tank capacity in b (bucket) units. */
        public final int fuelCapacity;
        /** Distance in blocks per 1b fuel consumed. */
        public final double distancePerFuel;
        /** Speed multiplier at full load (totalLoad == maxLoad). */
        public final double fullLoadSpeed;
        /** Speed multiplier at zero load. */
        public final double emptySpeed;
        /** Base armor provided by the core itself. */
        public final int baseArmor;
        /** Armor toughness provided by the core. */
        public final int armorToughness;

        ShipType(int healthBonus, int maxLoad, int weaponSlots, int ammoSlots, int enhancementSlots,
                 int fuelCapacity, double distancePerFuel,
                 double fullLoadSpeed, double emptySpeed, int baseArmor, int armorToughness) {
            this.healthBonus = healthBonus;
            this.maxLoad = maxLoad;
            this.weaponSlots = weaponSlots;
            this.ammoSlots = ammoSlots;
            this.enhancementSlots = enhancementSlots;
            this.fuelCapacity = fuelCapacity;
            this.distancePerFuel = distancePerFuel;
            this.fullLoadSpeed = fullLoadSpeed;
            this.emptySpeed = emptySpeed;
            this.baseArmor = baseArmor;
            this.armorToughness = armorToughness;
        }

        public int totalSlots() {
            return weaponSlots + ammoSlots + enhancementSlots;
        }
    }

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
            FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                    new FuelData(0, shipType.fuelCapacity));
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

        // Fuel refueling: coal → +1 fuel per item (works in both GUI and no-GUI modes)
        if (!other.isEmpty() && other.is(net.minecraft.world.item.Items.COAL)) {
            FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                    new FuelData(0, shipType.fuelCapacity));
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
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
            FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                    new FuelData(0, shipType.fuelCapacity));
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
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
            FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                    new FuelData(0, shipType.fuelCapacity));
            if (!fuel.isFull()) {
                int space = fuel.maxFuel() - fuel.currentFuel();
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

        if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) return false;

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
        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            // No-GUI mode: find the best (highest maxLoad) core in inventory.
            // If this core is the best (effective) one, show load info; otherwise show "不生效".
            // Safe: FMLEnvironment.dist check prevents Minecraft.getInstance() from loading on server
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                net.minecraft.world.entity.player.Player clientPlayer =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (clientPlayer != null) {
                    Inventory inv = clientPlayer.getInventory();
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
                                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                    }
                    int weaponLoad = com.piranport.combat.TransformationManager
                            .getInventoryWeaponLoad(inv);
                    int armorLoad = com.piranport.combat.TransformationManager
                            .getCoreArmorLoad(stack);
                    tooltipComponents.add(Component.translatable(
                            "container.piranport.load", weaponLoad + armorLoad,
                            isActive ? bestMaxLoad : shipType.maxLoad));
                    // Show stored armor plates
                    int capacity = shipType.enhancementSlots;
                    ItemContainerContents armorContents = stack.getOrDefault(
                            ModDataComponents.SHIP_CORE_ARMOR.get(), ItemContainerContents.EMPTY);
                    NonNullList<ItemStack> storedArmor = NonNullList.withSize(capacity, ItemStack.EMPTY);
                    armorContents.copyInto(storedArmor);
                    int armorBonus = com.piranport.combat.TransformationManager.getCoreArmorBonus(stack);
                    tooltipComponents.add(Component.translatable(
                            "tooltip.piranport.core_armor_slots", armorBonus, capacity));
                    for (ItemStack s : storedArmor) {
                        if (!s.isEmpty()) {
                            tooltipComponents.add(Component.literal("  • ").append(s.getHoverName()));
                        }
                    }
                }
            }
        } else {
            // GUI mode: show load from this core's equipped container slots.
            ItemContainerContents contents = stack.getOrDefault(
                    ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
            NonNullList<ItemStack> items = NonNullList.withSize(shipType.totalSlots(), ItemStack.EMPTY);
            contents.copyInto(items);
            int totalLoad = 0;
            for (int i = 0; i < shipType.weaponSlots; i++) {
                totalLoad += com.piranport.combat.TransformationManager.getItemLoad(items.get(i));
            }
            int eStart = shipType.weaponSlots + shipType.ammoSlots;
            for (int i = eStart; i < shipType.totalSlots(); i++) {
                totalLoad += com.piranport.combat.TransformationManager.getItemLoad(items.get(i));
            }
            tooltipComponents.add(Component.translatable(
                    "container.piranport.load", totalLoad, shipType.maxLoad));
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
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.armor", shipType.baseArmor)
                        .withStyle(net.minecraft.ChatFormatting.BLUE));
                if (shipType.armorToughness > 0) {
                    tooltipComponents.add(Component.translatable("tooltip.piranport.core.toughness", shipType.armorToughness)
                            .withStyle(net.minecraft.ChatFormatting.AQUA));
                }
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.empty_speed",
                        String.format("%.2f", shipType.emptySpeed)).withStyle(net.minecraft.ChatFormatting.GREEN));
                tooltipComponents.add(Component.translatable("tooltip.piranport.core.full_speed",
                        String.format("%.2f", shipType.fullLoadSpeed)).withStyle(net.minecraft.ChatFormatting.YELLOW));
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
            if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
                return InteractionResultHolder.pass(stack);
            }
            // Toggle transformation
            if (!level.isClientSide) {
                if (!isTransformed) {
                    // Check fuel before transforming
                    FuelData fuel = stack.getOrDefault(ModDataComponents.SHIP_CORE_FUEL.get(),
                            new FuelData(0, shipType.fuelCapacity));
                    if (fuel.isEmpty()) {
                        player.displayClientMessage(
                                Component.translatable("message.piranport.no_fuel"), true);
                        return InteractionResultHolder.fail(stack);
                    }
                }
                TransformationManager.setTransformed(stack, !isTransformed);
                if (!isTransformed) {
                    TransformationManager.applyTransformationAttributes(player, stack);
                    refillAircraftFuel(player, stack);
                    player.displayClientMessage(
                            Component.translatable("message.piranport.transformed"), true);
                    int _load = com.piranport.combat.TransformationManager.getInventoryWeaponLoad(player.getInventory());
                    com.piranport.debug.PiranPortDebug.event(
                            "Transform ON | player={} core={} weight={}/{}",
                            player.getName().getString(),
                            BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(),
                            _load, shipType.maxLoad);
                } else {
                    TransformationManager.removeTransformationAttributes(player);
                    TransformationManager.removeOverweightPenalty(player);
                    player.removeEffect(ModMobEffects.FLAMMABLE);
                    player.removeEffect(net.minecraft.world.effect.MobEffects.WATER_BREATHING);
                    player.displayClientMessage(
                            Component.translatable("message.piranport.untransformed"), true);
                    com.piranport.debug.PiranPortDebug.event(
                            "Transform OFF | player={}", player.getName().getString());
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        // Right-click (no shift) while transformed → recall all airborne aircraft
        // Only when core is in main hand — in no-GUI mode the core sits in offhand and
        // the offhand use() fires after every weapon use(), which would immediately recall
        // any aircraft that was just launched by the main-hand weapon item.
        if (isTransformed && hand == InteractionHand.MAIN_HAND
                && !level.isClientSide && level instanceof ServerLevel sl) {
            int recalled = recallAllAircraft(sl, player);
            if (recalled > 0) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.aircraft_recalled", recalled), true);
                return InteractionResultHolder.consume(stack);
            }
        }

        if (isTransformed) {
            if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
                // GUI mode: core in hand fires the selected weapon slot
                if (!level.isClientSide) {
                    int coreSlot = hand == InteractionHand.MAIN_HAND
                            ? player.getInventory().selected
                            : 40;
                    fireCurrentWeapon(level, player, stack, coreSlot);
                }
                return InteractionResultHolder.consume(stack);
            }
            // No-GUI mode: weapons fire via their own use(); return pass so offhand may trigger
            return InteractionResultHolder.pass(stack);
        }

        // Open GUI (guarded by config)
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            int slot = hand == InteractionHand.MAIN_HAND
                    ? player.getInventory().selected
                    : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, playerInv, p) -> new ShipCoreMenu(containerId, playerInv, slot),
                            stack.getHoverName()
                    ),
                    buf -> buf.writeVarInt(slot)
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void fireCurrentWeapon(Level level, Player player, ItemStack stack, int coreInventorySlot) {
        // GUI mode only; no-GUI mode fires via weapon items' own use() → tryFireFromInventory()
        ItemContainerContents contents = stack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        int totalSlots = shipType.totalSlots();
        NonNullList<ItemStack> items = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
        contents.copyInto(items);

        // Find weapon at current index
        int weaponIndex = TransformationManager.getWeaponIndex(stack);
        if (weaponIndex >= shipType.weaponSlots) weaponIndex = 0;

        // If current slot is empty, find next available weapon
        if (items.get(weaponIndex).isEmpty()) {
            boolean found = false;
            for (int i = 0; i < shipType.weaponSlots; i++) {
                if (!items.get(i).isEmpty()) {
                    weaponIndex = i;
                    found = true;
                    break;
                }
            }
            if (!found) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.no_weapon"), true);
                return;
            }
        }

        ItemStack weapon = items.get(weaponIndex);

        // Per-slot cooldown gate
        SlotCooldowns cooldowns = stack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        if (cooldowns.isOnCooldown(weaponIndex, level.getGameTime())) return;

        // Handle torpedo launcher
        if (weapon.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            fireTorpedos(level, player, stack, items, weaponIndex, torpedoLauncher);
            return;
        }

        // Handle depth charge launcher
        if (weapon.getItem() instanceof DepthChargeLauncherItem dcLauncher) {
            fireDepthChargesGui(level, player, stack, items, weaponIndex, dcLauncher);
            return;
        }

        // Handle missile launcher
        if (weapon.getItem() instanceof MissileLauncherItem missileLauncher) {
            fireMissileGui(level, player, stack, items, weaponIndex, missileLauncher, cooldowns);
            return;
        }

        // Handle aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            launchAircraft(level, player, stack, items, weaponIndex, coreInventorySlot);
            return;
        }

        // Find matching ammo
        int ammoSlot = -1;
        int ammoEnd = shipType.weaponSlots + shipType.ammoSlots;
        for (int i = shipType.weaponSlots; i < ammoEnd; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && matchesCaliber(ammo, weapon)) {
                ammoSlot = i;
                break;
            }
        }

        if (ammoSlot == -1) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        ItemStack ammoStack = items.get(ammoSlot);
        boolean isType3 = isType3Shell(ammoStack);
        boolean isVT = isVTShell(ammoStack);
        boolean isHE = isHEShell(ammoStack) || isVT;
        ItemStack shellForRender = ammoStack.copyWithCount(1);

        // Consume ammo
        ammoStack.shrink(1);

        // Cooldown (reduced by ReloadBoostEffect if active)
        int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));

        // Save updated contents + slot cooldown
        stack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        stack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponIndex, cooldownTicks, level.getGameTime()));
        TransformationManager.setWeaponIndex(stack, weaponIndex);

        if (isType3) {
            fireSanshikiSpread(level, player, weapon, shellForRender);
        } else {
            // Create and launch projectile
            float damage = getGunDamage(weapon);
            float explosionPower = getExplosionPower(weapon);
            float velocity = getProjectileVelocity(weapon);
            float inaccuracy = getProjectileInaccuracy(weapon);

            CannonProjectileEntity projectile = new CannonProjectileEntity(
                    level, player, shellForRender, damage, isHE, explosionPower);
            if (isVT) projectile.setVT(true);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                    0.0f, velocity, inaccuracy);
            level.addFreshEntity(projectile);
        }

        com.piranport.debug.PiranPortDebug.event(
                "Fire | weapon={} ammo={} remaining={}",
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                BuiltInRegistries.ITEM.getKey(shellForRender.getItem()).getPath(),
                ammoStack.getCount());

        // Sound
        float pitch = getSoundPitch(weapon);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
    }

    // ===== Inventory-mode firing =====

    /**
     * Entry point for weapon items' use() in no-GUI mode.
     * Scans inventory for an active (transformed) ship core, then fires the weapon at the given hand slot.
     * Returns true if firing was attempted (so the weapon can return CONSUME).
     */
    public static boolean tryFireFromInventory(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide || com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) return false;

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

        fireWeaponAtSlot(level, player, coreStack, weaponSlot, coreInventorySlot);
        return true;
    }

    /** Fires the weapon at the given inventory slot using the ship core's ammo pool and cooldowns. */
    private static void fireWeaponAtSlot(Level level, Player player, ItemStack coreStack, int weaponSlot, int coreInventorySlot) {
        Inventory inv = player.getInventory();
        ItemStack weapon = (weaponSlot == 40) ? inv.offhand.get(0) : inv.items.get(weaponSlot);

        // Per-slot cooldown (keyed by inventory slot index)
        SlotCooldowns cooldowns = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        if (cooldowns.isOnCooldown(weaponSlot, level.getGameTime())) return;

        // Torpedo launcher — requires 鱼雷再装填 enhancement for auto-reload
        if (weapon.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            if (TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
                fireTorpedosInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, torpedoLauncher, cooldowns);
            } else {
                fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, torpedoLauncher, cooldowns);
            }
            return;
        }

        // Depth charge launcher
        if (weapon.getItem() instanceof DepthChargeLauncherItem dcLauncher) {
            fireDepthCharges(level, player, coreStack, inv, weaponSlot, coreInventorySlot, dcLauncher, cooldowns);
            return;
        }

        // Missile launcher
        if (weapon.getItem() instanceof MissileLauncherItem missileLauncher) {
            fireMissiles(level, player, coreStack, inv, weaponSlot, coreInventorySlot, missileLauncher, cooldowns);
            return;
        }

        // Aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            launchAircraftInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, cooldowns);
            return;
        }

        // Cannon
        // Small gun always auto-reloads from inventory in no-GUI mode
        boolean cannonAutoMode = com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()
                || weapon.is(ModItems.SMALL_GUN.get());
        if (!cannonAutoMode) {
            // Manual mode: use LOADED_AMMO component on weapon item
            fireCannonManualMode(level, player, coreStack, weapon, weaponSlot, cooldowns);
        } else {
            // Auto mode (or small gun): find matching ammo anywhere in inventory
            int ammoSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreInventorySlot || i == weaponSlot) continue;
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && matchesCaliber(ammo, weapon)) {
                    ammoSlot = i;
                    break;
                }
            }
            if (ammoSlot == -1 && weaponSlot != 40 && coreInventorySlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && matchesCaliber(oh, weapon)) {
                    ammoSlot = 40;
                }
            }

            if (ammoSlot == -1) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            ItemStack ammoStack = ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot);
            boolean isType3 = isType3Shell(ammoStack);
            boolean isVT = isVTShell(ammoStack);
            boolean isHE = isHEShell(ammoStack) || isVT;
            ItemStack shellForRender = ammoStack.copyWithCount(1);
            ammoStack.shrink(1);

            // Check if more ammo remains for next reload
            boolean hasNextRound = false;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreInventorySlot || i == weaponSlot) continue;
                ItemStack check = inv.items.get(i);
                if (!check.isEmpty() && matchesCaliber(check, weapon)) {
                    hasNextRound = true;
                    break;
                }
            }
            if (!hasNextRound && weaponSlot != 40 && coreInventorySlot != 40) {
                ItemStack oh = inv.offhand.get(0);
                if (!oh.isEmpty() && matchesCaliber(oh, weapon)) {
                    hasNextRound = true;
                }
            }

            if (hasNextRound) {
                int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, level.getGameTime()));
                weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        new WeaponCooldown(level.getGameTime() + cooldownTicks, cooldownTicks));
            } else {
                int penaltyTicks = 10;
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
                weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        new WeaponCooldown(level.getGameTime() + penaltyTicks, penaltyTicks));
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            }

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
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
                level.addFreshEntity(projectile);
            }

            com.piranport.debug.PiranPortDebug.event(
                    "Fire | weapon={} ammo={} remaining={}",
                    BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                    BuiltInRegistries.ITEM.getKey(shellForRender.getItem()).getPath(),
                    ammoStack.getCount());

            float pitch = getSoundPitch(weapon);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
        }
    }

    /** Manual-mode cannon: consume LOADED_AMMO component on the weapon item. */
    private static void fireCannonManualMode(Level level, Player player, ItemStack coreStack,
            ItemStack weapon, int weaponSlot, SlotCooldowns cooldowns) {
        LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (!loaded.hasAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        boolean isType3 = isType3Shell(loaded.ammoItemId());
        boolean isVT = isVTShell(loaded.ammoItemId());
        boolean isHE = isHEShell(loaded.ammoItemId()) || isVT;
        net.minecraft.world.item.Item shellItem = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse(loaded.ammoItemId()))
                .orElse(net.minecraft.world.item.Items.AIR);
        ItemStack shellForRender = new ItemStack(shellItem, 1);

        // Consume the loaded round (cooldown was already applied when R was pressed)
        weapon.remove(ModDataComponents.LOADED_AMMO.get());

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
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
            level.addFreshEntity(projectile);
        }

        com.piranport.debug.PiranPortDebug.event(
                "Fire (manual) | weapon={} ammo={}",
                BuiltInRegistries.ITEM.getKey(weapon.getItem()).getPath(),
                loaded.ammoItemId());

        float pitch = getSoundPitch(weapon);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
    }

    /** Manual-mode torpedo: consume LOADED_AMMO component on the launcher item. */
    private static void fireTorpedosManualMode(Level level, Player player, ItemStack coreStack,
            Inventory inv, int weaponSlot, TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);

        int tubeCount = launcher.getTubeCount();
        if (!loaded.hasAmmo() || loaded.count() < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        int caliber = launcher.getCaliber();
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
                    new WeaponCooldown(level.getGameTime() + boostedCooldown, boostedCooldown));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    private static void fireTorpedosInventoryMode(Level level, Player player, ItemStack coreStack,
                                            Inventory inv, int weaponSlot, int coreSlot,
                                            TorpedoLauncherItem launcher, SlotCooldowns cooldowns) {
        int caliber = launcher.getCaliber();
        int tubeCount = launcher.getTubeCount();
        int cooldown = launcher.getCooldownTicks();

        // Count available torpedo ammo in inventory
        int available = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                available += s.getCount();
            }
        }
        if (weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                available += oh.getCount();
            }
        }

        if (available < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Consume ammo before spawning entities (prevent TOCTOU)
        boolean magnetic = false;
        boolean acousticHoming = false;
        boolean wireGuided = false;
        TorpedoItem firstTorpedo = null;
        int toConsume = tubeCount;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                if (firstTorpedo == null) firstTorpedo = ti;
                if (ti.isMagnetic()) magnetic = true;
                if (ti.isAcoustic()) acousticHoming = true;
                if (ti.isWireGuided()) wireGuided = true;
                int take = Math.min(toConsume, s.getCount());
                s.shrink(take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                if (firstTorpedo == null) firstTorpedo = ti;
                if (ti.isMagnetic()) magnetic = true;
                if (ti.isAcoustic()) acousticHoming = true;
                if (ti.isWireGuided()) wireGuided = true;
                int take = Math.min(toConsume, oh.getCount());
                oh.shrink(take);
                toConsume -= take;
            }
        }

        float torpedoSpeed = firstTorpedo != null ? firstTorpedo.getSpeed() : (caliber == 610 ? 1.0f : 1.2f);
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            if (firstTorpedo != null) {
                torpedo.setDamage(firstTorpedo.getDamage());
                torpedo.setSpeed(firstTorpedo.getSpeed());
                torpedo.setLifetime(firstTorpedo.getLifetimeTicks());
            }
            if (magnetic) torpedo.setMagnetic(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            if (wireGuided) torpedo.setWireGuided(true);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
        }

        // Damage launcher in-inventory
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
                        new WeaponCooldown(level.getGameTime() + boostedCooldown, boostedCooldown));
            } else {
                int penaltyTicks = 10;
                coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                        cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
                launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        new WeaponCooldown(level.getGameTime() + penaltyTicks, penaltyTicks));
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            }
        }
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    private static void fireDepthCharges(Level level, Player player, ItemStack coreStack,
                                          Inventory inv, int weaponSlot, int coreSlot,
                                          DepthChargeLauncherItem launcher, SlotCooldowns cooldowns) {
        int chargeCount = launcher.getChargeCount();
        int cooldown = launcher.getCooldownTicks();

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
                s.shrink(take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.is(ModItems.DEPTH_CHARGE.get())) {
                int take = Math.min(toConsume, oh.getCount());
                oh.shrink(take);
                toConsume -= take;
            }
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
                    new WeaponCooldown(level.getGameTime() + boostedCooldown, boostedCooldown));
        }
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

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
        if (launcher.isManualReload()
                && !TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
            fireMissileManual(level, player, coreStack, inv, weaponSlot, launcher, cooldowns);
        } else {
            fireMissileAutoReload(level, player, coreStack, inv, weaponSlot, coreSlot, launcher, cooldowns);
        }
    }

    /** 反舰导弹/火箭弹：消耗 LOADED_AMMO，无冷却，仅装填设施装弹。 */
    private static void fireMissileManual(Level level, Player player, ItemStack coreStack,
                                           Inventory inv, int weaponSlot,
                                           MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
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
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 导弹自动装填：从背包消耗弹药，发射后进入冷却。有鱼雷再装填时反舰/火箭也走此路径。 */
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
        if (ammoSlot == -1) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // 消耗1枚弹药
        String ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
        (ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot)).shrink(1);

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

        // 应用冷却
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        if (nextAvailable > 0) {
            int cd = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cd, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    new WeaponCooldown(level.getGameTime() + cd, cd));
        } else {
            int penaltyTicks = 10;
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, penaltyTicks, level.getGameTime()));
            launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    new WeaponCooldown(level.getGameTime() + penaltyTicks, penaltyTicks));
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
        }
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    // ===== Missile firing (GUI mode) =====

    private void fireMissileGui(Level level, Player player, ItemStack stack,
                                 NonNullList<ItemStack> items, int weaponIndex,
                                 MissileLauncherItem launcher, SlotCooldowns cooldowns) {
        ItemStack weapon = items.get(weaponIndex);

        if (launcher.isManualReload()) {
            // 反舰/火箭：消耗 LOADED_AMMO
            LoadedAmmo loaded = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            if (!loaded.hasAmmo()) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            String ammoId = loaded.ammoItemId();
            spawnMissile(level, player, launcher, ammoId);

            int remaining = loaded.count() - 1;
            if (remaining <= 0) {
                weapon.remove(ModDataComponents.LOADED_AMMO.get());
            } else {
                weapon.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(remaining, ammoId));
            }
            stack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        } else {
            // 防空：从弹药槽消耗
            Item ammoItem = launcher.getAmmoItem();
            int ammoSlot = -1;
            int ammoEnd = shipType.weaponSlots + shipType.ammoSlots;
            for (int i = shipType.weaponSlots; i < ammoEnd; i++) {
                if (!items.get(i).isEmpty() && items.get(i).is(ammoItem)) {
                    ammoSlot = i;
                    break;
                }
            }
            if (ammoSlot == -1) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            String ammoId = BuiltInRegistries.ITEM.getKey(ammoItem).toString();
            items.get(ammoSlot).shrink(1);

            spawnMissile(level, player, launcher, ammoId);

            int cd = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
            stack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
            stack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponIndex, cd, level.getGameTime()));
        }

        TransformationManager.setWeaponIndex(stack, weaponIndex);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    /** 生成导弹实体：玩家前方0.5格处，沿视线方向发射。 */
    private static void spawnMissile(Level level, Player player,
                                      MissileLauncherItem launcher, String displayItemId) {
        MissileEntity missile = new MissileEntity(level, launcher.getMissileType(),
                launcher.getDamage(), launcher.getArmorPen(),
                launcher.getExplosionPower(), displayItemId);
        Vec3 look = player.getLookAngle();
        missile.setOwner(player);
        missile.setPos(
                player.getX() + look.x * 0.5,
                player.getEyeY() - 0.1,
                player.getZ() + look.z * 0.5);
        float initSpeed = launcher.getMissileType().initialSpeed;
        missile.setDeltaMovement(look.x * initSpeed, look.y * initSpeed, look.z * initSpeed);
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
                default -> { }
            }
        }

        // Consume payload from inventory if needed
        if (!payloadType.isEmpty() && !hasBullets) {
            net.minecraft.world.item.Item payloadItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse(payloadType));
            boolean consumed = false;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreInventorySlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && s.getItem() == payloadItem) {
                    s.shrink(1);
                    consumed = true;
                    break;
                }
            }
            if (!consumed) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
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
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

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

        if (onCooldown) {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_not_loaded")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        } else if (isManualMode && !(stack.getItem() instanceof AircraftItem)) {
            // Manual mode: also require LOADED_AMMO to be present
            LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
            boolean hasAmmo;
            if (stack.getItem() instanceof TorpedoLauncherItem tl) {
                hasAmmo = loaded.count() >= tl.getTubeCount();
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
        } else {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_ready")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        }
    }

    // ===== Caliber matching =====
    // TODO: replace hardcoded item matching with item tags (e.g. piranport:small_caliber_ammo)

    public static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) {
            return ammo.is(ModItems.SMALL_HE_SHELL.get()) || ammo.is(ModItems.SMALL_AP_SHELL.get())
                    || ammo.is(ModItems.SMALL_VT_SHELL.get()) || ammo.is(ModItems.SMALL_TYPE3_SHELL.get());
        } else if (weapon.is(ModItems.MEDIUM_GUN.get())) {
            return ammo.is(ModItems.MEDIUM_HE_SHELL.get()) || ammo.is(ModItems.MEDIUM_AP_SHELL.get())
                    || ammo.is(ModItems.MEDIUM_TYPE3_SHELL.get());
        } else if (weapon.is(ModItems.LARGE_GUN.get())) {
            return ammo.is(ModItems.LARGE_HE_SHELL.get()) || ammo.is(ModItems.LARGE_AP_SHELL.get())
                    || ammo.is(ModItems.LARGE_TYPE3_SHELL.get());
        }
        return false;
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
        if (weapon.getItem() instanceof CannonItem ci) return ci.getDamage();
        return 6f;
    }

    private static int getGunCooldown(ItemStack weapon) {
        if (weapon.getItem() instanceof CannonItem ci) return ci.getCooldownTicks();
        return 30;
    }

    private static float getExplosionPower(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 1.0f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.5f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 2.0f;
        return 1.0f;
    }

    private static float getProjectileVelocity(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 2.0f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 2.5f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 3.0f;
        return 2.0f;
    }

    private static float getProjectileInaccuracy(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.0f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.5f;
        return 1.0f;
    }

    private static float getSoundPitch(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 1.5f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 1.2f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 0.8f;
        return 1.0f;
    }

    // ===== Type 3 (Sanshiki) spread firing =====

    /**
     * Spawns 64 pellets in a circular spread pattern (sunflower/golden-angle distribution).
     * Each pellet deals 1/4 of the same-caliber HE base damage.
     */
    private static void fireSanshikiSpread(Level level, Player player, ItemStack weapon, ItemStack shellForRender) {
        float baseDamage = getGunDamage(weapon);
        float pelletDamage = baseDamage * 0.25f;
        float velocity = getProjectileVelocity(weapon);

        float baseYaw = player.getYRot();
        float basePitch = player.getXRot();
        float maxRadius = 7.0f; // degrees, same spread range as before
        int pelletCount = 64;
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

    // ===== Torpedo firing =====

    private void fireTorpedos(Level level, Player player, ItemStack coreStack,
                               NonNullList<ItemStack> items, int weaponIndex,
                               TorpedoLauncherItem launcher) {
        // GUI mode also requires 鱼雷再装填 enhancement for auto-reload from ammo slots
        if (!TransformationManager.hasTorpedoReloadEquipped(player, coreStack)) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.need_torpedo_reload"), true);
            return;
        }

        int caliber = launcher.getCaliber();
        int tubeCount = launcher.getTubeCount();
        int cooldown = launcher.getCooldownTicks();

        // Count available ammo of the right caliber (ammo slots only)
        int ammoEnd = shipType.weaponSlots + shipType.ammoSlots;
        int available = 0;
        for (int i = shipType.weaponSlots; i < ammoEnd; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                available += ammo.getCount();
            }
        }

        if (available < tubeCount) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Consume ammo before spawning entities (prevent TOCTOU)
        boolean magnetic = false;
        boolean acousticHoming = false;
        boolean wireGuided = false;
        TorpedoItem firstTorpedo = null;
        int toConsume = tubeCount;
        for (int i = shipType.weaponSlots; i < ammoEnd && toConsume > 0; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                if (firstTorpedo == null) firstTorpedo = ti;
                if (ti.isMagnetic()) magnetic = true;
                if (ti.isAcoustic()) acousticHoming = true;
                if (ti.isWireGuided()) wireGuided = true;
                int take = Math.min(toConsume, ammo.getCount());
                ammo.shrink(take);
                toConsume -= take;
            }
        }

        // Fire torpedoes in spread pattern
        float torpedoSpeed = firstTorpedo != null ? firstTorpedo.getSpeed() : (caliber == 610 ? 1.0f : 1.2f);
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            if (firstTorpedo != null) {
                torpedo.setDamage(firstTorpedo.getDamage());
                torpedo.setSpeed(firstTorpedo.getSpeed());
                torpedo.setLifetime(firstTorpedo.getLifetimeTicks());
            }
            if (magnetic) torpedo.setMagnetic(true);
            if (acousticHoming) torpedo.setAcoustic(true);
            if (wireGuided) torpedo.setWireGuided(true);
            torpedo.setPos(
                    player.getX() + dir.x * 0.5,
                    player.getEyeY() - 0.3,
                    player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
        }

        // Damage launcher (manual, since it lives in a container slot)
        ItemStack launcherStack = items.get(weaponIndex);
        int newDamage = launcherStack.getDamageValue() + 1;
        if (newDamage >= launcherStack.getMaxDamage()) {
            items.set(weaponIndex, ItemStack.EMPTY);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
        } else {
            launcherStack.setDamageValue(newDamage);
        }

        // Cooldown (reduced by ReloadBoostEffect if active)
        int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);

        // Save updated contents + slot cooldown
        SlotCooldowns cd = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cd.withSlotCooldown(weaponIndex, boostedCooldown, level.getGameTime()));
        TransformationManager.setWeaponIndex(coreStack, weaponIndex);

        // Launch sound
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    // ===== Depth charge firing (GUI mode) =====

    private void fireDepthChargesGui(Level level, Player player, ItemStack coreStack,
                                      NonNullList<ItemStack> items, int weaponIndex,
                                      DepthChargeLauncherItem launcher) {
        int chargeCount = launcher.getChargeCount();
        int cooldown = launcher.getCooldownTicks();

        // Count available depth charge ammo in ammo slots
        int ammoEnd = shipType.weaponSlots + shipType.ammoSlots;
        int available = 0;
        for (int i = shipType.weaponSlots; i < ammoEnd; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ammo.is(ModItems.DEPTH_CHARGE.get())) {
                available += ammo.getCount();
            }
        }

        if (available < chargeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Consume ammo
        int toConsume = chargeCount;
        for (int i = shipType.weaponSlots; i < ammoEnd && toConsume > 0; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ammo.is(ModItems.DEPTH_CHARGE.get())) {
                int take = Math.min(toConsume, ammo.getCount());
                ammo.shrink(take);
                toConsume -= take;
            }
        }

        // Spawn depth charges
        Vec3 look = player.getLookAngle();
        Vec3 horizLook = new Vec3(look.x, 0, look.z).normalize();
        switch (launcher.getSpreadPattern()) {
            case SINGLE -> spawnDepthCharge(level, player, horizLook, 0.0, 0.6);
            case FRONT_BACK -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7);
                spawnDepthCharge(level, player, horizLook, 0.0, 0.4);
            }
            case TRIANGLE -> {
                spawnDepthCharge(level, player, horizLook, 0.0, 0.7);
                spawnDepthCharge(level, player, rotateHorizontal(horizLook, Math.toRadians(-20)), 0.0, 0.5);
                spawnDepthCharge(level, player, rotateHorizontal(horizLook, Math.toRadians(20)), 0.0, 0.5);
            }
        }

        // Damage launcher
        ItemStack launcherStack = items.get(weaponIndex);
        int newDamage = launcherStack.getDamageValue() + 1;
        if (newDamage >= launcherStack.getMaxDamage()) {
            items.set(weaponIndex, ItemStack.EMPTY);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
        } else {
            launcherStack.setDamageValue(newDamage);
        }

        int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
        SlotCooldowns cd = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cd.withSlotCooldown(weaponIndex, boostedCooldown, level.getGameTime()));
        TransformationManager.setWeaponIndex(coreStack, weaponIndex);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    private static float[] getSpreadAngles(int count) {
        return switch (count) {
            case 2 -> new float[]{-3f, 3f};
            case 3 -> new float[]{-4f, 0f, 4f};
            case 4 -> new float[]{-6f, -2f, 2f, 6f};
            default -> new float[]{0f};
        };
    }

    // ===== Aircraft launch =====

    private void launchAircraft(Level level, Player player, ItemStack coreStack,
                                 NonNullList<ItemStack> items, int weaponIndex, int coreInventorySlot) {
        ItemStack aircraftStack = items.get(weaponIndex);

        // Fuel check — refuse launch if currentFuel == 0
        AircraftInfo launchInfo = aircraftStack.get(ModDataComponents.AIRCRAFT_INFO.get());
        if (launchInfo == null || launchInfo.currentFuel() <= 0) {
            player.displayClientMessage(Component.translatable("message.piranport.no_fuel"), true);
            return;
        }

        // Look up group data for this weapon slot
        FlightGroupData groupData = coreStack.getOrDefault(
                ModDataComponents.FLIGHT_GROUP_DATA.get(), FlightGroupData.empty());
        FlightGroupData.AttackMode attackMode = FlightGroupData.AttackMode.FOCUS;
        boolean hasBullets = true; // default: fighters use bullets
        String payloadType = "";
        for (FlightGroupData.FlightGroup group : groupData.groups()) {
            if (group.slotIndices().contains(weaponIndex)) {
                attackMode = group.attackMode();
                hasBullets = group.getSlotBullets(weaponIndex);
                payloadType = group.getSlotPayload(weaponIndex);
                break;
            }
        }

        int ammoStart = shipType.weaponSlots;
        int ammoEnd = ammoStart + shipType.ammoSlots;

        // 挂载物品消耗（子弹不消耗物品；有挂载时必须有库存）
        if (!payloadType.isEmpty() && !hasBullets) {
            boolean consumed = false;
            for (int ai = ammoStart; ai < ammoEnd; ai++) {
                ItemStack ammoStack = items.get(ai);
                if (!ammoStack.isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem()).toString();
                    if (payloadType.equals(itemId)) {
                        ammoStack.shrink(1);
                        consumed = true;
                        break;
                    }
                }
            }
            if (!consumed) {
                player.displayClientMessage(
                        Component.translatable("message.piranport.no_ammo"), true);
                return;
            }
        }

        AircraftEntity aircraft = AircraftEntity.create(level, player, weaponIndex, aircraftStack,
                attackMode, coreInventorySlot, hasBullets, payloadType);
        level.addFreshEntity(aircraft);
        com.piranport.debug.PiranPortDebug.event(
                "Aircraft LAUNCH | type={} entityId={} payload={} mode={}",
                aircraft.getAircraftType().name(), aircraft.getId(), payloadType, attackMode.name());

        // Clear slot — aircraft is now airborne
        items.set(weaponIndex, ItemStack.EMPTY);
        int launchCooldown = TransformationManager.boostedCooldown(player, 20);
        SlotCooldowns launchCd = coreStack.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                launchCd.withSlotCooldown(weaponIndex, launchCooldown, level.getGameTime()));
        TransformationManager.setWeaponIndex(coreStack, weaponIndex);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        player.displayClientMessage(
                Component.translatable("message.piranport.aircraft_launched",
                        aircraftStack.getHoverName()), true);
    }

    // ===== Fuel refill =====

    /**
     * Recall all airborne aircraft owned by this player. Returns the count recalled.
     */
    private static int recallAllAircraft(ServerLevel level, Player player) {
        java.util.UUID ownerUUID = player.getUUID();
        java.util.List<AircraftEntity> aircraft = level.getEntitiesOfClass(
                AircraftEntity.class,
                new AABB(player.getX() - 300, player.getY() - 300, player.getZ() - 300,
                         player.getX() + 300, player.getY() + 300, player.getZ() + 300),
                a -> ownerUUID.equals(a.getOwnerUUID()) && a.isAlive());
        for (AircraftEntity a : aircraft) {
            a.recallAndRemove();
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

        if (!com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
            refillAircraftFuelInventoryMode(player);
            return;
        }

        ShipCoreItem sci = (ShipCoreItem) coreStack.getItem();
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        int totalSlots = sci.getShipType().totalSlots();
        NonNullList<ItemStack> items = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
        contents.copyInto(items);

        int ammoStart = sci.getShipType().weaponSlots;
        int ammoEnd = ammoStart + sci.getShipType().ammoSlots;
        boolean contentsChanged = false;

        for (int wi = 0; wi < sci.getShipType().weaponSlots; wi++) {
            ItemStack weapon = items.get(wi);
            if (!(weapon.getItem() instanceof AircraftItem)) continue;
            AircraftInfo info = weapon.get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info == null || info.currentFuel() >= info.fuelCapacity()) continue;

            for (int ai = ammoStart; ai < ammoEnd; ai++) {
                ItemStack ammo = items.get(ai);
                if (ammo.is(ModItems.AVIATION_FUEL.get()) && ammo.getCount() > 0) {
                    ammo.shrink(1);
                    weapon.set(ModDataComponents.AIRCRAFT_INFO.get(),
                            info.withCurrentFuel(info.fuelCapacity()));
                    contentsChanged = true;
                    break;
                }
            }
        }

        if (contentsChanged) {
            coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(),
                    ItemContainerContents.fromItems(items));
        }

        boolean hasFueled = false;
        for (int wi = 0; wi < sci.getShipType().weaponSlots; wi++) {
            AircraftInfo info = items.get(wi).get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info != null && info.currentFuel() > 0) { hasFueled = true; break; }
        }
        if (hasFueled && com.piranport.config.ModCommonConfig.isFlammableEffectActive()) {
            player.addEffect(new MobEffectInstance(ModMobEffects.FLAMMABLE, 999999, 0, false, true));
        }
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
                    ammo.shrink(1);
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

            // 战斗机默认有子弹（与手动发射一致）；非战斗机默认无子弹
            boolean isFighter = info.aircraftType() == AircraftInfo.AircraftType.FIGHTER;
            boolean hasBullets = isFighter; // default: fighters use bullets
            for (FlightGroupData.FlightGroup group : groupData.groups()) {
                if (group.slotIndices().contains(wi)) {
                    hasBullets = group.getSlotBullets(wi);
                    break;
                }
            }
            if (!hasBullets) continue; // 只自动升空配置了子弹的飞机

            sci.launchAircraft(level, player, coreStack, items, wi, coreSlot);
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
            (ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot)).shrink(1);

            // Spawn missile aimed at fire control target
            spawnMissileAutoAim(level, player, launcher, ammoId);

            // Apply cooldown
            int cd = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(slot, cd, gameTime));
            stack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                    new WeaponCooldown(gameTime + cd, cd));

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);
            return true;
        }
        return false;
    }

    /** Spawn a missile aimed toward the fire control target, or upward if no target. */
    private static void spawnMissileAutoAim(Level level, Player player,
                                             MissileLauncherItem launcher, String displayItemId) {
        MissileEntity missile = new MissileEntity(level, launcher.getMissileType(),
                launcher.getDamage(), launcher.getArmorPen(),
                launcher.getExplosionPower(), displayItemId);
        missile.setOwner(player);

        // Aim toward fire control target; fallback to player look direction with upward bias
        Vec3 aimDir = player.getLookAngle();
        if (level instanceof ServerLevel sl) {
            List<UUID> fcTargets = FireControlManager.getTargets(player.getUUID());
            for (UUID targetUUID : fcTargets) {
                net.minecraft.world.entity.Entity target = sl.getEntity(targetUUID);
                if (target != null && target.isAlive()) {
                    Vec3 toTarget = target.position().add(0, target.getBbHeight() * 0.5, 0)
                            .subtract(player.getEyePosition());
                    if (toTarget.lengthSqr() > 0.01) {
                        aimDir = toTarget.normalize();
                    }
                    break;
                }
            }
        }

        missile.setPos(
                player.getX() + aimDir.x * 0.5,
                player.getEyeY() - 0.1,
                player.getZ() + aimDir.z * 0.5);
        float initSpeed = launcher.getMissileType().initialSpeed;
        missile.setDeltaMovement(aimDir.x * initSpeed, aimDir.y * initSpeed, aimDir.z * initSpeed);
        level.addFreshEntity(missile);
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
}
