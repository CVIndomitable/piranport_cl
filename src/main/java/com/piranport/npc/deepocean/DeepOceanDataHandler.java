package com.piranport.npc.deepocean;

import com.piranport.PiranPort;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * 深海 NPC 数据加载器的事件桥接。
 * 依据：策划决策/架构/03-数据驱动vs硬编码.md
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class DeepOceanDataHandler {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new DeepOceanDataLoader());
    }
}