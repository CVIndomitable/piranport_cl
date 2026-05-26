package com.piranport.client.gui;

import com.piranport.menu.ArtilleryConfigToolMenu;
import com.piranport.network.ExportConfigPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * 火炮配置工具界面（客户端渲染）
 *
 * <p>显示火炮和弹药配置的GUI，支持实时编辑和导出。
 * <p>当前版本：基础框架，完整GUI功能开发中
 */
@OnlyIn(Dist.CLIENT)
public class ArtilleryConfigToolScreen extends AbstractContainerScreen<ArtilleryConfigToolMenu> {

    public ArtilleryConfigToolScreen(ArtilleryConfigToolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 添加导出按钮（临时位置）
        this.addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                Component.translatable("gui.piranport.config_tool.export"),
                button -> onExportClicked()
        ).bounds(x + 10, y + 210, 80, 20).build());

        // TODO: 添加滚动列表组件
        // TODO: 添加编辑框
        // TODO: 添加标签页切换按钮
        // TODO: 添加重置按钮
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 绘制背景
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 简单的灰色背景
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);

        // 绘制标题区域
        graphics.fill(x, y, x + this.imageWidth, y + 20, 0xFF5A5A5A);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // 绘制标题
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        graphics.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFF, false);

        // TODO: 绘制火炮列表
        // TODO: 绘制编辑区域
        // TODO: 绘制按钮
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESC键关闭
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 导出CSV按钮回调
     */
    private void onExportClicked() {
        PacketDistributor.sendToServer(new ExportConfigPayload());
    }
}
