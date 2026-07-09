/**
 * 事件处理器 — 玩家 Tick/连接/数据等全局事件的监听器。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.handler.PlayerTickHandler} — 玩家 Tick 驱动(变身/燃料/声呐/自动战斗)</li>
 *   <li>{@link com.piranport.handler.PlayerConnectionHandler} — 玩家登录/登出/死亡/维度切换</li>
 *   <li>{@link com.piranport.handler.PlayerAircraftHelper} — 飞机召回工具方法</li>
 * </ul>
 *
 * <h2>重构计划</h2>
 * <p>本包将逐步拆分为:
 * <ul>
 *   <li>{@code event/} 包 — 事件监听器(NeoForge EventBus)</li>
 *   <li>{@code service/} 包 — 业务服务接口</li>
 *   <li>{@code util/} 包 — 无状态工具类</li>
 * </ul>
 *
 * @see com.piranport.event
 * @since 1.0.0
 */
package com.piranport.handler;
