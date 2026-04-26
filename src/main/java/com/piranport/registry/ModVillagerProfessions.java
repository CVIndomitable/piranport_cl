package com.piranport.registry;

import com.google.common.collect.ImmutableSet;
import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModVillagerProfessions {
    public static final DeferredRegister<VillagerProfession> VILLAGER_PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, PiranPort.MOD_ID);

    public static final ResourceKey<PoiType> AILA_POI =
            ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                    PiranPort.id("aila"));

    public static final DeferredHolder<VillagerProfession, VillagerProfession> AILA =
            VILLAGER_PROFESSIONS.register("aila",
                    () -> new VillagerProfession(
                            "aila",
                            holder -> holder.is(AILA_POI),
                            holder -> holder.is(AILA_POI),
                            ImmutableSet.of(),
                            ImmutableSet.of(),
                            SoundEvents.VILLAGER_WORK_WEAPONSMITH
                    ));
}
