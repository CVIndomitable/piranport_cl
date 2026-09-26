package com.piranport.item;

import com.piranport.config.ConfigToolPermissions;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.debug.PiranPortDebug;
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
 * 调试终端 - 管理员专用
 *
 * <p>提供服务端运行参数的编辑页，以及调试开关和快照控制页。
 *
 * <p>权限沿用 {@link ConfigToolPermissions}：单机下房主即管理员，联机需要 OP 2 级。
 */
public class DebugTerminalItem extends Item {

    public DebugTerminalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (!ConfigToolPermissions.canUse(serverPlayer)) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.piranport.debug_terminal.admin_required")
                                .withStyle(ChatFormatting.RED));
                return InteractionResultHolder.fail(stack);
            }

            if (serverPlayer.isShiftKeyDown()) {
                long cooldownMs = PiranPortDebug.snapshot(serverPlayer);
                serverPlayer.sendSystemMessage(Component.translatable(
                        cooldownMs > 0
                                ? "message.piranport.snapshot.cooldown"
                                : "message.piranport.snapshot.done"));
                return InteractionResultHolder.sidedSuccess(stack, false);
            }

            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new DebugTerminalMenu(id, inv),
                    Component.translatable("gui.piranport.debug_terminal")
            ));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.debug_terminal.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.piranport.debug_terminal.admin_only")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
    }
}
