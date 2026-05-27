package com.piranport.item;

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
     * 添加Tooltip摘要（显示关键配置）
     */
    private void addSummaryTooltip(List<Component> tooltip) {
        tooltip.add(Component.literal("━━━━━━━━━━━━━━━━").withStyle(ChatFormatting.DARK_GRAY));

        // 核心开关
        tooltip.add(Component.literal("[核心开关]").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  友军伤害: " + formatBoolean(ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  爆炸破坏: " + formatBoolean(ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  自动装填: " + formatBoolean(ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()))
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
        addSection(player, "游戏模式与核心开关");
        addConfigLine(player, "首次赠书", formatBoolean(ModCommonConfig.GIVE_GUIDEBOOK_ON_FIRST_JOIN.get()));
        addConfigLine(player, "舰装槽位", ModCommonConfig.SHIP_CORE_SLOT_MODE.get());
        addConfigLine(player, "友军伤害", formatBoolean(ModCommonConfig.FRIENDLY_FIRE_ENABLED.get()));
        addConfigLine(player, "爆炸破坏", formatBoolean(ModCommonConfig.EXPLOSION_BLOCK_DAMAGE.get()));
        addConfigLine(player, "自动装填", formatBoolean(ModCommonConfig.AUTO_RESUPPLY_ENABLED.get()));
        addConfigLine(player, "战斗机弹药", formatBoolean(ModCommonConfig.FIGHTER_AMMO_ENABLED.get()));

        // 2. 舰装属性
        addSection(player, "舰装属性");
        addShipStats(player, "驱逐舰",
                ModShipsConfig.DESTROYER_ARMOR.get(),
                ModShipsConfig.DESTROYER_SPEED_BONUS.get(),
                ModShipsConfig.DESTROYER_KNOCKBACK_RESISTANCE.get());
        addShipStats(player, "轻巡舰",
                ModShipsConfig.LIGHT_CRUISER_ARMOR.get(),
                ModShipsConfig.LIGHT_CRUISER_SPEED_BONUS.get(),
                ModShipsConfig.LIGHT_CRUISER_KNOCKBACK_RESISTANCE.get());
        addShipStats(player, "重巡舰",
                ModShipsConfig.HEAVY_CRUISER_ARMOR.get(),
                ModShipsConfig.HEAVY_CRUISER_SPEED_BONUS.get(),
                ModShipsConfig.HEAVY_CRUISER_KNOCKBACK_RESISTANCE.get());
        addShipStats(player, "战列舰",
                ModShipsConfig.BATTLESHIP_ARMOR.get(),
                ModShipsConfig.BATTLESHIP_SPEED_BONUS.get(),
                ModShipsConfig.BATTLESHIP_KNOCKBACK_RESISTANCE.get());
        addShipStats(player, "航母",
                ModShipsConfig.CARRIER_ARMOR.get(),
                ModShipsConfig.CARRIER_SPEED_BONUS.get(),
                ModShipsConfig.CARRIER_KNOCKBACK_RESISTANCE.get());
        addShipStats(player, "潜艇",
                ModShipsConfig.SUBMARINE_ARMOR.get(),
                ModShipsConfig.SUBMARINE_SPEED_BONUS.get(),
                ModShipsConfig.SUBMARINE_KNOCKBACK_RESISTANCE.get());

        // 3. 飞机系统
        addSection(player, "飞机系统");
        addAircraftStats(player, "战斗机",
                ModAircraftConfig.FIGHTER_DAMAGE.get(),
                ModAircraftConfig.FIGHTER_SPEED.get(),
                ModAircraftConfig.FIGHTER_HEALTH.get());
        addAircraftStats(player, "火箭机",
                ModAircraftConfig.ROCKET_FIGHTER_DAMAGE.get(),
                ModAircraftConfig.ROCKET_FIGHTER_SPEED.get(),
                ModAircraftConfig.ROCKET_FIGHTER_HEALTH.get());
        addAircraftStats(player, "俯冲轰炸机",
                ModAircraftConfig.DIVE_BOMBER_DAMAGE.get(),
                ModAircraftConfig.DIVE_BOMBER_SPEED.get(),
                ModAircraftConfig.DIVE_BOMBER_HEALTH.get());
        addAircraftStats(player, "水平轰炸机",
                ModAircraftConfig.LEVEL_BOMBER_DAMAGE.get(),
                ModAircraftConfig.LEVEL_BOMBER_SPEED.get(),
                ModAircraftConfig.LEVEL_BOMBER_HEALTH.get());
        addAircraftStats(player, "鱼雷机",
                ModAircraftConfig.TORPEDO_BOMBER_DAMAGE.get(),
                ModAircraftConfig.TORPEDO_BOMBER_SPEED.get(),
                ModAircraftConfig.TORPEDO_BOMBER_HEALTH.get());
        addAircraftStats(player, "反潜机",
                ModAircraftConfig.ASW_AIRCRAFT_DAMAGE.get(),
                ModAircraftConfig.ASW_AIRCRAFT_SPEED.get(),
                ModAircraftConfig.ASW_AIRCRAFT_HEALTH.get());
        addConfigLine(player, "侦察机", String.format("速度%.1f 生命%.0f",
                ModAircraftConfig.RECON_AIRCRAFT_SPEED.get(),
                ModAircraftConfig.RECON_AIRCRAFT_HEALTH.get()));

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
        addConfigLine(player, "旧版装填HUD", formatBoolean(ModClientConfig.SHOW_LEGACY_RELOAD_HUD.get()));
        addConfigLine(player, "编组按钮", formatBoolean(ModClientConfig.FLIGHT_GROUP_ENABLED.get()));
        addConfigLine(player, "火控位置", ModClientConfig.FIRE_CONTROL_POSITION.get().toString());
        addConfigLine(player, "屏幕震动", ModClientConfig.SCREEN_SHAKE_MULTIPLIER.get() + "x");

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
     * 添加舰装属性行
     */
    private void addShipStats(Player player, String name, double armor, double speedBonus, double knockbackResist) {
        String stats = String.format("护甲%.1f 速度%s 击退抗性%d%%",
                armor,
                formatPercent(speedBonus),
                (int)(knockbackResist * 100));
        addConfigLine(player, name, stats);
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

    /**
     * 格式化百分比
     */
    private String formatPercent(double value) {
        if (value > 0.001) return "+" + (int)(value * 100) + "%";
        if (value < -0.001) return (int)(value * 100) + "%";
        return "±0%";
    }
}
