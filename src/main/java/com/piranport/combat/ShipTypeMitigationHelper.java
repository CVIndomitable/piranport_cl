package com.piranport.combat;

import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 船型职能分化 — 策划决策/数值/05-船型职能分化修订.md
 *
 * <p>小型船（{@link ShipType#SMALL}）受到以下减免：</p>
 * <ul>
 *   <li>大口径 AP 过穿：仅造成 AP 标伤的 5%</li>
 *   <li>AP 或 HE 单次伤害封顶：最大血量的 1/4（20 血 → 5 点）</li>
 * </ul>
 *
 * <p>判定对象 = 当前变身核心为 SMALL 的玩家；非玩家目标不受船型减免。</p>
 */
public final class ShipTypeMitigationHelper {

    private ShipTypeMitigationHelper() {}

    /**
     * 是否应套用"小船被动免伤"修饰。
     * @return true = 目标为变身 SMALL 的玩家
     */
    public static boolean isSmallTransformedPlayer(LivingEntity target) {
        if (!(target instanceof Player player)) return false;
        if (!TransformationManager.isPlayerTransformed(player)) return false;
        ItemStack core = TransformationManager.findTransformedCore(player);
        if (core.isEmpty() || !(core.getItem() instanceof ShipCoreItem sci)) return false;
        return sci.getShipType() == ShipType.SMALL;
    }

    /**
     * 计算 AP 大口径对小型船的过穿伤害（标伤 5%）。
     * @param baseApDamage AP 直击伤害计算结果
     * @param caliber 口径分类（>8 为大口径；<=4 小；4-8 中）
     */
    public static float applyLargeApOverpen(float baseApDamage, int caliber) {
        if (caliber > 8) return baseApDamage * 0.05f;
        return baseApDamage;
    }

    /**
     * 计算小型船受 AP/HE 单次伤害封顶：最大血量的 1/4。
     * 注：AP 已先走过穿判定（applyLargeApOverpen）后才进入此步。
     */
    public static float capSmallShipDamage(float incoming, float maxHealth) {
        float cap = maxHealth * 0.25f;
        return Math.min(incoming, cap);
    }
}