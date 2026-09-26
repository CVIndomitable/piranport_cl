package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.terminal.TerminalParameters;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** 断开世界时不继承上一服务器的参数镜像。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class TerminalClientEvents {
    private TerminalClientEvents() { }

    @SubscribeEvent
    public static void disconnected(ClientPlayerNetworkEvent.LoggingOut event) {
        TerminalParameters.clearClient();
    }
}
