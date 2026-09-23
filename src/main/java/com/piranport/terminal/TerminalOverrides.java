package com.piranport.terminal;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 调试终端覆盖值的「运行时只读镜像」。
 *
 * <p>WHY 需要这层镜像：覆盖数据本身存在 {@link TerminalOverridesSavedData}（服务端主线程持有，
 * 且只有服务端才知道当前存档），但读取方分布在三个互不相干的位置 ——
 * {@code TorpedoItem.getSpeed()}（物品定义，客户端渲染 tooltip 时也会走到）、
 * {@code TransformationManager}（服务端实体属性）、{@code ShipCoreItem}（tooltip）。
 * 让它们反向依赖 SavedData 会引入「客户端拿不到服务端存档对象」的空指针。
 *
 * <p>因此把覆盖值折叠成一层无状态的静态镜像：
 * <ul>
 *   <li>服务端在每次写入/加载/同步后调用 {@link #apply} 整体替换；</li>
 *   <li>客户端在收到 S2C 同步包后调用 {@link #apply}；</li>
 *   <li>读取方只调 {@link #torpedoSpeedDelta} / {@link #coreSpeedDelta}，天然为 O(1) 无锁。</li>
 * </ul>
 *
 * <p>线程模型：{@code snapshot} 用 {@code volatile} 引用整体替换、内部 Map 一律
 * {@code Map.copyOf} 成不可变对象，因此读线程永远看到自洽的一份快照，
 * 不会读到「改了一半」的中间态。写的频率是人工操作级别，无锁是安全的。
 */
public final class TerminalOverrides {

    /** 空快照常量：未加载任何存档时的初始状态，也用于「清空所有覆盖」。 */
    private static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of());

    /** 当前快照。整体替换，不做原地修改 —— 这是本类无锁安全的前提。 */
    private static volatile Snapshot snapshot = EMPTY;

    /**
     * 快照版本号，每次 {@link #apply} 自增。
     *
     * <p>WHY 需要它：终端界面的编辑框内容是在 {@code rebuildWidgets()} 时按当时镜像
     * 算出来的一次性文本，之后即便服务端把覆盖值改了/清了，屏上还是旧数字。
     * 界面靠比较自己记住的版本号来判断「镜像变过」，在下一帧把编辑框刷成权威值。
     * 不能直接比较 Map 内容 —— 那样每帧都要遍历两个 Map，而且分不清
     * 「服务端钳制后恰好等于旧值」与「压根没同步过」。
     */
    private static volatile long revision = 0L;

    private TerminalOverrides() {
    }

    /**
     * 鱼雷航速偏移上限（blocks/tick）。
     *
     * <p>WHY 需要钳制：终端接受手工输入，一个手滑的 1e9 会让鱼雷每 tick 跨越半个世界，
     * 直接把区块加载器拖垮。上限取「基准值大致量级 + 2.0」——足够做极端测试，
     * 又不至于产生物理上无意义的瞬移。
     */
    public static final float TORPEDO_DELTA_MIN = -0.5f;
    public static final float TORPEDO_DELTA_MAX = 2.0f;

    /** 舰娘核心航速倍率的合法区间。 */
    public static final double CORE_SPEED_MIN = 0.1;
    public static final double CORE_SPEED_MAX = 5.0;

    /**
     * 整体替换运行时快照。
     *
     * <p>调用方（SavedData 写入后 / 网络包收到后）负责传入已校验的值；
     * 本方法仍会再过滤一遍 NaN/Infinity —— 这是唯一收口点，一旦 NaN 进快照，
     * {@code TorpedoItem.getSpeed()} 会返回 NaN → 实体坐标 NaN。
     * 过滤是为了「让调用方的错误无处藏身」，而不是因为不信任调用方。
     */
    public static void apply(Map<String, Float> torpedoDeltas, Map<String, Double> coreDeltas) {
        snapshot = new Snapshot(
                finiteFloats(torpedoDeltas),
                finiteDoubles(coreDeltas));
        revision++;
    }

    /** 丢弃 null / 非有限（NaN、±Infinity）的鱼雷偏移条目。 */
    private static Map<String, Float> finiteFloats(Map<String, Float> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Float> filtered = new HashMap<>(raw.size());
        for (Map.Entry<String, Float> entry : raw.entrySet()) {
            Float value = entry.getValue();
            if (entry.getKey() != null && value != null && Float.isFinite(value)) {
                filtered.put(entry.getKey(), value);
            }
        }
        return Map.copyOf(filtered);
    }

    /** 丢弃 null / 非有限（NaN、±Infinity）的核心偏移条目。 */
    private static Map<String, Double> finiteDoubles(Map<String, Double> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> filtered = new HashMap<>(raw.size());
        for (Map.Entry<String, Double> entry : raw.entrySet()) {
            Double value = entry.getValue();
            if (entry.getKey() != null && value != null && Double.isFinite(value)) {
                filtered.put(entry.getKey(), value);
            }
        }
        return Map.copyOf(filtered);
    }

    /** 当前快照版本号；只在 {@link #apply} / {@link #clear} 后变化。见 {@link #revision}。 */
    public static long revision() {
        return revision;
    }

    /** 清空所有覆盖（重置功能）。 */
    public static void clear() {
        snapshot = EMPTY;
        revision++;
    }

    /**
     * 查某型号鱼雷的航速偏移。
     *
     * @param modelKey 型号注册 ID，<b>裸路径不带命名空间</b>（如 {@code torpedo_533mm_mk14}）；
     *                 为 null（compat 路径手工构造的实例）时返回 0，即不参与覆盖。
     * @return 偏移量，未覆盖时为 0
     */
    public static float torpedoSpeedDelta(@Nullable String modelKey) {
        if (modelKey == null) {
            return 0f;
        }
        // getOrDefault 而非 get + null 判断：Map.copyOf 产生的不可变 Map 不接受 null 查询
        // 之外的语义差异，这里保持最简单的一次查表。
        Float delta = snapshot.torpedoDeltas().get(modelKey);
        return delta == null ? 0f : delta;
    }

    /**
     * 查舰娘核心的航速偏移。
     *
     * @param coreKey 舰型名（如 {@code LARGE}）
     * @return 偏移量，未覆盖时为 0
     */
    public static double coreSpeedDelta(@Nullable String coreKey) {
        if (coreKey == null) {
            return 0d;
        }
        Double delta = snapshot.coreDeltas().get(coreKey);
        return delta == null ? 0d : delta;
    }

    /** 是否有任何覆盖生效（终端 UI 显示「已修改」角标用）。 */
    public static boolean hasAnyOverride() {
        Snapshot s = snapshot;
        return !s.torpedoDeltas().isEmpty() || !s.coreDeltas().isEmpty();
    }

    /** 当前鱼雷覆盖快照（只读，用于 S2C 同步与 CSV 导出）。 */
    public static Map<String, Float> torpedoSnapshot() {
        return snapshot.torpedoDeltas();
    }

    /** 当前核心覆盖快照（只读）。 */
    public static Map<String, Double> coreSnapshot() {
        return snapshot.coreDeltas();
    }

    /**
     * 不可变快照载体。用 record 而非两个独立的 volatile 字段，是为了让
     * {@link #apply} 的两次赋值在读者眼里成为一个原子切换。
     */
    private record Snapshot(Map<String, Float> torpedoDeltas, Map<String, Double> coreDeltas) {
    }
}
