package com.piranport.dungeon.event;

import com.piranport.dungeon.data.BranchRule;

import java.util.Locale;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * 《副本/22》分歧条件的纯判定：每条规则算出未满足的条件位掩码，不碰 MC 注册表，便于单测。
 * 门的亮灭由 {@link BranchDoors} 按“同一目标的任一规则全满足即亮”汇总。
 */
public final class BranchEvaluator {
    private BranchEvaluator() {}

    public static final int HULL = 1;
    public static final int ESCORTS = 2;
    public static final int ROLE = 4;
    public static final int CARRY = 8;

    /**
     * @param hull   玩家当前舰体（small/medium/large/submarine），未变身为 null
     * @param escorts 随从数量（未开发 → 0）
     * @param roles  随从职能集合（未开发 → 空）
     * @param carries 物品 id 是否在背包中
     * @param random 每条带 chance 的规则各掷一次 [0,1)
     */
    public record Context(String hull, int escorts, Set<String> roles,
                          Predicate<String> carries, DoubleSupplier random) {}

    /** 规则中未满足的非概率条件位（HULL/ESCORTS/ROLE/CARRY 的或）；0 表示全部满足。 */
    public static int unmetMask(BranchRule rule, Context ctx) {
        int mask = 0;
        if (rule.hull() != null && !(ctx.hull() != null && ctx.hull().equalsIgnoreCase(rule.hull()))) {
            mask |= HULL;
        }
        if (rule.escorts() != null && !compare(ctx.escorts(), rule.escorts())) {
            mask |= ESCORTS;
        }
        if (rule.role() != null && !(ctx.roles() != null
                && ctx.roles().contains(rule.role().toLowerCase(Locale.ROOT)))) {
            mask |= ROLE;
        }
        if (rule.carry() != null && !(ctx.carries() != null && ctx.carries().test(rule.carry()))) {
            mask |= CARRY;
        }
        return mask;
    }

    /**
     * 概率只决定门在这一次快照里是否存在，不向玩家展示百分比：未通过则该规则不出门。
     */
    public static boolean chancePasses(BranchRule rule, Context ctx) {
        if (rule.chance() == null) return true;
        double roll = ctx.random() == null ? 1.0 : ctx.random().getAsDouble();
        return roll < rule.chance();
    }

    /** 解析 ">=N" / "<=N" / ">N" / "<N" / "==N" / "=N" / "N"（等于）。格式错误视为不满足。 */
    public static boolean compare(int value, String expr) {
        if (expr == null) return true;
        String s = expr.trim();
        String op;
        if (s.startsWith(">=") || s.startsWith("<=") || s.startsWith("==")) {
            op = s.substring(0, 2);
            s = s.substring(2);
        } else if (s.startsWith(">") || s.startsWith("<") || s.startsWith("=")) {
            op = s.substring(0, 1);
            s = s.substring(1);
        } else {
            op = "==";
        }
        int n;
        try {
            n = Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return switch (op) {
            case ">=" -> value >= n;
            case "<=" -> value <= n;
            case ">" -> value > n;
            case "<" -> value < n;
            default -> value == n;
        };
    }

    public static boolean isValidComparison(String expr) {
        if (expr == null) return false;
        String s = expr.trim().replaceFirst("^(>=|<=|==|>|<|=)", "").trim();
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidHull(String hull) {
        if (hull == null) return false;
        return switch (hull.toLowerCase(Locale.ROOT)) {
            case "small", "medium", "large", "submarine" -> true;
            default -> false;
        };
    }
}
