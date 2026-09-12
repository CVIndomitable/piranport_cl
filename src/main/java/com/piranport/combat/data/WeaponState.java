package com.piranport.combat.data;

import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.WeaponCooldown;
import com.piranport.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 武器状态封装器 — 统一管理武器物品的冷却、装填弹药和偏好弹种。
 * <p>提供流畅的 API，隐藏底层 DataComponent 读写细节。</p>
 */
public class WeaponState {

    private final ItemStack weapon;

    /**
     * 构造武器状态封装器。
     * @param weapon 武器物品堆叠（火炮/鱼雷发射器）
     */
    public WeaponState(ItemStack weapon) {
        this.weapon = weapon;
    }

    /**
     * 获取武器装填冷却状态。
     * @return 冷却对象，若无冷却返回 null
     */
    @Nullable
    public WeaponCooldown getCooldown() {
        return weapon.get(ModDataComponents.WEAPON_COOLDOWN.get());
    }

    /**
     * 设置武器装填冷却（开火后调用）。
     * @param currentTick 当前游戏 tick
     * @param ticks 冷却时长（自动应用测试工具覆盖）
     */
    public void setCooldown(long currentTick, int ticks) {
        weapon.set(ModDataComponents.WEAPON_COOLDOWN.get(), WeaponCooldown.of(currentTick, ticks));
    }

    /**
     * 清除武器冷却状态（手动装填完成/切换模式时调用）。
     */
    public void clearCooldown() {
        weapon.remove(ModDataComponents.WEAPON_COOLDOWN.get());
    }

    /**
     * 判断武器是否处于冷却中。
     * @param currentTick 当前游戏 tick
     * @return true 表示仍在冷却，不可发射
     */
    public boolean isOnCooldown(long currentTick) {
        WeaponCooldown cd = getCooldown();
        return cd != null && cd.isOnCooldown(currentTick);
    }

    /**
     * 获取手动装填模式下已装填的弹药。
     * @return 装填数据（数量+弹种ID），未装填返回 EMPTY
     */
    public LoadedAmmo getLoadedAmmo() {
        return weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
    }

    /**
     * 设置手动装填的弹药（手动补给模式）。
     * @param count 装填数量（通常为管数）
     * @param ammoItemId 弹药物品注册 ID（如 "piranport:medium_ap_shell"）
     */
    public void setLoadedAmmo(int count, String ammoItemId) {
        weapon.set(ModDataComponents.LOADED_AMMO.get(), new LoadedAmmo(count, ammoItemId));
    }

    /**
     * 清空已装填弹药（发射后/切换弹种时调用）。
     */
    public void clearLoadedAmmo() {
        weapon.remove(ModDataComponents.LOADED_AMMO.get());
    }

    /**
     * 判断是否已装填弹药（手动模式）。
     * @return true 表示有弹药可发射
     */
    public boolean hasLoadedAmmo() {
        return getLoadedAmmo().hasAmmo();
    }

    /**
     * 获取玩家选定的偏好弹种（自动装填模式）。
     * @return 偏好弹种记录，未选择返回 EMPTY
     */
    public SelectedAmmoType getSelectedAmmoType() {
        return weapon.getOrDefault(ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
    }

    /**
     * 设置玩家偏好弹种（自动装填模式下优先消耗此弹种）。
     * @param ammoItemId 弹药物品注册 ID，空字符串表示清除偏好
     */
    public void setSelectedAmmoType(String ammoItemId) {
        if (ammoItemId == null || ammoItemId.isEmpty()) {
            weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        } else {
            weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), new SelectedAmmoType(ammoItemId));
        }
    }

    /**
     * 获取底层武器物品堆叠（用于链式调用/直接访问）。
     * @return 武器 ItemStack
     */
    public ItemStack getWeapon() {
        return weapon;
    }
}
