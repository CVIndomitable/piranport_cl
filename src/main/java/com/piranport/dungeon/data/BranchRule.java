package com.piranport.dungeon.data;

/**
 * 策划《副本/22》分歧带路：一条有序判定规则。条件全部满足（AND）时带往 {@code to}。
 * 条件字段为 null 表示该维度不限制。
 *
 * @param hull    舰体：small / medium / large / submarine（不区分大小写）
 * @param escorts 随从数量比较式，如 ">=2"、"<=1"、"==0"、"3"（随从系统未开发，按 0 计）
 * @param role    随从职能（随从系统未开发，按“无”计，写了即不满足）
 * @param carry   携带物品 id（背包内任意一格即可）
 * @param chance  概率 0~1；null 表示不掷骰
 * @param to      满足时的目标节点
 */
public record BranchRule(String hull, String escorts, String role, String carry, Double chance, String to) {
    public boolean hasAnyCondition() {
        return hull != null || escorts != null || role != null || carry != null || chance != null;
    }
}
