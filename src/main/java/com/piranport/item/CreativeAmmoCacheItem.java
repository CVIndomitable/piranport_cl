package com.piranport.item;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.component.CreativeAmmoSelection;
import com.piranport.platform.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 创造模式弹药库（调试工具）。
 *
 * <p>放在玩家物品栏任意位置时，只有 GUI 中勾选的弹药/燃料会被视为数量无限；
 * 未勾选的条目仍按真实库存结算。右键打开勾选界面。</p>
 *
 * <p>这是测试工具：没有生存配方，也不参与战斗数值，只负责给"从背包读弹药"的系统
 * 提供一个可控的无限来源。</p>
 */
public class CreativeAmmoCacheItem extends Item {

    public CreativeAmmoCacheItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            ClientHooks.openCreativeAmmoCacheScreen(hand);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !CreativeAmmoCache.selection(stack).itemIds().isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CreativeAmmoSelection selection = CreativeAmmoCache.selection(stack);
        tooltipComponents.add(Component.translatable(
                        "tooltip.piranport.creative_ammo_cache.selected", selection.itemIds().size())
                .withStyle(ChatFormatting.AQUA));
        tooltipComponents.add(Component.translatable(
                        "tooltip.piranport.creative_ammo_cache.use")
                .withStyle(ChatFormatting.GRAY));
    }
}
