package com.piranport.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 负重变化事件 — 玩家快捷栏武器负重发生变化时发布。
 *
 * <p>该事件在以下情况触发:
 * <ul>
 *   <li>快捷栏武器变化(切换/丢弃/拾取)</li>
 *   <li>核心内装甲板/引擎变化</li>
 *   <li>首次变身(初始负重计算)</li>
 * </ul>
 *
 * <p>订阅者可以响应该事件:
 * <ul>
 *   <li>重新计算移动速度(基于负重比例)</li>
 *   <li>应用超重惩罚(挖掘疲劳/虚弱/中毒)</li>
 *   <li>更新 HUD 显示</li>
 * </ul>
 *
 * @param player 负重变化的玩家
 * @param core 舰装核心
 * @param totalLoad 当前总负重(武器 + 装甲)
 * @param maxLoad 最大负重(由舰装类型决定)
 * @since 1.1.0
 */
public record LoadChangedEvent(Player player, ItemStack core, int totalLoad, int maxLoad) {
}
