package com.piranport.villager;

import com.piranport.PiranPort;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ModVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.WEAPONSMITH) {
            addWeaponsmithTrades(event);
        }
    }

    private static void addWeaponsmithTrades(VillagerTradesEvent event) {
        // Level 1: Basic ammo + small gun
        event.getTrades().get(1).addAll(List.of(
                emeraldForItem(ModItems.SMALL_HE_SHELL.get(), 16, 2, 12, 5),
                emeraldForItem(ModItems.SMALL_AP_SHELL.get(), 16, 2, 12, 5),
                emeraldForItem(ModItems.TORPEDO_533MM.get(), 8, 3, 12, 5),
                emeraldForItem(ModItems.SINGLE_SMALL_GUN.get(), 1, 6, 3, 5)
        ));

        // Level 2: Medium ammo + basic launchers + armor
        event.getTrades().get(2).addAll(List.of(
                emeraldForItem(ModItems.MEDIUM_HE_SHELL.get(), 12, 3, 12, 10),
                emeraldForItem(ModItems.MEDIUM_AP_SHELL.get(), 12, 3, 12, 10),
                emeraldForItem(ModItems.SMALL_GUN.get(), 1, 10, 3, 10),
                emeraldForItem(ModItems.TWIN_TORPEDO_LAUNCHER.get(), 1, 8, 3, 10),
                emeraldForItem(ModItems.DEPTH_CHARGE.get(), 16, 2, 12, 10),
                emeraldForItem(ModItems.SMALL_ARMOR_PLATE.get(), 1, 8, 3, 10)
        ));

        // Level 3: Large ammo + medium equipment
        event.getTrades().get(3).addAll(List.of(
                emeraldForItem(ModItems.LARGE_HE_SHELL.get(), 8, 4, 12, 20),
                emeraldForItem(ModItems.LARGE_AP_SHELL.get(), 8, 4, 12, 20),
                emeraldForItem(ModItems.TORPEDO_610MM.get(), 6, 5, 12, 20),
                emeraldForItem(ModItems.MEDIUM_GUN.get(), 1, 14, 3, 20),
                emeraldForItem(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 1, 12, 3, 20),
                emeraldForItem(ModItems.MEDIUM_ARMOR_PLATE.get(), 1, 12, 3, 20)
        ));

        // Level 4: Special ammo + advanced equipment
        event.getTrades().get(4).addAll(List.of(
                emeraldForItem(ModItems.SMALL_VT_SHELL.get(), 12, 4, 12, 30),
                emeraldForItem(ModItems.SMALL_TYPE3_SHELL.get(), 12, 4, 12, 30),
                emeraldForItem(ModItems.MEDIUM_TYPE3_SHELL.get(), 8, 5, 12, 30),
                emeraldForItem(ModItems.MAGNETIC_TORPEDO_533MM.get(), 6, 6, 12, 30),
                emeraldForItem(ModItems.LARGE_GUN.get(), 1, 20, 3, 30),
                emeraldForItem(ModItems.DEPTH_CHARGE_LAUNCHER.get(), 1, 10, 3, 30),
                emeraldForItem(ModItems.LARGE_ARMOR_PLATE.get(), 1, 16, 3, 30)
        ));

        // Level 5: Advanced equipment + rare ammo
        event.getTrades().get(5).addAll(List.of(
                emeraldForItem(ModItems.LARGE_TYPE3_SHELL.get(), 6, 6, 12, 30),
                emeraldForItem(ModItems.QUAD_TORPEDO_LAUNCHER.get(), 1, 16, 3, 30),
                emeraldForItem(ModItems.ACOUSTIC_TORPEDO_533MM.get(), 4, 8, 12, 30),
                emeraldForItem(ModItems.WIRE_GUIDED_TORPEDO_533MM.get(), 4, 8, 12, 30),
                emeraldForItem(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), 1, 14, 3, 30),
                emeraldForItem(ModItems.TORPEDO_610MM_TYPE93_MK1.get(), 4, 10, 12, 30)
        ));
    }

    private static VillagerTrades.ItemListing emeraldForItem(net.minecraft.world.item.Item item, int count, int emeraldCost, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, emeraldCost),
                new ItemStack(item, count),
                maxUses,
                xp,
                0.05f
        );
    }
}
