package com.piranport.menu;

import com.piranport.item.ShipType;
import com.piranport.network.ApplyModificationPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 舰装核心改装器界面
 * 显示核心槽位、配置滑块和应用按钮
 */
public class ShipCoreModifierScreen extends AbstractContainerScreen<ShipCoreModifierMenu> {
    private static final int CORE_SLOT_X = 80;
    private static final int CORE_SLOT_Y = 35;

    private Button weaponMinusButton;
    private Button weaponPlusButton;
    private Button enhancementMinusButton;
    private Button enhancementPlusButton;
    private Button applyButton;

    private int tempWeaponSlots = 2;
    private int tempEnhancementSlots = 1;

    public ShipCoreModifierScreen(ShipCoreModifierMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 72;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();

        // 同步当前配置
        tempWeaponSlots = menu.getWeaponSlots();
        tempEnhancementSlots = menu.getEnhancementSlots();

        int x = this.leftPos;
        int y = this.topPos;

        // 武器槽调整按钮
        weaponMinusButton = Button.builder(Component.literal("-"), btn -> {
            if (tempWeaponSlots > 2) {
                tempWeaponSlots--;
            }
        }).bounds(x + 20, y + 20, 20, 20).build();

        weaponPlusButton = Button.builder(Component.literal("+"), btn -> {
            if (tempWeaponSlots < 8) {
                tempWeaponSlots++;
            }
        }).bounds(x + 120, y + 20, 20, 20).build();

        // 强化槽调整按钮
        enhancementMinusButton = Button.builder(Component.literal("-"), btn -> {
            if (tempEnhancementSlots > 1) {
                tempEnhancementSlots--;
            }
        }).bounds(x + 20, y + 50, 20, 20).build();

        enhancementPlusButton = Button.builder(Component.literal("+"), btn -> {
            if (tempEnhancementSlots < 6) {
                tempEnhancementSlots++;
            }
        }).bounds(x + 120, y + 50, 20, 20).build();

        // 应用按钮
        applyButton = Button.builder(Component.translatable("gui.piranport.apply_modification"), btn -> {
            // 发送网络包到服务端应用改装
            PacketDistributor.sendToServer(new ApplyModificationPayload(
                    menu.getBlockPos(),
                    tempWeaponSlots,
                    tempEnhancementSlots
            ));
        }).bounds(x + 38, y + 60, 100, 20).build();

        addRenderableWidget(weaponMinusButton);
        addRenderableWidget(weaponPlusButton);
        addRenderableWidget(enhancementMinusButton);
        addRenderableWidget(enhancementPlusButton);
        addRenderableWidget(applyButton);
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

        // 核心槽位背景
        drawSlotBg(gfx, x + CORE_SLOT_X, y + CORE_SLOT_Y);

        // 玩家背包槽位（3x9）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(gfx, x + 8 + col * 18, y + 84 + row * 18);
            }
        }

        // 快捷栏槽位（1x9）
        for (int col = 0; col < 9; col++) {
            drawSlotBg(gfx, x + 8 + col * 18, y + 142);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderLabels(gfx, mouseX, mouseY);

        // 显示当前配置
        gfx.drawString(font, Component.translatable("gui.piranport.weapon_slots", tempWeaponSlots), 45, 25, 0x404040, false);
        gfx.drawString(font, Component.translatable("gui.piranport.enhancement_slots", tempEnhancementSlots), 45, 55, 0x404040, false);

        // 显示经验消耗
        ShipType shipType = menu.getShipType();
        int defaultSlots = shipType.weaponSlots + shipType.enhancementSlots;
        int totalSlots = tempWeaponSlots + tempEnhancementSlots;
        int extraSlots = Math.max(0, totalSlots - defaultSlots);
        int expCost = extraSlots * 5;

        if (expCost > 0) {
            gfx.drawString(font, Component.translatable("gui.piranport.exp_cost", expCost), 38, 72, 0x00AA00, false);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        com.piranport.client.GuiHelper.drawSlotBg(gfx, x, y);
    }
}
