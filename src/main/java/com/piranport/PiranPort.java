package com.piranport;

import com.mojang.logging.LogUtils;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModCreativeTabs;
import com.piranport.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PiranPort.MOD_ID)
public class PiranPort {
    public static final String MOD_ID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        LOGGER.info("Piran Port mod initialized!");
    }
}
