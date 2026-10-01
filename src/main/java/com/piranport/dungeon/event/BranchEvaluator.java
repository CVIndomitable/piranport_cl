package com.piranport.dungeon.event;

import com.piranport.dungeon.data.BranchRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;

/**
 * 策划《副本/22》分歧带路纯逻辑：按书写顺序判定，第一条全部满足的规则生效，都不满足走 default。
 * 不碰 MC 注册表，便于单测。
 */
public final class BranchEvaluator {
    private BranchEvaluator() {}

    /**
     * @param hull   玩家当前舰体（small/medium/large/submarine），未变身为 null
     * @param escorts 随从数量（未开发 → 0）
     * @param roles  随从职能集合（未开发 → 空）
     * @param carries 物品 id 是否在背包中
     * @param random 每条带 chance 的规则各掷一次 [0,1)
     */
    public record Context(String hull, int escorts, Set<String> roles,
                          Predicate<String> carries, DoubleSupplier random) {}

    /** 单个条件的判定结果，用于聊天展示“条件 ✓/✗”。 */
    public record ConditionResult(String label, boolean met) {}

    public record RuleResult(BranchRule rule, List<ConditionResult> conditions, boolean matched) {}

    /**
     * @param target   最终目标节点（可能为 null：既无规则命中也无 default）
     * @param results  已判定的规则（命中后的规则不再判定）
     * @param diverted 是否被“沟”：没有任何规则命中而落到 default
     */
    public record Decision(String target, List<RuleResult> results, boolean diverted) {}

    public static Decision evaluate(List<BranchRule> rules, String branchDefault, Context ctx) {
        List<RuleResult> results = new ArrayList<>();
        for (BranchRule rule : rules) {
            RuleResult rr = evaluateRule(rule, ctx);
            results.add(rr);
            if (rr.matched()) {
                return new Decision(rule.to(), List.copyOf(results), false);
            }
        }
        return new Decision(branchDefault, List.copyOf(results), !rules.isEmpty());
    }

    static RuleResult evaluateRule(BranchRule rule, Context ctx) {
        List<ConditionResult> conds = new ArrayList<>();
        if (rule.hull() != null) {
            boolean ok = ctx.hull() != null && ctx.hull().equalsIgnoreCase(rule.hull());
            conds.add(new ConditionResult("舰体=" + hullName(rule.hull()), ok));
        }
        if (rule.escorts() != null) {
            conds.add(new ConditionResult("随从数" + normalizeComparison(rule.escorts()),
                    compare(ctx.escorts(), rule.escorts())));
        }
        if (rule.role() != null) {
            boolean ok = ctx.roles() != null && ctx.roles().contains(rule.role().toLowerCase(Locale.ROOT));
            conds.add(new ConditionResult("随从职能=" + rule.role(), ok));
        }
        if (rule.carry() != null) {
            boolean ok = ctx.carries() != null && ctx.carries().test(rule.carry());
            conds.add(new ConditionResult("携带 " + rule.carry(), ok));
        }
        if (rule.chance() != null) {
            double roll = ctx.random() == null ? 1.0 : ctx.random().getAsDouble();
            conds.add(new ConditionResult("概率 " + Math.round(rule.chance() * 100) + "%",
                    roll < rule.chance()));
        }
        boolean matched = true;
        for (ConditionResult c : conds) matched &= c.met();
        return new RuleResult(rule, List.copyOf(conds), matched);
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

    private static String normalizeComparison(String expr) {
        String s = expr.trim();
        return Character.isDigit(s.isEmpty() ? 'x' : s.charAt(0)) ? "=" + s : s;
    }

    public static boolean isValidHull(String hull) {
        if (hull == null) return false;
        return switch (hull.toLowerCase(Locale.ROOT)) {
            case "small", "medium", "large", "submarine" -> true;
            default -> false;
        };
    }

    public static String hullName(String hull) {
        if (hull == null) return "未变身";
        return switch (hull.toLowerCase(Locale.ROOT)) {
            case "small" -> "小型";
            case "medium" -> "中型";
            case "large" -> "大型";
            case "submarine" -> "潜艇";
            default -> hull;
        };
    }
}
