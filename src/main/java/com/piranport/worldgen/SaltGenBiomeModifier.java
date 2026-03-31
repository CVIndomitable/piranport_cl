package com.piranport.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.config.ModCommonConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Custom BiomeModifier that conditionally adds features based on the
 * {@link ModCommonConfig#SALT_GENERATION_ENABLED} config flag.
 * When the config is false (default), salt blocks do not generate in rivers.
 */
public record SaltGenBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        GenerationStep.Decoration step
) implements BiomeModifier {

    public static final MapCodec<SaltGenBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(SaltGenBiomeModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(SaltGenBiomeModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(SaltGenBiomeModifier::step)
    ).apply(inst, SaltGenBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && ModCommonConfig.SALT_GENERATION_ENABLED.get()) {
            if (biomes.contains(biome)) {
                BiomeGenerationSettingsBuilder gen = builder.getGenerationSettings();
                features.forEach(f -> gen.addFeature(step, f));
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
