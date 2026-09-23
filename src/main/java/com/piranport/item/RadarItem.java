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
 * 雷达 — 舰装强化槽内的索敌设备，作用是「高亮目标」而非造成伤害。
 *
 * <p>三种雷达的探测目标互斥，由 {@link RadarTarget} 决定：
 * <ul>
 *   <li>{@link RadarTarget#SURFACE} 对海雷达 — 只标记水面/陆上目标</li>
 *   <li>{@link RadarTarget#AIR} 对空雷达 — 只高亮飞行中的目标</li>
 *   <li>{@link RadarTarget#SUBMARINE} 声纳 — 只高亮水下目标</li>
 * </ul>
 * 互斥是有意设计：单台雷达无法覆盖全部目标分层，玩家需要占用多个强化槽才能全向索敌，
 * 这与声呐、动力等强化件争夺槽位的取舍一致。
 */
public class RadarItem extends Item {

    /** 索敌目标分层 —— 决定本雷达能高亮哪一类实体。 */
    public enum RadarTarget {
        /** 对海：水面舰船、陆上生物等非空中、非水下的目标 */
        SURFACE("surface"),
        /** 对空：飞行中的目标 */
        AIR("air"),
        /** 声纳：水下目标 */
        SUBMARINE("submarine");

        private final String serializedName;

        RadarTarget(String serializedName) {
            this.serializedName = serializedName;
        }

        /** 用于翻译键拼接：tooltip.piranport.radar.target.<serializedName> */
        public String getSerializedName() {
            return serializedName;
        }
    }

    private final int weight;
    /** 索敌范围（格）。策划给定的「32 区块」在服务端会按模拟距离二次钳制，见 PlayerTickHandler。 */
    private final int range;
    private final RadarTarget target;

    public RadarItem(Properties properties, int weight, int range, RadarTarget target) {
        super(properties);
        this.weight = weight;
        this.range = range;
        this.target = target;
    }

    public int getWeight() {
        return weight;
    }

    public int getRange() {
        return range;
    }

    public RadarTarget getTarget() {
        return target;
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
                tooltip.add(Component.translatable("tooltip.piranport.radar.range", range)
                        .withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.translatable("tooltip.piranport.radar.target."
                                + target.getSerializedName())
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("tooltip.piranport.radar.weight", weight)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.piranport.shift_for_details")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
