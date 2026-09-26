package com.piranport.config;

import com.piranport.terminal.TerminalParameterSpec;
import com.piranport.terminal.TerminalParameters;
import java.util.ArrayList;
import java.util.List;

/** 旧配置调用点的轻量过渡接口；数值只从终端镜像读取。 */
public final class TerminalConfigValue<T> {
    private static final List<TerminalParameterSpec> SPECS = new ArrayList<>();
    private final String key;
    private final T base;

    private TerminalConfigValue(String key, T base) {
        this.key = key;
        this.base = base;
    }

    public static TerminalConfigValue<Integer> integer(String group, String target, String property,
                                                        int base, int min, int max) {
        String key = "global." + target + "." + property;
        SPECS.add(new TerminalParameterSpec(key, group, target, property,
                TerminalParameterSpec.ValueType.INTEGER, Integer.toString(base), min, max));
        return new TerminalConfigValue<>(key, base);
    }

    public static TerminalConfigValue<Double> number(String group, String target, String property,
                                                      double base, double min, double max) {
        String key = "global." + target + "." + property;
        SPECS.add(new TerminalParameterSpec(key, group, target, property,
                TerminalParameterSpec.ValueType.DOUBLE, Double.toString(base), min, max));
        return new TerminalConfigValue<>(key, base);
    }

    public static TerminalConfigValue<Boolean> bool(String group, String target, String property, boolean base) {
        String key = "global." + target + "." + property;
        SPECS.add(new TerminalParameterSpec(key, group, target, property,
                TerminalParameterSpec.ValueType.BOOLEAN, Boolean.toString(base), 0, 1));
        return new TerminalConfigValue<>(key, base);
    }

    @SuppressWarnings("unchecked")
    public T get() {
        if (base instanceof Integer value) return (T) Integer.valueOf(TerminalParameters.getInt(key, value));
        if (base instanceof Double value) return (T) Double.valueOf(TerminalParameters.getDouble(key, value));
        if (base instanceof Boolean value) return (T) Boolean.valueOf(TerminalParameters.getBoolean(key, value));
        throw new IllegalStateException("Unsupported parameter type: " + key);
    }

    public static List<TerminalParameterSpec> specs() {
        return List.copyOf(SPECS);
    }
}
