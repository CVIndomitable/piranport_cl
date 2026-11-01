package com.piranport.registry;

import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主线关卡钥匙注册表（每关一把，共 30 把）。
 *
 * <p>关卡表必须与 {@code data/piranport/dungeon/chapters/chapter_*.json} 的 {@code stages}
 * 完全一致；物品 ID 由关卡 ID 派生（{@code 1-1} → {@code dungeon_key_1_1}）。
 * 关卡固定在物品类型上，插上书台就进这一关，不再通过组件选关。</p>
 *
 * <p>与其他注册表一样共享 {@link ModItems#ITEMS}，避免重复注册；注册顺序按章、按关稳定。</p>
 */
public final class DungeonKeyItems {
    private DungeonKeyItems() {}

    /**
     * 空方法，仅用于在模组构造器里显式触发本类静态初始化。
     *
     * <p>30 个注册项必须在 {@code RegisterEvent} 之前入队；如果等到第一次配方/奖励访问
     * 本类时才初始化，注册事件早已过去，钥匙不会进注册表。</p>
     */
    public static void init() {
    }

    private static final net.neoforged.neoforge.registries.DeferredRegister.Items ITEMS = ModItems.ITEMS;

    /** 章号 → 关卡 ID。顺序即创造栏顺序，也是“下一关”的推导依据。 */
    public static final Map<Integer, List<String>> MAINLINE_STAGES = buildMainlineStages();

    /** 全部主线关卡，按章顺序展开。 */
    public static final List<String> ALL_STAGES = MAINLINE_STAGES.values().stream()
            .flatMap(List::stream)
            .toList();

    /** 关卡 ID → 钥匙物品，按注册顺序。 */
    private static final Map<String, DeferredItem<DungeonKeyItem>> STAGE_KEYS = registerStageKeys();

    private static Map<Integer, List<String>> buildMainlineStages() {
        Map<Integer, List<String>> stages = new LinkedHashMap<>();
        stages.put(1, List.of("1-1", "1-2", "1-3", "1-4"));
        stages.put(2, List.of("2-1", "2-2", "2-3", "2-4"));
        stages.put(3, List.of("3-1", "3-2", "3-3", "3-4"));
        stages.put(4, List.of("4-1", "4-2", "4-3", "4-4"));
        stages.put(5, List.of("5-1", "5-2", "5-3", "5-4", "5-5"));
        stages.put(6, List.of("6-1", "6-2", "6-3", "6-4"));
        stages.put(7, List.of("7-1", "7-2", "7-3", "7-4", "7-5"));
        return Collections.unmodifiableMap(stages);
    }

    private static Map<String, DeferredItem<DungeonKeyItem>> registerStageKeys() {
        Map<String, DeferredItem<DungeonKeyItem>> keys = new LinkedHashMap<>();
        for (List<String> stages : MAINLINE_STAGES.values()) {
            for (String stageId : stages) {
                String itemId = "dungeon_key_" + stageId.replace('-', '_');
                DeferredItem<DungeonKeyItem> key = ITEMS.register(itemId,
                        () -> new DungeonKeyItem(stageId, new Item.Properties().stacksTo(1)
                                .component(ModDataComponents.DUNGEON_PROGRESS.get(), DungeonProgress.EMPTY)));
                keys.put(stageId, key);
            }
        }
        return Collections.unmodifiableMap(keys);
    }

    /** 返回关卡对应的钥匙物品；非法关卡返回 null。 */
    public static Item stageKey(String stageId) {
        DeferredItem<DungeonKeyItem> holder = STAGE_KEYS.get(stageId);
        return holder == null ? null : holder.get();
    }

    /** 该关是否本章最后一关（章末关发奖章，不再发下一关钥匙）。 */
    public static boolean isFinalStage(String stageId) {
        for (List<String> stages : MAINLINE_STAGES.values()) {
            if (!stages.isEmpty() && stages.get(stages.size() - 1).equals(stageId)) return true;
        }
        return false;
    }

    /** 同章下一关；章末关或未知关卡返回 null。 */
    public static String nextStage(String stageId) {
        for (List<String> stages : MAINLINE_STAGES.values()) {
            int index = stages.indexOf(stageId);
            if (index >= 0) {
                return index + 1 < stages.size() ? stages.get(index + 1) : null;
            }
        }
        return null;
    }
}
