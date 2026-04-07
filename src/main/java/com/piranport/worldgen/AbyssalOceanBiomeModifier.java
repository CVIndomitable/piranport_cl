package com.piranport.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Placeholder BiomeModifier for injecting the abyssal_ocean biome into the overworld.
 * Not activated in v0.0.11 (方案B: use Structure Set biome filters instead).
 * Will be completed in a future version for immersive water color/fog changes.
 */
public record AbyssalOceanBiomeModifier(
        HolderSet<Biome> targetBiomes
) implements BiomeModifier {

    public static final MapCodec<AbyssalOceanBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Biome.LIST_CODEC.fieldOf("target_biomes").forGetter(AbyssalOceanBiomeModifier::targetBiomes)
    ).apply(inst, AbyssalOceanBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // Intentionally empty — reserved for future version.
        // Will modify water_color, water_fog_color, fog_color for deep ocean biomes
        // to create the abyssal atmosphere.
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
