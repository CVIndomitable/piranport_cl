package com.piranport.client.gui;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.ArtilleryConfig;
import com.piranport.artillery.config.override.ClientConfigCache;
import com.piranport.menu.ArtilleryConfigToolMenu;
import com.piranport.network.ExportConfigPayload;
import com.piranport.network.ImportConfigPayload;
import com.piranport.network.ResetConfigPayload;
import com.piranport.network.UpdateConfigOverridePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 火炮配置工具界面（客户端渲染）
 *
 * <p>显示火炮和弹药配置的GUI，支持实时编辑和导出。
 */
@OnlyIn(Dist.CLIENT)
public class ArtilleryConfigToolScreen extends AbstractContainerScreen<ArtilleryConfigToolMenu> {

    // 标签页
    private static final int TAB_CANNONS = 0;
    private static final int TAB_PROJECTILES = 1;
    private int currentTab = TAB_CANNONS;

    // 滚动相关
    private int scrollOffset = 0;
    private static final int VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 20;

    // 编辑框缓存
    private final Map<String, EditBox> editBoxes = new HashMap<>();

    // 数据缓存
    private List<String> cannonNames = new ArrayList<>();
    private List<String> projectileKeys = Arrays.asList(
            "HE_ARMOR_PENETRATION",
            "HE_DAMAGE_FALLOFF",
            "AP_DAMAGE_MULTIPLIER",
            "AP_ARMOR_IGNORE",
            "UNDERWATER_EXPLOSION_MULTIPLIER",
            "UNDERWATER_EXPLODE"
    );

    // 可编辑字段
    @SuppressWarnings("unused")
    private static final String[] PROJECTILE_FIELDS = {
            "HE_ARMOR_PENETRATION", "HE_DAMAGE_FALLOFF", "AP_DAMAGE_MULTIPLIER",
            "AP_ARMOR_IGNORE", "UNDERWATER_EXPLOSION_MULTIPLIER", "UNDERWATER_EXPLODE"
    };

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

        // 加载火炮列表
        cannonNames = new ArrayList<>(ArtilleryConfig.getAllCannonNames());
        Collections.sort(cannonNames);

        // 标签页按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.tab.cannons"),
                button -> switchTab(TAB_CANNONS)
        ).bounds(x + 10, y + 25, 60, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.tab.projectiles"),
                button -> switchTab(TAB_PROJECTILES)
        ).bounds(x + 75, y + 25, 60, 20).build());

        // 滚动按钮
        this.addRenderableWidget(Button.builder(
                Component.literal("▲"),
                button -> scrollUp()
        ).bounds(x + 235, y + 50, 15, 15).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("▼"),
                button -> scrollDown()
        ).bounds(x + 235, y + 195, 15, 15).build());

        // 底部按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.export"),
                button -> onExportClicked()
        ).bounds(x + 10, y + 215, 50, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.import"),
                button -> onImportClicked()
        ).bounds(x + 65, y + 215, 50, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.reset_all"),
                button -> onResetAllClicked()
        ).bounds(x + 120, y + 215, 60, 20).build());

        // 为每行火炮创建详情按钮
        createDetailButtons();

        // 创建弹药编辑框
        createProjectileEditBoxes();
    }

    /**
     * 为每行火炮创建"火炮详情"按钮
     */
    private void createDetailButtons() {
        if (currentTab != TAB_CANNONS) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < Math.min(VISIBLE_ROWS, cannonNames.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= cannonNames.size()) break;

            String cannonName = cannonNames.get(index);
            int rowY = y + 52 + i * ROW_HEIGHT;

            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.piranport.config_tool.detail"),
                    button -> openDetailScreen(cannonName)
            ).bounds(x + 145, rowY, 80, 16).build());
        }
    }

    /**
     * 创建弹药配置编辑框
     */
    private void createProjectileEditBoxes() {
        if (currentTab != TAB_PROJECTILES) return;

        editBoxes.clear();
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        for (int i = 0; i < Math.min(VISIBLE_ROWS, projectileKeys.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= projectileKeys.size()) break;

            String key = projectileKeys.get(index);

            EditBox editBox = new EditBox(
                    this.font,
                    x + 150,
                    y + 52 + i * ROW_HEIGHT,
                    80,
                    16,
                    Component.empty()
            );

            // 设置当前值
            String currentValue = getProjectileValue(key);
            editBox.setValue(currentValue);
            editBox.setMaxLength(15);

            editBoxes.put(key, editBox);
            this.addRenderableWidget(editBox);
        }
    }

    /**
     * 获取弹药配置当前值
     */
    private String getProjectileValue(String key) {
        // 优先从缓存读取
        Optional<String> override = ClientConfigCache.getProjectileOverride(key);
        if (override.isPresent()) {
            return override.get();
        }

        // 否则使用默认值
        return switch (key) {
            case "HE_ARMOR_PENETRATION" -> "0.3";
            case "HE_DAMAGE_FALLOFF" -> "true";
            case "AP_DAMAGE_MULTIPLIER" -> "1.3";
            case "AP_ARMOR_IGNORE" -> "0.5";
            case "UNDERWATER_EXPLOSION_MULTIPLIER" -> "0.5";
            case "UNDERWATER_EXPLODE" -> "false";
            default -> "";
        };
    }

    /**
     * 切换标签页
     */
    private void switchTab(int tab) {
        if (currentTab != tab) {
            currentTab = tab;
            scrollOffset = 0;
            this.rebuildWidgets();
        }
    }

    /**
     * 向上滚动
     */
    private void scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--;
            this.rebuildWidgets();
        }
    }

    /**
     * 向下滚动
     */
    private void scrollDown() {
        int maxItems = currentTab == TAB_CANNONS ? cannonNames.size() : projectileKeys.size();
        if (scrollOffset < maxItems - VISIBLE_ROWS) {
            scrollOffset++;
            this.rebuildWidgets();
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 主背景
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF8B8B8B);

        // 标题区域
        graphics.fill(x, y, x + this.imageWidth, y + 20, 0xFF5A5A5A);

        // 列表区域背景
        graphics.fill(x + 5, y + 50, x + 230, y + 210, 0xFF6B6B6B);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // 绘制标题
        graphics.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFF, false);

        // 绘制列表内容
        if (currentTab == TAB_CANNONS) {
            renderCannonList(graphics, x, y);
        } else {
            renderProjectileList(graphics, x, y);
        }
    }

    /**
     * 渲染火炮列表
     */
    private void renderCannonList(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < Math.min(VISIBLE_ROWS, cannonNames.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= cannonNames.size()) break;

            String cannonName = cannonNames.get(index);
            int rowY = y + 52 + i * ROW_HEIGHT;

            // 绘制火炮名称
            String displayName = getCannonDisplayName(cannonName);
            graphics.drawString(this.font, displayName, x + 10, rowY + 2, 0xFFFFFF, false);
        }
    }

    /**
     * 渲染弹药配置列表
     */
    private void renderProjectileList(GuiGraphics graphics, int x, int y) {
        for (int i = 0; i < Math.min(VISIBLE_ROWS, projectileKeys.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= projectileKeys.size()) break;

            String key = projectileKeys.get(index);
            int rowY = y + 52 + i * ROW_HEIGHT;

            // 绘制配置名称
            String displayName = Component.translatable("config.piranport." + key.toLowerCase()).getString();
            graphics.drawString(this.font, displayName, x + 10, rowY + 4, 0xFFFFFF, false);
        }
    }

    /**
     * 获取火炮显示名称
     */
    private String getCannonDisplayName(String cannonName) {
        return Component.translatable("item.piranport." + cannonName).getString();
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

    @Override
    public void removed() {
        super.removed();
        // 关闭时应用所有修改
        applyAllChanges();
    }

    /**
     * 打开火炮详情编辑界面
     */
    private void openDetailScreen(String cannonName) {
        ArtilleryCannonData data = ArtilleryConfig.get(cannonName);
        this.minecraft.setScreen(new ArtilleryCannonDetailScreen(this, cannonName, data));
    }

    /**
     * 应用所有修改（弹药配置）
     */
    private void applyAllChanges() {
        for (String configKey : projectileKeys) {
            EditBox editBox = editBoxes.get(configKey);
            if (editBox != null && !editBox.getValue().isEmpty()) {
                sendUpdate("projectile", configKey, "", editBox.getValue());
            }
        }
    }

    /**
     * 发送更新到服务端
     */
    private void sendUpdate(String category, String key, String field, String value) {
        PacketDistributor.sendToServer(new UpdateConfigOverridePayload(category, key, field, value));
    }

    /**
     * 导出CSV按钮回调
     */
    private void onExportClicked() {
        applyAllChanges();
        PacketDistributor.sendToServer(new ExportConfigPayload());
    }

    /**
     * 导入CSV按钮回调
     */
    private void onImportClicked() {
        // 打开文件选择对话框
        this.minecraft.setScreen(new ImportFileSelectionScreen(this));
    }

    /**
     * 重置全部按钮回调
     */
    private void onResetAllClicked() {
        // 发送重置请求到服务端
        PacketDistributor.sendToServer(new ResetConfigPayload());

        // 清空客户端缓存
        ClientConfigCache.clearCache();

        // 重建GUI以显示原始值
        this.rebuildWidgets();

        // 显示提示消息
        this.minecraft.player.sendSystemMessage(
                Component.translatable("message.piranport.config_reset")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
        );
    }

    /**
     * 文件选择界面
     */
    @OnlyIn(Dist.CLIENT)
    private static class ImportFileSelectionScreen extends net.minecraft.client.gui.screens.Screen {
        private final ArtilleryConfigToolScreen parent;
        private EditBox cannonFileBox;
        private EditBox projectileFileBox;

        protected ImportFileSelectionScreen(ArtilleryConfigToolScreen parent) {
            super(Component.translatable("gui.piranport.config_tool.import_files"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int x = (this.width - 300) / 2;
            int y = (this.height - 150) / 2;

            // 火炮配置文件输入框
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.piranport.config_tool.cannon_file"),
                    button -> {}
            ).bounds(x + 10, y + 30, 100, 20).build());

            cannonFileBox = new EditBox(this.font, x + 115, y + 30, 175, 20, Component.empty());
            cannonFileBox.setMaxLength(100);
            cannonFileBox.setValue("cannons_latest.csv");
            this.addRenderableWidget(cannonFileBox);

            // 弹药配置文件输入框
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.piranport.config_tool.projectile_file"),
                    button -> {}
            ).bounds(x + 10, y + 60, 100, 20).build());

            projectileFileBox = new EditBox(this.font, x + 115, y + 60, 175, 20, Component.empty());
            projectileFileBox.setMaxLength(100);
            projectileFileBox.setValue("projectiles_latest.csv");
            this.addRenderableWidget(projectileFileBox);

            // 确认按钮
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.piranport.config_tool.confirm_import"),
                    button -> onConfirmImport()
            ).bounds(x + 50, y + 100, 80, 20).build());

            // 取消按钮
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.done"),
                    button -> this.minecraft.setScreen(parent)
            ).bounds(x + 170, y + 100, 80, 20).build());
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);

            int x = (this.width - 300) / 2;
            int y = (this.height - 150) / 2;

            // 背景
            graphics.fill(x, y, x + 300, y + 150, 0xFF8B8B8B);
            graphics.fill(x, y, x + 300, y + 20, 0xFF5A5A5A);

            // 标题
            graphics.drawString(this.font, this.title, x + 10, y + 6, 0xFFFFFF, false);

            // 提示文本
            graphics.drawString(this.font,
                    Component.translatable("gui.piranport.config_tool.import_hint"),
                    x + 10, y + 130, 0xFFFFFF, false);
        }

        private void onConfirmImport() {
            String cannonFile = cannonFileBox.getValue().trim();
            String projectileFile = projectileFileBox.getValue().trim();

            if (cannonFile.isEmpty() && projectileFile.isEmpty()) {
                // 显示错误消息
                this.minecraft.player.sendSystemMessage(
                        Component.translatable("message.piranport.import_no_files")
                                .withStyle(net.minecraft.ChatFormatting.RED)
                );
                return;
            }

            // 发送导入请求
            PacketDistributor.sendToServer(new ImportConfigPayload(cannonFile, projectileFile));

            // 返回主界面
            this.minecraft.setScreen(parent);

            // 显示提示消息
            this.minecraft.player.sendSystemMessage(
                    Component.translatable("message.piranport.import_requested")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW)
            );
        }
    }
}
