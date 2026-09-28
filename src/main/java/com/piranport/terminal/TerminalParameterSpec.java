package com.piranport.terminal;

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
            double value = Double.parseDouble(raw) * displayScale();
            return "drag_coeff".equals(property)
                    ? String.format(Locale.ROOT, "%.6e", value)
                    : isLinearSpeed() ? String.format(Locale.ROOT, "%.6f", value) : raw;
        } catch (NumberFormatException ignored) {
            return raw;
        }
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
