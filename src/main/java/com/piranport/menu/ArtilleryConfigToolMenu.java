package com.piranport.menu;

import com.piranport.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 火炮配置工具菜单（服务端逻辑）
 *
 * <p>处理GUI的服务端逻辑，包括按钮点击、数据同步等。
 */
public class ArtilleryConfigToolMenu extends AbstractContainerMenu {

    // 按钮ID定义
    public static final int BTN_TAB_CANNONS = 0;
    public static final int BTN_TAB_PROJECTILES = 1;
    public static final int BTN_EXPORT_CSV = 100;
    public static final int BTN_RESET_ALL = 102;

    // 数据同步（用于标签页状态等）
    private final ContainerData data;

    /**
     * 客户端构造函数（从网络）
     */
    public ArtilleryConfigToolMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainerData(1));
    }

    /**
     * 服务端构造函数
     */
    public ArtilleryConfigToolMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(1));
    }

    /**
     * 通用构造函数
     */
    private ArtilleryConfigToolMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenuTypes.ARTILLERY_CONFIG_TOOL_MENU.get(), containerId);
        this.data = data;

        // 添加数据槽（用于同步）
        addDataSlots(data);

        // 注意：此Menu不包含物品槽位，纯配置界面
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        // 无物品槽位，返回空
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // 仅创造模式可用
        return player.isCreative();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id == BTN_TAB_CANNONS) {
            // 切换到火炮标签页
            data.set(0, 0);
            return true;
        } else if (id == BTN_TAB_PROJECTILES) {
            // 切换到弹药标签页
            data.set(0, 1);
            return true;
        } else if (id == BTN_EXPORT_CSV) {
            // 导出CSV（实际通过网络包处理）
            return true;
        } else if (id == BTN_RESET_ALL) {
            // 重置全部（实际通过网络包处理）
            return true;
        }
        return false;
    }

    /**
     * 获取当前标签页索引
     */
    public int getCurrentTab() {
        return data.get(0);
    }
}
