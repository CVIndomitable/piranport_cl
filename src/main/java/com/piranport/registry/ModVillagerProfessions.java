package com.piranport.registry;

import com.google.common.collect.ImmutableSet;
import com.piranport.PiranPort;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 皮兰港三个独立村民职业与工作站点（策划决策/经济/02）。
 *
 * <p>不复用原版职业，避免绿宝石折扣、职业方块与交易池互相污染。
 * 工作方块均为已有方块；POI 另在 {@code data/minecraft/tags/point_of_interest_type/acquirable_job_site.json}
 * 登记，否则无业村民不会认领。职业外观贴图在 {@code textures/entity/(zombie_)villager/profession/<职业>.png}，
 * 当前为原版图层占位（美术待办）。交易表见 {@code handler/VillagerTradeHandler}。
 */
public final class ModVillagerProfessions {
    private ModVillagerProfessions() {}

    public static final DeferredRegister<PoiType> POI_TYPES =
            DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, PiranPort.MOD_ID);
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, PiranPort.MOD_ID);

    public static final DeferredHolder<PoiType, PoiType> WEAPON_WORKBENCH_POI =
            poi("weapon_workbench", ModBlocks.WEAPON_WORKBENCH);
    public static final DeferredHolder<PoiType, PoiType> STONE_MILL_POI =
            poi("stone_mill", ModBlocks.STONE_MILL);
    public static final DeferredHolder<PoiType, PoiType> AMMO_WORKBENCH_POI =
            poi("ammo_workbench", ModBlocks.AMMO_WORKBENCH);

    /** 皮兰港武器匠：基础武器、金属回收、高级蓝图与不可合成武器。 */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PORT_WEAPONSMITH =
            profession("port_weaponsmith", WEAPON_WORKBENCH_POI, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
    /** 皮兰港农夫：模组作物种子与美食。 */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PORT_FARMER =
            profession("port_farmer", STONE_MILL_POI, SoundEvents.VILLAGER_WORK_FARMER);
    /** 皮兰港后勤员：弹药原料回收、常规弹药、特殊弹药蓝图。 */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> PORT_QUARTERMASTER =
            profession("port_quartermaster", AMMO_WORKBENCH_POI, SoundEvents.VILLAGER_WORK_TOOLSMITH);

    private static DeferredHolder<PoiType, PoiType> poi(String name, Supplier<? extends Block> block) {
        return POI_TYPES.register(name, () -> new PoiType(
                ImmutableSet.copyOf(block.get().getStateDefinition().getPossibleStates()), 1, 1));
    }

    private static DeferredHolder<VillagerProfession, VillagerProfession> profession(
            String name, DeferredHolder<PoiType, PoiType> poi, SoundEvent workSound) {
        ResourceKey<PoiType> key = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,
                ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, poi.getId().getPath()));
        return PROFESSIONS.register(name, () -> new VillagerProfession(
                name,
                (Holder<PoiType> holder) -> holder.is(key),
                (Holder<PoiType> holder) -> holder.is(key),
                ImmutableSet.of(), ImmutableSet.of(), workSound));
    }
}
