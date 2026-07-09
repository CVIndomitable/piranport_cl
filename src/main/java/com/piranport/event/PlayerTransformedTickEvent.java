package com.piranport.event;

import net.minecraft.world.entity.player.Player;

/**
 * 玩家 Tick 事件 — 服务端每 tick 发布一次(仅变身状态下)。
 *
 * <p>该事件在 {@link com.piranport.handler.PlayerTickHandler} 中发布,
 * 用于驱动各个子系统的 tick 逻辑:
 * <ul>
 *   <li>燃料消耗系统</li>
 *   <li>水面行走系统</li>
 *   <li>声呐扫描系统</li>
 *   <li>自动战斗系统</li>
 * </ul>
 *
 * <h2>性能考量</h2>
 * <p>该事件每 tick 发布一次,监听器应避免:
 * <ul>
 *   <li>重复计算(使用缓存或错峰执行)</li>
 *   <li>阻塞操作(I/O/网络请求)</li>
 *   <li>大量实体查询(使用区域限制)</li>
 * </ul>
 *
 * @param player 正在 tick 的玩家(已确认处于变身状态)
 * @param tickCount 当前 tick 计数(player.tickCount)
 * @since 1.1.0
 */
public record PlayerTransformedTickEvent(Player player, int tickCount) {
}
