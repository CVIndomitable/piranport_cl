package com.piranport.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 变身事件 — 玩家激活或解除舰娘变身时发布。
 *
 * <p>该事件允许多个系统独立响应变身状态变化:
 * <ul>
 *   <li>属性系统: 应用护甲/速度/血量修饰器</li>
 *   <li>UI 系统: 显示变身提示消息</li>
 *   <li>粒子系统: 播放变身特效</li>
 *   <li>音效系统: 播放变身音效</li>
 * </ul>
 *
 * @param player 触发变身的玩家
 * @param core 舰装核心物品
 * @param activated {@code true} 表示激活变身, {@code false} 表示解除变身
 */
public record TransformationEvent(Player player, ItemStack core, boolean activated) {
}
