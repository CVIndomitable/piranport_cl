package com.piranport.compat.maid;

import com.piranport.compat.IModCompat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

public final class MaidCompat implements IModCompat {
    public static final String TLM_MOD_ID = "touhou_little_maid";

    @Override
    public String modId() {
        return TLM_MOD_ID;
    }

    @Override
    public String displayName() {
        return "Touhou Little Maid";
    }

    @Override
    public void init(IEventBus modEventBus) {
        MaidCompatLoader.init(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MaidCompatClient.init();
        }
    }
}
