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
 * 火炮固定使用手动装填：只能由 R 键启动读条，读条到期后扣弹并写入 LOADED_AMMO。
 */
public final class CannonReloading {
    private CannonReloading() {}

    /**
     * 服务端每 tick：结算读条到期的火炮。
     *
     * <p>WHY 需要这段而不仅仅是 R 键入口：《武器/09-装填类似弩模型》规定"读条完成时才从背包
     * 消耗足量弹药，并写入 LOADED_AMMO"——读条走完就该装好，不该要求玩家再按一次 R。
     * 2026-09-26 的 a48acb7f「固定关闭火炮自动装填」把这里的 COMPLETE 结算连同"自动启动"
     * 一起删掉了，导致读条满了仍是空膛（与 docs/1.0/测试计划.md B.2.2「已装弹 → 再按 R →
     * 提示已装弹」也对不上）。本方法只恢复"到期结算"，启动依然只能由 R 键触发。
     */
    public static void tickCannonAutoReload(Player player, ItemStack coreStack) {
        if (player.level().isClientSide() || !player.isAlive() || player.isSpectator() || coreStack.isEmpty()) return;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            tickSlot(player, coreStack, slot, inventory.items.get(slot));
        }
        tickSlot(player, coreStack, 40, inventory.offhand.get(0));
    }

    private static void tickSlot(Player player, ItemStack core, int slot, ItemStack weapon) {
        if (!(weapon.getItem() instanceof ArtilleryItem)) return;
        int coreSlot = findCoreSlotIndex(player.getInventory(), player, slot);
        if (coreSlot == -1) return;
        WeaponState state = new WeaponState(weapon);
        boolean loaded = isCannonReadyToFire(weapon, player.level());
        WeaponCooldown cooldown = state.getCooldown();
        // automatic 固定传 false：本方法只做"读条到期结算"，不启动装填（启动只能由 R 键触发），
        // 所以 START 分支在这里不可达。
        CannonReloadPhase phase = CannonReloadPhase.resolve(loaded, false,
                cooldown == null ? null : cooldown.endTick(), player.level().getGameTime());
        boolean changed = switch (phase) {
            case LOADED -> clearCannonReloadState(core, weapon, slot);
            case IDLE, START, WAIT -> false;
            case COMPLETE -> completeCannonReload(player, core, player.getInventory(), slot, coreSlot, weapon);
        };
        if (changed) TransformationManager.writeCoreToConfiguredSlot(player, core);
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
        if (!(weapon.getItem() instanceof ArtilleryItem)
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
        int ticks = TransformationManager.boostedCooldown(player, getGunCooldown(player, weapon, player.level()));
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
