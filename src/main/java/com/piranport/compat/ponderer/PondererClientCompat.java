package com.piranport.compat.ponderer;

import com.piranport.PiranPort;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Path;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class PondererClientCompat {
    private PondererClientCompat() {}

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("ponderer")) {
            return;
        }

        Path gameDirectory = FMLPaths.GAMEDIR.get();
        Path target = gameDirectory.resolve(PondererPackInstaller.PACK_PATH);
        try {
            PondererPackInstaller.Result result = PondererPackInstaller.installBundledPack(gameDirectory);
            switch (result) {
                case INSTALLED, UPDATED -> PiranPort.LOGGER.info(
                        "[Ponderer] {} bundled tutorials at {}", result, target);
                case UNCHANGED -> PiranPort.LOGGER.debug("[Ponderer] Bundled tutorials are current at {}", target);
                case PRESERVED_USER_FILE -> PiranPort.LOGGER.warn(
                        "[Ponderer] Preserved an untracked or modified tutorial pack at {}; automatic update skipped",
                        target);
            }
        } catch (IOException | SecurityException exception) {
            PiranPort.LOGGER.error(
                    "[Ponderer] Could not install bundled tutorials at {}; gameplay remains available", target, exception);
        }
    }
}
