package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.menu.CookingPotMenu;
import com.piranport.menu.ReloadFacilityMenu;
import com.piranport.menu.StoneMillMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PiranPort.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StoneMillMenu>> STONE_MILL_MENU =
            MENU_TYPES.register("stone_mill",
                    () -> IMenuTypeExtension.create(StoneMillMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<CookingPotMenu>> COOKING_POT_MENU =
            MENU_TYPES.register("cooking_pot",
                    () -> IMenuTypeExtension.create(CookingPotMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>, MenuType<ReloadFacilityMenu>> RELOAD_FACILITY_MENU =
            MENU_TYPES.register("reload_facility",
                    () -> IMenuTypeExtension.create(ReloadFacilityMenu::fromNetwork));

}
