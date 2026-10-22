package com.piranport.item;

import com.piranport.client.ReloadProgressHudLayer;
import com.piranport.combat.TransformationManager;
import com.piranport.combat.cannon.CannonReloading;
import com.piranport.config.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 配置检查器 - Config Inspector
 * 用于显示当前游戏配置状态的道具
 */
public class ConfigInspectorItem extends Item {

    public ConfigInspectorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            sendConfigReport(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        addSummaryTooltip(tooltip);
    }

    /**
     * 添加 Tooltip 摘要。
     *
     * <p>只列真正可调的项：友军伤害固定开启、战斗机弹药固定无限是既定设计，不是配置，
     * 这两行和聊天栏报告完全重复，留着只会多一处会和代码脱节的文案。</p>
     */
    private void addSummaryTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        tooltip.add(Component.literal("[核心开关]").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  爆炸破坏: " + formatBoolean(ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()))
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal("")); // 空行
        tooltip.add(Component.translatable("tooltip.piranport.config_inspector.use")
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }

    /**
     * 发送完整配置报告到聊天栏
     */
    private void sendConfigReport(Player player) {
        // 标题
        player.sendSystemMessage(Component.literal("━━━━━━ 配置报告 ━━━━━━")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        // 1. 游戏模式与核心开关
        // 这里的原则：能读到出处的就真读，读不到的要么是既定设计（标明"固定"），
        // 要么补进调试终端（见 ModEquipmentConfig）。不再写死一句会和代码脱节的文案。
        addSection(player, "游戏模式与核心开关");
        addConfigLine(player, "首次赠书", formatBoolean(ModCommonConfig.GIVE_GUIDEBOOK_ON_FIRST_JOIN.get()));
        addConfigLine(player, "舰装槽位",
                TransformationManager.CORE_HAND == net.minecraft.world.InteractionHand.OFF_HAND
                        ? "offhand" : "mainhand");
        // 友军伤害固定开启：没有配置项，也不会开放（多人同舰队是核心玩法）。
        addConfigLine(player, "友军伤害", "开启（固定）");
        addConfigLine(player, "爆炸破坏", formatBoolean(ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()));
        // 自动装填固定关闭"自动启动"，读条到期仍会自动结算。
        addConfigLine(player, "自动装填",
                CannonReloading.AUTO_RELOAD_ENABLED ? "开启" : "关闭（固定）");
        // 战斗机弹药固定无限：弹药只由舰载机数据定义，没有补给消耗。
        addConfigLine(player, "战斗机弹药", "无限（固定）");

        // 2. 舰装属性
        addSection(player, "舰装属性");
        for (ShipType core : ShipType.values()) {
            addConfigLine(player, core.name(), String.format("护甲%d 载重%d 空载速度%.2f 满载速度%.2f",
                    core.effectiveBaseArmor(), core.effectiveMaxLoad(),
                    core.effectiveEmptySpeed(), core.effectiveFullLoadSpeed()));
        }

        // 3. 飞机系统
        addSection(player, "飞机系统");
        com.piranport.aviation.AircraftDefinitionService.snapshot().values().stream()
                .sorted(java.util.Comparator.comparing(com.piranport.aviation.AircraftDefinition::id))
                .forEach(definition -> {
                    var stats = com.piranport.aviation.AircraftStatsService.resolve(definition);
                    addAircraftStats(player, definition.id().toString(), stats.damage(), stats.speed(), stats.health());
                });

        // 4. 火炮与弹药系统
        addSection(player, "火炮与弹药系统");
        addConfigLine(player, "最大炮弹数", String.valueOf(ModArtilleryConfig.ARTILLERY_MAX_PROJECTILES.get()));
        addConfigLine(player, "水中销毁时间", ModArtilleryConfig.ARTILLERY_UNDERWATER_DESTROY_TIME.get() + "秒");
        addConfigLine(player, "三式弹上限", String.valueOf(ModArtilleryConfig.PERF_SHRAPNEL_LIMIT.get()));
        addConfigLine(player, "VT扫描间隔", ModArtilleryConfig.PERF_VT_CHECK_INTERVAL.get() + " tick");

        // 5. 装备系统
        addSection(player, "装备系统");
        addConfigLine(player, "声呐范围", ModEquipmentConfig.SONAR_RANGE.get() + "格");
        addConfigLine(player, "雷达范围", ModEquipmentConfig.RADAR_RANGE.get() + "格");
        addConfigLine(player, "火控范围", ModEquipmentConfig.FIRE_CONTROL_RANGE.get() + "格");
        addConfigLine(player, "装甲板保护", String.valueOf(ModEquipmentConfig.ARMOR_PLATE_PROTECTION.get()));

        // 6. 客户端显示
        addSection(player, "客户端显示");
        addConfigLine(player, "装填HUD样式", ReloadProgressHudLayer.STYLE);
        addConfigLine(player, "火控位置",
                HudPosition.byIndex(ModEquipmentConfig.FIRE_CONTROL_HUD_CORNER.get()).name());
        addConfigLine(player, "屏幕震动",
                String.format("%.2fx", ModEquipmentConfig.CAMERA_SHAKE_SCALE.get()));

        // 结束分隔线
        player.sendSystemMessage(Component.literal("━━━━━━━━━━━━━━━━━━━━")
                .withStyle(ChatFormatting.GOLD));
    }

    /**
     * 添加分节标题
     */
    private void addSection(Player player, String title) {
        player.sendSystemMessage(Component.literal("")); // 空行
        player.sendSystemMessage(Component.literal("[" + title + "]")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    /**
     * 添加配置行
     */
    private void addConfigLine(Player player, String key, String value) {
        player.sendSystemMessage(Component.literal("  " + key + ": " + value)
                .withStyle(ChatFormatting.GRAY));
    }

    /**
     * 添加飞机属性行
     */
    private void addAircraftStats(Player player, String name, double damage, double speed, double health) {
        String stats = String.format("伤害%.1f 速度%.1f 生命%.0f", damage, speed, health);
        addConfigLine(player, name, stats);
    }

    /**
     * 格式化布尔值
     */
    private String formatBoolean(boolean value) {
        return value ? "✓ 开启" : "✗ 关闭";
    }

}
