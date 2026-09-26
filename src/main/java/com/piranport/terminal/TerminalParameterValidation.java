package com.piranport.terminal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 一次目录快照验证整个事务，避免逐键重建数据包目录。 */
public final class TerminalParameterValidation {
    private TerminalParameterValidation() { }

    public static Map<String, String> checked(Map<String, String> proposed,
                                                Collection<TerminalParameterSpec> metadata) {
        Map<String, TerminalParameterSpec> specs = new HashMap<>();
        for (TerminalParameterSpec spec : metadata) specs.put(spec.key(), spec);
        if (proposed.size() > 4096) throw new IllegalArgumentException("参数数量超出上限");
        Map<String, String> checked = new HashMap<>();
        for (var entry : proposed.entrySet()) {
            TerminalParameterSpec spec = specs.get(entry.getKey());
            if (spec == null) throw new IllegalArgumentException("未知参数");
            String value = spec.canonical(entry.getValue());
            if (!value.equals(spec.canonical(spec.baseValue()))) checked.put(spec.key(), value);
        }
        for (TerminalParameterSpec spec : metadata) {
            if (!"cannon".equals(spec.group()) || !"min_elevation".equals(spec.property())) continue;
            TerminalParameterSpec maximum = specs.get("cannon." + spec.target() + ".max_elevation");
            if (maximum == null) continue;
            double min = Double.parseDouble(checked.getOrDefault(spec.key(), spec.baseValue()));
            double max = Double.parseDouble(checked.getOrDefault(maximum.key(), maximum.baseValue()));
            if (min >= max) throw new IllegalArgumentException("火炮最小仰角必须小于最大仰角");
        }
        return checked;
    }
}
