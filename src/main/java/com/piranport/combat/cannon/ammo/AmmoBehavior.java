package com.piranport.combat.cannon.ammo;

/** 炮弹在火炮战斗流程中的行为类别。 */
public enum AmmoBehavior {
    /** 高爆弹：普通爆炸和范围伤害。 */
    HE,
    /** 穿甲弹：命中后使用穿甲规则。 */
    AP,
    /** 可变时间/近炸引信弹。 */
    VT,
    /** 三式对空霰弹。 */
    TYPE3,
    /** MK23 特殊核炮弹，保留独立行为以避免与普通 HE 混淆。 */
    MK23;
}
