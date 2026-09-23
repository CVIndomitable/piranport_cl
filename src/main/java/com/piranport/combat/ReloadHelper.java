package com.piranport.combat;

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

/**
 * 装填系统工具类 — 处理鱼雷发射器和导弹发射器的手动装填逻辑。
 */
public class ReloadHelper {

    /**
     * 从背包装填鱼雷发射器（需要装备"鱼雷再装填"增强）。
     *
     * @param player      玩家
     * @param inv         玩家背包
     * @param launcherStack 发射器物品
     * @param weaponSlot  发射器所在槽位（0-8 主手，40 副手）
     * @param coreStack   舰装核心物品
     * @param coreSlot    核心所在槽位
     */
    public static void reloadTorpedoLauncher(Player player, Inventory inv, ItemStack launcherStack,
                                              int weaponSlot, ItemStack coreStack, int coreSlot) {
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
        if (torpedoType == null) {
            player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
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

        if (available < needed) {
            player.displayClientMessage(Component.translatable("message.piranport.insufficient_same_ammo"), true);
            return;
        }

        // 消耗弹药
        int toConsume = needed;
        for (int i = 0; i < inv.items.size() && toConsume > 0; i++) {
            if (i == coreSlot || i == weaponSlot) continue;
            ItemStack s = inv.items.get(i);
            if (!s.isEmpty() && s.getItem() == torpedoType) {
                int take = Math.min(toConsume, s.getCount());
                s.shrink(take);
                toConsume -= take;
            }
        }
        if (toConsume > 0 && weaponSlot != 40 && coreSlot != 40) {
            ItemStack oh = inv.offhand.get(0);
            if (!oh.isEmpty() && oh.getItem() == torpedoType) {
                int take = Math.min(toConsume, oh.getCount());
                oh.shrink(take);
                toConsume -= take;
            }
        }

        // 设置装填状态
        String ammoId = BuiltInRegistries.ITEM.getKey(torpedoType).toString();
        launcherStack.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(tubeCount, ammoId));

        // 设置冷却时间。
        // WHY 只写武器栈的 WEAPON_COOLDOWN、不写船核心栈的 SLOT_COOLDOWNS：
        // ShipCoreCombat 的开火前检查只读 SLOT_COOLDOWNS，写了它会让"刚装填完"的武器被静默拦截
        // 5-7 秒（无任何提示），即玩家报告的"显示装填了但无法发射"。
        // 保留 WEAPON_COOLDOWN 是为了让 WeaponReloadDecorator 继续画装填进度条、
        // 并让上面的 already_reloading 分支仍能区分"装填中"与"已装填"。
        int cooldownTicks = TransformationManager.boostedCooldown(player, launcher.getCooldownTicks());
        launcherStack.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                WeaponCooldown.of(player.getUUID(), player.level().getGameTime(), cooldownTicks));

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5f, 1.4f);
        player.displayClientMessage(Component.translatable("message.piranport.reload_start"), true);
    }
}
