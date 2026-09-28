package com.piranport.item;

import com.piranport.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 经验炮弹：只要在玩家背包中，就提供经验提升效果。
 *
 * <p>经验炮弹不是武器强化材料，不修改伤害、爆炸威力、冷却、舰载机面板或装备耐久。
 * 下面保留的数值方法仅用于兼容仍在迁移中的旧战斗调用点，始终返回基础值。</p>
 */
public class ExperienceShellItem extends TooltipItem {
    public ExperienceShellItem(Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    /** 玩家主背包（含快捷栏）中是否持有经验炮弹。 */
    public static boolean hasInInventory(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.EXP_SHELL.get())) {
                return true;
            }
        }
        return false;
    }

    /** 经验炮弹不再提供武器强化等级。 */
    public static int getPlayerEnhancementLevel(Player player) {
        return 0;
    }

    /** 保留旧接口，经验炮弹本身没有装备强化等级。 */
    public static int getEnhancementLevel(ItemStack stack) {
        return 0;
    }

    /** 兼容旧战斗入口：经验炮弹不改变伤害。 */
    public static float applyDamageBonus(Player player, float baseDamage) {
        return baseDamage;
    }

    /** 兼容旧战斗入口：经验炮弹不改变伤害。 */
    public static float applyDamageBonus(ItemStack stack, float baseDamage) {
        return baseDamage;
    }

    /** 兼容旧战斗入口：经验炮弹不改变爆炸威力。 */
    public static float applyExplosionBonus(Player player, float baseExplosion) {
        return baseExplosion;
    }

    /** 兼容旧战斗入口：经验炮弹不改变爆炸威力。 */
    public static float applyExplosionBonus(ItemStack stack, float baseExplosion) {
        return baseExplosion;
    }

    /** 兼容旧战斗入口：经验炮弹不改变冷却时间。 */
    public static int applyCooldownReduction(Player player, int baseTicks) {
        return baseTicks;
    }

    /** 兼容旧战斗入口：经验炮弹不改变舰载机面板伤害。 */
    public static float applyAircraftPanelDamageBonus(Player player, float baseDamage) {
        return baseDamage;
    }

    /** 兼容旧战斗入口：经验炮弹不改变舰载机面板速度。 */
    public static float applyAircraftPanelSpeedBonus(Player player, float baseSpeed) {
        return baseSpeed;
    }

    /** 旧装备强化提示入口保留为空，避免其他武器显示已不存在的强化效果。 */
    public static void appendEnhancementTooltip(ItemStack stack, List<Component> tooltipComponents) {
        // Intentionally empty: the shell only grants the experience effect.
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
