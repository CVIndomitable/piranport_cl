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

    // 13个编辑框
    private EditBox projectileWeightBox;
    private EditBox initialSpeedBox;
    private EditBox reloadTimeBox;
    private EditBox fireCooldownBox;
    private EditBox salvoCountBox;
    private EditBox salvoIntervalBox;
    private EditBox verticalSpreadBox;
    private EditBox horizontalSpreadBox;
    private EditBox maxElevationBox;
    private EditBox minElevationBox;
    private EditBox turretSpeedBox;
    private EditBox durabilityBox;
    private EditBox barrelsBox;

    // 布局常量
    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 320;
    private static final int LABEL_X_OFFSET = 10;
    private static final int INPUT_X_OFFSET = 110;
    private static final int INPUT_WIDTH = 100;
    private static final int ROW_HEIGHT = 19;
    private static final int FIELDS_START_Y = 40;
    private static final int BUTTON_Y_OFFSET = 285;

    /**
     * 字段定义：翻译键、字段名、是否为浮点数
     */
    private record FieldDef(String transKey, String fieldName, boolean isFloat) {}

    private static final FieldDef[] FIELDS = {
            new FieldDef("projectile_weight", "projectileWeight", true),
            new FieldDef("initial_speed", "initialSpeed", true),
            new FieldDef("reload_time", "reloadTime", false),
            new FieldDef("fire_cooldown", "fireCooldown", false),
            new FieldDef("salvo_count", "salvoCount", false),
            new FieldDef("salvo_interval", "salvoInterval", true),
            new FieldDef("vertical_spread", "verticalSpread", true),
            new FieldDef("horizontal_spread", "horizontalSpread", true),
            new FieldDef("max_elevation", "maxElevation", true),
            new FieldDef("min_elevation", "minElevation", true),
            new FieldDef("turret_speed", "turretSpeed", true),
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
        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        createEditBoxes(x, y);

        // 保存按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.save"),
                button -> onSave()
        ).bounds(x + 20, y + BUTTON_Y_OFFSET, 80, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.config_tool.cancel"),
                button -> onCancel()
        ).bounds(x + 130, y + BUTTON_Y_OFFSET, 80, 20).build());
    }

    /**
     * 创建所有编辑框并填充当前值
     */
    private void createEditBoxes(int guiX, int guiY) {
        projectileWeightBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 0 * ROW_HEIGHT,
                getFieldDisplayValue("projectileWeight", originalData.projectileWeight()));
        initialSpeedBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 1 * ROW_HEIGHT,
                getFieldDisplayValue("initialSpeed", originalData.initialSpeed()));
        reloadTimeBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 2 * ROW_HEIGHT,
                String.valueOf(getFieldIntValue("reloadTime", originalData.reloadTime())));
        fireCooldownBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 3 * ROW_HEIGHT,
                String.valueOf(getFieldIntValue("fireCooldown", originalData.fireCooldown())));
        salvoCountBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 4 * ROW_HEIGHT,
                String.valueOf(getFieldIntValue("salvoCount", originalData.salvoCount())));
        salvoIntervalBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 5 * ROW_HEIGHT,
                getFieldDisplayValue("salvoInterval", originalData.salvoInterval()));
        verticalSpreadBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 6 * ROW_HEIGHT,
                getFieldDisplayValue("verticalSpread", originalData.verticalSpread()));
        horizontalSpreadBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 7 * ROW_HEIGHT,
                getFieldDisplayValue("horizontalSpread", originalData.horizontalSpread()));
        maxElevationBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 8 * ROW_HEIGHT,
                getFieldDisplayValue("maxElevation", originalData.maxElevation()));
        minElevationBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 9 * ROW_HEIGHT,
                getFieldDisplayValue("minElevation", originalData.minElevation()));
        turretSpeedBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 10 * ROW_HEIGHT,
                getFieldDisplayValue("turretSpeed", originalData.turretSpeed()));
        durabilityBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 11 * ROW_HEIGHT,
                String.valueOf(getFieldIntValue("durability", originalData.durability())));
        barrelsBox = createFieldBox(guiX + INPUT_X_OFFSET, guiY + FIELDS_START_Y + 12 * ROW_HEIGHT,
                String.valueOf(getFieldIntValue("barrels", originalData.barrels())));
    }

    /**
     * 创建单个编辑框
     */
    private EditBox createFieldBox(int x, int y, String currentValue) {
        EditBox box = new EditBox(this.font, x, y, INPUT_WIDTH, 16, Component.empty());
        box.setValue(currentValue);
        box.setMaxLength(12);
        this.addRenderableWidget(box);
        return box;
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

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = (this.width - GUI_WIDTH) / 2;
        int y = (this.height - GUI_HEIGHT) / 2;

        // 主背景
        graphics.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B8B8B);

        // 标题区域
        graphics.fill(x, y, x + GUI_WIDTH, y + 20, 0xFF5A5A5A);

        // 标题
        String title = Component.translatable("gui.piranport.config_tool.cannon_detail_title",
                Component.translatable("item.piranport." + cannonName).getString()).getString();
        graphics.drawString(this.font, title, x + 8, y + 6, 0xFFFFFF, false);

        // 绘制字段标签
        for (int i = 0; i < FIELDS.length; i++) {
            FieldDef field = FIELDS[i];
            int labelY = y + FIELDS_START_Y + i * ROW_HEIGHT + 2;
            String label = Component.translatable("gui.piranport.config_tool." + field.transKey()).getString();
            graphics.drawString(this.font, label, x + LABEL_X_OFFSET, labelY, 0xFFFFFF, false);
        }
    }

    /**
     * 保存按钮回调：验证并发送所有字段更新
     */
    private void onSave() {
        if (!validateAllInputs()) {
            return;
        }

        // 发送13个字段的更新到服务端
        sendFieldUpdate("projectileWeight", projectileWeightBox.getValue());
        sendFieldUpdate("initialSpeed", initialSpeedBox.getValue());
        sendFieldUpdate("reloadTime", reloadTimeBox.getValue());
        sendFieldUpdate("fireCooldown", fireCooldownBox.getValue());
        sendFieldUpdate("salvoCount", salvoCountBox.getValue());
        sendFieldUpdate("salvoInterval", salvoIntervalBox.getValue());
        sendFieldUpdate("verticalSpread", verticalSpreadBox.getValue());
        sendFieldUpdate("horizontalSpread", horizontalSpreadBox.getValue());
        sendFieldUpdate("maxElevation", maxElevationBox.getValue());
        sendFieldUpdate("minElevation", minElevationBox.getValue());
        sendFieldUpdate("turretSpeed", turretSpeedBox.getValue());
        sendFieldUpdate("durability", durabilityBox.getValue());
        sendFieldUpdate("barrels", barrelsBox.getValue());

        // 直接更新客户端缓存，主界面立即反映变更
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
        fields.put("projectileWeight", projectileWeightBox.getValue());
        fields.put("initialSpeed", initialSpeedBox.getValue());
        fields.put("reloadTime", reloadTimeBox.getValue());
        fields.put("fireCooldown", fireCooldownBox.getValue());
        fields.put("salvoCount", salvoCountBox.getValue());
        fields.put("salvoInterval", salvoIntervalBox.getValue());
        fields.put("verticalSpread", verticalSpreadBox.getValue());
        fields.put("horizontalSpread", horizontalSpreadBox.getValue());
        fields.put("maxElevation", maxElevationBox.getValue());
        fields.put("minElevation", minElevationBox.getValue());
        fields.put("turretSpeed", turretSpeedBox.getValue());
        fields.put("durability", durabilityBox.getValue());
        fields.put("barrels", barrelsBox.getValue());

        ClientConfigCache.setCannonOverrides(cannonName, fields);
    }

    /**
     * 验证所有输入值
     */
    private boolean validateAllInputs() {
        try {
            validateFloat("projectileWeight", projectileWeightBox.getValue(), 0.1f, 10000f);
            validateFloat("initialSpeed", initialSpeedBox.getValue(), 0.1f, 50f);
            validateInt("reloadTime", reloadTimeBox.getValue(), 1, 6000);
            validateInt("fireCooldown", fireCooldownBox.getValue(), 0, 6000);
            validateInt("salvoCount", salvoCountBox.getValue(), 1, 20);
            validateFloat("salvoInterval", salvoIntervalBox.getValue(), 0f, 100f);
            validateFloat("verticalSpread", verticalSpreadBox.getValue(), 0f, 10f);
            validateFloat("horizontalSpread", horizontalSpreadBox.getValue(), 0f, 10f);
            validateFloat("maxElevation", maxElevationBox.getValue(), -90f, 90f);
            validateFloat("minElevation", minElevationBox.getValue(), -90f, 90f);
            validateFloat("turretSpeed", turretSpeedBox.getValue(), 0.1f, 20f);
            validateInt("durability", durabilityBox.getValue(), 1, 100000);
            validateInt("barrels", barrelsBox.getValue(), 1, 20);

            // 验证最小仰角 < 最大仰角
            float minElev = Float.parseFloat(minElevationBox.getValue());
            float maxElev = Float.parseFloat(maxElevationBox.getValue());
            if (minElev >= maxElev) {
                showError("message.piranport.min_elevation_must_be_less");
                return false;
            }

            return true;

        } catch (IllegalArgumentException e) {
            // 验证失败时显示具体错误信息
            showError(e.getMessage());
            return false;
        }
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
}
