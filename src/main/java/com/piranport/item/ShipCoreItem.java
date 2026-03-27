package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.component.AircraftInfo;
import com.piranport.component.FlightGroupData;
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
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

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
                } else {
                    TransformationManager.removeTransformationAttributes(player);
                    player.removeEffect(ModMobEffects.FLAMMABLE);
                    player.displayClientMessage(
                            Component.translatable("message.piranport.untransformed"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (isTransformed) {
            // Fire weapon
            if (!level.isClientSide) {
                int coreSlot = hand == InteractionHand.MAIN_HAND
                        ? player.getInventory().selected
                        : 40; // offhand slot index in Inventory
                fireCurrentWeapon(level, player, stack, coreSlot);
            }
            return InteractionResultHolder.consume(stack);
        }

        // Open GUI
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
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

        // Save updated contents
        stack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
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

        // Cooldown
        int cooldownTicks = getGunCooldown(weapon);
        player.getCooldowns().addCooldown(this, cooldownTicks);

        // Sound
        float pitch = getSoundPitch(weapon);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, pitch);
    }

    // ===== Caliber matching =====

    private static boolean matchesCaliber(ItemStack ammo, ItemStack weapon) {
        if (weapon.is(ModItems.SMALL_GUN.get())) {
            return ammo.is(ModItems.SMALL_HE_SHELL.get()) || ammo.is(ModItems.SMALL_AP_SHELL.get());
        } else if (weapon.is(ModItems.MEDIUM_GUN.get())) {
            return ammo.is(ModItems.MEDIUM_HE_SHELL.get()) || ammo.is(ModItems.MEDIUM_AP_SHELL.get());
        } else if (weapon.is(ModItems.LARGE_GUN.get())) {
            return ammo.is(ModItems.LARGE_HE_SHELL.get()) || ammo.is(ModItems.LARGE_AP_SHELL.get());
        }
        return false;
    }

    private static boolean isHEShell(ItemStack stack) {
        return stack.is(ModItems.SMALL_HE_SHELL.get())
                || stack.is(ModItems.MEDIUM_HE_SHELL.get())
                || stack.is(ModItems.LARGE_HE_SHELL.get());
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

        // Count available ammo of the right caliber
        int available = 0;
        for (int i = shipType.weaponSlots; i < shipType.totalSlots(); i++) {
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

        // Consume ammo
        int toConsume = tubeCount;
        for (int i = shipType.weaponSlots; i < shipType.totalSlots() && toConsume > 0; i++) {
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

        // Save updated contents
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        TransformationManager.setWeaponIndex(coreStack, weaponIndex);

        // Cooldown and launch sound
        player.getCooldowns().addCooldown(launcher, cooldown);
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
        String ammoTypeId = "";
        for (FlightGroupData.FlightGroup group : groupData.groups()) {
            if (group.slotIndices().contains(weaponIndex)) {
                attackMode = group.attackMode();
                ammoTypeId = group.getSlotAmmo(weaponIndex); // per-slot ammo assignment
                break;
            }
        }

        // Require ammo assignment — refuse launch if not configured
        if (ammoTypeId.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.piranport.no_ammo"), true);
            return;
        }

        // Deduct 1 of the assigned ammo type from ammo slots
        boolean consumed = false;
        int ammoStart = shipType.weaponSlots;
        int ammoEnd = ammoStart + shipType.ammoSlots;
        for (int ai = ammoStart; ai < ammoEnd; ai++) {
            ItemStack ammoStack = items.get(ai);
            if (!ammoStack.isEmpty()) {
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(ammoStack.getItem());
                if (ammoTypeId.equals(itemId.toString())) {
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

        AircraftEntity aircraft = AircraftEntity.create(level, player, weaponIndex, aircraftStack, attackMode, coreInventorySlot);
        level.addFreshEntity(aircraft);

        // Clear slot — aircraft is now airborne
        items.set(weaponIndex, ItemStack.EMPTY);
        coreStack.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.fromItems(items));
        TransformationManager.setWeaponIndex(coreStack, weaponIndex);

        player.getCooldowns().addCooldown(this, 20);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        player.displayClientMessage(
                Component.translatable("message.piranport.aircraft_launched",
                        aircraftStack.getHoverName()), true);
    }

    // ===== Fuel refill =====

    /**
     * On transformation: consume aviation_fuel from ammo slots to fill aircraft.
     * One aviation_fuel item fills one aircraft to full fuelCapacity.
     * Applies FlammableEffect if any aircraft ends up with fuel > 0.
     */
    private static void refillAircraftFuel(Player player, ItemStack coreStack) {
        if (!(coreStack.getItem() instanceof ShipCoreItem sci)) return;

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

            // Find and consume one aviation_fuel item to fill this aircraft
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

        // Apply FlammableEffect if any aircraft in weapon slots has fuel > 0
        boolean hasFueled = false;
        for (int wi = 0; wi < sci.getShipType().weaponSlots; wi++) {
            AircraftInfo info = items.get(wi).get(ModDataComponents.AIRCRAFT_INFO.get());
            if (info != null && info.currentFuel() > 0) { hasFueled = true; break; }
        }
        if (hasFueled) {
            player.addEffect(new MobEffectInstance(ModMobEffects.FLAMMABLE, 999999, 0, false, true));
        }
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
