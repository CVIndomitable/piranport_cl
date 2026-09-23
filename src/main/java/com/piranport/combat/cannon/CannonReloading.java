package com.piranport.combat.cannon;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.data.AmmoInventory;
import com.piranport.combat.data.WeaponState;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

import static com.piranport.combat.cannon.CannonAmmoRules.isCannonReadyToFire;
import static com.piranport.combat.cannon.CannonInventory.findCoreSlotIndex;
import static com.piranport.combat.cannon.CannonStats.getBarrelCount;
import static com.piranport.combat.cannon.CannonStats.getGunCooldown;

/**
 * 火炮装填生命周期：武器保存唯一读条，HUD 和提示直接读取武器。
 * 自动与手动模式只影响读条启动；两者都在到期后才扣弹并写入 LOADED_AMMO。
 */
public final class CannonReloading {
    private CannonReloading() {}

    public static void tickCannonAutoReload(Player player, ItemStack coreStack) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator() || coreStack.isEmpty()) return;
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            changed |= tickSlot(player, coreStack, slot, inventory.items.get(slot));
        }
        changed |= tickSlot(player, coreStack, 40, inventory.offhand.get(0));
        if (changed) TransformationManager.writeCoreToConfiguredSlot(player, coreStack);
    }

    private static boolean tickSlot(Player player, ItemStack core, int slot, ItemStack weapon) {
        if (!(weapon.getItem() instanceof ArtilleryItem artillery)) return false;
        int coreSlot = findCoreSlotIndex(player.getInventory(), player, slot);
        if (coreSlot == -1) return false;
        WeaponState state = new WeaponState(weapon);
        boolean loaded = isCannonReadyToFire(weapon, player.level());
        if (!loaded && state.hasLoadedAmmo()) state.clearLoadedAmmo();
        WeaponCooldown cooldown = state.getCooldown();
        CannonReloadPhase phase = CannonReloadPhase.resolve(loaded,
                artillery.getEffectiveData(player.level()).isAutoLoading(),
                cooldown == null ? null : cooldown.endTick(), player.level().getGameTime());
        boolean legacyRemoved = removeLegacyCooldown(core, slot);
        boolean changed = switch (phase) {
            case LOADED -> clearCannonReloadState(core, weapon, slot);
            case IDLE, WAIT -> false;
            case START -> startCannonReloadIfPossible(player, core, player.getInventory(),
                    slot, coreSlot, weapon);
            case COMPLETE -> completeCannonReload(player, core, player.getInventory(),
                    slot, coreSlot, weapon);
        };
        return legacyRemoved || changed;
    }

    static boolean startCannonReloadIfPossible(Player player, ItemStack core, Inventory inventory,
            int weaponSlot, int coreSlot, ItemStack weapon) {
        if (coreSlot == -1 || player.level().isClientSide()) return false;
        WeaponState state = new WeaponState(weapon);
        // 连续点击空炮不能反复重置读条，物品换槽也不会继承另一个槽位的计时。
        if (state.getCooldown() != null && state.getCooldown().endTick() > 0) {
            return removeLegacyCooldown(core, weaponSlot);
        }
        AmmoInventory ammo = new AmmoInventory(inventory, coreSlot, weaponSlot);
        Item type = ammo.chooseReloadAmmo(weapon, getBarrelCount(weapon, player.level()),
                player.getAbilities().instabuild, player.level());
        if (type == null) return clearCannonReloadState(core, weapon, weaponSlot);
        ammo.recordAmmoType(weapon, type);
        startTimer(player, core, weaponSlot, weapon);
        CannonSounds.playCannonReloadStartSound(player, weapon);
        return true;
    }

    public static void tryManualCannonReload(Player player, ItemStack core, int coreSlot, ItemStack weapon) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator()) return;
        if (core.isEmpty() || !(weapon.getItem() instanceof ArtilleryItem artillery)) return;
        int slot = CannonInventory.findWeaponSlot(player.getInventory(), weapon);
        if (slot < 0 || coreSlot == -1) return;
        if (artillery.getEffectiveData(player.level()).isAutoLoading()) {
            player.displayClientMessage(Component.translatable("message.piranport.weapon_auto_reload"), true);
            return;
        }
        if (isCannonReadyToFire(weapon, player.level())) {
            player.displayClientMessage(Component.translatable("message.piranport.weapon_already_loaded"), true);
            return;
        }
        WeaponState state = new WeaponState(weapon);
        WeaponCooldown cooldown = state.getCooldown();
        if (cooldown != null && cooldown.endTick() > 0) {
            if (cooldown.isOnCooldown(player.level().getGameTime())) {
                player.displayClientMessage(Component.translatable("message.piranport.weapon_reloading"), true);
            } else {
                if (completeCannonReload(player, core, player.getInventory(), slot, coreSlot, weapon)) {
                    TransformationManager.writeCoreToConfiguredSlot(player, core);
                }
            }
            return;
        }
        state.clearLoadedAmmo();
        if (startCannonReloadIfPossible(player, core, player.getInventory(), slot, coreSlot, weapon)) {
            TransformationManager.writeCoreToConfiguredSlot(player, core);
        }
    }

    /** 已经完成持续使用读条的手动装填入口，共享到期装填的弹药事务。 */
    public static void completeManualUse(Player player, ItemStack weapon) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator()) return;
        if (!(weapon.getItem() instanceof ArtilleryItem artillery)
                || artillery.getEffectiveData(player.level()).isAutoLoading()
                || isCannonReadyToFire(weapon, player.level())) return;
        Inventory inventory = player.getInventory();
        int slot = CannonInventory.findWeaponSlot(inventory, weapon);
        if (slot < 0) return;
        int coreSlot = findCoreSlotIndex(inventory, player, slot);
        ItemStack core = CannonInventory.coreAt(player, coreSlot);
        if (coreSlot == -1 || core.isEmpty()) return;
        if (completeCannonReload(player, core, inventory, slot, coreSlot, weapon)) {
            TransformationManager.writeCoreToConfiguredSlot(player, core);
        }
    }

    private static boolean completeCannonReload(Player player, ItemStack core, Inventory inventory,
            int weaponSlot, int coreSlot, ItemStack weapon) {
        int barrels = getBarrelCount(weapon, player.level());
        AmmoInventory ammo = new AmmoInventory(inventory, coreSlot, weaponSlot);
        Item type = ammo.chooseReloadAmmo(weapon, barrels, player.getAbilities().instabuild, player.level());
        if (type == null || (!player.getAbilities().instabuild && !ammo.consumeAmmo(player.getUUID(), type, barrels))) {
            return clearCannonReloadState(core, weapon, weaponSlot);
        }
        new WeaponState(weapon).setLoadedAmmo(barrels, BuiltInRegistries.ITEM.getKey(type).toString());
        clearCannonReloadState(core, weapon, weaponSlot);
        ammo.recordAmmoType(weapon, type);
        CannonSounds.playCannonReloadCompleteSound(player, weapon);
        return true;
    }

    /** 切弹只重启未完成读条；已装弹和空闲手动炮保持原来的装填状态。 */
    public static void selectAmmo(Player player, ItemStack source, String ammoId) {
        if (player.level().isClientSide() || !(source.getItem() instanceof ArtilleryItem)) return;
        ItemStack core = TransformationManager.findTransformedCore(player);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            updateSelection(player, core, slot, inventory.items.get(slot), source.getItem(), ammoId);
        }
        updateSelection(player, core, 40, inventory.offhand.get(0), source.getItem(), ammoId);
        if (!core.isEmpty()) TransformationManager.writeCoreToConfiguredSlot(player, core);
    }

    public static void syncAmmoToSiblingGuns(Player player) {
        ItemStack source = player.getMainHandItem();
        String selected = new WeaponState(source).getSelectedAmmoType().ammoItemId();
        if (selected != null && !selected.isEmpty()) selectAmmo(player, source, selected);
    }

    private static void updateSelection(Player player, ItemStack core, int slot,
            ItemStack weapon, Item type, String ammoId) {
        if (weapon.getItem() != type) return;
        WeaponState state = new WeaponState(weapon);
        if (Objects.equals(state.getSelectedAmmoType().ammoItemId(), ammoId)) return;
        state.setSelectedAmmoType(ammoId);
        WeaponCooldown cooldown = state.getCooldown();
        if (!core.isEmpty() && CannonReloadPhase.shouldRestartAfterSelection(
                isCannonReadyToFire(weapon, player.level()), cooldown == null ? null : cooldown.endTick())) {
            startTimer(player, core, slot, weapon);
        }
    }

    private static void startTimer(Player player, ItemStack core, int slot, ItemStack weapon) {
        int ticks = TransformationManager.boostedCooldown(player, getGunCooldown(weapon, player.level()));
        WeaponState state = new WeaponState(weapon);
        state.setCooldown(player.getUUID(), player.level().getGameTime(), ticks);
        removeLegacyCooldown(core, slot);
    }

    private static boolean clearCannonReloadState(ItemStack core, ItemStack weapon,
            int slot) {
        WeaponState state = new WeaponState(weapon);
        boolean changed = state.getCooldown() != null;
        state.clearCooldown();
        return removeLegacyCooldown(core, slot) || changed;
    }

    /** 清除旧版火炮的槽位镜像，避免换成其他武器后继承旧火炮的装填时间。 */
    private static boolean removeLegacyCooldown(ItemStack core, int slot) {
        SlotCooldowns old = cooldowns(core);
        SlotCooldowns updated = old.withoutSlotCooldown(slot);
        if (updated == old) return false;
        core.set(ModDataComponents.SLOT_COOLDOWNS.get(), updated);
        return true;
    }

    private static SlotCooldowns cooldowns(ItemStack core) {
        return core.getOrDefault(ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
    }
}
