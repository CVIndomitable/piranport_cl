package com.piranport.combat;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.TorpedoItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

/**
 * 装填系统工具类 — 处理鱼雷发射器和导弹发射器的手动装填逻辑。
 */
public class ReloadHelper {

    /**
     * 从背包装填鱼雷发射器（策划决策/武器/鱼雷-再装填机制，2026-10-19 修订）。
     *
     * <p>开读条时只检查同口径鱼雷数量，不扣物品；读到 {@code durationTicks} 结束后由
     * {@code WeaponReloadLifecycle} 统一扣除。无强化 3000 tick、有强化 400 tick。</p>
     *
     * @param player      玩家
     * @param inv         玩家背包
     * @param launcherStack 发射器物品
     * @param weaponSlot  发射器所在槽位（0-8 主手，40 副手）
     * @param coreStack   舰装核心物品
     * @param coreSlot    核心所在槽位
     */
    public static void reloadTorpedoLauncher(Player player, Inventory inv, ItemStack launcherStack,
                                              int weaponSlot, ItemStack coreStack, int coreSlot,
                                              int durationTicks, boolean showStartMessage) {
        // P1修复: 添加服务器端验证
        if (player.level().isClientSide()) return;

        if (!(launcherStack.getItem() instanceof TorpedoLauncherItem launcher)) return;

        int tubeCount = launcher.getTubeCount();
        int caliber = launcher.getCaliber();

        // 检查是否已满载
        LoadedAmmo current = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (current.hasAmmo() && current.count() >= tubeCount) {
            WeaponCooldown wc = launcherStack.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
            if (wc.isOnCooldown(player.level().getGameTime())) {
                player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
            }
            return;
        }

        // 查找第一个匹配的鱼雷类型（严格模式：只消耗同类型物品）
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
        // 背包里没有实体鱼雷时，使用弹药库勾选的同口径鱼雷（不消耗实体物品）。
        boolean infinite = false;
        if (torpedoType == null) {
            torpedoType = CreativeAmmoCache.findInfiniteTorpedo(player, caliber);
            infinite = torpedoType != null;
        }
        if (torpedoType == null) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }
        if (!infinite) infinite = CreativeAmmoCache.isInfinite(player, torpedoType);

        // 管内残留旧弹种时禁止混装：残留弹的 LoadedAmmo 是按旧 ammoId 记的，
        // 直接覆盖成新弹种会让库存里的旧鱼雷"静默变成"新鱼雷（既不消耗也不退还）。
        String ammoId = BuiltInRegistries.ITEM.getKey(torpedoType).toString();
        if (current.hasAmmo() && !ammoId.equals(current.ammoItemId())) {
            player.displayClientMessage(Component.translatable("message.piranport.must_be_empty_to_reload"), true);
            return;
        }

        // 统计可用弹药数量
        int needed = tubeCount - (current.hasAmmo() ? current.count() : 0);
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

        if (!infinite && available < needed) {
            player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
            return;
        }

        // 读条期间不扣物品：只记下弹种和待装数量，到期由 tick 统一结算（2026-10-19 修订）。
        launcherStack.set(ModDataComponents.PENDING_TORPEDO_RELOAD.get(),
                new com.piranport.component.PendingTorpedoReload(needed, ammoId));

        // 设置读条时间。
        // WHY 只写武器栈的 WEAPON_COOLDOWN、不写船核心栈的 SLOT_COOLDOWNS：
        // ShipCoreCombat 的开火前检查只读 SLOT_COOLDOWNS，写了它会让"刚装填完"的武器被静默拦截
        // 5-7 秒（无任何提示），即玩家报告的"显示装填了但无法发射"。
        // 保留 WEAPON_COOLDOWN 是为了让 ReloadProgressHudLayer 继续画装填进度条。
        int cooldownTicks = TransformationManager.boostedCooldown(player, durationTicks);
        launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                WeaponCooldown.of(player.getUUID(), player.level().getGameTime(), cooldownTicks));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        if (showStartMessage) {
            player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
        }
    }

    /** R 键给防空导弹发射器装入一枚弹药。 */
    public static void reloadAntiAirMissile(Player player, Inventory inv, ItemStack launcherStack,
                                            int weaponSlot, ItemStack coreStack, int coreSlot,
                                            com.piranport.item.MissileLauncherItem launcher) {
        if (player.level().isClientSide() || !player.isAlive()) return;
        var cooldown = launcherStack.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
        if (cooldown.isOnCooldown(player.level().getGameTime())) {
            player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            return;
        }
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loaded.hasAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
            return;
        }
        Item ammo = launcher.getAmmoItem();
        boolean infinite = CreativeAmmoCache.isInfinite(player, ammo);
        int ammoSlot = findAmmoSlot(inv, ammo, weaponSlot, coreSlot);
        if (ammoSlot < 0 && !player.getAbilities().instabuild && !infinite) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }
        if (ammoSlot >= 0 && !player.getAbilities().instabuild && !infinite) {
            ItemStack source = ammoSlot == 40 ? inv.offhand.get(0) : inv.items.get(ammoSlot);
            com.piranport.testtools.PiranPortTestTools.consumeAmmo(player.getUUID(), source, 1);
        }
        String ammoId = BuiltInRegistries.ITEM.getKey(ammo).toString();
        launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(1, ammoId));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
    }

    /** R 键给深弹发射器装入完整一轮。 */
    public static void reloadDepthChargeLauncher(Player player, Inventory inv, ItemStack launcherStack,
                                                 int weaponSlot, int coreSlot,
                                                 com.piranport.item.DepthChargeLauncherItem launcher) {
        if (player.level().isClientSide() || !player.isAlive()) return;
        var cooldown = launcherStack.getOrDefault(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.EMPTY);
        if (cooldown.isOnCooldown(player.level().getGameTime())) {
            player.displayClientMessage(Component.translatable("message.piranport.already_reloading"), true);
            return;
        }
        LoadedAmmo loaded = launcherStack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loaded.hasAmmo()) {
            player.displayClientMessage(Component.translatable("message.piranport.already_loaded"), true);
            return;
        }
        Item ammo = com.piranport.registry.ModItems.DEPTH_CHARGE.get();
        int needed = launcher.getChargeCount();
        boolean infinite = CreativeAmmoCache.isInfinite(player, ammo);
        int available = countAmmo(inv, ammo, weaponSlot, coreSlot);
        if (available < needed && !player.getAbilities().instabuild && !infinite) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
            return;
        }
        if (!player.getAbilities().instabuild && !infinite) {
            consumeAmmo(inv, ammo, needed, weaponSlot, coreSlot, player.getUUID());
        }
        launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(needed,
                BuiltInRegistries.ITEM.getKey(ammo).toString()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
    }

    private static int findAmmoSlot(Inventory inv, Item ammo, int weaponSlot, int coreSlot) {
        for (int i = 0; i < inv.items.size(); i++) {
            if (i == weaponSlot || i == coreSlot) continue;
            if (!inv.items.get(i).isEmpty() && inv.items.get(i).is(ammo)) return i;
        }
        if (weaponSlot != 40 && coreSlot != 40 && inv.offhand.get(0).is(ammo)) return 40;
        return -1;
    }

    private static int countAmmo(Inventory inv, Item ammo, int weaponSlot, int coreSlot) {
        int count = 0;
        for (int i = 0; i < inv.items.size(); i++) {
            if (i != weaponSlot && i != coreSlot && inv.items.get(i).is(ammo)) count += inv.items.get(i).getCount();
        }
        if (weaponSlot != 40 && coreSlot != 40 && inv.offhand.get(0).is(ammo)) count += inv.offhand.get(0).getCount();
        return count;
    }

    private static void consumeAmmo(Inventory inv, Item ammo, int count, int weaponSlot, int coreSlot,
                                    java.util.UUID owner) {
        int remaining = count;
        for (int i = 0; i < inv.items.size() && remaining > 0; i++) {
            if (i == weaponSlot || i == coreSlot) continue;
            ItemStack stack = inv.items.get(i);
            if (!stack.is(ammo)) continue;
            int take = Math.min(remaining, stack.getCount());
            com.piranport.testtools.PiranPortTestTools.consumeAmmo(owner, stack, take);
            remaining -= take;
        }
        if (remaining > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack stack = inv.offhand.get(0);
            if (stack.is(ammo)) com.piranport.testtools.PiranPortTestTools.consumeAmmo(owner, stack, remaining);
        }
    }
}
