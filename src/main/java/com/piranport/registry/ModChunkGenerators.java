package com.piranport.registry;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import com.piranport.dungeon.instance.DungeonOceanGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, PiranPort.MOD_ID);

    static {
        GENERATORS.register("dungeon_ocean", () -> DungeonOceanGenerator.CODEC);
    }

    private ModChunkGenerators() {}
}
