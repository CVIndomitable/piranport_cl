package com.piranport.item;

import com.piranport.menu.ArtilleryConfigToolMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 火炮配置工具 - 创造模式专用
 *
 * <p>右键使用打开GUI，可在游戏内调整火炮和弹药数值。
 * <p>修改仅在当前存档生效，可导出为CSV文件。
 */
public class ArtilleryConfigToolItem extends Item {

    public ArtilleryConfigToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // 打开配置GUI
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new ArtilleryConfigToolMenu(id, inv),
                    Component.translatable("gui.piranport.artillery_config_tool")
            ));
        }

        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.artillery_config_tool.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.piranport.artillery_config_tool.creative_only")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}
