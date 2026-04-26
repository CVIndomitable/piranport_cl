package com.piranport.registry;

import com.google.common.collect.ImmutableSet;
import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModPoiTypes {
    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, PiranPort.MOD_ID);

    private static Set<BlockState> getBlockStates(net.minecraft.world.level.block.Block block) {
        return ImmutableSet.copyOf(block.getStateDefinition().getPossibleStates());
    }

    public static final DeferredHolder<PoiType, PoiType> AILA =
            POI_TYPES.register("aila",
                    () -> new PoiType(getBlockStates(ModBlocks.RELOAD_FACILITY.get()), 1, 1));
}
