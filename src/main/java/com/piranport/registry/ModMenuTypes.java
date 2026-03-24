package com.piranport.registry;

import com.piranport.PiranPort;
import com.piranport.menu.ShipCoreMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, PiranPort.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ShipCoreMenu>> SHIP_CORE_MENU =
            MENU_TYPES.register("ship_core",
                    () -> IMenuTypeExtension.create(ShipCoreMenu::fromNetwork));
}
