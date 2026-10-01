package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModVillagerProfessions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * 皮兰港村民交易表（依据：策划决策/经济/02-皮兰港村民职业与交易.md）。
 *
 * <p>只给三个皮兰港职业加交易；不再往原版农民/工具匠里塞模组商品（02 共同规则第一条）。
 * 战利品（{@code hentai_trophy}）是独立货币：只出现在支付侧，不与绿宝石互换。
 * 战利品交易补货次数 {@link #TROPHY_USES} 低于绿宝石交易，防止刷运输舰买空后期内容。
 *
 * <p>文档标「需新增」或「待确认」的商品不在此出现，也不得用其他物品静默替换：
 * 导弹发射器蓝图、VT 弹蓝图、大口径炮弹蓝图、导弹蓝图、特殊弹药蓝图池、
 * 大师级不可合成火炮/鱼雷管池、农夫种子/美食池。补齐物品后再在对应等级加行。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class VillagerTradeHandler {

    /** 等级经验（村民侧），按原版各级经验量级取。 */
    private static final int[] XP = {0, 2, 10, 20, 15, 30};
    /** 绿宝石交易补货前最大次数。 */
    static final int EMERALD_USES = 12;
    /** 回收（卖给村民换绿宝石）交易补货前最大次数。 */
    static final int RECYCLE_USES = 16;
    /** 战利品交易补货前最大次数，必须低于绿宝石交易。 */
    static final int TROPHY_USES = 3;
    private static final float PRICE_MULTIPLIER = 0.05f;

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession type = event.getType();
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        if (type == ModVillagerProfessions.PORT_WEAPONSMITH.get()) {
            addWeaponsmith(trades);
        } else if (type == ModVillagerProfessions.PORT_FARMER.get()) {
            addFarmer(trades);
        } else if (type == ModVillagerProfessions.PORT_QUARTERMASTER.get()) {
            addQuartermaster(trades);
        }
    }

    // ===== 一、皮兰港武器匠 =====
    private static void addWeaponsmith(Int2ObjectMap<List<VillagerTrades.ItemListing>> t) {
        // 新手：可合成基础武器，不卖联装鱼雷管和中大型火炮
        t.get(1).add(sell(() -> Items.EMERALD, 8, ModItems.SINGLE_SMALL_GUN, 1, EMERALD_USES, 1));
        t.get(1).add(sell(() -> Items.EMERALD, 10, ModItems.TORPEDO_533MM, 4, EMERALD_USES, 1));
        // 学徒：回收模组金属；「油桶」暂无注册物品，按文档暂用燃料
        t.get(2).add(sell(ModItems.ALUMINUM_INGOT, 8, () -> Items.EMERALD, 1, RECYCLE_USES, 2));
        t.get(2).add(sell(ModItems.FUEL, 8, () -> Items.EMERALD, 1, RECYCLE_USES, 2));
        // 老手：战利品换蓝图（导弹发射器蓝图需新增，暂缺）
        t.get(3).add(sell(ModItems.HENTAI_TROPHY, 4, ModItems.MEDIUM_GUN_BLUEPRINT, 1, TROPHY_USES, 3));
        t.get(3).add(sell(ModItems.HENTAI_TROPHY, 8, ModItems.LARGE_GUN_BLUEPRINT, 1, TROPHY_USES, 3));
        // 专家：不可合成特殊武器
        t.get(4).add(sell(ModItems.HENTAI_TROPHY, 12, ModItems.HATSUYUKI_MAIN_GUN, 1, TROPHY_USES, 4));
        t.get(4).add(sell(ModItems.HENTAI_TROPHY, 12, ModItems.TAIHOU_UMBRELLA, 1, TROPHY_USES, 4));
        // 大师：不可合成火炮池 / 鱼雷管池待确认，暂缺
    }

    // ===== 二、皮兰港农夫 =====
    /** 已注册模组作物（产物）。种子/美食售卖池未定稿，只做文档补充的作物回收。 */
    private static final List<Supplier<? extends ItemLike>> CROPS = List.of(
            ModItems.TOMATO, ModItems.SOYBEAN, ModItems.CHILI, ModItems.LETTUCE, ModItems.RICE,
            ModItems.ONION, ModItems.GARLIC, ModItems.LABLAB_BEAN, ModItems.ORMOSIA,
            ModItems.CELERY, ModItems.RYE);

    private static void addFarmer(Int2ObjectMap<List<VillagerTrades.ItemListing>> t) {
        // 文档：新手至老手可用作物 ×16 换绿宝石 ×1。作物分级未定，统一挂新手。
        for (Supplier<? extends ItemLike> crop : CROPS) {
            t.get(1).add(sell(crop, 16, () -> Items.EMERALD, 1, RECYCLE_USES, 1));
        }
        // 种子（新手/学徒/老手）、可合成美食（专家）、特色美食（大师）池待确认，暂缺
    }

    // ===== 三、皮兰港后勤员 =====
    private static void addQuartermaster(Int2ObjectMap<List<VillagerTrades.ItemListing>> t) {
        // 新手：原料回收 + 基础消耗弹药
        t.get(1).add(sell(() -> Items.GUNPOWDER, 12, () -> Items.EMERALD, 1, RECYCLE_USES, 1));
        t.get(1).add(sell(() -> Items.COPPER_INGOT, 8, () -> Items.EMERALD, 1, RECYCLE_USES, 1));
        t.get(1).add(sell(() -> Items.EMERALD, 6, ModItems.SMALL_HE_SHELL, 8, EMERALD_USES, 1));
        t.get(1).add(sell(() -> Items.EMERALD, 8, ModItems.SMALL_AP_SHELL, 8, EMERALD_USES, 1));
        t.get(1).add(sell(() -> Items.EMERALD, 10, ModItems.MEDIUM_HE_SHELL, 4, EMERALD_USES, 1));
        t.get(1).add(sell(() -> Items.EMERALD, 12, ModItems.MEDIUM_AP_SHELL, 4, EMERALD_USES, 1));
        // 学徒：只卖 533mm 标准鱼雷，不含磁性/线导/声导/氧气
        t.get(2).add(sell(() -> Items.EMERALD, 12, ModItems.TORPEDO_533MM, 2, EMERALD_USES, 2));
        // 老手～大师：VT 弹蓝图、大口径炮弹蓝图、导弹蓝图、特殊弹药蓝图池均需新增，暂缺。
        // 不出售 MK23 核炮弹（只由终章 Boss 首通发放）。
    }

    /** 单输入交易：支付 costCount 个 cost，获得 resultCount 个 result。 */
    static VillagerTrades.ItemListing sell(Supplier<? extends ItemLike> cost, int costCount,
                                           Supplier<? extends ItemLike> result, int resultCount,
                                           int maxUses, int level) {
        int xp = XP[level];
        return (trader, rand) -> new MerchantOffer(
                new ItemCost(cost.get(), costCount),
                new ItemStack(result.get(), resultCount),
                maxUses, xp, PRICE_MULTIPLIER);
    }
}
