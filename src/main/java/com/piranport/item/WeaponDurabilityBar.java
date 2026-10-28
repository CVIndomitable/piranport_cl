package com.piranport.item;

import net.minecraft.world.item.ItemStack;

/**
 * 武器物品的耐久条显示规则。
 *
 * <p>WHY：武器图标底部通常画着弹种/装填/空膛警告等状态（例如鱼雷发射器的黄色警告三角、
 * 火炮的弹种条），原版耐久条正好画在同一位置，会把它们盖住。武器平时不显示耐久条，
 * 只有耐久真正归零（即将损坏）时才显示一条空耐久条，既保留"该换了"的提示，又不遮挡状态图标。
 * 剩余耐久数值仍可在物品 tooltip 查看。</p>
 */
public final class WeaponDurabilityBar {
    private WeaponDurabilityBar() {}

    /**
     * 武器是否已经损坏（耐久只剩最后一点，不能再使用）。
     *
     * <p>火炮原本就用「damage >= 最大耐久 - 1」作为损坏判定；鱼雷/深弹发射器现在也统一
     * 停在这个状态，不再把物品直接清空，所以所有武器可以共用同一条判据。</p>
     */
    public static boolean isBroken(ItemStack stack) {
        return stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    /** 仅在武器损坏时显示原版耐久条（此时图标底部状态已被玩家看过，空条用来提示该换了）。 */
    public static boolean showWhenBroken(ItemStack stack) {
        return isBroken(stack);
    }
}
