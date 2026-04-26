package com.piranport.villager;

import com.piranport.PiranPort;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModVillagerProfessions;
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
        if (event.getType() == VillagerProfession.CLERIC) {
            addClericTrades(event);
        }
        if (event.getType() == ModVillagerProfessions.AILA.get()) {
            addAilaTrades(event);
        }
    }

    private static void addClericTrades(VillagerTradesEvent event) {
        // Level 1: Trophy -> Emeralds (1 trophy = 10 emeralds)
        event.getTrades().get(1).addAll(List.of(
                itemForEmerald(ModItems.HENTAI_TROPHY.get(), 1, 10, 16, 2)
        ));
    }

    private static void addAilaTrades(VillagerTradesEvent event) {
        // Level 1: 基础弹药
        event.getTrades().get(1).addAll(List.of(
                trophyForItem(ModItems.SMALL_HE_SHELL.get(), 16, 5, 16, 5),
                trophyForItem(ModItems.SMALL_AP_SHELL.get(), 16, 5, 16, 5),
                trophyForItem(ModItems.TORPEDO_533MM.get(), 8, 8, 16, 5),
                trophyForItem(ModItems.SINGLE_SMALL_GUN.get(), 1, 20, 6, 5)
        ));

        // Level 2: 中级弹药 + 基础发射器
        event.getTrades().get(2).addAll(List.of(
                trophyForItem(ModItems.MEDIUM_HE_SHELL.get(), 12, 8, 16, 10),
                trophyForItem(ModItems.MEDIUM_AP_SHELL.get(), 12, 8, 16, 10),
                trophyForItem(ModItems.SMALL_GUN.get(), 1, 40, 6, 10),
                trophyForItem(ModItems.TWIN_TORPEDO_LAUNCHER.get(), 1, 30, 6, 10),
                trophyForItem(ModItems.DEPTH_CHARGE.get(), 16, 6, 16, 10),
                trophyForItem(ModItems.SMALL_ARMOR_PLATE.get(), 1, 30, 6, 10)
        ));

        // Level 3: 高级弹药 + 中级装备
        event.getTrades().get(3).addAll(List.of(
                trophyForItem(ModItems.LARGE_HE_SHELL.get(), 8, 12, 16, 20),
                trophyForItem(ModItems.LARGE_AP_SHELL.get(), 8, 12, 16, 20),
                trophyForItem(ModItems.TORPEDO_610MM.get(), 6, 15, 16, 20),
                trophyForItem(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 1, 48, 6, 20),
                trophyForItem(ModItems.MEDIUM_ARMOR_PLATE.get(), 1, 48, 6, 20)
        ));

        // Level 4: 特殊弹药 + 高级装备
        event.getTrades().get(4).addAll(List.of(
                trophyForItem(ModItems.SMALL_VT_SHELL.get(), 12, 12, 16, 30),
                trophyForItem(ModItems.SMALL_TYPE3_SHELL.get(), 12, 12, 16, 30),
                trophyForItem(ModItems.MEDIUM_TYPE3_SHELL.get(), 8, 16, 16, 30),
                trophyForItem(ModItems.MAGNETIC_TORPEDO_533MM.get(), 6, 18, 16, 30),
                trophyForItem(ModItems.DEPTH_CHARGE_LAUNCHER.get(), 1, 40, 6, 30),
                trophyForItem(ModItems.LARGE_ARMOR_PLATE.get(), 1, 64, 6, 30)
        ));

        // Level 5: 顶级装备
        event.getTrades().get(5).addAll(List.of(
                trophyForItem(ModItems.LARGE_TYPE3_SHELL.get(), 6, 20, 16, 30),
                trophyForItem(ModItems.QUAD_TORPEDO_LAUNCHER.get(), 1, 64, 6, 30),
                trophyForItem(ModItems.ACOUSTIC_TORPEDO_533MM.get(), 4, 24, 16, 30),
                trophyForItem(ModItems.WIRE_GUIDED_TORPEDO_533MM.get(), 4, 24, 16, 30),
                trophyForItem(ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), 1, 56, 6, 30),
                trophyForItem(ModItems.TORPEDO_610MM_TYPE93_MK1.get(), 4, 30, 16, 30)
        ));
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
                emeraldForItem(ModItems.TRIPLE_TORPEDO_LAUNCHER.get(), 1, 12, 3, 20),
                emeraldForItem(ModItems.MEDIUM_ARMOR_PLATE.get(), 1, 12, 3, 20)
        ));

        // Level 4: Special ammo + advanced equipment
        event.getTrades().get(4).addAll(List.of(
                emeraldForItem(ModItems.SMALL_VT_SHELL.get(), 12, 4, 12, 30),
                emeraldForItem(ModItems.SMALL_TYPE3_SHELL.get(), 12, 4, 12, 30),
                emeraldForItem(ModItems.MEDIUM_TYPE3_SHELL.get(), 8, 5, 12, 30),
                emeraldForItem(ModItems.MAGNETIC_TORPEDO_533MM.get(), 6, 6, 12, 30),
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

    private static VillagerTrades.ItemListing itemForEmerald(net.minecraft.world.item.Item item, int itemCount, int emeraldCount, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemCost(item, itemCount),
                new ItemStack(Items.EMERALD, emeraldCount),
                maxUses,
                xp,
                0.05f
        );
    }

    private static VillagerTrades.ItemListing trophyForItem(net.minecraft.world.item.Item item, int count, int trophyCost, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(
                new ItemCost(ModItems.HENTAI_TROPHY.get(), trophyCost),
                new ItemStack(item, count),
                maxUses,
                xp,
                0.05f
        );
    }
}
