package com.piranport.worldgen;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class AbyssalDimensions {
    public static final ResourceKey<Level> ABYSSAL_WORLD =
            ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "abyssal_world"));

    private AbyssalDimensions() {
    }
}
