/**
 * 航空系统 — 飞机召唤、火控锁定、侦察模式等航空作战逻辑。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.aviation.FireControlManager} — 火控锁定管理</li>
 *   <li>{@link com.piranport.aviation.ReconManager} — 侦察模式管理</li>
 *   <li>{@link com.piranport.aviation.AircraftIndex} — 飞机实体索引</li>
 * </ul>
 *
 * <h2>职责范围</h2>
 * <ul>
 *   <li>火控系统: 多目标锁定/自动瞄准</li>
 *   <li>侦察系统: 远程视野/身体锁定</li>
 *   <li>飞机管理: 召回/燃料/索引维护</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.piranport.aviation;
