package com.piranport.item;

import com.piranport.combat.TransformationManager;
import com.piranport.entity.CannonProjectileEntity;
import com.piranport.menu.ShipCoreMenu;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public class ShipCoreItem extends Item {

    public enum ShipType {
        SMALL(0, 40, 4, 4),
        MEDIUM(5, 64, 5, 4),
        LARGE(10, 112, 6, 4);

        public final int healthBonus;
        public final int maxLoad;
        public final int weaponSlots;
        public final int ammoSlots;

        ShipType(int healthBonus, int maxLoad, int weaponSlots, int ammoSlots) {
            this.healthBonus = healthBonus;
            this.maxLoad = maxLoad;
            this.weaponSlots = weaponSlots;
            this.ammoSlots = ammoSlots;
        }

        public int totalSlots() {
            return weaponSlots + ammoSlots;
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
                    player.displayClientMessage(
                            Component.translatable("message.piranport.transformed"), true);
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.piranport.untransformed"), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (isTransformed) {
            // Fire weapon
            if (!level.isClientSide) {
                fireCurrentWeapon(level, player, stack);
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

    private void fireCurrentWeapon(Level level, Player player, ItemStack stack) {
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
}
