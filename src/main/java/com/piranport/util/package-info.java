/**
 * 工具类集合 — 无状态的数学/碰撞/序列化等辅助工具。
 *
 * <h2>设计原则</h2>
 * <ul>
 *   <li>所有工具类都是无状态的(只包含静态方法)</li>
 *   <li>私有构造函数禁止实例化</li>
 *   <li>方法参数和返回值都是不可变对象或基本类型</li>
 * </ul>
 *
 * <h2>规划中的工具类</h2>
 * <ul>
 *   <li>{@code MathUtils} — 数学计算(三角/插值/随机)</li>
 *   <li>{@code CollisionUtils} — 碰撞检测/射线追踪</li>
 *   <li>{@code NBTUtils} — NBT 序列化工具</li>
 *   <li>{@code NetworkUtils} — 网络包工具方法</li>
 * </ul>
 *
 * @since 1.1.0
 */
package com.piranport.util;
