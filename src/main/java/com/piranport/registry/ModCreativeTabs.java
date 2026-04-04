package com.piranport.registry;

import com.piranport.PiranPort;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PiranPort.MOD_ID);

    /** Items excluded from the creative tab (internal/deprecated). */
    private static final Set<DeferredItem<? extends Item>> EXCLUDED = Set.of(
            ModItems.TAB_ICON,
            ModItems.AERIAL_BOMB_SMALL,
            ModItems.AERIAL_BOMB_MEDIUM
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIRANPORT_TAB =
            CREATIVE_TABS.register("piranport_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.piranport"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.TAB_ICON.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        // Auto-populate from registry in declaration order, skipping excluded items
                        for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
                            if (!EXCLUDED.contains(entry)) {
                                output.accept(entry.get());
                            }
                        }
                    }).build());
}
