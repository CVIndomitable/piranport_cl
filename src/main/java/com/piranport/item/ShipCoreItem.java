package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.menu.ShipCoreMenu;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ShipCoreItem extends Item {

    public enum ShipType {
        SMALL(0, 40, 4, 4, 2),
        MEDIUM(5, 64, 5, 4, 3),
        LARGE(10, 112, 6, 4, 4);

        public final int healthBonus;
        public final int maxLoad;
        public final int weaponSlots;
        public final int ammoSlots;
        public final int enhancementSlots;

        ShipType(int healthBonus, int maxLoad, int weaponSlots, int ammoSlots, int enhancementSlots) {
            this.healthBonus = healthBonus;
            this.maxLoad = maxLoad;
            this.weaponSlots = weaponSlots;
            this.ammoSlots = ammoSlots;
            this.enhancementSlots = enhancementSlots;
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
            // No-GUI mode: find the best (highest maxLoad) core in inventory.
            // If this core is the best (effective) one, show load info; otherwise show "不生效".
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
                    if (shipType.maxLoad >= bestMaxLoad) {
                        int totalLoad = com.piranport.combat.TransformationManager
                                .getInventoryWeaponLoad(inv);
                        tooltipComponents.add(Component.translatable(
                                "container.piranport.load", totalLoad, bestMaxLoad));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.piranport.core_inactive"));
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
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isTransformed = TransformationManager.isTransformed(stack);

        if (player.isShiftKeyDown()) {
            // Toggle transformation
            if (!level.isClientSide) {
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
                    player.removeEffect(ModMobEffects.FLAMMABLE);
                    player.displayClientMessage(
                            Component.translatable("message.piranport.untransformed"), true);
                    com.piranport.debug.PiranPortDebug.event(
                            "Transform OFF | player={}", player.getName().getString());
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (isTransformed) {
            if (com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
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
                && com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
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

        // Handle aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            launchAircraft(level, player, stack, items, weaponIndex, coreInventorySlot);
            return;
        }

        // Find matching ammo
        int ammoSlot = -1;
        for (int i = shipType.weaponSlots; i < totalSlots; i++) {
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
        boolean isHE = isHEShell(ammoStack);
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

        // Create and launch projectile
        float damage = getGunDamage(weapon);
        float explosionPower = getExplosionPower(weapon);
        float velocity = getProjectileVelocity(weapon);
        float inaccuracy = getProjectileInaccuracy(weapon);

        CannonProjectileEntity projectile = new CannonProjectileEntity(
                level, player, shellForRender, damage, isHE, explosionPower);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(),
                0.0f, velocity, inaccuracy);
        level.addFreshEntity(projectile);

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
        if (level.isClientSide || com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) return false;

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

        // Torpedo launcher
        if (weapon.getItem() instanceof TorpedoLauncherItem torpedoLauncher) {
            if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
                fireTorpedosManualMode(level, player, coreStack, inv, weaponSlot, torpedoLauncher, cooldowns);
            } else {
                fireTorpedosInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, torpedoLauncher, cooldowns);
            }
            return;
        }

        // Aircraft
        if (weapon.getItem() instanceof AircraftItem) {
            launchAircraftInventoryMode(level, player, coreStack, inv, weaponSlot, coreInventorySlot, cooldowns);
            return;
        }

        // Cannon
        if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) {
            // Manual mode: use LOADED_AMMO component on weapon item
            fireCannonManualMode(level, player, coreStack, weapon, weaponSlot, cooldowns);
        } else {
            // Auto mode: find matching ammo anywhere in inventory
            int ammoSlot = -1;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreInventorySlot || i == weaponSlot) continue;
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && matchesCaliber(ammo, weapon)) {
                    ammoSlot = i;
                    break;
                }
            }

            if (ammoSlot == -1) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            ItemStack ammoStack = inv.items.get(ammoSlot);
            boolean isHE = isHEShell(ammoStack);
            ItemStack shellForRender = ammoStack.copyWithCount(1);
            ammoStack.shrink(1);

            int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));
            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                    cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, level.getGameTime()));

            float damage = getGunDamage(weapon);
            float explosionPower = getExplosionPower(weapon);
            float velocity = getProjectileVelocity(weapon);
            float inaccuracy = getProjectileInaccuracy(weapon);

            CannonProjectileEntity projectile = new CannonProjectileEntity(
                    level, player, shellForRender, damage, isHE, explosionPower);
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
            level.addFreshEntity(projectile);

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

        boolean isHE = isHEShell(loaded.ammoItemId());
        net.minecraft.world.item.Item shellItem = BuiltInRegistries.ITEM
                .getOptional(ResourceLocation.tryParse(loaded.ammoItemId()))
                .orElse(net.minecraft.world.item.Items.AIR);
        ItemStack shellForRender = new ItemStack(shellItem, 1);

        // Consume the loaded round
        weapon.remove(ModDataComponents.LOADED_AMMO.get());

        int cooldownTicks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon));
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, cooldownTicks, level.getGameTime()));

        float damage = getGunDamage(weapon);
        float explosionPower = getExplosionPower(weapon);
        float velocity = getProjectileVelocity(weapon);
        float inaccuracy = getProjectileInaccuracy(weapon);

        CannonProjectileEntity projectile = new CannonProjectileEntity(
                level, player, shellForRender, damage, isHE, explosionPower);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, inaccuracy);
        level.addFreshEntity(projectile);

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
        float torpedoSpeed = caliber == 610 ? 1.0f : 1.2f;
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
        }

        // Consume all loaded torpedoes
        launcherStack.remove(ModDataComponents.LOADED_AMMO.get());

        // Damage launcher
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));

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

        if (available < tubeCount) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        float torpedoSpeed = caliber == 610 ? 1.0f : 1.2f;
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setPos(player.getX() + dir.x * 0.5, player.getEyeY() - 0.3, player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
        }

        // Consume ammo from inventory
        int toConsume = tubeCount;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                int take = Math.min(toConsume, s.getCount());
                s.shrink(take);
                toConsume -= take;
            }
        }

        // Damage launcher in-inventory
        ItemStack launcherStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);
        if (!launcherStack.isEmpty()) {
            int newDamage = launcherStack.getDamageValue() + 1;
            if (newDamage >= launcherStack.getMaxDamage()) {
                if (weaponSlot == 40) inv.offhand.set(0, ItemStack.EMPTY);
                else inv.items.set(weaponSlot, ItemStack.EMPTY);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f + level.random.nextFloat() * 0.4f);
            } else {
                launcherStack.setDamageValue(newDamage);
            }
        }

        int boostedCooldown = TransformationManager.boostedCooldown(player, cooldown);
        coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(),
                cooldowns.withSlotCooldown(weaponSlot, boostedCooldown, level.getGameTime()));
        TransformationManager.setWeaponIndex(coreStack, weaponSlot);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.4f, 0.4f);
    }

    private static void launchAircraftInventoryMode(Level level, Player player, ItemStack coreStack,
                                              Inventory inv, int weaponSlot, int coreInventorySlot,
                                              SlotCooldowns cooldowns) {
        ItemStack aircraftStack = weaponSlot == 40 ? inv.offhand.get(0) : inv.items.get(weaponSlot);

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

        // Consume payload from inventory if needed
        if (!payloadType.isEmpty() && !hasBullets) {
            boolean consumed = false;
            for (int i = 0; i < inv.items.size(); i++) {
                if (i == coreInventorySlot || i == weaponSlot) continue;
                ItemStack s = inv.items.get(i);
                if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals(payloadType)) {
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

    static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) {
            return ammo.is(ModItems.SMALL_HE_SHELL.get()) || ammo.is(ModItems.SMALL_AP_SHELL.get());
        } else if (weapon.is(ModItems.MEDIUM_GUN.get())) {
            return ammo.is(ModItems.MEDIUM_HE_SHELL.get()) || ammo.is(ModItems.MEDIUM_AP_SHELL.get());
        } else if (weapon.is(ModItems.LARGE_GUN.get())) {
            return ammo.is(ModItems.LARGE_HE_SHELL.get()) || ammo.is(ModItems.LARGE_AP_SHELL.get());
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

    // ===== Gun stats =====

    private static float getGunDamage(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 6f;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 12f;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 20f;
        return 6f;
    }

    private static int getGunCooldown(ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) return 30;
        if (weapon.is(ModItems.MEDIUM_GUN.get())) return 50;
        if (weapon.is(ModItems.LARGE_GUN.get())) return 80;
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

    // ===== Torpedo firing =====

    private void fireTorpedos(Level level, Player player, ItemStack coreStack,
                               NonNullList<ItemStack> items, int weaponIndex,
                               TorpedoLauncherItem launcher) {
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

        // Fire torpedoes in spread pattern
        float torpedoSpeed = caliber == 610 ? 1.0f : 1.2f;
        float[] angles = getSpreadAngles(tubeCount);
        Vec3 look = player.getLookAngle();

        for (float angle : angles) {
            Vec3 dir = rotateHorizontal(look, Math.toRadians(angle));
            TorpedoEntity torpedo = new TorpedoEntity(level, player, caliber);
            torpedo.setPos(
                    player.getX() + dir.x * 0.5,
                    player.getEyeY() - 0.3,
                    player.getZ() + dir.z * 0.5);
            torpedo.setDeltaMovement(dir.x * torpedoSpeed, 0, dir.z * torpedoSpeed);
            level.addFreshEntity(torpedo);
        }

        // Consume ammo (ammo slots only)
        int toConsume = tubeCount;
        for (int i = shipType.weaponSlots; i < ammoEnd && toConsume > 0; i++) {
            ItemStack ammo = items.get(i);
            if (!ammo.isEmpty() && ammo.getItem() instanceof TorpedoItem ti && ti.getCaliber() == caliber) {
                int take = Math.min(toConsume, ammo.getCount());
                ammo.shrink(take);
                toConsume -= take;
            }
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

        // Look up group data for this weapon slot
        FlightGroupData groupData = coreStack.getOrDefault(
                ModDataComponents.FLIGHT_GROUP_DATA.get(), FlightGroupData.empty());
        FlightGroupData.AttackMode attackMode = FlightGroupData.AttackMode.FOCUS;
        boolean hasBullets = false;
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
     * On transformation: consume aviation_fuel to fill aircraft.
     * One aviation_fuel item fills one aircraft to full fuelCapacity.
     * Applies FlammableEffect if any aircraft ends up with fuel > 0.
     */
    private static void refillAircraftFuel(Player player, ItemStack coreStack) {
        if (!com.piranport.config.ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()) return; // manual mode: no auto fuel
        if (!(coreStack.getItem() instanceof ShipCoreItem)) return;

        if (!com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
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
        if (hasFueled && com.piranport.config.ModCommonConfig.FLAMMABLE_EFFECT_ENABLED.get()) {
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

        if (hasFueled && com.piranport.config.ModCommonConfig.FLAMMABLE_EFFECT_ENABLED.get()) {
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

            // 找到配置了子弹的飞机（战斗机行为）
            boolean hasBullets = false;
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
