/**
 * 事件监听器 — 响应领域事件的独立监听器。
 *
 * <h2>设计原则</h2>
 * <p>本包中的监听器遵循以下原则:
 * <ul>
 *   <li><b>单一职责</b>: 每个监听器只处理一个关注点(粒子/消息/属性)</li>
 *   <li><b>无状态</b>: 监听器不持有状态,所有逻辑都是纯函数</li>
 *   <li><b>可组合</b>: 多个监听器可独立订阅同一事件</li>
 *   <li><b>声明式注册</b>: 通过静态 {@code register()} 方法注册</li>
 * </ul>
 *
 * <h2>vs 传统 EventBusSubscriber</h2>
 * <table>
 *   <tr><th>维度</th><th>事件监听器</th><th>EventBusSubscriber</th></tr>
 *   <tr><td>注册方式</td><td>程序化(register方法)</td><td>声明式(@Subscribe注解)</td></tr>
 *   <tr><td>职责范围</td><td>单一关注点</td><td>可能混合多个关注点</td></tr>
 *   <tr><td>测试性</td><td>易测试(纯函数)</td><td>难测试(静态方法)</td></tr>
 *   <tr><td>动态性</td><td>可动态启用/禁用</td><td>类加载时固定</td></tr>
 * </table>
 *
 * <h2>注册流程</h2>
 * <p>所有监听器应在模组初始化时注册:
 * <pre>{@code
 * // PiranPort 构造函数中
 * TransformationAttributeListener.register();
 * TransformationMessageListener.register();
 * TransformationParticleListener.register();
 * }</pre>
 *
 * <h2>现有监听器</h2>
 * <ul>
 *   <li>{@link com.piranport.event.listener.TransformationAttributeListener} — 变身属性应用</li>
 *   <li>{@link com.piranport.event.listener.TransformationMessageListener} — 变身消息提示</li>
 *   <li>{@link com.piranport.event.listener.TransformationParticleListener} — 变身粒子效果</li>
 * </ul>
 *
 * <h2>命名规范</h2>
 * <ul>
 *   <li>类名格式: {@code <Event><Aspect>Listener}</li>
 *   <li>示例: {@code TransformationAttributeListener}, {@code FuelDepletedMessageListener}</li>
 *   <li>避免: {@code TransformationListener}(过于宽泛)</li>
 * </ul>
 *
 * @see com.piranport.event.EventBus
 * @see com.piranport.event.TransformationEvent
 * @since 1.1.0
 */
package com.piranport.event.listener;
