package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.worldgen.AbyssalSeepFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, PiranPort.MOD_ID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ABYSSAL_SEEP =
            FEATURES.register("abyssal_seep",
                    () -> new AbyssalSeepFeature(NoneFeatureConfiguration.CODEC));
}
