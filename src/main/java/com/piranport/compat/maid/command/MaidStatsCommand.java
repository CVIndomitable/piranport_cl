package com.piranport.compat.maid.command;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.piranport.compat.maid.combat.MaidCombatStats;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

public class MaidStatsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("maidstats")
                        .then(Commands.argument("maid", EntityArgument.entity())
                                .executes(MaidStatsCommand::showStats)
                                .then(Commands.literal("reset")
                                        .requires(src -> src.hasPermission(2))
                                        .executes(MaidStatsCommand::resetStats)
                                )
                        )
        );
    }

    private static int showStats(CommandContext<CommandSourceStack> context) {
        try {
            Entity entity = EntityArgument.getEntity(context, "maid");
            if (!(entity instanceof EntityMaid maid)) {
                context.getSource().sendFailure(Component.literal("§c目标不是女仆实体"));
                return 0;
            }

            MaidCombatStats.Stats stats = MaidCombatStats.get(maid);
            String name = maid.hasCustomName() ? maid.getCustomName().getString() : "女仆";

            context.getSource().sendSuccess(() ->
                    Component.translatable("command.piranport.maidstats.header", name), false);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.piranport.maidstats.kills", stats.kills), false);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.piranport.maidstats.damage", stats.totalDamage), false);
            context.getSource().sendSuccess(() ->
                    Component.translatable("command.piranport.maidstats.shots", stats.shotsFired), false);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c命令执行失败: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetStats(CommandContext<CommandSourceStack> context) {
        try {
            Entity entity = EntityArgument.getEntity(context, "maid");
            if (!(entity instanceof EntityMaid maid)) {
                context.getSource().sendFailure(Component.literal("§c目标不是女仆实体"));
                return 0;
            }

            MaidCombatStats.reset(maid);
            String name = maid.hasCustomName() ? maid.getCustomName().getString() : "女仆";

            context.getSource().sendSuccess(() ->
                    Component.translatable("command.piranport.maidstats.reset", name), true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§c命令执行失败: " + e.getMessage()));
            return 0;
        }
    }
}
