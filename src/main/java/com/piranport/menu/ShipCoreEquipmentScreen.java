package com.piranport.menu;

import com.piranport.item.ShipType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 舰装核心装备界面的客户端渲染。
 * 显示核心槽位、武器槽位、强化槽位和玩家背包。
 */
public class ShipCoreEquipmentScreen extends AbstractContainerScreen<ShipCoreEquipmentMenu> {

    // ===== 布局常量 =====
    private static final int CORE_SLOT_X = 200;
    private static final int CORE_SLOT_Y = 20;

    private static final int WEAPON_AREA_X = 8;
    private static final int WEAPON_AREA_Y = 50;
    private static final int WEAPON_COLS = 3;

    private static final int ENHANCEMENT_AREA_X = 120;
    private static final int ENHANCEMENT_AREA_Y = 50;
    private static final int ENHANCEMENT_COLS = 2;

    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 140;
    private static final int HOTBAR_Y = 198;

    public ShipCoreEquipmentScreen(ShipCoreEquipmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 222;
        this.inventoryLabelY = 128;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 主背景
        gfx.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);

        // 3D 边框
        gfx.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        gfx.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        gfx.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        // 核心槽位背景（右上角，特殊高亮）
        drawCoreSlotBg(gfx, x + CORE_SLOT_X, y + CORE_SLOT_Y);

        // 武器槽位区域
        int weaponSlots = menu.getWeaponSlotCount();
        for (int i = 0; i < weaponSlots; i++) {
            int slotX = x + WEAPON_AREA_X + (i % WEAPON_COLS) * 18;
            int slotY = y + WEAPON_AREA_Y + (i / WEAPON_COLS) * 18;
            drawSlotBg(gfx, slotX, slotY);
        }

        // 强化槽位区域
        int enhancementSlots = menu.getEnhancementSlotCount();
        for (int i = 0; i < enhancementSlots; i++) {
            int slotX = x + ENHANCEMENT_AREA_X + (i % ENHANCEMENT_COLS) * 18;
            int slotY = y + ENHANCEMENT_AREA_Y + (i / ENHANCEMENT_COLS) * 18;
            drawEnhancementSlotBg(gfx, slotX, slotY);
        }

        // 玩家背包槽位（3x9）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + PLAYER_INV_X + col * 18, y + PLAYER_INV_Y + row * 18);
            }
        }

        // 快捷栏槽位（1x9）
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + PLAYER_INV_X + col * 18, y + HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // 标题
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        // 背包标签
        gfx.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // 核心信息显示
        ItemStack coreStack = menu.getCoreStack();
        if (!coreStack.isEmpty()) {
            ShipType shipType = menu.getShipType();
            String typeText = getShipTypeName(shipType);
            gfx.drawString(this.font, typeText, CORE_SLOT_X - 30, CORE_SLOT_Y + 22, 0x404040, false);
        }

        // 区域标签
        gfx.drawString(this.font, Component.translatable("gui.piranport.weapons"),
                WEAPON_AREA_X, WEAPON_AREA_Y - 12, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("gui.piranport.enhancements"),
                ENHANCEMENT_AREA_X, ENHANCEMENT_AREA_Y - 12, 0x404040, false);
        gfx.drawString(this.font, Component.translatable("gui.piranport.core"),
                CORE_SLOT_X - 20, CORE_SLOT_Y - 12, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    /**
     * 绘制普通槽位背景
     */
    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        com.piranport.client.GuiHelper.drawSlotBg(gfx, x, y);
    }

    /**
     * 绘制核心槽位背景（金色高亮）
     */
    private void drawCoreSlotBg(GuiGraphics gfx, int x, int y) {
        // 外边框（金色）
        gfx.fill(x - 1, y - 1, x + 19, y, 0xFFFFAA00);
        gfx.fill(x - 1, y - 1, x, y + 19, 0xFFFFAA00);
        gfx.fill(x + 18, y - 1, x + 19, y + 19, 0xFFCC8800);
        gfx.fill(x - 1, y + 18, x + 19, y + 19, 0xFFCC8800);

        // 内部槽位
        gfx.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        gfx.fill(x, y, x + 17, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 17, 0xFF373737);
        gfx.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    /**
     * 绘制强化槽位背景（蓝色边框）
     */
    private void drawEnhancementSlotBg(GuiGraphics gfx, int x, int y) {
        // 外边框（蓝色）
        gfx.fill(x - 1, y - 1, x + 19, y, 0xFF4488FF);
        gfx.fill(x - 1, y - 1, x, y + 19, 0xFF4488FF);
        gfx.fill(x + 18, y - 1, x + 19, y + 19, 0xFF2266CC);
        gfx.fill(x - 1, y + 18, x + 19, y + 19, 0xFF2266CC);

        // 内部槽位
        gfx.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        gfx.fill(x, y, x + 17, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 17, 0xFF373737);
        gfx.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    /**
     * 获取舰型显示名称
     */
    private String getShipTypeName(ShipType type) {
        return Component.translatable("gui.piranport.ship_type." + type.name().toLowerCase()).getString();
    }
}
