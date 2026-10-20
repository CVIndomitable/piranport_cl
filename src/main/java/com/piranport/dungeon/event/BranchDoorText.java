package com.piranport.dungeon.event;

import com.piranport.dungeon.data.BranchRule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 门上与暗门标题共用的条件文案。概率不出文案：它只决定门在不在。 */
public final class BranchDoorText {
    private BranchDoorText() {}

    /** 一条可展示的条件，bit 为 {@link BranchEvaluator} 中的条件位。 */
    public record Line(int bit, Component text) {}

    public static List<Line> conditions(BranchRule rule) {
        List<Line> lines = new ArrayList<>();
        if (rule.hull() != null) {
            lines.add(new Line(BranchEvaluator.HULL, Component.translatable(
                    "dungeon.piranport.branch.hull." + rule.hull().toLowerCase(Locale.ROOT))));
        }
        if (rule.escorts() != null) {
            String expr = rule.escorts().trim();
            Component text = expr.startsWith(">=")
                    ? Component.translatable("dungeon.piranport.branch.cond.escorts_min", expr.substring(2).trim())
                    : Component.translatable("dungeon.piranport.branch.cond.escorts_expr", expr);
            lines.add(new Line(BranchEvaluator.ESCORTS, text));
        }
        if (rule.role() != null) {
            lines.add(new Line(BranchEvaluator.ROLE,
                    Component.translatable("dungeon.piranport.branch.cond.role", rule.role())));
        }
        if (rule.carry() != null) {
            lines.add(new Line(BranchEvaluator.CARRY,
                    Component.translatable("dungeon.piranport.branch.cond.carry", itemName(rule.carry()))));
        }
        return lines;
    }

    public static Component join(List<Component> parts) {
        Component separator = Component.translatable("dungeon.piranport.branch.cond.join");
        Component out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out = out.copy().append(separator);
            out = out.copy().append(parts.get(i));
        }
        return out;
    }

    private static Component itemName(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) return Component.literal(id);
        Item item = BuiltInRegistries.ITEM.get(rl);
        return Component.translatable(item.getDescriptionId());
    }
}
