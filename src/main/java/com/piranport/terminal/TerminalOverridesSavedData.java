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
 * <p>存储位置: world/data/piranport_terminal_overrides.dat
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
     * @param coreKey 舰型名（如 {@code LARGE}）
     * @param delta   偏移量，将被钳制到合法区间
     */
    public void setCoreSpeedDelta(String coreKey, double delta) {
        if (coreKey == null || coreKey.isEmpty()) {
            return;
        }
        double clamped = clampCoreDelta(delta);
        if (clamped == 0d) {
            if (coreDeltas.remove(coreKey) != null) {
                setDirty();
            }
        } else {
            coreDeltas.put(coreKey, clamped);
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
            for (String coreKey : coresTag.getAllKeys()) {
                byte type = coresTag.getTagType(coreKey);
                // 兼容 putFloat 写出来的旧数据：double 是主格式，float 也接受
                double raw;
                if (type == Tag.TAG_DOUBLE) {
                    raw = coresTag.getDouble(coreKey);
                } else if (type == Tag.TAG_FLOAT) {
                    raw = coresTag.getFloat(coreKey);
                } else {
                    PiranPort.LOGGER.warn("Skipping non-numeric core override: {}", coreKey);
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
     * @param level 服务端世界（通常使用主世界 —— 存档级数据的锚点）
     */
    public static TerminalOverridesSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        TerminalOverridesSavedData::new,
                        TerminalOverridesSavedData::load,
                        null
                ),
                DATA_NAME
        );
    }
}
