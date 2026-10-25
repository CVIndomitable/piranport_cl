package com.piranport.dungeon.event;

import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 副本/21 主线奖励：每次通关固定三样——材料包、勋章、随机蓝图，重打照发。
 * <p>节点自身的 {@code rewards} 由 {@link DungeonSettlementService} 另行发放，不走这里。
 * 活动 {@code goldencatcat}、彩蛋 {@code ulan} 与《副本/18》的妄想舰队/地狱群岛
 * 是特例，不套主线表。</p>
 */
public final class MainlineRewardService {
    private MainlineRewardService() {}

    /** 材料区间含两端，按《副本/21》材料包表。 */
    private record Amount(String item, int min, int max) {}

    private static final Map<Integer, List<Amount>> MATERIAL_POOLS = Map.of(
            1, List.of(new Amount("minecraft:iron_ingot", 12, 20),
                    new Amount("minecraft:coal", 8, 16),
                    new Amount("minecraft:copper_ingot", 4, 8)),
            2, List.of(new Amount("minecraft:iron_ingot", 16, 28),
                    new Amount("minecraft:copper_ingot", 8, 12),
                    new Amount("minecraft:gunpowder", 8, 16),
                    new Amount("piranport:aluminum_ingot", 4, 8)),
            3, List.of(new Amount("minecraft:iron_ingot", 24, 36),
                    new Amount("piranport:aluminum_ingot", 8, 16),
                    new Amount("minecraft:gunpowder", 12, 20),
                    new Amount("minecraft:redstone", 8, 16)),
            4, List.of(new Amount("minecraft:iron_ingot", 32, 48),
                    new Amount("piranport:aluminum_ingot", 12, 20),
                    new Amount("minecraft:gold_ingot", 4, 8),
                    new Amount("piranport:fuel", 4, 8)),
            5, List.of(new Amount("minecraft:iron_ingot", 40, 56),
                    new Amount("minecraft:diamond", 1, 3),
                    new Amount("minecraft:gold_ingot", 6, 12),
                    new Amount("piranport:fuel", 6, 12)),
            6, List.of(new Amount("minecraft:iron_ingot", 48, 64),
                    new Amount("minecraft:diamond", 2, 4),
                    new Amount("minecraft:ghast_tear", 1, 2),
                    new Amount("minecraft:shulker_shell", 1, 2),
                    new Amount("piranport:fuel", 8, 16)),
            7, List.of(new Amount("minecraft:iron_ingot", 48, 64),
                    new Amount("minecraft:netherite_scrap", 1, 2),
                    new Amount("minecraft:echo_shard", 1, 3),
                    new Amount("minecraft:ghast_tear", 1, 3),
                    new Amount("minecraft:shulker_shell", 1, 2),
                    new Amount("piranport:fuel", 8, 16))
    );

    /**
     * 《副本/18》活动关每次通关发燃料桶。航空燃料已按《舰装/02》并入燃料桶，
     * 所以「燃料桶 ×8 + 航空燃料 ×4」在这里直接记成 ×12。
     */
    private static final Map<String, Integer> EVENT_FUEL_REWARDS = Map.of(
            "delusion_1", 8, "delusion_2", 12, "delusion_3", 16, "delusion_4", 20,
            "hell_1", 8, "hell_2", 12, "hell_3", 16, "hell_4", 20);

    /** 通关后直接塞进背包的下一关钥匙；第 4 关没有后继，两场也不交叉。 */
    private static final Map<String, String> EVENT_NEXT_KEYS = Map.of(
            "delusion_1", "delusion_2", "delusion_2", "delusion_3", "delusion_3", "delusion_4",
            "hell_1", "hell_2", "hell_2", "hell_3", "hell_3", "hell_4");

    /** 关卡额外材料，既不算进材料包，也不替代蓝图与勋章。 */
    private static final Map<String, Amount> STAGE_EXTRAS = Map.of(
            "1-3", new Amount("minecraft:gunpowder", 8, 8),
            "1-4", new Amount("minecraft:copper_ingot", 8, 8),
            "2-1", new Amount("minecraft:gunpowder", 12, 12),
            "2-2", new Amount("piranport:aluminum_ingot", 8, 8),
            "2-3", new Amount("piranport:aluminum_ingot", 8, 8),
            "2-4", new Amount("piranport:fuel", 4, 4)
    );

    // 下面两个共用池必须先于 BLUEPRINT_POOLS 声明：Java 按源码顺序初始化静态字段，
    // buildBlueprintPools() 在 BLUEPRINT_POOLS 初始化时就会执行；若把这两个字段挪到它之后，
    // Map 里会存进「键存在、值为 null」的条目，抽蓝图时 for-each 直接 NPE（2-1 曾因此崩溃）。
    private static final List<String> CIWS_BLUEPRINTS = List.of(
            "french_quad_37mm_aa", "us_quad_bofors_40mm", "octuple_pom_pom_40mm",
            "japanese_28_tube_aa_rocket", "sm24_zif_twin_57mm",
            "ak230", "british_sextuple_bofors_40mm", "sm20_zif_quad_45mm",
            "bofors_m1951_twin_57mm", "zif75_quad_57mm", "us_twin_3inch_aa");

    private static final List<String> MISSILE_LAUNCHER_BLUEPRINTS = List.of(
            "ship_rocket_launcher", "seacat_launcher", "sea_dart_launcher",
            "terrier_launcher", "mk14_harpoon_launcher", "sy1_launcher");

    /** 蓝图池只写装备注册名，发放时拼 {@code _blueprint} 并以实际注册表过滤。 */
    private static final Map<String, List<String>> BLUEPRINT_POOLS = buildBlueprintPools();

    private static Map<String, List<String>> buildBlueprintPools() {
        Map<String, List<String>> m = new LinkedHashMap<>();
        // 2-1 起加入可合成的改良、先进近防炮；初期/标准可合成但不要蓝图。
        m.put("2-1", Objects.requireNonNull(CIWS_BLUEPRINTS));
        m.put("3-1", List.of("oto_twin_76mm_rapid_gun", "type79_twin_100mm_gun",
                "small_type3_shell", "medium_type3_shell", "large_type3_shell"));
        m.put("3-2", List.of("bofors_twin_120mm_gun", "us_twin_5inch_dp_gun"));
        m.put("3-3", List.of("us_triple_6inch_gun", "mk16_triple_6inch_gun_flashless"));
        m.put("3-4", List.of("german_twin_203mm_gun", "soviet_triple_180mm_gun"));
        m.put("4-1", List.of("us_triple_8inch_gun", "german_triple_283mm_gun"));
        m.put("4-2", List.of("us_triple_12inch_gun", "italian_triple_320mm_gun",
                "btd_destroyer", "improved_engine", "advanced_engine",
                "improved_surface_radar", "improved_aa_radar"));
        m.put("4-3", List.of("british_quad_14inch_gun", "british_twin_15inch_gun_kai",
                "italian_triple_381mm_gun", "french_quad_380mm_gun"));
        m.put("4-4", List.of("us_twin_16inch_gun_kai", "us_triple_16inch_gun_mk6",
                "british_triple_16inch_gun_kai", "british_single_18inch_gun",
                "quintuple_533mm_torpedo", "quintuple_533mm_torpedo_mk17",
                "quintuple_21in_torpedo_mk35", "quintuple_torpedo_launcher_53_56",
                "quintuple_torpedo_launcher_53_65", "quad_61cm_torpedo",
                "quad_61cm_oxygen_torpedo", "torpedo_launcher_61cm_type9",
                "quintuple_61cm_torpedo", "quintuple_61cm_oxygen_torpedo"));
        m.put("5-1", List.of("british_twin_3inch_rapid_gun", "type79b_twin_100mm_gun",
                "twin_5inch_l54_dp_gun", "depth_charge_launcher_advanced",
                "small_vt_shell", "advanced_surface_radar", "advanced_aa_radar"));
        m.put("5-2", List.of("improved_triple_6inch_gun", "triple_152mm_dp_gun_46",
                "swedish_triple_152mm_gun", "improved_fire_control_radar"));
        m.put("5-3", List.of("soviet_improved_triple_180mm_gun", "italian_twin_203mm_gun",
                "german_triple_203mm_gun"));
        m.put("5-4", List.of("us_triple_14inch_gun_kai", "british_quad_14inch_gun_284"));
        m.put("6-1", List.of("italian_triple_381mm_gun_kai", "german_twin_406mm_gun",
                "us_triple_16inch_gun_mk7", "german_skc40_triple_420mm_gun",
                "advanced_fire_control_radar"));
        m.put("6-2", List.of("ship_rocket_launcher", "seacat_launcher", "sea_dart_launcher"));
        m.put("6-3", List.of("japanese_sextuple_61cm_torpedo"));
        m.put("6-4", List.of("seiun_kai_bomber", "b25_bomber", "reppu", "seafang", "xf5u"));
        m.put("7-1", List.of("terrier_launcher", "mk14_harpoon_launcher",
                "wyvern", "f4u7", "skyraider"));
        m.put("7-2", List.of("sy1_launcher", "tenga", "ryusei"));
        m.put("7-4", Objects.requireNonNull(MISSILE_LAUNCHER_BLUEPRINTS));
        m.put("7-5", Objects.requireNonNull(MISSILE_LAUNCHER_BLUEPRINTS));
        return m;
    }

    /** 5-5 是 5-1 至 5-4 的合并池；7-3 是 5-1 至 7-2 的先进档合并池。 */
    private static List<String> unionPools(String... stages) {
        List<String> out = new ArrayList<>();
        for (String stage : stages) {
            List<String> p = BLUEPRINT_POOLS.get(stage);
            if (p != null) out.addAll(p);
        }
        return out;
    }

    /**
     * 在 Boss 节点结算瞬间调用。调用方已按《副本/22》筛过「通关瞬间在 Boss 节点内」的玩家。
     */
    public static void grant(ServerPlayer player, StageData stage, List<String> result) {
        String stageId = stage.stageId();
        if ("goldencatcat".equals(stageId)) {
            give(player, "piranport:exp_shell", 16, result);
            give(player, "piranport:toast_bread", 16, result);
            return;
        }
        if ("ulan".equals(stageId)) {
            give(player, "piranport:fuel", 4, result);
            return;
        }

        // 《副本/18》妄想舰队 / 地狱群岛：不套主线奖励，只发燃料桶并递进下一关钥匙。
        Integer eventFuel = EVENT_FUEL_REWARDS.get(stageId);
        if (eventFuel != null) {
            give(player, "piranport:fuel", eventFuel, result);
            String nextStage = EVENT_NEXT_KEYS.get(stageId);
            if (nextStage != null) {
                ItemStack key = new ItemStack(ModItems.DUNGEON_KEY.get());
                DungeonKeyItem.setStageId(key, nextStage);
                giveStack(player, key, result);
            }
            return;
        }

        int chapter = chapterNumber(stage.chapter());
        if (chapter < 1 || chapter > 7) return;

        // 材料包：从本章池独立抽 2 次，允许重复。
        List<Amount> pool = MATERIAL_POOLS.get(chapter);
        if (pool != null && !pool.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                Amount a = pool.get(player.getRandom().nextInt(pool.size()));
                give(player, a.item(), roll(player.getRandom(), a), result);
            }
        }

        // 勋章：每次通关 1 枚，旧「只有章末发」的规则作废。
        giveMedal(player, chapter, result);

        // 蓝图：1-1 至 1-4 没有，2-1 起按关卡池抽 1 张。
        rollBlueprint(player, stageId, result);

        Amount extra = STAGE_EXTRAS.get(stageId);
        if (extra != null) give(player, extra.item(), roll(player.getRandom(), extra), result);

        // 4-3 及以后，每次通关独立 40% 给精英损害管制小队 ×1。
        if (chapter > 4 || (chapter == 4 && stageNumber(stageId) >= 3)) {
            if (player.getRandom().nextFloat() < 0.40f) {
                give(player, "piranport:elite_damage_control", 1, result);
            }
        }

        giveBossDrops(player, stageId, result);

        // 1-4 另给空白 ulan 钥匙；它不替代第二章奖章，也不从遗迹箱出。
        if ("1-4".equals(stageId)) {
            ItemStack key = new ItemStack(ModItems.DUNGEON_KEY.get());
            DungeonKeyItem.setStageId(key, "ulan");
            giveStack(player, key, result);
        }
    }

    private static int chapterNumber(String chapter) {
        if (chapter == null || !chapter.matches("chapter_[1-7]")) return -1;
        return Integer.parseInt(chapter.substring(8));
    }

    private static int stageNumber(String stageId) {
        int dash = stageId == null ? -1 : stageId.indexOf('-');
        if (dash < 0) return 0;
        try {
            return Integer.parseInt(stageId.substring(dash + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int roll(RandomSource random, Amount a) {
        return a.min() + (a.max() > a.min() ? random.nextInt(a.max() - a.min() + 1) : 0);
    }

    private static void giveMedal(ServerPlayer player, int chapter, List<String> result) {
        ItemStack medal = switch (chapter) {
            case 1 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH1.get());
            case 2 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH2.get());
            case 3 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH3.get());
            case 4 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH4.get());
            case 5 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH5.get());
            case 6 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH6.get());
            case 7 -> new ItemStack(ModItems.DEPLOY_MEDAL_CH7.get());
            default -> ItemStack.EMPTY;
        };
        if (!medal.isEmpty()) giveStack(player, medal, result);
    }

    private static void rollBlueprint(ServerPlayer player, String stageId, List<String> result) {
        List<String> pool;
        if ("5-5".equals(stageId)) {
            pool = unionPools("5-1", "5-2", "5-3", "5-4");
        } else if ("7-3".equals(stageId)) {
            // 「先进蓝图」按蓝图物品稀有度判定，避免在代码里再抄一份档位表。
            pool = unionPools("5-1", "5-2", "5-3", "5-4", "5-5",
                    "6-1", "6-2", "6-3", "6-4", "7-1", "7-2");
            pool = pool.stream().filter(MainlineRewardService::isAdvancedBlueprint).toList();
        } else {
            // 注意 get() 而非 getOrDefault()：若键存在但值为 null，getOrDefault 会返回 null。
            List<String> p = BLUEPRINT_POOLS.get(stageId);
            pool = p != null ? p : List.of();
        }
        List<Item> available = new ArrayList<>();
        for (String base : pool) {
            Item blueprint = blueprintItem(base);
            if (blueprint != null) available.add(blueprint);
        }
        if (available.isEmpty()) return;
        Item chosen = available.get(player.getRandom().nextInt(available.size()));
        giveStack(player, new ItemStack(chosen), result);
    }

    private static boolean isAdvancedBlueprint(String base) {
        Item blueprint = blueprintItem(base);
        return blueprint != null && blueprint.getDefaultInstance().getRarity() == Rarity.RARE;
    }

    /** 装备没有注册时返回 null：奖池先按文档收全，等装备落地后自动进入随机。 */
    private static Item blueprintItem(String base) {
        ResourceLocation id = parse(base + "_blueprint");
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    private static void giveBossDrops(ServerPlayer player, String stageId, List<String> result) {
        switch (stageId) {
            case "4-4" -> give(player, "piranport:japanese_triple_460mm_gun", 1, result);
            case "6-3" -> give(player, "piranport:soviet_triple_460mm_gun", 1, result);
            case "7-3" -> {
                give(player, "piranport:f2h_banshee", 1, result);
                give(player, "piranport:xa2j_bomber", 1, result);
                give(player, "piranport:sky_pirate_torpedo", 1, result);
            }
            case "7-5" -> {
                give(player, "piranport:us_twin_20inch_gun", 1, result);
                give(player, "piranport:british_twin_20inch_gun", 1, result);
                give(player, "piranport:japanese_triple_72cm_torpedo", 1, result);
                give(player, "piranport:quad_type0_72cm_torpedo", 1, result);
                give(player, "piranport:clt_quad_torpedo_launcher_type0", 1, result);
                give(player, "piranport:sextuple_61cm_torpedo_launcher_93m3", 1, result);
                give(player, "piranport:septuple_torpedo_launcher", 1, result);
                give(player, "piranport:septuple_torpedo_launcher_93m3", 1, result);
                give(player, "piranport:confidential_cargo",
                        4 + player.getRandom().nextInt(4), result);
            }
            default -> { }
        }
    }

    private static void give(ServerPlayer player, String id, int count, List<String> result) {
        if (count <= 0) return;
        ResourceLocation rl = parse(id);
        if (rl == null) return;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == Items.AIR) {
            com.piranport.PiranPort.LOGGER.warn("主线奖励物品未注册，跳过: {}", id);
            return;
        }
        giveStack(player, new ItemStack(item, count), result);
    }

    private static void giveStack(ServerPlayer player, ItemStack stack, List<String> result) {
        if (stack.isEmpty()) return;
        result.add(stack.getHoverName().getString() + (stack.getCount() > 1 ? " x" + stack.getCount() : ""));
        ItemStack copy = stack.copy();
        if (!player.getInventory().add(copy)) player.drop(copy, false);
    }

    private static ResourceLocation parse(String id) {
        if (id == null || id.isEmpty()) return null;
        return ResourceLocation.tryParse(id.indexOf(':') >= 0 ? id : "piranport:" + id);
    }
}
