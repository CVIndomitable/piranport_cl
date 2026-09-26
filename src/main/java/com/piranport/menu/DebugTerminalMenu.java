package com.piranport.menu;

import com.piranport.config.ConfigToolPermissions;
import com.piranport.registry.ModMenuTypes;
import com.piranport.terminal.TerminalParametersSavedData;
import com.piranport.terminal.TerminalOverridesSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 调试终端菜单（服务端逻辑）
 *
 * 纯配置界面、无物品槽位；打开时推送当前存档的权威参数快照。
 */
public class DebugTerminalMenu extends AbstractContainerMenu {

    /** 客户端构造函数（从网络） */
    public DebugTerminalMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    /** 服务端构造函数 */
    public DebugTerminalMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.DEBUG_TERMINAL_MENU.get(), containerId);

        if (playerInventory.player instanceof net.minecraft.server.level.ServerPlayer sp) {
            TerminalOverridesSavedData data = TerminalOverridesSavedData.get(sp.serverLevel());
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp,
                    com.piranport.network.SyncTerminalOverridesPayload.from(data));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp,
                    com.piranport.network.SyncTerminalParametersPayload.from(
                            TerminalParametersSavedData.get(sp.serverLevel()), ""));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (player.level().isClientSide()) {
            return true;
        }
        // 与火炮配置工具一致：联机掉权限后菜单自动关闭，避免开着的终端继续改数。
        return ConfigToolPermissions.canUse(player);
    }
}
