package com.piranport.compat;

import com.piranport.PiranPort;
import com.piranport.compat.maid.MaidCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import java.util.List;

public final class ModCompats {
    private static final List<IModCompat> COMPATS = List.of(
            new MaidCompat()
    );

    private ModCompats() {}

    public static void initialize(IEventBus modEventBus) {
        for (IModCompat compat : COMPATS) {
            String id = compat.modId();
            if (!ModList.get().isLoaded(id)) {
                PiranPort.LOGGER.debug("[Compat] Skipped {} ({}) — mod not present", compat.displayName(), id);
                continue;
            }
            try {
                compat.init(modEventBus);
                PiranPort.LOGGER.info("[Compat] Loaded {} ({})", compat.displayName(), id);
            } catch (Throwable t) {
                PiranPort.LOGGER.error("[Compat] Failed to load {} ({})", compat.displayName(), id, t);
            }
        }
    }
}
