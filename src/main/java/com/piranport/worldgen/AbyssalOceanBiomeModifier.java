package com.piranport.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Adds abyssal atmosphere and light deep-ocean enemy presence to target biomes.
 * This does not replace the overworld noise biome source with a custom biome;
 * it modifies existing deep-ocean biomes as the conservative runtime layer.
 */
public record AbyssalOceanBiomeModifier(
        HolderSet<Biome> targetBiomes,
        boolean deepSeaForces
) implements BiomeModifier {
    private static final int ABYSSAL_FOG_COLOR = 0x1D1D31;
    private static final int ABYSSAL_WATER_COLOR = 0x201D54;
    private static final int ABYSSAL_WATER_FOG_COLOR = 0x151531;
    private static final int ABYSSAL_SKY_COLOR = 0x404040;

    public static final MapCodec<AbyssalOceanBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Biome.LIST_CODEC.fieldOf("target_biomes").forGetter(AbyssalOceanBiomeModifier::targetBiomes),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("deep_sea_forces", false)
                    .forGetter(AbyssalOceanBiomeModifier::deepSeaForces)
    ).apply(inst, AbyssalOceanBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (!targetBiomes.contains(biome)) {
            return;
        }

        if (phase == Phase.ADD) {
            addDeepOceanSpawns(builder, deepSeaForces);
        } else if (phase == Phase.MODIFY) {
            builder.getSpecialEffects()
                    .fogColor(ABYSSAL_FOG_COLOR)
                    .waterColor(ABYSSAL_WATER_COLOR)
                    .waterFogColor(ABYSSAL_WATER_FOG_COLOR)
                    .skyColor(ABYSSAL_SKY_COLOR);
        }
    }

    private static void addDeepOceanSpawns(ModifiableBiomeInfo.BiomeInfo.Builder builder, boolean deepSeaForces) {
        var spawns = builder.getMobSpawnSettings();
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_DESTROYER.get(), 8, 1, 2));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_LIGHT_CRUISER.get(), 4, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_SUBMARINE.get(), 3, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_SUPPLY.get(), 2, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_ARCHIVIST.get(), deepSeaForces ? 3 : 1, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_ENGINEER.get(), deepSeaForces ? 3 : 1, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_NAVIGATOR.get(), deepSeaForces ? 3 : 1, 1, 1));
        spawns.addSpawn(MobCategory.MONSTER,
                new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_QUARTERMASTER.get(), deepSeaForces ? 3 : 1, 1, 1));
        if (deepSeaForces) {
            spawns.addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_BATTLESHIP.get(), 3, 1, 1));
            spawns.addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_CARRIER.get(), 2, 1, 1));
            spawns.addSpawn(MobCategory.MONSTER,
                    new MobSpawnSettings.SpawnerData(ModEntityTypes.DEEP_OCEAN_FLAGSHIP.get(), 1, 1, 1));
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
