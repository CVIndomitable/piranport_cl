package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.platform.ClientHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** 客户端专属入口，在注册阶段构造物品之前安装客户端能力。 */
@Mod(value = PiranPort.MOD_ID, dist = Dist.CLIENT)
public final class PiranPortClient {
    public PiranPortClient() {
        ClientHooks.install(new ClientItemHooks());
    }
}
