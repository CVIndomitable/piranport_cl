package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.config.TerminalConfigValue;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModVillagerProfessions;
import com.piranport.registry.WeaponItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 皮兰港村民交易表（依据：策划决策/经济/02-皮兰港村民职业与交易.md）。
 *
 * <p>只给三个皮兰港职业加交易；不再往原版农民/工具匠里塞模组商品（02 共同规则第一条）。
 * 战利品（{@code hentai_trophy}）是独立货币：只出现在支付侧，不与绿宝石互换。
 * 战利品交易补货次数 {@link #TROPHY_USES} 低于绿宝石交易，防止刷运输舰买空后期内容。
 *
 * <p>商品池按注册表过滤：文档里「注册前不出现」的机体（梭鱼、Ju87C）与尚未注册的
 * 命名鱼雷不会静默替换成别的物品，只会从池中缺席。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class VillagerTradeHandler {

    /** 等级经验（村民侧），按原版各级经验量级取。 */
    private static final int[] XP = {0, 2, 10, 20, 15, 30};
    /** 绿宝石交易补货前最大次数（终端可调，global.villager_trade.emerald_uses，默认 12）。 */
    static final TerminalConfigValue<Integer> EMERALD_USES = ModEquipmentConfig.VILLAGER_EMERALD_USES;
    /** 回收（卖给村民换绿宝石）交易补货前最大次数（终端可调，默认 16）。 */
    static final TerminalConfigValue<Integer> RECYCLE_USES = ModEquipmentConfig.VILLAGER_RECYCLE_USES;
    /** 战利品交易补货前最大次数，设计上应低于绿宝石交易（终端可调，默认 3）。 */
    static final TerminalConfigValue<Integer> TROPHY_USES = ModEquipmentConfig.VILLAGER_TROPHY_USES;

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
        // 新手：初期/标准武器。日本双联 127 的注册名在代码里是 japanese_127mm_twin_gun。
        addTrade(t.get(1), sellItem("german_single_127mm_gun", 8, "german_single_127mm_gun", 1, EMERALD_USES, 1));
        addTrade(t.get(1), sellItem("japanese_127mm_twin_gun", 12, "japanese_127mm_twin_gun", 1, EMERALD_USES, 1));
        addTrade(t.get(1), sellItem("torpedo_533mm", 10, "torpedo_533mm", 4, EMERALD_USES, 1));

        // 学徒：标准飞机随机 1 架；回收铝锭与燃料。梭鱼、Ju87C 未注册，池里自动缺席。
        addTrade(t.get(2), sellRandomItem("minecraft:emerald", 20,
                pool("seafire", "zero_model52", "sbd_dauntless", "firefly_as_mk5", "barracuda", "ju87c"),
                1, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("petrel_bomber", 16, "petrel_bomber", 1, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("swordfish_torpedo", 16, "swordfish_torpedo", 1, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("piranport:aluminum_ingot", 8, "minecraft:emerald", 1, RECYCLE_USES, 2));
        // 文档写作 fuel_bucket；当前只有燃料物品 fuel，按同一物品处理。
        addTrade(t.get(2), sellItem("piranport:fuel", 4, "minecraft:emerald", 1, RECYCLE_USES, 2));

        // 老手：改良蓝图（火炮/鱼雷管/飞机/三式弹）与六件导弹发射器的先进蓝图。
        addTrade(t.get(3), sellRandomItem("piranport:hentai_trophy", 4, improvedWeaponBlueprints(),
                1, TROPHY_USES, 3));
        addTrade(t.get(3), sellRandomItem("piranport:hentai_trophy", 8,
                pool("ship_rocket_launcher_blueprint", "seacat_launcher_blueprint",
                        "sea_dart_launcher_blueprint", "terrier_launcher_blueprint",
                        "mk14_harpoon_launcher_blueprint", "sy1_launcher_blueprint"),
                1, TROPHY_USES, 3));

        // 专家：舰娘专属主炮与伞。
        addTrade(t.get(4), sellItem("piranport:hentai_trophy", 12, "hatsuyuki_main_gun", 1, TROPHY_USES, 4));
        addTrade(t.get(4), sellItem("piranport:hentai_trophy", 12, "taihou_umbrella", 1, TROPHY_USES, 4));

        // 大师：无法合成的火炮 / 发射器 / 飞机，每次随机 1 件（与 Boss 掉落并行）。
        addTrade(t.get(5), sellRandomItem("piranport:hentai_trophy", 16,
                pool("japanese_triple_460mm_gun", "soviet_triple_460mm_gun",
                        "us_twin_20inch_gun", "british_twin_20inch_gun"),
                1, TROPHY_USES, 5));
        addTrade(t.get(5), sellRandomItem("piranport:hentai_trophy", 16,
                pool("japanese_triple_72cm_torpedo", "quad_type0_72cm_torpedo",
                        "clt_quad_torpedo_launcher_type0", "sextuple_61cm_torpedo_launcher_93m3",
                        "septuple_torpedo_launcher", "septuple_torpedo_launcher_93m3",
                        "f2h_banshee", "xa2j_bomber", "sky_pirate_torpedo"),
                1, TROPHY_USES, 5));
    }

    // ===== 二、皮兰港农夫 =====
    /** 新手/学徒/老手三档种子与作物。 */
    private static final String[] BASIC_SEEDS = {
            "tomato_seeds", "lettuce_seeds", "onion_seeds", "garlic_seeds"};
    private static final String[] BASIC_CROPS = {"tomato", "lettuce", "onion", "garlic"};
    private static final String[] MID_SEEDS = {
            "soybean_seeds", "chili_seeds", "rice_seeds", "celery_seeds"};
    private static final String[] MID_CROPS = {"soybean", "chili", "rice", "celery"};
    private static final String[] HIGH_SEEDS = {"lablab_bean_seeds", "ormosia_seeds", "rye_seeds"};
    private static final String[] HIGH_CROPS = {"lablab_bean", "ormosia", "rye"};

    /** 专家档标准主食池（经济/02 的 31 种）。 */
    private static final String[] STANDARD_FOODS = {
            "naval_curry", "fried_fish_and_chips", "bagel", "egg_sandwich", "apple_pie",
            "assorted_char_siu_fried_rice", "barbecue", "black_forest_gateau", "black_tea_sandwich",
            "black_tea_scone", "borscht", "bouillabaisse", "dongpo_pork", "double_shell_american_burger",
            "eggs_benedict", "fried_fish_miso_soup", "mussolinis_oo", "schweinshaxe", "salami_pizza",
            "okroshka", "royal_naval_salted_beef", "russian_dumpling", "soba_noodle", "tarte_tatin",
            "tempura_soba_noodle", "thuringer_rostbratwurst_und_bier", "thuringer_rostbratwurst",
            "triple_shell_american_burger", "venice_cuttlefish_noodles", "weisswurst_mit_der_bagel",
            "yorkshire_pudding"};

    private static void addFarmer(Int2ObjectMap<List<VillagerTrades.ItemListing>> t) {
        addSeedAndCropTier(t, 1, BASIC_SEEDS, BASIC_CROPS, 1, 2);
        addSeedAndCropTier(t, 2, MID_SEEDS, MID_CROPS, 2, 2);
        addSeedAndCropTier(t, 3, HIGH_SEEDS, HIGH_CROPS, 4, 1);

        // 专家：标准主食随机 2 份；Buff 食物随机 1 份。
        addTrade(t.get(4), sellRandomItem("minecraft:emerald", 4, pool(STANDARD_FOODS), 2, EMERALD_USES, 4));
        addTrade(t.get(4), sellRandomItem("minecraft:emerald", 6,
                pool("torpedo_juice", "kvass", "chicken_tatsuta", "tempura"), 1, EMERALD_USES, 4));

        // 大师：高级料理随机 1 份；烤面包。
        addTrade(t.get(5), sellRandomItem("minecraft:emerald", 8,
                pool("american_burger", "bolognese_linguine_recipe"), 1, EMERALD_USES, 5));
        addTrade(t.get(5), sellItem("minecraft:emerald", 12, "toast_bread", 1, EMERALD_USES, 5));
    }

    private static void addSeedAndCropTier(Int2ObjectMap<List<VillagerTrades.ItemListing>> t, int level,
                                           String[] seeds, String[] crops, int seedPrice, int seedCount) {
        for (String seed : seeds) {
            t.get(level).add(sellItem("minecraft:emerald", seedPrice, seed, seedCount, EMERALD_USES, level));
        }
        for (String crop : crops) {
            t.get(level).add(sellItem(crop, 16, "minecraft:emerald", 1, RECYCLE_USES, level));
        }
    }

    // ===== 三、皮兰港后勤员 =====
    private static void addQuartermaster(Int2ObjectMap<List<VillagerTrades.ItemListing>> t) {
        // 新手：原料回收 + 基础消耗弹药。
        addTrade(t.get(1), sellItem("minecraft:gunpowder", 12, "minecraft:emerald", 1, RECYCLE_USES, 1));
        addTrade(t.get(1), sellItem("minecraft:copper_ingot", 8, "minecraft:emerald", 1, RECYCLE_USES, 1));
        addTrade(t.get(1), sellItem("minecraft:emerald", 6, "small_he_shell", 8, EMERALD_USES, 1));
        addTrade(t.get(1), sellItem("minecraft:emerald", 8, "small_ap_shell", 8, EMERALD_USES, 1));
        addTrade(t.get(1), sellItem("minecraft:emerald", 10, "medium_he_shell", 8, EMERALD_USES, 1));
        addTrade(t.get(1), sellItem("minecraft:emerald", 12, "medium_ap_shell", 8, EMERALD_USES, 1));

        // 学徒：航空弹药与深弹。
        addTrade(t.get(2), sellItem("minecraft:emerald", 8, "aerial_torpedo", 4, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("minecraft:emerald", 8, "aerial_bomb", 8, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("minecraft:emerald", 6, "depth_charge", 4, EMERALD_USES, 2));

        // 命名鱼雷、磁性/声导、610 标准与舰载火箭弹：绿宝石交易。
        addTrade(t.get(2), sellRandomItem("minecraft:emerald", 20,
                pool("torpedo_533mm_g7a", "torpedo_533mm_mk14", "torpedo_530mm_type95",
                        "torpedo_533mm_53_38"), 2, EMERALD_USES, 2));
        addTrade(t.get(2), sellRandomItem("minecraft:emerald", 18,
                pool("magnetic_torpedo_533mm_g7a", "magnetic_torpedo_533mm_g7e"),
                2, EMERALD_USES, 2));
        addTrade(t.get(2), sellRandomItem("minecraft:emerald", 22,
                pool("acoustic_torpedo_533mm_g7e", "acoustic_torpedo_533mm_mk27"),
                2, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("minecraft:emerald", 24, "torpedo_610mm", 2, EMERALD_USES, 2));
        addTrade(t.get(2), sellItem("minecraft:emerald", 16, "rocket_ammo", 8, EMERALD_USES, 2));

        // 老手：VT 与三式弹蓝图，只卖蓝图不卖成品。
        addTrade(t.get(3), sellItem("piranport:hentai_trophy", 4, "small_vt_shell_blueprint",
                1, TROPHY_USES, 3));
        addTrade(t.get(3), sellRandomItem("piranport:hentai_trophy", 4,
                pool("small_type3_shell_blueprint", "medium_type3_shell_blueprint",
                        "large_type3_shell_blueprint"),
                1, TROPHY_USES, 3));

        // 专家：精英损管、九三式三型、零式。
        addTrade(t.get(4), sellItem("piranport:hentai_trophy", 18, "elite_damage_control",
                1, TROPHY_USES, 4));
        addTrade(t.get(4), sellItem("piranport:hentai_trophy", 8, "torpedo_610mm_type93_mk3",
                1, TROPHY_USES, 4));
        addTrade(t.get(4), sellItem("piranport:hentai_trophy", 8, "torpedo_720mm_type0",
                1, TROPHY_USES, 4));

        // 大师：一式穿甲弹与超重弹。不出售 MK23、核航弹、机密货物和防空导弹成品。
        addTrade(t.get(5), sellItem("piranport:hentai_trophy", 12, "type_1_ap_shell",
                1, TROPHY_USES, 5));
        addTrade(t.get(5), sellItem("piranport:hentai_trophy", 12, "super_heavy_ap_shell",
                2, TROPHY_USES, 5));
    }

    // ===== 物品池 =====

    /** 按注册名取物品；缺失时返回 AIR，池构建阶段会被过滤掉。 */
    private static Item item(String id) {
        // 不带命名空间的一律按本模组解析；直接 tryParse 会把裸名当成 minecraft:。
        ResourceLocation key = id.contains(":")
                ? ResourceLocation.tryParse(id)
                : ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, id);
        return key == null ? Items.AIR : BuiltInRegistries.ITEM.get(key);
    }

    private static boolean exists(String id) {
        return item(id) != Items.AIR;
    }

    /** 过滤未注册物品后的候选池；空池不会生成交易。 */
    private static List<Supplier<? extends ItemLike>> pool(String... ids) {
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        for (String id : ids) {
            if (exists(id)) out.add(() -> item(id));
        }
        return out;
    }

    /**
     * 武器匠老手档的改良蓝图池：火炮、鱼雷管、飞机，外加三式弹蓝图。
     * 按稀有度取改良档，再排除雷达、引擎、深弹投射器和近防炮——近防炮蓝图走主线 2-1，不进这个池。
     */
    private static List<Supplier<? extends ItemLike>> improvedWeaponBlueprints() {
        Set<String> ciws = WeaponItems.CIWS_ITEMS.stream()
                .map(holder -> holder.getId().getPath())
                .collect(Collectors.toSet());
        List<Supplier<? extends ItemLike>> out = new ArrayList<>();
        for (var entry : WeaponItems.EQUIPMENT_BLUEPRINTS.entrySet()) {
            String id = entry.getKey();
            if (id.contains("radar") || id.contains("engine") || id.contains("depth_charge") || ciws.contains(id)) {
                continue;
            }
            Item blueprint = entry.getValue().get();
            if (new ItemStack(blueprint).getRarity() != Rarity.UNCOMMON) continue;
            out.add(entry.getValue());
        }
        out.addAll(pool("small_type3_shell_blueprint", "medium_type3_shell_blueprint",
                "large_type3_shell_blueprint"));
        return out;
    }

    /** 商品缺失/池为空时构造结果为 null，直接跳过，不能把 null 塞进交易列表。 */
    private static void addTrade(List<VillagerTrades.ItemListing> list,
                                 VillagerTrades.ItemListing listing) {
        if (listing != null) list.add(listing);
    }

    // ===== 交易构造 =====

    /** 单输入交易：支付 costCount 个 cost，获得 resultCount 个 result。 */
    static VillagerTrades.ItemListing sell(Supplier<? extends ItemLike> cost, int costCount,
                                           Supplier<? extends ItemLike> result, int resultCount,
                                           TerminalConfigValue<Integer> maxUses, int level) {
        int xp = XP[level];
        int uses = maxUses.get();
        float priceMultiplier = (float) (double) ModEquipmentConfig.VILLAGER_PRICE_MULTIPLIER.get();
        return (trader, rand) -> new MerchantOffer(
                new ItemCost(cost.get(), costCount),
                new ItemStack(result.get(), resultCount),
                uses, xp, priceMultiplier);
    }

    /** 按注册名构造单输入交易；商品缺失时返回 null，调用方直接跳过。 */
    private static VillagerTrades.ItemListing sellItem(String costId, int costCount,
                                                       String resultId, int resultCount,
                                                       TerminalConfigValue<Integer> maxUses, int level) {
        if (!exists(costId) || !exists(resultId)) return null;
        return sell(() -> item(costId), costCount, () -> item(resultId), resultCount, maxUses, level);
    }

    /**
     * 随机池交易：每次补货从池中随机取一件。池为空时返回 null。
     * 战利品/绿宝石只按文档数量支付，不因抽到哪件商品而变化。
     */
    static VillagerTrades.ItemListing sellRandom(Supplier<? extends ItemLike> cost, int costCount,
                                                 List<Supplier<? extends ItemLike>> results, int resultCount,
                                                 TerminalConfigValue<Integer> maxUses, int level) {
        if (results.isEmpty()) return null;
        int xp = XP[level];
        int uses = maxUses.get();
        float priceMultiplier = (float) (double) ModEquipmentConfig.VILLAGER_PRICE_MULTIPLIER.get();
        return (trader, rand) -> {
            Supplier<? extends ItemLike> pick = results.get(rand.nextInt(results.size()));
            return new MerchantOffer(
                    new ItemCost(cost.get(), costCount),
                    new ItemStack(pick.get(), resultCount),
                    uses, xp, priceMultiplier);
        };
    }

    private static VillagerTrades.ItemListing sellRandomItem(String costId, int costCount,
                                                             List<Supplier<? extends ItemLike>> results,
                                                             int resultCount,
                                                             TerminalConfigValue<Integer> maxUses, int level) {
        if (!exists(costId)) return null;
        return sellRandom(() -> item(costId), costCount, results, resultCount, maxUses, level);
    }
}
