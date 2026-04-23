package com.piranport.compat.maid;

import com.piranport.compat.maid.combat.MaidCombatEvents;
import com.piranport.compat.maid.command.MaidStatsCommand;
import com.piranport.compat.maid.task.TaskInjector;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class MaidCompatLoader {
    private MaidCompatLoader() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.register(TaskInjector.class);
        NeoForge.EVENT_BUS.register(MaidCombatEvents.class);
        NeoForge.EVENT_BUS.addListener(MaidCompatLoader::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        MaidStatsCommand.register(event.getDispatcher());
    }
}
