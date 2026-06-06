package com.piranport.client.gui;

import com.piranport.artillery.config.ArtilleryCannonData;
import com.piranport.artillery.config.override.ClientConfigCache;
import com.piranport.network.UpdateConfigOverridePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 火炮详情编辑界面
 *
 * <p>显示13个可配置字段，支持输入验证和保存到服务端。
 * <p>从火炮配置工具主界面通过"火炮详情"按钮打开。
 */
@OnlyIn(Dist.CLIENT)
public class ArtilleryCannonDetailScreen extends Screen {

    private final Screen parent;
    private final String cannonName;
    private final ArtilleryCannonData originalData;

    // EditBox 引用数组（索引对应 FIELDS 数组）
    private final EditBox[] editBoxRefs = new EditBox[18];

    // 字段值缓存（用于滚动时保留用户输入）
    private final Map<String, String> fieldValueCache = new HashMap<>();

    // 动态布局参数（在 init() 中计算）
    private int guiHeight;
    private int visibleRows;
    private int buttonYOffset;
    private int maxScrollOffset;

    // 滚动状态
    private int scrollOffset = 0;

    // 滚动按钮引用（可能为null，如果不需要滚动）
    private Button scrollUpButton;
    private Button scrollDownButton;

    // 布局常量
    private static final int GUI_WIDTH = 250;
    private static final int LABEL_X_OFFSET = 10;
    private static final int INPUT_X_OFFSET = 110;
    private static final int INPUT_WIDTH = 100;
    private static final int ROW_HEIGHT = 19;
    private static final int FIELDS_START_Y = 40;
    private static final int MIN_VISIBLE_ROWS = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_MARGIN = 10;
    private static final int GUI_PADDING = 20;
    private static final int SCROLL_BUTTON_WIDTH = 15;

    /**
     * 字段定义：翻译键、字段名、是否为浮点数
     */
    private record FieldDef(String transKey, String fieldName, boolean isFloat) {}

    private static final FieldDef[] FIELDS = {
            new FieldDef("damage", "damage", true),
            new FieldDef("explosion_power", "explosionPower", true),
            new FieldDef("dispersion", "dispersion", true),
            new FieldDef("drag_coeff", "dragCoeff", true),
            new FieldDef("gravity", "gravity", true),
            new FieldDef("initial_speed", "initialSpeed", true),
            new FieldDef("reload_time", "reloadTime", false),
            new FieldDef("fire_cooldown", "fireCooldown", false),
            new FieldDef("salvo_count", "salvoCount", false),
            new FieldDef("salvo_interval", "salvoInterval", true),
            new FieldDef("durability", "durability", false),
            new FieldDef("barrels", "barrels", false)
    };

    public ArtilleryCannonDetailScreen(Screen parent, String cannonName, ArtilleryCannonData data) {
        super(Component.translatable("gui.piranport.config_tool.cannon_detail_title",
                Component.translatable("item.piranport." + cannonName).getString()));
        this.parent = parent;
        this.cannonName = cannonName;
        this.originalData = data;
    }

    @Override
    protected void init() {
        // 计算可用于显示字段的最大高度
        int availableHeight = this.height - GUI_PADDING * 2 - FIELDS_START_Y - BUTTON_HEIGHT - BUTTON_MARGIN;

        // 计算可见行数（至少6行，最多18行）
        visibleRows = Math.max(MIN_VISIBLE_ROWS, Math.min(FIELDS.length, availableHeight / ROW_HEIGHT));

        // 计算实际GUI高度
        guiHeight = FIELDS_START_Y + visibleRows * ROW_HEIGHT + BUTTON_MARGIN + BUTTON_HEIGHT + GUI_PADDING;

        // 计算按钮Y偏移
        buttonYOffset = FIELDS_START_Y + visibleRows * ROW_HEIGHT + BUTTON_MARGIN;

        // 计算最大滚动偏移
        maxScrollOffset = Math.max(0, FIELDS.length - visibleRows);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - guiHeight) / 2;

        // 只有需要滚动时才创建滚动按钮
        if (maxScrollOffset > 0) {
            scrollUpButton = Button.builder(
                    Component.literal("▲"),
                    button -> scrollUp()
            ).bounds(x + GUI_WIDTH - SCROLL_BUTTON_WIDTH - 5, y + FIELDS_START_Y, SCROLL_BUTTON_WIDTH, 15).build();
            this.addRenderableWidget(scrollUpButton);

            scrollDownButton = Button.builder(
                    Component.literal("▼"),
                    button -> scrollDown()
            ).bounds(x + GUI_WIDTH - SCROLL_BUTTON_WIDTH - 5, y + FIELDS_START_Y + visibleRows * ROW_HEIGHT - 15, SCROLL_BUTTON_WIDTH, 15).build();
            this.addRenderableWidget(scrollDownButton);
        }

        // 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.save"),
                button -> onSave()
        ).bounds(x + 20, y + buttonYOffset, 80, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.cancel"),
                button -> onCancel()
        ).bounds(x + 130, y + buttonYOffset, 80, 20).build());

        // 动态创建可见的编辑框
        rebuildEditBoxes();

        // 更新滚动按钮状态
        if (maxScrollOffset > 0) {
            updateScrollButtonStates();
        }
    }

    /**
     * 根据当前滚动位置重建可见的编辑框
     */
    private void rebuildEditBoxes() {
        // 移除所有现有的 EditBox（保留按钮）
        this.renderables.removeIf(widget -> widget instanceof EditBox);
        this.children().removeIf(widget -> widget instanceof EditBox);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - guiHeight) / 2;

        // 只创建可见范围内的 EditBox
        for (int i = 0; i < visibleRows && (scrollOffset + i) < FIELDS.length; i++) {
            int fieldIndex = scrollOffset + i;
            FieldDef field = FIELDS[fieldIndex];

            int boxY = y + FIELDS_START_Y + i * ROW_HEIGHT;
            EditBox box = new EditBox(this.font, x + INPUT_X_OFFSET, boxY, INPUT_WIDTH, 16, Component.empty());

            // 从缓存或原始数据获取当前值
            String currentValue = getCachedFieldValue(field.fieldName(), field.isFloat());
            box.setValue(currentValue);
            box.setMaxLength(12);

            this.addRenderableWidget(box);

            // 将 EditBox 关联到对应的字段
            editBoxRefs[fieldIndex] = box;
        }
    }

    /**
     * 获取缓存的字段值（优先从缓存，否则从原始数据）
     */
    private String getCachedFieldValue(String fieldName, boolean isFloat) {
        // 优先从缓存读取
        if (fieldValueCache.containsKey(fieldName)) {
            return fieldValueCache.get(fieldName);
        }

        // 否则从客户端缓存或原始数据读取
        if (isFloat) {
            return getFieldDisplayValue(fieldName, getOriginalFloatValue(fieldName));
        } else {
            return getFieldIntValue(fieldName, getOriginalIntValue(fieldName));
        }
    }

    /**
     * 从原始数据获取浮点值
     */
    private float getOriginalFloatValue(String fieldName) {
        return switch (fieldName) {
            case "damage" -> originalData.damage();
            case "explosionPower" -> originalData.explosionPower();
            case "dispersion" -> originalData.dispersion();
            case "dragCoeff" -> originalData.dragCoeff();
            case "gravity" -> originalData.gravity();
            case "initialSpeed" -> originalData.initialSpeed();
            case "salvoInterval" -> originalData.salvoInterval();
            default -> 0f;
        };
    }

    /**
     * 从原始数据获取整数值
     */
    private int getOriginalIntValue(String fieldName) {
        return switch (fieldName) {
            case "reloadTime" -> originalData.reloadTime();
            case "fireCooldown" -> originalData.fireCooldown();
            case "salvoCount" -> originalData.salvoCount();
            case "durability" -> originalData.durability();
            case "barrels" -> originalData.barrels();
            default -> 0;
        };
    }

    /**
     * 保存当前可见 EditBox 的值到缓存
     */
    private void cacheCurrentEditBoxValues() {
        for (int i = 0; i < visibleRows && (scrollOffset + i) < FIELDS.length; i++) {
            int fieldIndex = scrollOffset + i;
            FieldDef field = FIELDS[fieldIndex];
            EditBox box = editBoxRefs[fieldIndex];
            if (box != null) {
                fieldValueCache.put(field.fieldName(), box.getValue());
            }
        }
    }

    /**
     * 将所有未缓存的字段从原始数据填充到缓存
     */
    private void fillMissingFieldsToCache() {
        for (FieldDef field : FIELDS) {
            if (!fieldValueCache.containsKey(field.fieldName())) {
                String value = getCachedFieldValue(field.fieldName(), field.isFloat());
                fieldValueCache.put(field.fieldName(), value);
            }
        }
    }

    /**
     * 获取浮点字段的显示值（优先从客户端缓存读取覆盖）
     */
    private String getFieldDisplayValue(String fieldName, float defaultValue) {
        Optional<String> override = ClientConfigCache.getCannonOverride(cannonName, fieldName);
        return override.orElse(String.format("%.1f", defaultValue));
    }

    /**
     * 获取整数字段的显示值（优先从客户端缓存读取覆盖）
     */
    private String getFieldIntValue(String fieldName, int defaultValue) {
        Optional<String> override = ClientConfigCache.getCannonOverride(cannonName, fieldName);
        return override.orElse(String.valueOf(defaultValue));
    }

    /**
     * 向上滚动
     */
    private void scrollUp() {
        if (scrollOffset > 0) {
            // 保存当前可见字段的值
            cacheCurrentEditBoxValues();

            // 更新滚动偏移
            scrollOffset--;

            // 清空 EditBox 引用
            java.util.Arrays.fill(editBoxRefs, null);

            // 重建 EditBox
            rebuildEditBoxes();

            // 更新按钮状态
            updateScrollButtonStates();
        }
    }

    /**
     * 向下滚动
     */
    private void scrollDown() {
        if (scrollOffset < maxScrollOffset) {
            // 保存当前可见字段的值
            cacheCurrentEditBoxValues();

            // 更新滚动偏移
            scrollOffset++;

            // 清空 EditBox 引用
            java.util.Arrays.fill(editBoxRefs, null);

            // 重建 EditBox
            rebuildEditBoxes();

            // 更新按钮状态
            updateScrollButtonStates();
        }
    }

    /**
     * 更新滚动按钮的启用/禁用状态
     */
    private void updateScrollButtonStates() {
        if (scrollUpButton != null) {
            scrollUpButton.active = (scrollOffset > 0);
        }
        if (scrollDownButton != null) {
            scrollDownButton.active = (scrollOffset < maxScrollOffset);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - guiHeight) / 2;

        // 绘制背景
        graphics.fill(x, y, x + GUI_WIDTH, y + guiHeight, 0xFF8B8B8B);
        graphics.fill(x, y, x + GUI_WIDTH, y + 20, 0xFF5A5A5A);

        // 绘制滚动区域背景
        int scrollAreaHeight = visibleRows * ROW_HEIGHT;
        graphics.fill(x + 5, y + FIELDS_START_Y - 5, x + GUI_WIDTH - 25, y + FIELDS_START_Y + scrollAreaHeight + 5, 0xFF6B6B6B);

        // 绘制标题
        String title = Component.translatable("gui.piranport.config_tool.cannon_detail_title",
                Component.translatable("item.piranport." + cannonName).getString()).getString();
        graphics.drawString(this.font, title, x + 8, y + 6, 0xFFFFFF, false);

        // 启用裁剪，限制 EditBox 渲染区域
        graphics.enableScissor(
                x,
                y + FIELDS_START_Y,
                x + GUI_WIDTH,
                y + FIELDS_START_Y + scrollAreaHeight
        );

        // 渲染 EditBox（在裁剪区域内）
        for (int i = 0; i < visibleRows && (scrollOffset + i) < FIELDS.length; i++) {
            int fieldIndex = scrollOffset + i;
            EditBox box = editBoxRefs[fieldIndex];
            if (box != null) {
                box.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        // 绘制可见字段的标签
        for (int i = 0; i < visibleRows && (scrollOffset + i) < FIELDS.length; i++) {
            int fieldIndex = scrollOffset + i;
            FieldDef field = FIELDS[fieldIndex];
            int labelY = y + FIELDS_START_Y + i * ROW_HEIGHT + 2;
            String label = Component.translatable("gui.piranport.config_tool." + field.transKey()).getString();
            graphics.drawString(this.font, label, x + LABEL_X_OFFSET, labelY, 0xFFFFFF, false);
        }

        // 禁用裁剪
        graphics.disableScissor();

        // 渲染按钮（在裁剪区域外，确保可见）
        this.renderables.forEach(widget -> {
            if (widget instanceof Button) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        });

        // 绘制滚动指示器
        if (maxScrollOffset > 0) {
            renderScrollIndicators(graphics, x, y);
        }
    }

    /**
     * 渲染滚动指示器
     */
    private void renderScrollIndicators(GuiGraphics graphics, int x, int y) {
        // 上方指示器（有更多内容）
        if (scrollOffset > 0) {
            String indicator = "▲ " + scrollOffset + " 更多";
            graphics.drawString(this.font, indicator, x + 10, y + FIELDS_START_Y - 15, 0xFFFFAA, false);
        }

        // 下方指示器（有更多内容）
        if (scrollOffset < maxScrollOffset) {
            int remaining = FIELDS.length - scrollOffset - visibleRows;
            String indicator = "▼ " + remaining + " 更多";
            graphics.drawString(this.font, indicator, x + 10, y + FIELDS_START_Y + visibleRows * ROW_HEIGHT + 5, 0xFFFFAA, false);
        }

        // 滚动进度指示
        String progress = String.format("%d-%d / %d",
                scrollOffset + 1,
                Math.min(scrollOffset + visibleRows, FIELDS.length),
                FIELDS.length);
        graphics.drawString(this.font, progress, x + 160, y + FIELDS_START_Y - 15, 0xAAAAAA, false);
    }

    /**
     * 保存按钮回调：验证并发送所有字段更新
     */
    private void onSave() {
        // 先保存当前可见字段的值到缓存
        cacheCurrentEditBoxValues();

        // 将所有未缓存的字段从原始数据填充到缓存
        fillMissingFieldsToCache();

        // 验证所有字段
        if (!validateAllInputsFromCache()) {
            return;
        }

        // 发送18个字段的更新到服务端（从缓存读取）
        for (FieldDef field : FIELDS) {
            String value = fieldValueCache.get(field.fieldName());
            if (value != null && !value.isEmpty()) {
                sendFieldUpdate(field.fieldName(), value);
            }
        }

        // 更新客户端缓存
        updateClientCacheAfterSave();

        // 显示成功提示
        this.minecraft.player.sendSystemMessage(
                Component.translatable("message.piranport.config_saved")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
        );

        // 返回主界面
        this.minecraft.setScreen(parent);
    }

    /**
     * 发送单个字段更新到服务端
     */
    private void sendFieldUpdate(String field, String value) {
        PacketDistributor.sendToServer(
                new UpdateConfigOverridePayload("cannon", cannonName, field, value)
        );
    }

    /**
     * 保存后直接更新客户端缓存，避免等待服务端同步
     */
    private void updateClientCacheAfterSave() {
        Map<String, String> fields = new HashMap<>();

        // 从缓存读取所有字段值
        for (FieldDef field : FIELDS) {
            String value = fieldValueCache.get(field.fieldName());
            if (value != null) {
                fields.put(field.fieldName(), value);
            }
        }

        ClientConfigCache.setCannonOverrides(cannonName, fields);
    }

    /**
     * 从缓存验证所有输入值
     */
    private boolean validateAllInputsFromCache() {
        try {
            // 验证所有18个字段
            for (FieldDef field : FIELDS) {
                String value = fieldValueCache.get(field.fieldName());
                if (value == null || value.isEmpty()) {
                    showError("字段 " + field.transKey() + " 不能为空");
                    return false;
                }

                // 根据字段类型验证
                if (field.isFloat()) {
                    validateFloatField(field.fieldName(), value);
                } else {
                    validateIntField(field.fieldName(), value);
                }
            }

            return true;

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return false;
        }
    }

    /**
     * 验证浮点字段
     */
    private void validateFloatField(String fieldName, String valueStr) {
        float min = 0f, max = 1000f;

        // 根据字段设置范围
        switch (fieldName) {
            case "damage" -> { min = 0.1f; max = 1000f; }
            case "explosionPower" -> { min = 0f; max = 20f; }
            case "dispersion" -> { min = 0f; max = 10f; }
            case "dragCoeff" -> { min = 0f; max = 50f; }
            case "gravity" -> { min = 0.1f; max = 100f; }
            case "initialSpeed" -> { min = 0.1f; max = 50f; }
            case "salvoInterval" -> { min = 0f; max = 100f; }
        }

        validateFloat(fieldName, valueStr, min, max);
    }

    /**
     * 验证整数字段
     */
    private void validateIntField(String fieldName, String valueStr) {
        int min = 1, max = 10000;

        // 根据字段设置范围
        switch (fieldName) {
            case "reloadTime" -> { min = 1; max = 6000; }
            case "fireCooldown" -> { min = 0; max = 6000; }
            case "salvoCount" -> { min = 1; max = 20; }
            case "durability" -> { min = 1; max = 100000; }
            case "barrels" -> { min = 1; max = 20; }
        }

        validateInt(fieldName, valueStr, min, max);
    }

    /**
     * 验证浮点数值范围
     */
    private void validateFloat(String fieldName, String valueStr, float min, float max) {
        try {
            float value = Float.parseFloat(valueStr);
            if (value < min || value > max) {
                String fieldLabel = Component.translatable("gui.piranport.config_tool." + fieldName).getString();
                throw new IllegalArgumentException(
                        Component.translatable("message.piranport.value_out_of_range", fieldLabel, min, max).getString()
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    Component.translatable("message.piranport.invalid_number_format").getString()
            );
        }
    }

    /**
     * 验证整数值范围
     */
    private void validateInt(String fieldName, String valueStr, int min, int max) {
        try {
            int value = Integer.parseInt(valueStr);
            if (value < min || value > max) {
                String fieldLabel = Component.translatable("gui.piranport.config_tool." + fieldName).getString();
                throw new IllegalArgumentException(
                        Component.translatable("message.piranport.value_out_of_range", fieldLabel, min, max).getString()
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    Component.translatable("message.piranport.invalid_number_format").getString()
            );
        }
    }

    /**
     * 显示错误消息
     */
    private void showError(String messageKey) {
        if (this.minecraft != null && this.minecraft.player != null) {
            Component message;
            // 如果 messageKey 已经是翻译键（没有冒号）
            if (messageKey.startsWith("message.piranport.")) {
                message = Component.translatable(messageKey);
            } else {
                message = Component.literal(messageKey);
            }
            this.minecraft.player.sendSystemMessage(
                    message.copy().withStyle(net.minecraft.ChatFormatting.RED)
            );
        }
    }

    /**
     * 取消按钮回调
     */
    private void onCancel() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onCancel();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 只有需要滚动时才处理
        if (maxScrollOffset <= 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        // 检查鼠标是否在滚动区域内
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - guiHeight) / 2;
        int scrollAreaX = x;
        int scrollAreaY = y + FIELDS_START_Y;
        int scrollAreaWidth = GUI_WIDTH;
        int scrollAreaHeight = visibleRows * ROW_HEIGHT;

        if (mouseX >= scrollAreaX && mouseX < scrollAreaX + scrollAreaWidth &&
                mouseY >= scrollAreaY && mouseY < scrollAreaY + scrollAreaHeight) {

            if (scrollY > 0) {
                // 向上滚动
                scrollUp();
                return true;
            } else if (scrollY < 0) {
                // 向下滚动
                scrollDown();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
