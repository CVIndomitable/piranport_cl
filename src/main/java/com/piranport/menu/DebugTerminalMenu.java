package com.piranport.menu;

import com.piranport.config.ConfigToolPermissions;
import com.piranport.registry.ModMenuTypes;
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
 * <p>与 {@link ArtilleryConfigToolMenu} 同构：纯配置界面、无物品槽位，
 * 真正的读写都通过自定义网络包在服务端完成，这里只负责权限校验和打开时推一次快照。
 *
 * <p>WHY 打开时就要推快照：客户端 {@code TerminalOverrides} 是服务端覆盖值的只读镜像，
 * 只在 sync 包到达时更新。不在这里推一次，玩家第一次开终端会看到一份过期数据
 * （上次同步后可能已经被重置/被其他终端改过）。
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
