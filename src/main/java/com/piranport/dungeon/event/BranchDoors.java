package com.piranport.dungeon.event;

import com.piranport.dungeon.data.BranchRule;
import com.piranport.dungeon.data.NodeData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * 分歧节点出口门的亮灭快照。每个目标节点一扇门；同一目标下任一规则全满足即亮。
 *
 * <ul>
 *   <li>{@link State#LIT}：可走入，走入即写入 branchChoices。</li>
 *   <li>{@link State#DARK}：暗门。已清目标（mask 恒为 0）或条件未满足（mask 为未满足位）。</li>
 *   <li>{@link State#HIDDEN}：概率未通过或已被别的路线选走，玩家看不到这扇门。</li>
 *   <li>{@link State#PASSED}：玩家已经从这扇门走过，门灯熄灭但仍可回走。</li>
 * </ul>
 */
public final class BranchDoors {
    private BranchDoors() {}

    public enum State { HIDDEN, LIT, DARK, PASSED }

    /** ruleIndex 指向 {@code NodeData.branches()} 中用于展示的规则；无规则时为 -1。 */
    public record Door(String target, State state, int unmetMask, int ruleIndex) {}

    /**
     * @param chosen 玩家已选定的目标；非 null 时整组门按选择重算，不再掷概率
     */
    public static List<Door> compute(NodeData from, BranchEvaluator.Context ctx,
                                     Predicate<String> cleared, String chosen) {
        LinkedHashSet<String> targets = new LinkedHashSet<>();
        for (BranchRule rule : from.branches()) targets.add(rule.to());
        if (from.branchDefault() != null) targets.add(from.branchDefault());

        List<Door> doors = new ArrayList<>(targets.size());
        for (String target : targets) {
            if (chosen != null) {
                doors.add(new Door(target, target.equals(chosen) ? State.PASSED : State.HIDDEN, 0, -1));
            } else {
                doors.add(doorFor(from, target, ctx, cleared));
            }
        }
        return List.copyOf(doors);
    }

    private static Door doorFor(NodeData from, String target, BranchEvaluator.Context ctx,
                                Predicate<String> cleared) {
        if (cleared.test(target)) return new Door(target, State.DARK, 0, -1);
        if (target.equals(from.branchDefault())) return new Door(target, State.LIT, 0, -1);

        int bestRule = -1;
        int bestMask = 0;
        for (int i = 0; i < from.branches().size(); i++) {
            BranchRule rule = from.branches().get(i);
            if (!rule.to().equals(target)) continue;
            if (!BranchEvaluator.chancePasses(rule, ctx)) continue;
            int mask = BranchEvaluator.unmetMask(rule, ctx);
            if (bestRule < 0 || Integer.bitCount(mask) < Integer.bitCount(bestMask)) {
                bestRule = i;
                bestMask = mask;
            }
            if (mask == 0) break;
        }
        if (bestRule < 0) return new Door(target, State.HIDDEN, 0, -1);
        return bestMask == 0
                ? new Door(target, State.LIT, 0, bestRule)
                : new Door(target, State.DARK, bestMask, bestRule);
    }
}
