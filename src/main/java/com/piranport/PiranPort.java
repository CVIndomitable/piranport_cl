package com.piranport;

import com.mojang.logging.LogUtils;
import com.piranport.config.ModClientConfig;
import com.piranport.config.ModCommonConfig;
import com.piranport.recipe.ModBrewingRecipes;
import com.piranport.registry.ModBiomeModifiers;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModCreativeTabs;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMenuTypes;
import com.piranport.registry.ModMobEffects;
import com.piranport.registry.ModArmorMaterials;
import com.piranport.registry.ModRecipeTypes;
import com.piranport.worldgen.ModStructureProcessors;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import org.slf4j.Logger;

@Mod(PiranPort.MOD_ID)
public class PiranPort {
    public static final String MOD_ID = "piranport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public PiranPort(IEventBus modEventBus, ModContainer modContainer) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ModRecipeTypes.RECIPE_SERIALIZERS.register(modEventBus);
        ModBiomeModifiers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        ModStructureProcessors.STRUCTURE_PROCESSORS.register(modEventBus);
        ModArmorMaterials.ARMOR_MATERIALS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::registerBrewingRecipes);
        modContainer.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, ModCommonConfig.SPEC);

        LOGGER.info("Piran Port mod initialized!");
    }

    private void registerBrewingRecipes(final RegisterBrewingRecipesEvent event) {
        ModBrewingRecipes.register(event.getBuilder());
    }
}
