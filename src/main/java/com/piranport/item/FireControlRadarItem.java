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
 * 吸附范围按「区块」配置，运行时还会被服务端模拟距离二次钳制。
 *
 * <p>它与中键火控锁定列表（{@code FireControlManager} / {@code ClientFireControlData}）无关：
 * 后者是航空投放的锁定目标，本装备只做准星辅助。
 *
 * <p><b>注意：本物品尚未注册到 {@code AircraftItems}，也没有模型/贴图/配方。</b>
 * 它目前只是一份类型骨架，用于让已有的负重、槽位、瞄准吸附逻辑能编译并提前定型。
 * 注册时需要同步处理三处，否则会出现「注册了但装不上核心」或「有物品但客户端崩模型」：
 * <ol>
 *   <li>{@code AircraftItems} 中注册，并在 {@code ModItems} 转发；</li>
 *   <li>{@code ShipCoreItem} 强化槽白名单加上 {@code FireControlRadarItem}；</li>
 *   <li>{@code models/item/} + {@code textures/item/} + 双语 lang key + 配方。</li>
 * </ol>
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

    public int getSnapRangeChunks() {
        return snapRangeChunks;
    }

    /**
     * 吸附半径（格）。策划口径：32 区块 = 512 格；服务端模拟距离会二次钳制，
     * 所以实际生效半径取本值与模拟距离的较小者。
     */
    public double getSnapRangeBlocks() {
        return snapRangeChunks * 16.0;
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
