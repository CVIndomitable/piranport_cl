/**
 * 航空系统 — 飞机定义、出击准备、火控锁定、侦察模式等航空作战逻辑。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.aviation.AircraftDefinition} — 数据包驱动的机型定义</li>
 *   <li>{@link com.piranport.aviation.AircraftCatalog} — 物品注册与定义的集中校验目录</li>
 *   <li>{@link com.piranport.aviation.AircraftFireStrategy} — 放飞、装填、召回</li>
 *   <li>{@link com.piranport.aviation.FireControlManager} — 火控锁定管理</li>
 *   <li>{@link com.piranport.aviation.ReconManager} — 侦察模式管理</li>
 *   <li>{@link com.piranport.aviation.AircraftIndex} — 飞机实体索引</li>
 * </ul>
 *
 * <p>攻击战术在 {@code aviation.combat.strategy}，飞行主循环在
 * {@code entity.AircraftFlight}。定义 ID 与飞机物品注册名一一对应。
 *
 * @since 1.0.0
 */
package com.piranport.aviation;
