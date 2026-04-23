package com.piranport.compat;

import net.neoforged.bus.api.IEventBus;

public interface IModCompat {
    String modId();

    void init(IEventBus modEventBus);

    default String displayName() {
        return modId();
    }
}
