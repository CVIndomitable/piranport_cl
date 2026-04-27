package com.piranport.registry;

import com.mojang.serialization.MapCodec;
import com.piranport.PiranPort;
import com.piranport.loot.TrophyLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, PiranPort.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<TrophyLootModifier>> TROPHY =
            LOOT_MODIFIER_SERIALIZERS.register("trophy", () -> TrophyLootModifier.CODEC);
}
