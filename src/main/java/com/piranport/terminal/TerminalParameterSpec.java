package com.piranport.terminal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Locale;

/** 调试终端单项参数的稳定描述与服务端校验边界。 */
public record TerminalParameterSpec(String key, String group, String target, String property,
                                    ValueType type, String baseValue, double min, double max) {
    public enum ValueType { DOUBLE, INTEGER, BOOLEAN }

    public TerminalParameterSpec {
        if (key == null || key.isBlank() || key.length() > 128 || group == null || group.length() > 128
                || target == null || target.length() > 128 || property == null || property.length() > 128
                || type == null || baseValue == null || baseValue.length() > 64) {
            throw new IllegalArgumentException("Invalid terminal parameter metadata");
        }
        if (!Double.isFinite(min) || !Double.isFinite(max) || min > max) {
            throw new IllegalArgumentException("Invalid terminal parameter range: " + key);
        }
        // 创建目录时即检查默认值，避免非法目录使 UI、存档与网络的校验标准分叉。
        parse(type, baseValue, min, max, key);
    }

    public String canonical(String raw) {
        if (raw == null || raw.length() > 64) {
            throw new IllegalArgumentException("Invalid value length for " + key);
        }
        return parse(type, raw, min, max, key);
    }

    /**
     * The simulation stores movement speeds per game tick, while the terminal
     * deliberately exposes every linear speed in blocks per second.
     */
    public boolean isLinearSpeed() {
        return switch (property) {
            case "speed", "panel_speed", "initial_speed", "full_load_speed", "empty_speed",
                    "movement_speed" -> true;
            default -> false;
        };
    }

    /** Tick based durations are shown and edited in seconds in the debug terminal. */
    public boolean isTickDuration() {
        return "reload_time".equals(property) || "fire_cooldown".equals(property)
                || "salvo_interval".equals(property) || property.endsWith("_cooldown");
    }

    public double displayScale() {
        if (isLinearSpeed()) return 20.0;
        return isTickDuration() ? 0.05 : 1.0;
    }

    /** Convert a stored (per tick) value to the value shown in the terminal. */
    public String displayValue(String raw) {
        if (isTickDuration() && type == ValueType.INTEGER) {
            try {
                return String.format(Locale.ROOT, "%.2f", Integer.parseInt(raw.trim()) * displayScale());
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        if (type != ValueType.DOUBLE) return raw;
        try {
            double parsed = Double.parseDouble(raw);
            double value = parsed * displayScale();
            // drag_coeff 保留科学计数法；其余全部走 tidyDecimal 去二进制尾数。
            // 原先 isLinearSpeed 用 %.6f——%.6f 写死 6 位小数，既会产生 4.600000
            // 这种冗余尾零，也不会消除 float→double 加宽带来的二进制尾数
            //（如 0.23f ×20 = 4.600000083355955 → %.6f 显示 4.600000 看起来正常，
            // 但 raw 里的 0.23000000416819775 一旦绕过缩放路径直接显示就成了"一堆 9"）。
            if ("drag_coeff".equals(property)) return String.format(Locale.ROOT, "%.6e", value);
            return tidyDecimal(value);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    /**
     * 把 double 显示串整理成人类可读形式，抹掉二进制尾数噪声。
     * <p>
     * WHY：不少参数底层是 {@code float}（如火炮垂直散布 0.05f），加宽成 double 后
     * 会带出 {@code 0.05000000074505806} 这类尾巴。这里按 6 位有效数字取整并去尾零，
     * 既得到 {@code 0.05}，又保证 {@code 12000} / {@code 100} / {@code 1.15} 这类
     * 有效位数足够的真值原样保留；输出恒为 toPlainString（非科学计数法），可直接
     * 被 {@link Double#parseDouble} 回填到编辑框。
     */
    private static String tidyDecimal(double value) {
        BigDecimal exact = BigDecimal.valueOf(value);
        BigDecimal rounded = exact.round(new MathContext(6)).stripTrailingZeros();
        // 取整本来就无损：真值有效位数 ≤ 6，直接用（12000 → 12000）。
        if (rounded.doubleValue() == value) return rounded.toPlainString();
        // 原值恰是 float 加宽而来 = 纯二进制噪声，改用 float 的最短十进制表示，
        // 避免 6 位取整把 float 本身的高 7 位真值（如 123456.78f）截坏。
        if (value != 0.0 && (double) (float) value == value) {
            return new BigDecimal(Float.toString((float) value)).stripTrailingZeros().toPlainString();
        }
        // 真·高精度 double：宁可保留原串，也不猜着截坏它。
        return exact.toPlainString();
    }

    /** Convert a terminal value back to the canonical stored value. */
    public String canonicalDisplay(String raw) {
        if (isTickDuration() && type == ValueType.INTEGER) {
            try {
                double seconds = Double.parseDouble(raw.trim());
                if (!Double.isFinite(seconds)) throw new NumberFormatException();
                return canonical(Integer.toString((int) Math.round(seconds / displayScale())));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Expected seconds for " + key, exception);
            }
        }
        if (!isLinearSpeed() || type != ValueType.DOUBLE) return canonical(raw);
        try {
            return canonical(Double.toString(Double.parseDouble(raw.trim()) / displayScale()));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected number for " + key, exception);
        }
    }

    public double displayMin() { return min * displayScale(); }
    public double displayMax() { return max * displayScale(); }

    /** 解析成功后的读取侧钳制：把任意 double 钳到本 spec 的 [min, max]。 */
    public double clamp(double value) {
        return Math.min(max, Math.max(min, value));
    }

    /** 解析成功后的读取侧钳制：把任意 int 钳到本 spec 的 [min, max]。 */
    public int clamp(int value) {
        return (int) Math.min((long) max, Math.max((long) min, value));
    }

    private static String parse(ValueType type, String raw, double min, double max, String key) {
        String value = raw.trim();
        return switch (type) {
            case BOOLEAN -> {
                if (!"true".equals(value) && !"false".equals(value)) {
                    throw new IllegalArgumentException("Expected boolean for " + key);
                }
                yield value;
            }
            case INTEGER -> {
                int parsed = Integer.parseInt(value);
                if (parsed < min || parsed > max) {
                    throw new IllegalArgumentException("Out of range: " + key);
                }
                yield Integer.toString(parsed);
            }
            case DOUBLE -> {
                double parsed = Double.parseDouble(value);
                if (!Double.isFinite(parsed) || parsed < min || parsed > max) {
                    throw new IllegalArgumentException("Out of range: " + key);
                }
                yield String.format(Locale.ROOT, "%s", parsed);
            }
        };
    }
}
