package com.piranport.menu;

import com.piranport.block.entity.BlueprintChestBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

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
     * BlueprintSlot 与 CopySlot 注册在完全相同的坐标（8+col*18, 34）。槽位显隐由各自的
     * {@code isActive()} 按标签页决定，原版 findSlot 会跳过隐藏槽，因此两页互不干扰。
     *
     * <p>这里再做一层下标兜底：CopySlot 是投影槽，getItem() 每次返回预览副本，
     * set()/remove() 都不经过容器。原版 SWAP（数字键/中键换入快捷栏）分支不调用
     * remove()，而是直接把 slot.getItem() 塞进快捷栏再 setByPlayer(EMPTY)——对
     * CopySlot 来说就是「白拿一份、不扣纸」。所以非取件类点击必须落回真正的蓝图槽。</p>
     */
    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null && getMenu().getCurrentTab() == 1) {
            // 取件类点击命中蓝图槽（旧路径/直接调用）→ 转发到对应的 CopySlot
            if (slot.index >= 0 && slot.index < BlueprintChestBlockEntity.BLUEPRINT_SLOTS
                    && isTakeClick(type)) {
                sendClick(slot.index + BlueprintChestMenu.COPY_SLOT_START, mouseButton, type);
                return;
            }
            // 非取件类点击命中 CopySlot（isActive 生效后的常规路径）→ 转回真实蓝图槽
            if (slot.index >= BlueprintChestMenu.COPY_SLOT_START
                    && slot.index < BlueprintChestMenu.PLAYER_INV_START
                    && !isTakeClick(type)) {
                sendClick(slot.index - BlueprintChestMenu.COPY_SLOT_START, mouseButton, type);
                return;
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    /** 取件类点击：会走 CopySlot#remove 从而扣纸。其余类型必须在真实蓝图槽上执行。 */
    private static boolean isTakeClick(ClickType type) {
        return type == ClickType.PICKUP || type == ClickType.QUICK_MOVE || type == ClickType.THROW;
    }

    private void sendClick(int slotIndex, int mouseButton, ClickType type) {
        this.minecraft.gameMode.handleInventoryMouseClick(
                this.menu.containerId, slotIndex, mouseButton, type, this.minecraft.player);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFFE0E0E0);
    }

    /**
     * 与原版箱子一致：super.render 负责 renderBackground（内部已调 renderBg，背景只画一次）、
     * 槽位、按钮和 hoveredSlot；tooltip 由子类补一次 renderTooltip（1.21.1 的
     * AbstractContainerScreen.render 不自己画 tooltip）。
     */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    /**
     * 顶部两行是自绘标签页按钮，原版标题会压在按钮上，所以只保留玩家背包标签。
     */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 4210752, false);
    }
}