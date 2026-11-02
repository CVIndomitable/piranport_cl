/**
 * 事件处理器 — 玩家 Tick/连接/数据等全局事件的监听器。
 *
 * <h2>玩家 Tick 职责</h2>
 * <p>{@link com.piranport.handler.PlayerTickHandler} 编排舰装玩法的逻辑端判断、执行顺序与生命周期。
 * 以下模块均为包内可见，不自行订阅事件，也不相互调用；游戏状态仍在服务端主线程更新：
 * <ul>
 *   <li>{@link com.piranport.handler.PlayerLoadoutTick} — 核心槽位、变身、负重属性；10 tick检查</li>
 *   <li>{@link com.piranport.handler.PlayerFuelTick} — 移动距离与燃料；5 tick采样，独占距离缓存</li>
 *   <li>{@link com.piranport.handler.PlayerEquipmentTick} — 装备被动与潜艇效果</li>
 *   <li>{@link com.piranport.handler.PlayerReconTick} — 侦察身体锁定、20 tick残留减速清理</li>
 *   <li>{@link com.piranport.handler.PlayerDetectionTick} — 声呐/雷达，40 tick按UUID错峰扫描</li>
 *   <li>{@link com.piranport.handler.PlayerAutoCombatTick} — 40 tick自动升空/导弹、每tick近防炮</li>
 * </ul>
 *
 * <h2>执行顺序与清理约束</h2>
 * <p>服务端先执行装备被动、负重/变身检查和侦察锁身；确认变身后扣燃料，
 * 耗尽时先清缓存再发布事件，入口重新判断变身状态，停止后续战斗。
 * 水上行走仍由两端各自的玩家实例执行。
 * <p>火炮与飞机装填到期结算始终由入口直接调用，不受自动模式开关影响。
 * 自动升空/导弹之后衰减防空静默窗口，再执行近防炮，不能合并到同一个40 tick周期。
 * <p>登出、停服、离线清理及终端参数变更仍使用 {@code PlayerTickHandler} 原有公开方法，
 * 由它转发给缓存所有者。未变身时只清移动距离，不清负重模块的缺油提示哨兵。
 *
 * <h2>其他入口</h2>
 * <ul>
 *   <li>{@link com.piranport.handler.PlayerConnectionHandler} — 玩家登录/登出/死亡/维度切换</li>
 *   <li>{@link com.piranport.handler.PlayerAircraftHelper} — 飞机召回工具方法</li>
 * </ul>
 *
 * @see com.piranport.event
 * @since 1.0.0
 */
package com.piranport.handler;
