/**
 * 领域事件系统 — 解耦跨系统通信的发布/订阅机制。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.event.EventBus} — 事件总线核心(单例模式)</li>
 *   <li>{@link com.piranport.event.TransformationEvent} — 变身事件</li>
 * </ul>
 *
 * <h2>设计模式</h2>
 * <p>采用发布/订阅模式(Observer Pattern)解耦发布者和订阅者:
 * <ul>
 *   <li>发布者: 通过 {@link com.piranport.event.EventBus#post(Object)} 发布事件</li>
 *   <li>订阅者: 通过 {@link com.piranport.event.EventBus#subscribe(Class, java.util.function.Consumer)} 注册监听器</li>
 *   <li>事件: 使用 {@code record} 定义不可变事件对象</li>
 * </ul>
 *
 * <h2>vs Minecraft/NeoForge EventBus</h2>
 * <table>
 *   <tr><th>维度</th><th>领域事件总线</th><th>Minecraft EventBus</th></tr>
 *   <tr><td>作用域</td><td>模组内部业务逻辑解耦</td><td>模组间 API 交互</td></tr>
 *   <tr><td>性能</td><td>轻量级(无反射/无注解扫描)</td><td>基于 ASM 字节码生成</td></tr>
 *   <tr><td>类型安全</td><td>编译期类型检查</td><td>运行时注解匹配</td></tr>
 *   <tr><td>注册时机</td><td>程序化注册(初始化时)</td><td>声明式注册(类加载时)</td></tr>
 * </table>
 *
 * <h2>使用指南</h2>
 * <p><b>1. 定义事件</b>
 * <pre>{@code
 * public record WeaponFiredEvent(Player player, ItemStack weapon, Vec3 targetPos) {}
 * }</pre>
 *
 * <p><b>2. 订阅事件</b>(在模组初始化或事件监听器初始化时)
 * <pre>{@code
 * EventBus.getInstance().subscribe(WeaponFiredEvent.class, event -> {
 *     PiranPort.LOGGER.info("Player {} fired weapon at {}",
 *         event.player().getName(), event.targetPos());
 * });
 * }</pre>
 *
 * <p><b>3. 发布事件</b>(在业务逻辑触发点)
 * <pre>{@code
 * EventBus.getInstance().post(new WeaponFiredEvent(player, weapon, target));
 * }</pre>
 *
 * <h2>性能考量</h2>
 * <ul>
 *   <li>事件处理是<b>同步的</b>,避免在监听器中执行长时间操作</li>
 *   <li>使用 {@link java.util.concurrent.CopyOnWriteArrayList} 避免迭代时的并发修改</li>
 *   <li>读多写少场景(订阅在初始化时完成,运行时只发布事件)</li>
 *   <li>如需异步处理,监听器内部应使用 {@link net.minecraft.server.level.ServerLevel#getServer()} 获取调度器</li>
 * </ul>
 *
 * <h2>测试支持</h2>
 * <ul>
 *   <li>{@link com.piranport.event.EventBus#getListenerCount(Class)} — 查询监听器数量</li>
 *   <li>{@link com.piranport.event.EventBus#clearAll()} — 清空所有监听器(测试后清理)</li>
 *   <li>{@link com.piranport.event.EventBus#unsubscribe(Class, java.util.function.Consumer)} — 取消订阅</li>
 * </ul>
 *
 * @since 1.1.0
 */
package com.piranport.event;
