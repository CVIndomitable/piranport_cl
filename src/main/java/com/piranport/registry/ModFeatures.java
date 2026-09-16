package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.worldgen.AbyssalSeepFeature;
import com.piranport.worldgen.AbandonedPortalStructure;
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

    /**
     * 废弃传送门结构（决策/副本/09 + 17）：
     * 讲台 + 深渊传送门 多方块结构，预设教学关卡钥匙，作为世界教学触点。
     */
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ABANDONED_PORTAL =
            FEATURES.register("abandoned_portal",
                    () -> new AbandonedPortalStructure());
}
