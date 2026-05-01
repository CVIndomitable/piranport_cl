package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.menu.ReloadFacilityMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, PiranPort.MOD_ID);

    public static final RegistryObject<MenuType<ReloadFacilityMenu>> RELOAD_FACILITY_MENU =
            MENU_TYPES.register("reload_facility",
                    () -> IForgeMenuType.create(ReloadFacilityMenu::fromNetwork));

    private ModMenuTypes() {}
}
