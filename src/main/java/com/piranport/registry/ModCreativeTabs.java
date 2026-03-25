package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIRANPORT_TAB =
            CREATIVE_TABS.register("piranport_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.SMALL_SHIP_CORE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Blocks
                        output.accept(ModItems.BAUXITE_ORE.get());
                        output.accept(ModItems.ALUMINUM_BLOCK.get());
                        output.accept(ModItems.SALT_BLOCK.get());
                        // Materials
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.SALT.get());
                        // Ship Cores
                        output.accept(ModItems.SMALL_SHIP_CORE.get());
                        output.accept(ModItems.MEDIUM_SHIP_CORE.get());
                        output.accept(ModItems.LARGE_SHIP_CORE.get());
                        // HE Shells
                        output.accept(ModItems.SMALL_HE_SHELL.get());
                        output.accept(ModItems.MEDIUM_HE_SHELL.get());
                        output.accept(ModItems.LARGE_HE_SHELL.get());
                        // AP Shells
                        output.accept(ModItems.SMALL_AP_SHELL.get());
                        output.accept(ModItems.MEDIUM_AP_SHELL.get());
                        output.accept(ModItems.LARGE_AP_SHELL.get());
                        // Guns
                        output.accept(ModItems.SMALL_GUN.get());
                        output.accept(ModItems.MEDIUM_GUN.get());
                        output.accept(ModItems.LARGE_GUN.get());
                        // Torpedo Ammo
                        output.accept(ModItems.TORPEDO_533MM.get());
                        output.accept(ModItems.TORPEDO_610MM.get());
                        // Torpedo Launchers
                        output.accept(ModItems.TWIN_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.QUAD_TORPEDO_LAUNCHER.get());
                        // Armor Plates
                        output.accept(ModItems.SMALL_ARMOR_PLATE.get());
                        output.accept(ModItems.MEDIUM_ARMOR_PLATE.get());
                        output.accept(ModItems.LARGE_ARMOR_PLATE.get());
                        // Crop Seeds
                        output.accept(ModItems.TOMATO_SEEDS.get());
                        output.accept(ModItems.SOYBEAN_SEEDS.get());
                        output.accept(ModItems.CHILI_SEEDS.get());
                        output.accept(ModItems.ONION_SEEDS.get());
                        output.accept(ModItems.RICE_SEEDS.get());
                        output.accept(ModItems.LETTUCE_SEEDS.get());
                        output.accept(ModItems.GARLIC_SEEDS.get());
                        // Crop Produce
                        output.accept(ModItems.TOMATO.get());
                        output.accept(ModItems.SOYBEAN.get());
                        output.accept(ModItems.CHILI.get());
                        output.accept(ModItems.ONION.get());
                        output.accept(ModItems.RICE.get());
                        output.accept(ModItems.LETTUCE.get());
                        output.accept(ModItems.GARLIC.get());
                        // Food Ingredients
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.RICE_FLOUR.get());
                        output.accept(ModItems.CHILI_POWDER.get());
                        output.accept(ModItems.PORK_PASTE.get());
                        output.accept(ModItems.EDIBLE_OIL.get());
                        output.accept(ModItems.BUTTER.get());
                        output.accept(ModItems.CREAM.get());
                        output.accept(ModItems.SOYBEAN_MILK.get());
                        output.accept(ModItems.TOFU.get());
                        output.accept(ModItems.CHEESE.get());
                        output.accept(ModItems.YEAST.get());
                        output.accept(ModItems.SOY_SAUCE.get());
                        output.accept(ModItems.VINEGAR.get());
                        output.accept(ModItems.COOKING_WINE.get());
                        output.accept(ModItems.MISO.get());
                        output.accept(ModItems.BRINE.get());
                        output.accept(ModItems.PIE_CRUST.get());
                        output.accept(ModItems.RAW_PASTA.get());
                        output.accept(ModItems.FERMENTED_FISH.get());
                        output.accept(ModItems.PIZZA_BASE.get());
                        output.accept(ModItems.GYPSUM_CHIP.get());
                        output.accept(ModItems.QUICKLIME.get());
                        // Functional Blocks
                        output.accept(ModItems.STONE_MILL.get());
                    }).build());
}
