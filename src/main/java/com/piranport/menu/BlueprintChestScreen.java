package com.piranport.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BlueprintChestScreen extends AbstractContainerScreen<BlueprintChestMenu> {
    public BlueprintChestScreen(BlueprintChestMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = 120;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;

        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.blueprint_chest.tab_view"),
                b -> getMenu().setCurrentTab(0))
                .bounds(x + 7, y + 4, 50, 12)
                .build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.blueprint_chest.tab_copy"),
                b -> getMenu().setCurrentTab(1))
                .bounds(x + 60, y + 4, 50, 12)
                .build());
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int tab = getMenu().getCurrentTab();

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // 高亮当前 tab
        int activeX = x + 7 + tab * 53;
        g.fill(activeX - 1, y + 3, activeX + 51, y + 17, 0xFF202020);

        if (tab == 0) {
            g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.cache"),
                    x + 8, y + 22, 0xFF404040, false);
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 7 + col * 18, y + 33);
            }
            g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.paper"),
                    x + 70, y + 56, 0xFF404040, false);
            drawSlotBg(g, x + 79, y + 66);
        } else {
            g.drawString(font, Component.translatable("gui.piranport.blueprint_chest.copy"),
                    x + 8, y + 22, 0xFF404040, false);
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 7 + col * 18, y + 33);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 7 + col * 18, y + 131 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 7 + col * 18, y + 189);
        }
    }

    /**
     * 修复：BlueprintSlot 与 CopySlot 注册在完全相同的坐标（8+col*18, 34），
     * 而原版私有 findSlot 返回 slots 列表里第一个命中的槽位——BlueprintSlot 先注册，
     * 导致复印 tab 下点击永远命中蓝图槽，CopySlot 的取出逻辑永远不会触发。
     * findSlot 是 private 无法覆写，因此在 slotClicked（protected）里做下标重映射：
     * 复印 tab 下命中蓝图槽下标(0~8)时，实际向服务端发送对应 CopySlot 的下标(+10)。
     *
     * <p>只重映射「取件」类点击。CopySlot 是投影槽：getItem() 每次返回预览副本，
     * set()/remove() 都不经过容器。原版 SWAP（数字键/中键换入快捷栏）分支不调用
     * remove()，而是直接把 slot.getItem() 塞进快捷栏再 setByPlayer(EMPTY)——对
     * CopySlot 来说就是「白拿一份、不扣纸」。所以 SWAP/PICKUP_ALL/CLONE 必须留在
     * 真正的蓝图槽上（那是玩家真实物品），不能重映射。</p>
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType type) {
        if (slot != null && getMenu().getCurrentTab() == 1
                && slot.index >= 0 && slot.index < 9
                && (type == net.minecraft.world.inventory.ClickType.PICKUP
                        || type == net.minecraft.world.inventory.ClickType.QUICK_MOVE
                        || type == net.minecraft.world.inventory.ClickType.THROW)) {
            this.minecraft.gameMode.handleInventoryMouseClick(
                    this.menu.containerId, slot.index + BlueprintChestMenu.COPY_SLOT_START,
                    mouseButton, type, this.minecraft.player);
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFE0E0E0);
    }

    /**
     * 重写 render，手动控制槽位渲染：仅渲染与当前 tab 匹配的槽位（蓝图/Paper 或 Copy）。
     * 玩家物品栏始终可见。
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        renderBg(g, partialTick, mouseX, mouseY);

        int tab = getMenu().getCurrentTab();
        int idx = 0;
        for (Slot slot : menu.slots) {
            boolean visible;
            if (idx < 10) visible = tab == 0;
            else if (idx < 19) visible = tab == 1;
            else visible = true;
            idx++;
            if (visible) {
                renderSlotContents(g, slot);
            }
        }

        // 按钮和 tooltip
        for (net.minecraft.client.gui.components.Renderable r : new java.util.ArrayList<>(this.renderables)) {
            r.render(g, mouseX, mouseY, partialTick);
        }
        renderTooltip(g, mouseX, mouseY);
    }

    private void renderSlotContents(GuiGraphics g, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) return;
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        g.pose().pushPose();
        try {
            // 简单绘制：使用 itemRenderer
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            g.renderItem(stack, x, y);
            g.renderItemDecorations(mc.font, stack, x, y, null);
        } catch (Exception ignored) {
            // 渲染失败时跳过
        }
        g.pose().popPose();
    }
}