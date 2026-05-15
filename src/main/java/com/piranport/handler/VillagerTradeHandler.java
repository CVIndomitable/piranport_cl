package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.registry.ModItems;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class VillagerTradeHandler {

    // MerchantOffer(input, result, maxUses, xpReward, priceMultiplier)
    // maxUses: 补货前最大交易次数  xpReward: 给予村民的经验值  priceMultiplier: 需求价格浮动乘数

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != VillagerProfession.FARMER) return;

        var trades = event.getTrades();
        // 1级交易（1-2绿宝石）
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.GYPSUM_CHIP.get(), 4),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.QUICKLIME.get(), 4),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.BLACK_PEPPER.get(), 2),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.WHITE_PEPPER.get(), 2),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.GINGER.get(), 3),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.BLACK_TEA.get(), 2),
                16, 2, 0.05f));
        trades.get(1).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 1),
                new ItemStack(ModItems.ALMOND.get(), 3),
                16, 2, 0.05f));

        // 2级交易（3-5绿宝石）
        trades.get(2).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 3),
                new ItemStack(ModItems.SAUSAGE.get(), 1),
                12, 5, 0.05f));
        trades.get(2).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 2),
                new ItemStack(ModItems.SLICED_SALAMI.get(), 2),
                12, 5, 0.05f));

        // 3级交易（5-8绿宝石）
        trades.get(3).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 5),
                new ItemStack(ModItems.ASSORTED_CHAR_SIU_FRIED_RICE.get(), 1),
                8, 10, 0.05f));
        trades.get(3).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 4),
                new ItemStack(ModItems.MISO_SOUP.get(), 1),
                8, 10, 0.05f));
        trades.get(3).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 5),
                new ItemStack(ModItems.BARBECUE.get(), 1),
                8, 10, 0.05f));
        trades.get(3).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 4),
                new ItemStack(ModItems.SOBA_NOODLE.get(), 1),
                8, 10, 0.05f));
        trades.get(3).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 4),
                new ItemStack(ModItems.YORKSHIRE_PUDDING.get(), 1),
                8, 10, 0.05f));

        // 4级交易（8-12绿宝石）
        trades.get(4).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 8),
                new ItemStack(ModItems.BLACK_FOREST_GATEAU.get(), 1),
                6, 15, 0.05f));
        trades.get(4).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 7),
                new ItemStack(ModItems.SCHWEINSHAXE.get(), 1),
                6, 15, 0.05f));
        trades.get(4).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 6),
                new ItemStack(ModItems.TEMPURA_SOBA_NOODLE.get(), 1),
                6, 15, 0.05f));
        trades.get(4).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 7),
                new ItemStack(ModItems.VENICE_CUTTLEFISH_NOODLES.get(), 1),
                6, 15, 0.05f));

        // 5级交易（10-15绿宝石）
        trades.get(5).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 10),
                new ItemStack(ModItems.HE_WEI_DAO.get(), 1),
                4, 20, 0.05f));
        trades.get(5).add((trader, rand) -> new MerchantOffer(
                new ItemCost(Items.EMERALD, 12),
                new ItemStack(ModItems.PLATED_ROYAL_NAVAL_SALTED_BEEF.get(), 1),
                4, 20, 0.05f));
    }
}
