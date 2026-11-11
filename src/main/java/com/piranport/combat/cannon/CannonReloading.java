package com.piranport.combat.cannon;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.data.AmmoInventory;
import com.piranport.combat.data.WeaponState;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.piranport.combat.cannon.CannonAmmoRules.isCannonReadyToFire;
import static com.piranport.combat.cannon.CannonInventory.findCoreSlotIndex;
import static com.piranport.combat.cannon.CannonStats.getBarrelCount;
import static com.piranport.combat.cannon.CannonStats.getGunCooldown;

/**
 * 火炮装填生命周期：武器保存唯一读条，HUD 和提示直接读取武器。
 * 所有火炮空膛时由服务端 tick 自动启动，到期结算并扣弹。
 */
public final class CannonReloading {

    private CannonReloading() {}

    /**
     * 服务端每 tick 启动空膛火炮的读条，并结算所有到期读条。
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
        CannonReloadPhase phase = CannonReloadPhase.resolve(loaded,
                cooldown == null ? null : cooldown.endTick(), player.level().getGameTime());
        boolean changed = switch (phase) {
            case LOADED -> clearCannonReloadState(core, weapon, slot);
            case START -> startCannonReloadIfPossible(player, core, player.getInventory(), slot, coreSlot, weapon);
            case WAIT -> false;
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
        Item type = ammo.chooseReloadAmmo(weapon, getBarrelCount(weapon, player.level()), player.level());
        if (type == null) return clearCannonReloadState(core, weapon, weaponSlot);
        ammo.recordAmmoType(weapon, type);
        startTimer(player, core, weaponSlot, weapon);
        CannonSounds.playCannonReloadStartSound(player, weapon);
        return true;
    }

    /**
     * 预览下一发装填的弹种，与开始/结算读条走同一套选弹逻辑，供火炮提示显示。
     * <p>WHY 不能只看 SELECTED_AMMO_TYPE：偏好弹种库存不足时会回退到背包顺序，只有这里的结果才是实际会装的弹种。
     * @return 下一发弹种；无核心、不在玩家背包或背包里没有可用弹药时返回 null
     */
    @Nullable
    public static Item previewNextReloadAmmo(Player player, ItemStack weapon) {
        Inventory inventory = player.getInventory();
        int slot = CannonInventory.findWeaponSlot(inventory, weapon);
        if (slot < 0) return null;
        int coreSlot = findCoreSlotIndex(inventory, player, slot);
        if (coreSlot == -1) return null;
        return new AmmoInventory(inventory, coreSlot, slot).chooseReloadAmmo(
                weapon, getBarrelCount(weapon, player.level()), player.level());
    }

    private static boolean completeCannonReload(Player player, ItemStack core, Inventory inventory,
            int weaponSlot, int coreSlot, ItemStack weapon) {
        int barrels = getBarrelCount(weapon, player.level());
        AmmoInventory ammo = new AmmoInventory(inventory, coreSlot, weaponSlot);
        Item type = ammo.chooseReloadAmmo(weapon, barrels, player.level());
        if (type == null || !ammo.consumeAmmo(player.getUUID(), type, barrels)) {
            return clearCannonReloadState(core, weapon, weaponSlot);
        }
        new WeaponState(weapon).setLoadedAmmo(barrels, BuiltInRegistries.ITEM.getKey(type).toString());
        clearCannonReloadState(core, weapon, weaponSlot);
        ammo.recordAmmoType(weapon, type);
        CannonSounds.playCannonReloadCompleteSound(player, weapon);
        return true;
    }

    /** 切弹只重启未完成读条；已装弹保持原来的装填状态。 */
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
