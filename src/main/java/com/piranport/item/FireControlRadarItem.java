package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.core.registries.BuiltInRegistries;
import com.piranport.terminal.TerminalParameters;

import java.util.List;

/**
 * 火控雷达 — 舰装强化槽内的瞄准辅助设备。
 *
 * <p>装入舰装核心后，玩家可用 0 键开启/关闭火控；开启时准星会向敌对目标吸附（火炮瞄准辅助）。
 * 吸附的<b>实际</b>生效半径只由「准星吸附处理器里的搜索半径常量」与「服务端下发的模拟距离」
 * 两者中较小的那个决定（见 {@code FireControlRadarSnapHandler}）。本物品构造参数里的
 * {@code snapRangeChunks} <b>不参与</b>吸附判定，只用于 tooltip 展示 —— 改它不会改变吸附手感。
 *
 * <p>炮弹追踪（策划决策/火控/05）：装备后若 {@code FireControlManager} 存在火控锁定目标，
 * 发射的炮弹在距该目标 {@code levelN_range} 格内每 tick 按转向系数偏转，见
 * {@code FireControlRadarTracking}。追踪与 0 键吸附开关无关，只看装备与锁定。
 *
 * <p>注册在 {@code AircraftItems#STANDARD_FIRE_CONTROL_RADAR}，模型/贴图/配方/双语 lang
 * 均已齐全。新增同类装备时记得同步 {@code ShipCoreItem} 的强化槽白名单，否则会「注册了但装不上」。
 */
public class FireControlRadarItem extends Item {

    private final int weight;
    private final int snapRangeChunks;
    /** 追踪等级 1～3（策划决策/火控/05 §1），决定炮弹追踪范围与转向系数。 */
    private final int level;

    public FireControlRadarItem(Properties properties, int weight, int snapRangeChunks) {
        this(properties, weight, snapRangeChunks, 1);
    }

    public FireControlRadarItem(Properties properties, int weight, int snapRangeChunks, int level) {
        super(properties);
        this.weight = weight;
        this.snapRangeChunks = snapRangeChunks;
        this.level = Math.max(1, Math.min(3, level));
    }

    public int getLevel() { return level; }

    public int getWeight() {
        return TerminalParameters.getInt(parameterKey("weight"), weight);
    }
    public int getBaseWeight() { return weight; }

    public int getSnapRangeChunks() {
        return TerminalParameters.getInt(parameterKey("range"), snapRangeChunks);
    }
    public int getBaseSnapRangeChunks() { return snapRangeChunks; }

    private String parameterKey(String property) {
        var id = BuiltInRegistries.ITEM.getKey(this);
        return "equipment." + (id == null ? "fire_control_radar" : id) + "." + property;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
        if (cat != null) {
            tooltip.add(Component.translatable("tooltip.piranport.weapon_category." + cat.getSerializedName())
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
        if (ClientHooks.isClient()) {
            if (ClientHooks.hasShiftDown()) {
                tooltip.add(Component.translatable("tooltip.piranport.fire_control_radar.range", getSnapRangeChunks())
                        .withStyle(ChatFormatting.AQUA));
                var tracking = com.piranport.combat.cannon.FireControlRadarTracking.forLevel(level);
                tooltip.add(Component.translatable("tooltip.piranport.fire_control_radar.tracking",
                                level, String.format("%.0f", tracking.range()),
                                String.format("%.2f", tracking.turnCoefficient()))
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.piranport.fire_control_radar.weight", getWeight())
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
