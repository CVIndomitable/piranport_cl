package com.piranport.rigging;

import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * 一个挂点上的装备。
 *
 * <p>{@code stack} 是 count=1 的展示副本：同步与渲染都只需要外观，不携带真实数量，
 * 也避免客户端误把展示栈当成可操作物品。
 */
public record RiggingMount(RiggingSocket socket, RiggingCategory category, ItemStack stack) {

    public RiggingMount {
        Objects.requireNonNull(socket, "socket");
        Objects.requireNonNull(category, "category");
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Rigging mount stack must not be empty");
        }
        ItemStack display = stack.copy();
        display.setCount(1);
        stack = display;
    }
}
