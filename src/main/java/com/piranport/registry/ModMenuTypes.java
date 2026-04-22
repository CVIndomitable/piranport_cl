package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.menu.ReloadFacilityMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PiranPort.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ReloadFacilityMenu>> RELOAD_FACILITY_MENU =
            MENU_TYPES.register("reload_facility",
                    () -> IMenuTypeExtension.create(ReloadFacilityMenu::fromNetwork));

}
