/**
 * 战斗系统 — 玩家变身、伤害计算、武器系统等核心战斗逻辑。
 *
 * <h2>核心组件</h2>
 * <ul>
 *   <li>{@link com.piranport.combat.TransformationManager} — 变身状态管理</li>
 *   <li>{@link com.piranport.combat.BallisticSolver} — 弹道求解器</li>
 *   <li>{@link com.piranport.combat.SalvoManager} — 齐射调度器</li>
 *   <li>{@link com.piranport.combat.TorpedoGuidanceManager} — 鱼雷制导</li>
 * </ul>
 *
 * <h2>职责范围</h2>
 * <ul>
 *   <li>变身系统: 玩家从人类形态到舰娘形态的切换</li>
 *   <li>负重系统: 武器/装甲的重量计算与超重惩罚</li>
 *   <li>伤害计算: 护甲减免/穿透/友军伤害判定</li>
 *   <li>弹道系统: 火炮弹道求解/鱼雷制导</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.piranport.combat;
