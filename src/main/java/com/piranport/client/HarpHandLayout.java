package com.piranport.client;

import com.piranport.item.UnicornHarpItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** 只决定手持渲染读哪个槽，不改惯用手、物品栏和实际使用手。 */
public enum HarpHandLayout {
    VANILLA, MAIN_HARP, OFF_HARP;

    static HarpHandLayout of(boolean main, boolean off) {
        return main ? MAIN_HARP : off ? OFF_HARP : VANILLA;
    }

    public static HarpHandLayout of(Player player) {
        return of(player.getMainHandItem().getItem() instanceof UnicornHarpItem,
                player.getOffhandItem().getItem() instanceof UnicornHarpItem);
    }

    public HumanoidArm visualMainArm(HumanoidArm normal) {
        return switch (this) {
            case VANILLA -> normal;
            case MAIN_HARP -> HumanoidArm.LEFT;
            case OFF_HARP -> HumanoidArm.RIGHT;
        };
    }

    boolean hides(InteractionHand slot, boolean performing) {
        return this == MAIN_HARP && slot == InteractionHand.OFF_HAND
                || this == OFF_HARP && performing && slot == InteractionHand.MAIN_HAND;
    }

    @Nullable InteractionHand visibleSlot(HumanoidArm normal, HumanoidArm arm, boolean performing) {
        InteractionHand slot = arm == visualMainArm(normal) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        return hides(slot, performing) ? null : slot;
    }

    public ItemStack visibleStack(Player player, HumanoidArm arm, boolean performing) {
        InteractionHand slot = visibleSlot(player.getMainArm(), arm, performing);
        return slot == null ? ItemStack.EMPTY : player.getItemInHand(slot);
    }
}
