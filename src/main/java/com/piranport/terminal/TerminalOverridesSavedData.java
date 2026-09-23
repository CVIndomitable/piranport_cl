package com.piranport.terminal;

import com.piranport.PiranPort;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 存档级别的调试终端覆盖数据。
 *
 * <p>存储位置: &lt;主世界&gt;/data/piranport_terminal_overrides.dat —— 锚点固定为主世界，
 * 与 {@code DungeonSavedData} 一致。WHY 不用调用方所在维度：本域是存档级数据，
 * 而 MC 每个维度各持一份独立的 data 目录，按维度存会把一份覆盖拆成互不可见的几份，
 * 且 {@code publishToRuntime} 是全局单例，两个维度会互相覆盖镜像。
 * <p>用途: 调试终端「速度」标签页写入的数值覆盖。
 * <p>线程模型: 服务端主线程。
 *
 * <p>WHY 与火炮域（{@code ArtilleryConfigOverrideSavedData}）并列而非合并：
 * 两者的值与校验规则毫无重叠 —— 火炮域是「字段名 → 白名单类型值」的多层结构且强耦合
 * {@code ConfigOverrideManager} 的字段白名单；本域是「注册 ID / 舰型名 → 单个浮点偏移」，
 * 强行抽象成通用管理器只会得到一堆 {@code Map<String, Object>} 与 instanceof 分支。
 *
 * <p>WHY 存偏移量而不是绝对值：终端只调「快一点 / 慢一点」这种相对量，
 * 存偏移量时策划表改基准速度后存量覆盖自动跟随，不需要人工清理过期覆盖值。
 */
public class TerminalOverridesSavedData extends SavedData {
    private static final String DATA_NAME = "piranport_terminal_overrides";

    private static final String KEY_TORPEDOES = "torpedoes";
    private static final String KEY_CORES = "cores";

    /** 鱼雷型号覆盖: 注册 ID → 航速偏移（blocks/tick） */
    private final Map<String, Float> torpedoDeltas = new HashMap<>();

    /** 舰娘核心覆盖: 舰型名 → 航速倍率偏移 */
    private final Map<String, Double> coreDeltas = new HashMap<>();

    public TerminalOverridesSavedData() {
    }

    // ==================== 鱼雷航速覆盖 ====================

    /**
     * 设置某型号鱼雷的航速偏移。
     *
     * <p>偏移量为 0 时等价于移除覆盖，避免存档里堆积大量无意义的 0 值。
     *
     * @param modelKey 型号注册 ID
     * @param delta    偏移量（blocks/tick），将被钳制到合法区间
     */
    public void setTorpedoSpeedDelta(String modelKey, float delta) {
        if (modelKey == null || modelKey.isEmpty()) {
            return;
        }
        float clamped = clampTorpedoDelta(delta);
        if (clamped == 0f) {
            if (torpedoDeltas.remove(modelKey) != null) {
                setDirty();
            }
        } else {
            torpedoDeltas.put(modelKey, clamped);
            setDirty();
        }
        publishToRuntime();
    }

    /**
     * 读取某型号鱼雷的航速偏移。
     *
     * @return 偏移量，未覆盖时为 0
     */
    public float getTorpedoSpeedDelta(String modelKey) {
        Float delta = torpedoDeltas.get(modelKey);
        return delta == null ? 0f : delta;
    }

    public void removeTorpedoSpeedDelta(String modelKey) {
        if (torpedoDeltas.remove(modelKey) != null) {
            setDirty();
            publishToRuntime();
        }
    }

    /** 所有鱼雷覆盖（只读副本，用于 CSV 导出与 S2C 同步）。 */
    public Map<String, Float> getAllTorpedoDeltas() {
        return new HashMap<>(torpedoDeltas);
    }

    // ==================== 舰娘核心航速覆盖 ====================

    /**
     * 设置某舰型的核心航速倍率偏移。
     *
     * <p>偏移量为 0 时等价于移除覆盖 —— 与 {@link #setTorpedoSpeedDelta} 语义对齐。
     * WHY 要在钳制<b>之前</b>判 0：{@link #clampCoreDelta} 的下限是 0.1，
     * 先钳再判会让「填 0 清除」永远走不到移除分支（钳完最小也是 0.1），
     * 只有 NaN/Infinity 才可达，而调用方已把 non-finite 挡在门外 —— 即死代码。
     *
     * @param coreKey 舰型名（如 {@code LARGE}）
     * @param delta   偏移量，将被钳制到合法区间
     */
    public void setCoreSpeedDelta(String coreKey, double delta) {
        if (coreKey == null || coreKey.isEmpty()) {
            return;
        }
        if (delta == 0d) {
            removeCoreSpeedDelta(coreKey);
            return;
        }
        double clamped = clampCoreDelta(delta);
        Double existing = coreDeltas.put(coreKey, clamped);
        // 只有值真的变化才标脏：Double 装箱比较用 equals，避免 == 对 -0.0/NaN 的陷阱。
        if (existing == null || !existing.equals(clamped)) {
            setDirty();
        }
        publishToRuntime();
    }

    /**
     * 读取某舰型的核心航速倍率偏移。
     *
     * @return 偏移量，未覆盖时为 0
     */
    public double getCoreSpeedDelta(String coreKey) {
        Double delta = coreDeltas.get(coreKey);
        return delta == null ? 0d : delta;
    }

    public void removeCoreSpeedDelta(String coreKey) {
        if (coreDeltas.remove(coreKey) != null) {
            setDirty();
            publishToRuntime();
        }
    }

    /** 所有核心覆盖（只读副本）。 */
    public Map<String, Double> getAllCoreDeltas() {
        return new HashMap<>(coreDeltas);
    }

    // ==================== 批量操作 ====================

    /** 清空所有覆盖（终端「重置全部」按钮）。 */
    public void clearAll() {
        if (torpedoDeltas.isEmpty() && coreDeltas.isEmpty()) {
            return;
        }
        torpedoDeltas.clear();
        coreDeltas.clear();
        setDirty();
        publishToRuntime();
    }

    public boolean hasAnyOverride() {
        return !torpedoDeltas.isEmpty() || !coreDeltas.isEmpty();
    }

    // ==================== 校验 ====================

    /**
     * 钳制鱼雷航速偏移。
     *
     * <p>WHY 在数据层再钳一次：网络包 handler 已经钳过一次，但 NBT 加载路径
     * （有人手改 .dat，或旧版本存档里的越界值）不经过 handler，这里是最后一道闸。
     */
    public static float clampTorpedoDelta(float delta) {
        if (!Float.isFinite(delta)) {
            return 0f;
        }
        return Math.max(TerminalOverrides.TORPEDO_DELTA_MIN,
                Math.min(TerminalOverrides.TORPEDO_DELTA_MAX, delta));
    }

    public static double clampCoreDelta(double delta) {
        if (!Double.isFinite(delta)) {
            return 0d;
        }
        return Math.max(TerminalOverrides.CORE_SPEED_MIN,
                Math.min(TerminalOverrides.CORE_SPEED_MAX, delta));
    }

    /**
     * 校验核心覆盖键是否合法：必须是 {@code ShipType} 枚举常量名
     * （{@code SMALL} / {@code MEDIUM} / {@code LARGE} / {@code SUBMARINE}）。
     *
     * <p>WHY 需要白名单：读取方 {@code TransformationManager} 只用
     * {@code activeType.name()} 查表，任何非法键都永远匹配不到，却会永久写进存档
     * （每次读覆盖白查一次 Map）。鱼雷侧已在 {@code normalizeTorpedoKey} 入口挡掉，
     * 核心侧此前只判空串，构成设计不对称。
     *
     * @return 归一化后的枚举名；不是合法舰型时返回 null
     */
    public static String normalizeCoreKey(String rawKey) {
        if (rawKey == null || rawKey.isEmpty()) {
            return null;
        }
        for (com.piranport.item.ShipType type : com.piranport.item.ShipType.values()) {
            if (type.name().equals(rawKey)) {
                return type.name();
            }
        }
        return null;
    }

    /**
     * 把当前覆盖发布到 {@link TerminalOverrides} 运行时镜像。
     *
     * <p>WHY 在这里统一发布而不是让调用方各发一次：写入的三个入口
     * （C2S 包 handler、NBT 加载、重置）都会走到本类的 setter，
     * 把它挂在 setter 末尾就不会漏掉任何一条路径。
     */
    private void publishToRuntime() {
        TerminalOverrides.apply(torpedoDeltas, coreDeltas);
    }

    // ==================== NBT 序列化 ====================

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        CompoundTag torpedoesTag = new CompoundTag();
        for (Map.Entry<String, Float> entry : torpedoDeltas.entrySet()) {
            torpedoesTag.putFloat(entry.getKey(), entry.getValue());
        }
        tag.put(KEY_TORPEDOES, torpedoesTag);

        CompoundTag coresTag = new CompoundTag();
        for (Map.Entry<String, Double> entry : coreDeltas.entrySet()) {
            coresTag.putDouble(entry.getKey(), entry.getValue());
        }
        tag.put(KEY_CORES, coresTag);

        return tag;
    }

    public static TerminalOverridesSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TerminalOverridesSavedData data = new TerminalOverridesSavedData();

        // 加载鱼雷覆盖：类型不符的条目直接跳过，避免手改的 .dat 绕过运行时校验
        if (tag.contains(KEY_TORPEDOES, Tag.TAG_COMPOUND)) {
            CompoundTag torpedoesTag = tag.getCompound(KEY_TORPEDOES);
            for (String modelKey : torpedoesTag.getAllKeys()) {
                if (torpedoesTag.getTagType(modelKey) != Tag.TAG_FLOAT) {
                    PiranPort.LOGGER.warn("Skipping non-float torpedo override: {}", modelKey);
                    continue;
                }
                float clamped = clampTorpedoDelta(torpedoesTag.getFloat(modelKey));
                if (clamped != 0f) {
                    data.torpedoDeltas.put(modelKey, clamped);
                }
            }
        } else if (tag.contains(KEY_TORPEDOES)) {
            PiranPort.LOGGER.warn("Skipping malformed torpedo override root");
        }

        if (tag.contains(KEY_CORES, Tag.TAG_COMPOUND)) {
            CompoundTag coresTag = tag.getCompound(KEY_CORES);
            for (String rawKey : coresTag.getAllKeys()) {
                byte type = coresTag.getTagType(rawKey);
                // 兼容 putFloat 写出来的旧数据：double 是主格式，float 也接受
                double raw;
                if (type == Tag.TAG_DOUBLE) {
                    raw = coresTag.getDouble(rawKey);
                } else if (type == Tag.TAG_FLOAT) {
                    raw = coresTag.getFloat(rawKey);
                } else {
                    PiranPort.LOGGER.warn("Skipping non-numeric core override: {}", rawKey);
                    continue;
                }
                // 手改的 .dat 可能塞进任意键名，与网络入口同样过一道 ShipType 白名单，
                // 否则垃圾键会永远留在内存里且每次读覆盖白查一次。
                String coreKey = normalizeCoreKey(rawKey);
                if (coreKey == null) {
                    PiranPort.LOGGER.warn("Skipping unknown ship type override: {}", rawKey);
                    continue;
                }
                double clamped = clampCoreDelta(raw);
                if (clamped != 0d) {
                    data.coreDeltas.put(coreKey, clamped);
                }
            }
        } else if (tag.contains(KEY_CORES)) {
            PiranPort.LOGGER.warn("Skipping malformed core override root");
        }

        // 存档加载完毕后立即发布，保证「重进存档覆盖仍在」——此时还没有玩家在线，
        // 读到的第一批实体/物品就已经是覆盖后的值。
        data.publishToRuntime();
        return data;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 获取或创建 SavedData 实例。
     *
     * <p>WHY 形参是 {@link ServerLevel} 却锚到主世界：本域是存档级数据，
     * 但 {@code level.getDataStorage()} 返回的是该维度<b>私有</b>的 data 目录
     * （MC 的 {@code ServerChunkCache} 按 {@code level.dimension()} 分目录），
     * 直接用它会让主世界/下界/末地/副本各持一份互不可见的覆盖。
     * 调用方手上通常只有玩家所在维度，故此处统一改道
     * {@code level.getServer().overworld()}，调用方无需关心。
     *
     * @param level 服务端世界（任意维度都可，内部一律折算到主世界）
     */
    public static TerminalOverridesSavedData get(ServerLevel level) {
        // 用 getServer() 而非 getServerLevel(OVERWORLD)：单机集成服务端下同样成立，
        // 且不依赖 ClientLevel 侧的 ClientLevelData 实现。
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        TerminalOverridesSavedData::new,
                        TerminalOverridesSavedData::load,
                        null
                ),
                DATA_NAME
        );
    }
}
