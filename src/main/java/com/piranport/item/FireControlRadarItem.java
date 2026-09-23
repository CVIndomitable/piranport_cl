package com.piranport.item;

import com.piranport.component.WeaponCategory;
import com.piranport.platform.ClientHooks;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 火控雷达 — 舰装强化槽内的瞄准辅助设备。
 *
 * <p>装入舰装核心后，玩家可用 0 键开启/关闭火控；开启时准星会向敌对目标吸附（火炮瞄准辅助）。
 * 吸附的<b>实际</b>生效半径只由「准星吸附处理器里的搜索半径常量」与「服务端下发的模拟距离」
 * 两者中较小的那个决定（见 {@code FireControlRadarSnapHandler}）。本物品构造参数里的
 * {@code snapRangeChunks} <b>不参与</b>吸附判定，只用于 tooltip 展示 —— 改它不会改变吸附手感。
 *
 * <p>它与中键火控锁定列表（{@code FireControlManager} / {@code ClientFireControlData}）无关：
 * 后者是航空投放的锁定目标，本装备只做准星辅助。
 *
 * <p>注册在 {@code AircraftItems#STANDARD_FIRE_CONTROL_RADAR}，模型/贴图/配方/双语 lang
 * 均已齐全。新增同类装备时记得同步 {@code ShipCoreItem} 的强化槽白名单，否则会「注册了但装不上」。
 */
public class FireControlRadarItem extends Item {

    private final int weight;
    private final int snapRangeChunks;

    public FireControlRadarItem(Properties properties, int weight, int snapRangeChunks) {
        super(properties);
        this.weight = weight;
        this.snapRangeChunks = snapRangeChunks;
    }

    public int getWeight() {
        return weight;
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
                tooltip.add(Component.translatable("tooltip.piranport.fire_control_radar.range", snapRangeChunks)
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.piranport.fire_control_radar.weight", weight)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
