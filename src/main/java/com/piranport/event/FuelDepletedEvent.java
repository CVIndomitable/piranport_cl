package com.piranport.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 燃料耗尽事件 — 舰装核心燃料耗尽时发布。
 *
 * <p>该事件在玩家移动消耗燃料后检测到燃料为 0 时触发。
 * 订阅者可以响应该事件执行:
 * <ul>
 *   <li>自动解除变身</li>
 *   <li>清除属性修饰器</li>
 *   <li>召回所有飞机</li>
 *   <li>显示燃料耗尽提示</li>
 * </ul>
 *
 * @param player 燃料耗尽的玩家
 * @param core 燃料耗尽的舰装核心
 * @since 1.1.0
 */
public record FuelDepletedEvent(Player player, ItemStack core) {
}
