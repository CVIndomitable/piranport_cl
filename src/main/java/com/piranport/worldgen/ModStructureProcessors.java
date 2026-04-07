package com.piranport.worldgen;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModStructureProcessors {

    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, PiranPort.MOD_ID);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<LootChestProcessor>> LOOT_CHEST =
            STRUCTURE_PROCESSORS.register("loot_chest_processor", () -> LootChestProcessor.TYPE);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<RuinDegradationProcessor>> RUIN_DEGRADATION =
            STRUCTURE_PROCESSORS.register("ruin_degradation", () -> RuinDegradationProcessor.TYPE);
}
