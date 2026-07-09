/**
 * 业务服务层 — 有状态的业务逻辑服务接口。
 *
 * <h2>设计模式</h2>
 * <p>采用服务接口 + 单例实现模式:
 * <pre>{@code
 * public interface TransformationService {
 *     boolean isTransformed(ItemStack stack);
 * }
 *
 * public final class TransformationServiceImpl implements TransformationService {
 *     private static final TransformationService INSTANCE = new TransformationServiceImpl();
 *     public static TransformationService getInstance() { return INSTANCE; }
 *
 *     @Override
 *     public boolean isTransformed(ItemStack stack) { ... }
 * }
 * }</pre>
 *
 * <h2>vs Manager 类</h2>
 * <table>
 *   <tr><th>维度</th><th>Service</th><th>Manager</th></tr>
 *   <tr><td>接口</td><td>接口 + 实现分离</td><td>静态方法类</td></tr>
 *   <tr><td>可测试性</td><td>易 mock</td><td>难 mock</td></tr>
 *   <tr><td>扩展性</td><td>可替换实现</td><td>硬编码</td></tr>
 * </table>
 *
 * <h2>迁移计划</h2>
 * <p>现有 Manager 类将逐步迁移为 Service 接口:
 * <ul>
 *   <li>{@code TransformationManager} → {@code TransformationService}</li>
 *   <li>{@code FireControlManager} → {@code FireControlService}</li>
 *   <li>{@code ReconManager} → {@code ReconService}</li>
 * </ul>
 *
 * @since 1.1.0
 */
package com.piranport.service;
