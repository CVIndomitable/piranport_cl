package com.piranport.crafting;

import com.piranport.combat.cannon.CannonAmmoRules;
import com.piranport.component.EquipmentTier;
import com.piranport.registry.ModItems;
import com.piranport.registry.WeaponItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeaponWorkbenchRecipeRegistry {
    private static final List<WeaponWorkbenchRecipe> ALL_RECIPES;
    private static final Map<Integer, List<WeaponWorkbenchRecipe>> TAB_CACHE;

    static {
        List<WeaponWorkbenchRecipe> recipes = new ArrayList<>();
        registerAll(recipes);
        ALL_RECIPES = List.copyOf(recipes);

        Map<Integer, List<WeaponWorkbenchRecipe>> byTab = new HashMap<>();
        for (WeaponWorkbenchRecipe r : ALL_RECIPES) {
            byTab.computeIfAbsent(r.tab(), k -> new ArrayList<>()).add(r);
        }
        Map<Integer, List<WeaponWorkbenchRecipe>> frozen = new HashMap<>();
        for (Map.Entry<Integer, List<WeaponWorkbenchRecipe>> e : byTab.entrySet()) {
            frozen.put(e.getKey(), List.copyOf(e.getValue()));
        }
        TAB_CACHE = Map.copyOf(frozen);
    }

    /** 物品注册名是否在无法合成清单内（名单见 {@link WeaponCraftability}）。 */
    public static boolean isUncraftable(Item item) {
        return WeaponCraftability.isUncraftable(item);
    }

    private WeaponWorkbenchRecipeRegistry() {}

    public static List<WeaponWorkbenchRecipe> getAllRecipes() {
        return ALL_RECIPES;
    }

    public static List<WeaponWorkbenchRecipe> getRecipesForTab(int tab) {
        return TAB_CACHE.getOrDefault(tab, List.of());
    }

    @Nullable
    public static WeaponWorkbenchRecipe getRecipe(int tab, int index) {
        List<WeaponWorkbenchRecipe> tabRecipes = getRecipesForTab(tab);
        if (index >= 0 && index < tabRecipes.size()) {
            return tabRecipes.get(index);
        }
        return null;
    }

    private static void add(List<WeaponWorkbenchRecipe> list, int tab, Item result, List<ItemStack> materials,
                            @Nullable Item blueprint, int craftingTime) {
        add(list, tab, result, materials, blueprint, craftingTime, null);
    }

    private static void add(List<WeaponWorkbenchRecipe> list, int tab, Item result, List<ItemStack> materials,
                            @Nullable Item blueprint, int craftingTime, @Nullable String unlockStage) {
        list.add(new WeaponWorkbenchRecipe(tab, result, materials, blueprint, craftingTime, unlockStage));
    }

    /** 需要按通关进度解锁的关卡；顺序即菜单同步位掩码的位序。 */
    public static final List<String> GATE_STAGES = List.of("2-1", "4-2");

    /** 服务端把玩家已通关的关卡压成位掩码，随工作台菜单一起发给客户端。 */
    public static int gateMask(net.minecraft.world.entity.player.Player player) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return 0;
        var cleared = com.piranport.dungeon.saved.DungeonSavedData.get(level)
                .getFirstClearedStages(player.getUUID());
        int mask = 0;
        for (int i = 0; i < GATE_STAGES.size(); i++) {
            if (cleared.contains(GATE_STAGES.get(i))) mask |= 1 << i;
        }
        return mask;
    }

    /** 配方是否已对这名玩家解锁。 */
    public static boolean unlocked(WeaponWorkbenchRecipe recipe, int mask) {
        if (recipe.unlockStage() == null) return true;
        int bit = GATE_STAGES.indexOf(recipe.unlockStage());
        return bit >= 0 && (mask & (1 << bit)) != 0;
    }

    /**
     * 装备 → 所需蓝图（策划决策/数值/09）。
     * 初期、标准无需蓝图；改良、先进各用自己的专属蓝图，一张只解锁一件装备。
     * 未注册蓝图的可合成装备（数据异常）返回 null，避免配方被永久锁死。
     */
    @Nullable
    public static Item blueprintFor(String equipmentId, EquipmentTier tier) {
        if (tier == EquipmentTier.INITIAL || tier == EquipmentTier.STANDARD) return null;
        var holder = WeaponItems.EQUIPMENT_BLUEPRINTS.get(equipmentId);
        return holder == null ? null : holder.get();
    }

    /**
     * 能放进武器制造台蓝图格 / 蓝图箱的物品。
     * 旧五件通用蓝图仍注册（存档兼容），但不再当门槛，也不收进蓝图箱（数值/09）。
     */
    public static boolean isWorkbenchBlueprint(ItemStack stack) {
        if (stack.is(ModItems.CREATIVE_BLUEPRINT.get())) return true;
        for (var holder : WeaponItems.EQUIPMENT_BLUEPRINTS.values()) {
            if (stack.is(holder.get())) return true;
        }
        return false;
    }

    /**
     * 配方蓝图校验：requiredBlueprint 为 null = 无需蓝图（蓝图格可空）；创造蓝图通吃。
     * 依据：副本/21 §1.2「其余配方传 null = 直接可用」。旧实现把 null 当成「不可合成」，
     * 导致鱼雷/导弹/深弹/飞机配方在生存模式永远做不出来。
     */
    public static boolean blueprintSatisfied(WeaponWorkbenchRecipe recipe, ItemStack inSlot) {
        if (!inSlot.isEmpty() && inSlot.is(ModItems.CREATIVE_BLUEPRINT.get())) return true;
        if (recipe.requiredBlueprint() == null) return true;
        return !inSlot.isEmpty() && inSlot.is(recipe.requiredBlueprint());
    }

    /**
     * 火炮材料（策划决策/数值/09）：结构材料按口径族与联装数，第三种材料随档替换、不叠加。
     * n = 联装数；小口径用潜影壳、中口径用龙息、大口径用末影水晶作为先进档的末地材料。
     */
    static List<ItemStack> gunMaterials(CannonAmmoRules.CaliberFamily family, int barrels, EquipmentTier tier) {
        int n = Math.max(1, barrels);
        List<ItemStack> m = new ArrayList<>();
        switch (family) {
            case SMALL -> {
                m.add(new ItemStack(Items.IRON_INGOT, 4 + 2 * n));
                m.add(new ItemStack(Items.COPPER_INGOT, 2));
            }
            case MEDIUM -> {
                m.add(new ItemStack(Items.IRON_INGOT, 8 + 3 * n));
                m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2 + n));
            }
            case LARGE -> {
                m.add(new ItemStack(Items.IRON_INGOT, 12 + 4 * n));
                m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4 + 2 * n));
            }
        }
        switch (tier) {
            case INITIAL -> m.add(new ItemStack(Items.REDSTONE,
                    family == CannonAmmoRules.CaliberFamily.LARGE ? 4 : 2));
            case STANDARD -> m.add(new ItemStack(Items.DIAMOND, 1));
            case IMPROVED -> m.add(new ItemStack(Items.DIAMOND, 2));
            case ADVANCED -> {
                m.add(new ItemStack(Items.NETHERITE_SCRAP,
                        family == CannonAmmoRules.CaliberFamily.LARGE ? 3 : 2));
                m.add(new ItemStack(switch (family) {
                    case SMALL -> Items.SHULKER_SHELL;
                    case MEDIUM -> Items.DRAGON_BREATH;
                    case LARGE -> Items.END_CRYSTAL;
                }, 1));
            }
        }
        return List.copyOf(m);
    }

    /** 鱼雷发射器材料：步长 533=0、610=1、720=2；第三种材料随档替换（数值/09）。 */
    static List<ItemStack> torpedoMaterials(int caliberMm, int tubes, EquipmentTier tier) {
        int n = Math.max(1, tubes);
        int step = caliberMm >= 720 ? 2 : caliberMm >= 610 ? 1 : 0;
        List<ItemStack> m = new ArrayList<>();
        m.add(new ItemStack(Items.IRON_INGOT, 4 + n + 2 * step));
        m.add(new ItemStack(ModItems.ALUMINUM_INGOT.get(), 2 + n + step));
        switch (tier) {
            case INITIAL -> m.add(new ItemStack(Items.REDSTONE, 1 + step));
            case STANDARD -> m.add(new ItemStack(Items.DIAMOND, 1));
            case IMPROVED -> m.add(new ItemStack(Items.DIAMOND, 2));
            case ADVANCED -> {
                m.add(new ItemStack(Items.NETHERITE_SCRAP, 2));
                m.add(new ItemStack(Items.SHULKER_SHELL, 1));
            }
        }
        return List.copyOf(m);
    }

    static int craftTime(int baseTicks, EquipmentTier tier) {
        return switch (tier) {
            case INITIAL -> baseTicks;
            case STANDARD -> baseTicks * 5 / 4;
            case IMPROVED -> baseTicks * 3 / 2;
            case ADVANCED -> baseTicks * 2;
        };
    }

    /**
     * 飞机分档（策划决策/数值/09）。表内未列出的机体不生成配方：
     * 无法合成的三架（女妖、XA2J、空中海盗）、深海敌机、活动涂装与通用占位 {@code xtb2d}。
     */
    private static final Map<String, EquipmentTier> AIRCRAFT_TIERS = Map.ofEntries(
            // 初期
            Map.entry("petrel_bomber", EquipmentTier.INITIAL),
            Map.entry("type99_dive_bomber", EquipmentTier.INITIAL),
            Map.entry("swordfish_torpedo", EquipmentTier.INITIAL),
            Map.entry("swordfish_asw", EquipmentTier.INITIAL),
            Map.entry("type0_recon", EquipmentTier.INITIAL),
            // 标准
            Map.entry("f4f_wildcat", EquipmentTier.STANDARD),
            Map.entry("seafire", EquipmentTier.STANDARD),
            Map.entry("zero_model52", EquipmentTier.STANDARD),
            Map.entry("sbd_dauntless", EquipmentTier.STANDARD),
            Map.entry("firefly_as_mk5", EquipmentTier.STANDARD),
            Map.entry("type97_torpedo", EquipmentTier.STANDARD),
            Map.entry("c1_recon", EquipmentTier.STANDARD),
            // 改良
            Map.entry("f4u_corsair", EquipmentTier.IMPROVED),
            Map.entry("f6f_hellcat_rocket", EquipmentTier.IMPROVED),
            Map.entry("suisei_bomber", EquipmentTier.IMPROVED),
            Map.entry("tenzan_torpedo", EquipmentTier.IMPROVED),
            Map.entry("tbf_torpedo", EquipmentTier.IMPROVED),
            Map.entry("tbf_asw", EquipmentTier.IMPROVED),
            Map.entry("saiun_recon", EquipmentTier.IMPROVED),
            // 先进
            Map.entry("seiun_kai_bomber", EquipmentTier.ADVANCED),
            Map.entry("b25_bomber", EquipmentTier.ADVANCED));

    /** 飞机材料：铝锭为主，随档替换第二种材料（数值/09）。 */
    static List<ItemStack> aircraftMaterials(EquipmentTier tier) {
        return switch (tier) {
            case INITIAL -> List.of(
                    new ItemStack(ModItems.ALUMINUM_INGOT.get(), 8),
                    new ItemStack(Items.REDSTONE, 4));
            case STANDARD -> List.of(
                    new ItemStack(ModItems.ALUMINUM_INGOT.get(), 12),
                    new ItemStack(Items.DIAMOND, 1));
            case IMPROVED -> List.of(
                    new ItemStack(ModItems.ALUMINUM_INGOT.get(), 16),
                    new ItemStack(Items.DIAMOND, 2));
            case ADVANCED -> List.of(
                    new ItemStack(ModItems.ALUMINUM_INGOT.get(), 20),
                    new ItemStack(Items.NETHERITE_SCRAP, 3),
                    new ItemStack(Items.END_CRYSTAL, 1));
        };
    }

    /** 近防炮材料：比同档手持炮轻；只按档给固定量，不随联装数膨胀（28 联会失控）。 */
    static List<ItemStack> ciwsMaterials(EquipmentTier tier) {
        List<ItemStack> m = new ArrayList<>();
        switch (tier) {
            case INITIAL -> {
                m.add(new ItemStack(Items.IRON_INGOT, 4));
                m.add(new ItemStack(Items.COPPER_INGOT, 2));
                m.add(new ItemStack(Items.REDSTONE, 2));
            }
            case STANDARD -> {
                m.add(new ItemStack(Items.IRON_INGOT, 6));
                m.add(new ItemStack(Items.COPPER_INGOT, 2));
                m.add(new ItemStack(Items.DIAMOND, 1));
            }
            case IMPROVED -> {
                m.add(new ItemStack(Items.IRON_INGOT, 8));
                m.add(new ItemStack(Items.COPPER_INGOT, 4));
                m.add(new ItemStack(Items.DIAMOND, 2));
            }
            case ADVANCED -> {
                m.add(new ItemStack(Items.IRON_INGOT, 10));
                m.add(new ItemStack(Items.COPPER_INGOT, 4));
                m.add(new ItemStack(Items.NETHERITE_SCRAP, 2));
                m.add(new ItemStack(Items.SHULKER_SHELL, 1));
            }
        }
        return List.copyOf(m);
    }

    private static void registerAll(List<WeaponWorkbenchRecipe> r) {
        // ===== Tab 0: 火炮 =====
        // 数值/09：single_small_gun（日本12厘米单装炮，2026-10-25 已并入 07 表）与 medium_gun
        // 仍属通用占位炮，生存模式不给配方；其余 07 表火炮进工作台。
        // 数值/07 注册表 62 门：改良/先进验装备专属蓝图，材料按口径族与联装（数值/09）。
        for (Item gun : com.piranport.registry.WeaponItems.allCatalogGuns()) {
            if (gun == ModItems.SINGLE_SMALL_GUN.get()) continue;
            if (isUncraftable(gun)) continue;
            if (!(gun instanceof com.piranport.artillery.ArtilleryItem artillery)) continue;
            var data = artillery.getData();
            EquipmentTier tier = data.tierOrDefault();
            var family = CannonAmmoRules.familyForData(data);
            int base = switch (family) { case SMALL -> 100; case MEDIUM -> 200; case LARGE -> 400; };
            String id = BuiltInRegistries.ITEM.getKey(gun).getPath();
            add(r, 0, gun, gunMaterials(family, data.barrels(), tier), blueprintFor(id, tier), craftTime(base, tier));
        }

        // ===== Tab 1: 鱼雷发射器 =====
        // 通用发射器 + 数值/08 图鉴发射器；改良/先进验装备专属蓝图。
        List<com.piranport.item.TorpedoLauncherItem> launchers = new ArrayList<>();
        launchers.add(ModItems.TRIPLE_TORPEDO_LAUNCHER.get());
        launchers.add(ModItems.QUAD_TORPEDO_LAUNCHER.get());
        launchers.add(ModItems.QUINTUPLE_TORPEDO_LAUNCHER.get());
        ModItems.CATALOG_TORPEDO_LAUNCHERS.forEach(l -> launchers.add(l.get()));
        for (var launcher : launchers) {
            if (isUncraftable(launcher)) continue;
            EquipmentTier tier = launcher.getTier();
            int base = 150 + 25 * launcher.getTubeCount();
            String id = BuiltInRegistries.ITEM.getKey(launcher).getPath();
            add(r, 1, launcher, torpedoMaterials(launcher.getCaliber(), launcher.getTubeCount(), tier),
                    blueprintFor(id, tier), craftTime(base, tier));
        }

        // ===== Tab 2: 导弹发射器 =====
        // 数值/09：全部先进档，直接用先进材料，各要自己的先进蓝图。
        add(r, 2, ModItems.TERRIER_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.END_CRYSTAL, 1)
        ), blueprintFor("terrier_launcher", EquipmentTier.ADVANCED), 400);

        add(r, 2, ModItems.SEA_DART_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.DRAGON_BREATH, 1)
        ), blueprintFor("sea_dart_launcher", EquipmentTier.ADVANCED), 400);

        add(r, 2, ModItems.SEACAT_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.SHULKER_SHELL, 1)
        ), blueprintFor("seacat_launcher", EquipmentTier.ADVANCED), 400);

        add(r, 2, ModItems.SY1_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.NETHER_STAR, 1)
        ), blueprintFor("sy1_launcher", EquipmentTier.ADVANCED), 500);

        add(r, 2, ModItems.MK14_HARPOON_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 4),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.CONDUIT, 1)
        ), blueprintFor("mk14_harpoon_launcher", EquipmentTier.ADVANCED), 500);

        add(r, 2, ModItems.SHIP_ROCKET_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.GUNPOWDER, 4),
                new ItemStack(Items.NETHERITE_SCRAP, 1),
                new ItemStack(Items.DRAGON_BREATH, 1)
        ), blueprintFor("ship_rocket_launcher", EquipmentTier.ADVANCED), 400);

        // ===== Tab 3: 深弹 =====
        // 数值/09：初期/改良不查蓝图；先进档要自己的改良蓝图。
        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.REDSTONE, 2)
        ), null, 100);

        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER_IMPROVED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.DIAMOND, 1)
        ), null, 200);

        add(r, 3, ModItems.DEPTH_CHARGE_LAUNCHER_ADVANCED.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.DIAMOND, 2)
        ), blueprintFor("depth_charge_launcher_advanced", EquipmentTier.IMPROVED), 300);

        // ===== Tab 5: 装备（雷达三代，火控/08）=====
        add(r, 5, ModItems.STANDARD_SURFACE_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.DAYLIGHT_DETECTOR, 1),
                new ItemStack(Items.COPPER_INGOT, 2)), null, 200);
        add(r, 5, ModItems.IMPROVED_SURFACE_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.COPPER_BULB, 1),
                new ItemStack(Items.DIAMOND, 1),
                new ItemStack(Items.COPPER_INGOT, 2)),
                blueprintFor("improved_surface_radar", EquipmentTier.IMPROVED), 300);
        add(r, 5, ModItems.ADVANCED_SURFACE_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.ENDER_EYE, 1),
                new ItemStack(Items.NETHERITE_SCRAP, 1),
                new ItemStack(Items.COPPER_INGOT, 2)),
                blueprintFor("advanced_surface_radar", EquipmentTier.ADVANCED), 400);

        add(r, 5, ModItems.STANDARD_AIR_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.DAYLIGHT_DETECTOR, 1),
                new ItemStack(Items.GLOWSTONE_DUST, 2)), null, 200);
        add(r, 5, ModItems.IMPROVED_AA_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.COPPER_BULB, 1),
                new ItemStack(Items.DIAMOND, 1),
                new ItemStack(Items.GLOWSTONE_DUST, 2)),
                blueprintFor("improved_aa_radar", EquipmentTier.IMPROVED), 300);
        add(r, 5, ModItems.ADVANCED_AA_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.ENDER_EYE, 1),
                new ItemStack(Items.NETHERITE_SCRAP, 1),
                new ItemStack(Items.GLOWSTONE_DUST, 2)),
                blueprintFor("advanced_aa_radar", EquipmentTier.ADVANCED), 400);

        add(r, 5, ModItems.STANDARD_FIRE_CONTROL_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.DAYLIGHT_DETECTOR, 1),
                new ItemStack(Items.COMPARATOR, 1)), null, 200);
        add(r, 5, ModItems.IMPROVED_FIRE_CONTROL_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 4),
                new ItemStack(Items.COPPER_BULB, 1),
                new ItemStack(Items.DIAMOND, 1),
                new ItemStack(Items.COMPARATOR, 1)),
                blueprintFor("improved_fire_control_radar", EquipmentTier.IMPROVED), 300);
        add(r, 5, ModItems.ADVANCED_FIRE_CONTROL_RADAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 6),
                new ItemStack(Items.ENDER_EYE, 1),
                new ItemStack(Items.NETHERITE_SCRAP, 1),
                new ItemStack(Items.COMPARATOR, 1)),
                blueprintFor("advanced_fire_control_radar", EquipmentTier.ADVANCED), 400);

        // 标准型声呐：4-2 起可合成、不查蓝图（副本/21）。数值/09 未给材料，首轮取铁/铜/红石/比较器。
        add(r, 5, ModItems.STANDARD_SONAR.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(Items.COPPER_INGOT, 4),
                new ItemStack(Items.REDSTONE, 4),
                new ItemStack(Items.COMPARATOR, 1)), null, 300, "4-2");

        // 改良/先进引擎：蓝图进 4-2 奖池（副本/21）；材料按先进档口径取铁/铝/钻石/下界合金碎片。
        add(r, 5, ModItems.IMPROVED_ENGINE.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 8),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 6),
                new ItemStack(Items.DIAMOND, 2)),
                blueprintFor("improved_engine", EquipmentTier.IMPROVED), 300);
        add(r, 5, ModItems.ADVANCED_ENGINE.get(), List.of(
                new ItemStack(Items.IRON_INGOT, 10),
                new ItemStack(ModItems.ALUMINUM_INGOT.get(), 8),
                new ItemStack(Items.NETHERITE_SCRAP, 2),
                new ItemStack(Items.END_CRYSTAL, 1)),
                blueprintFor("advanced_engine", EquipmentTier.ADVANCED), 400);

        // ===== 自动近防炮（舰装/06）=====
        // 初期/标准不查蓝图，改良/先进各要自己的专属蓝图；2-1 通关池抽的正是后 11 张。
        for (var holder : com.piranport.registry.WeaponItems.CIWS_ITEMS) {
            com.piranport.item.AutoCIWSItem ciws = holder.get();
            String id = BuiltInRegistries.ITEM.getKey(ciws).getPath();
            EquipmentTier tier = ciws.getTier();
            // 初期/标准 2-1 通关后才出现在制造台；改良/先进靠蓝图，不再另设门槛。
            String gate = (tier == EquipmentTier.INITIAL || tier == EquipmentTier.STANDARD) ? "2-1" : null;
            add(r, 5, ciws, ciwsMaterials(tier), blueprintFor(id, tier), craftTime(100, tier), gate);
        }

        // ===== Tab 4: 飞机 =====
        // 数值/09：按机种分档，材料随档替换；无蓝图。列表只覆盖已注册且有分档的机体。
        for (var holder : com.piranport.registry.AircraftItems.AIRCRAFT_ITEMS) {
            Item airplane = holder.get();
            String id = BuiltInRegistries.ITEM.getKey(airplane).getPath();
            EquipmentTier tier = AIRCRAFT_TIERS.get(id);
            if (tier == null) continue;
            // 改良/先进机体各要自己的蓝图（数值/09）；初期/标准不查。
            add(r, 4, airplane, aircraftMaterials(tier), blueprintFor(id, tier), craftTime(200, tier));
        }
    }
}