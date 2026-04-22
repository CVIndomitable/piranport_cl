package com.piranport.registry;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import com.piranport.worldgen.SaltGenBiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModBiomeModifiers {

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, PiranPort.MOD_ID);

    @SuppressWarnings("unused") // held by DeferredRegister
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<SaltGenBiomeModifier>> SALT_GEN =
            BIOME_MODIFIER_SERIALIZERS.register("salt_gen", () -> SaltGenBiomeModifier.CODEC);
}
