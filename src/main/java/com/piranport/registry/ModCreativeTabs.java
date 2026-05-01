package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 阶段 2：把本阶段注册的所有 Item 按 1.0 的分组顺序堆进单一标签页。
 * 运行时方块（smoke_screen / flare_light）没有 BlockItem，天然不出现在标签页里。
 * projectile_bullet 是弹丸渲染用的隐藏物品，1.0 里也不入标签页。
 */
public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MOD_ID);

    public static final RegistryObject<CreativeModeTab> PIRANPORT_TAB = CREATIVE_TABS.register(
            "piranport_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport"))
                    .withTabsAfter(CreativeModeTabs.SPAWN_EGGS)
                    .icon(() -> new ItemStack(ModItems.TAB_ICON.get()))
                    .displayItems((params, output) -> {
                        // Blocks
                        output.accept(ModBlocks.BAUXITE_ORE.get());
                        output.accept(ModBlocks.ALUMINUM_BLOCK.get());
                        output.accept(ModBlocks.SALT_BLOCK.get());
                        output.accept(ModBlocks.SALT_CHIP.get());

                        // Materials
                        output.accept(ModItems.RAW_ALUMINUM.get());
                        output.accept(ModItems.ALUMINUM_INGOT.get());
                        output.accept(ModItems.SALT.get());

                        // Ship Cores
                        output.accept(ModItems.SMALL_SHIP_CORE.get());
                        output.accept(ModItems.MEDIUM_SHIP_CORE.get());
                        output.accept(ModItems.LARGE_SHIP_CORE.get());
                        output.accept(ModItems.SUBMARINE_CORE.get());

                        // Skin Cores
                        output.accept(ModItems.SKIN_CORE_1.get());
                        output.accept(ModItems.SKIN_CORE_2.get());
                        output.accept(ModItems.SKIN_CORE_3.get());

                        // Guns
                        output.accept(ModItems.SINGLE_SMALL_GUN.get());
                        output.accept(ModItems.SMALL_GUN.get());
                        output.accept(ModItems.MEDIUM_GUN.get());
                        output.accept(ModItems.LARGE_GUN.get());

                        // Shells
                        output.accept(ModItems.SMALL_HE_SHELL.get());
                        output.accept(ModItems.MEDIUM_HE_SHELL.get());
                        output.accept(ModItems.LARGE_HE_SHELL.get());
                        output.accept(ModItems.SMALL_AP_SHELL.get());
                        output.accept(ModItems.MEDIUM_AP_SHELL.get());
                        output.accept(ModItems.LARGE_AP_SHELL.get());
                        output.accept(ModItems.SMALL_VT_SHELL.get());
                        output.accept(ModItems.SMALL_TYPE3_SHELL.get());
                        output.accept(ModItems.MEDIUM_TYPE3_SHELL.get());
                        output.accept(ModItems.LARGE_TYPE3_SHELL.get());

                        // Torpedo Launchers
                        output.accept(ModItems.TWIN_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.QUAD_TORPEDO_LAUNCHER.get());
                        output.accept(ModItems.TORPEDO_RELOAD.get());

                        // Torpedo Ammo
                        output.accept(ModItems.TORPEDO_533MM.get());
                        output.accept(ModItems.TORPEDO_533MM_G7A.get());
                        output.accept(ModItems.TORPEDO_533MM_MK14.get());
                        output.accept(ModItems.TORPEDO_533MM_MK16.get());
                        output.accept(ModItems.TORPEDO_533MM_MK17.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM_G7A.get());
                        output.accept(ModItems.MAGNETIC_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.ACOUSTIC_TORPEDO_533MM_MK27.get());
                        output.accept(ModItems.WIRE_GUIDED_TORPEDO_533MM.get());
                        output.accept(ModItems.WIRE_GUIDED_TORPEDO_533MM_G7E.get());
                        output.accept(ModItems.TORPEDO_530MM_TYPE95.get());
                        output.accept(ModItems.TORPEDO_610MM.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE91.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE93_MK1.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE93_MK3.get());
                        output.accept(ModItems.TORPEDO_610MM_TYPE95_MK2.get());
                        output.accept(ModItems.TORPEDO_720MM_TYPE0.get());

                        // Missile Launchers & Ammo
                        output.accept(ModItems.SY1_LAUNCHER.get());
                        output.accept(ModItems.SY1_MISSILE.get());
                        output.accept(ModItems.MK14_HARPOON_LAUNCHER.get());
                        output.accept(ModItems.HARPOON_MISSILE.get());
                        output.accept(ModItems.TERRIER_LAUNCHER.get());
                        output.accept(ModItems.TERRIER_MISSILE.get());
                        output.accept(ModItems.SHIP_ROCKET_LAUNCHER.get());
                        output.accept(ModItems.ROCKET_AMMO.get());
                        output.accept(ModItems.SEA_DART_LAUNCHER.get());
                        output.accept(ModItems.SEACAT_LAUNCHER.get());
                        output.accept(ModItems.ANTI_AIR_MISSILE.get());

                        // Depth Charge
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER.get());
                        output.accept(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get());
                        output.accept(ModItems.DEPTH_CHARGE.get());

                        // Armor Plates
                        output.accept(ModItems.SMALL_ARMOR_PLATE.get());
                        output.accept(ModItems.MEDIUM_ARMOR_PLATE.get());
                        output.accept(ModItems.LARGE_ARMOR_PLATE.get());

                        // Sonar & Engines
                        output.accept(ModItems.STANDARD_SONAR.get());
                        output.accept(ModItems.STANDARD_ENGINE.get());
                        output.accept(ModItems.IMPROVED_ENGINE.get());
                        output.accept(ModItems.ADVANCED_ENGINE.get());
                        output.accept(ModItems.HIGH_PRESSURE_BOILER.get());
                        output.accept(ModItems.DIESEL_ENGINE.get());

                        // Aviation
                        output.accept(ModItems.AVIATION_FUEL.get());
                        output.accept(ModItems.AERIAL_BOMB.get());
                        output.accept(ModItems.AERIAL_TORPEDO.get());

                        // Fuel
                        output.accept(ModItems.FUEL.get());

                        // Food Ingredients
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.EDIBLE_OIL.get());
                        output.accept(ModItems.BUTTER.get());
                        output.accept(ModItems.CREAM.get());
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
                        output.accept(ModItems.QUICKLIME.get());

                        // Intermediate Products
                        output.accept(ModItems.SAUSAGE.get());
                        output.accept(ModItems.SLICED_SAUSAGE.get());
                        output.accept(ModItems.SALAMI.get());
                        output.accept(ModItems.SLICED_SALAMI.get());
                        output.accept(ModItems.BACON.get());
                        output.accept(ModItems.TOAST_BREAD_SLICES.get());
                        output.accept(ModItems.BEER.get());
                        output.accept(ModItems.ROUND_BUN.get());
                        output.accept(ModItems.WOODEN_BOWL.get());
                        output.accept(ModItems.WOODEN_BARREL.get());

                        // Foods (阶段 3)
                        output.accept(ModItems.HOTDOG.get());
                        output.accept(ModItems.BEANS_CAN.get());
                        output.accept(ModItems.BAGEL.get());
                        output.accept(ModItems.ROAST_PASTRY_OF_PIE.get());
                        output.accept(ModItems.TOAST_BREAD.get());
                        output.accept(ModItems.NAVAL_CURRY.get());
                        output.accept(ModItems.FRIED_FISH_AND_CHIPS.get());
                        output.accept(ModItems.SCONE.get());
                        output.accept(ModItems.APPLE_PIE.get());
                        output.accept(ModItems.ASSORTED_CHAR_SIU_FRIED_RICE.get());
                        output.accept(ModItems.SURSTROMMING.get());
                        output.accept(ModItems.AMERICAN_BURGER.get());
                        output.accept(ModItems.PASTA.get());
                        output.accept(ModItems.MISO_SOUP.get());
                        output.accept(ModItems.BARBECUE.get());
                        output.accept(ModItems.BLACK_FOREST_GATEAU.get());
                        output.accept(ModItems.BLACK_TEA_SANDWICH.get());
                        output.accept(ModItems.BLACK_TEA_SCONE.get());
                        output.accept(ModItems.BORSCHT.get());
                        output.accept(ModItems.BOUILLABAISSE.get());
                        output.accept(ModItems.DONGPO_PORK.get());
                        output.accept(ModItems.DOUBLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.EGGS_BENEDICT.get());
                        output.accept(ModItems.FRIED_FISH_MISO_SOUP.get());
                        output.accept(ModItems.MUSSOLINIS_OO.get());
                        output.accept(ModItems.SCHWEINSHAXE.get());
                        output.accept(ModItems.SALAMI_PIZZA.get());
                        output.accept(ModItems.SALAMI_PIZZA_PIECES.get());
                        output.accept(ModItems.OKROSHKA.get());
                        output.accept(ModItems.ROYAL_NAVAL_SALTED_BEEF.get());
                        output.accept(ModItems.RUSSIAN_DUMPLING.get());
                        output.accept(ModItems.SOBA_NOODLE.get());
                        output.accept(ModItems.TARTE_TATIN.get());
                        output.accept(ModItems.TEMPURA.get());
                        output.accept(ModItems.TEMPURA_SOBA_NOODLE.get());
                        output.accept(ModItems.THURINGER_ROSTBRATWURST_UND_BIER.get());
                        output.accept(ModItems.THURINGER_ROSTBRATWURST.get());
                        output.accept(ModItems.TRIPLE_SHELL_AMERICAN_BURGER.get());
                        output.accept(ModItems.VENICE_CUTTLEFISH_NOODLES.get());
                        output.accept(ModItems.WEISSWURST_MIT_DER_BAGEL.get());
                        output.accept(ModItems.YORKSHIRE_PUDDING.get());
                        output.accept(ModItems.CHICKEN_TATSUTA.get());
                        output.accept(ModItems.TAPTAP_ICE_CREAM.get());
                        output.accept(ModItems.HE_WEI_DAO.get());
                        output.accept(ModItems.EGG_SANDWICH.get());
                        output.accept(ModItems.BACON_SANDWICH.get());
                        output.accept(ModItems.BOLOGNESE_LINGUINE_RECIPE.get());

                        // Bottled drinks
                        output.accept(ModItems.TORPEDO_JUICE.get());
                        output.accept(ModItems.KVASS.get());
                        output.accept(ModItems.APPLE_JUICE.get());
                        output.accept(ModItems.APPLE_JAM.get());
                        output.accept(ModItems.WATERMELON_JUICE.get());
                        output.accept(ModItems.WATERMELON_JAM.get());
                        output.accept(ModItems.CHORUS_FRUIT_JAM.get());

                        // Tools & Items
                        output.accept(ModItems.UNICORN_HARP.get());
                        output.accept(ModItems.ELITE_DAMAGE_CONTROL.get());
                        output.accept(ModItems.DAMAGE_CONTROL.get());
                        output.accept(ModItems.QUICK_REPAIR.get());
                        output.accept(ModItems.SMOKE_CANDLE.get());
                        output.accept(ModItems.FLARE_LAUNCHER.get());
                        output.accept(ModItems.KIRIN_HEADBAND.get());
                        output.accept(ModItems.MYSTERIOUS_WEAPON.get());
                        output.accept(ModItems.RICHELIEU_COMMAND_SWORD.get());
                        output.accept(ModItems.TAIHOU_UMBRELLA.get());
                        output.accept(ModItems.EUGEN_SHIELD.get());
                        output.accept(ModItems.HATSUYUKI_MAIN_GUN.get());
                        output.accept(ModItems.GUNGNIR.get());

                        // Football Set
                        output.accept(ModItems.SPIDER_GLOVES.get());
                        output.accept(ModItems.BLUE_JERSEY.get());
                        output.accept(ModItems.RED_BLACK_SOCKS.get());
                        output.accept(ModItems.MIRACLE_BOOTS.get());

                        // Misc
                        output.accept(ModItems.HENTAI_TROPHY.get());
                        output.accept(ModItems.FLOATING_TARGET.get());
                        output.accept(ModItems.GUIDEBOOK.get());
                    })
                    .build()
    );

    private ModCreativeTabs() {}
}
