package com.piranport.terminal;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftStatsService;
import com.piranport.combat.BallisticSolver;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/** 存档级终端覆盖；始终存于主世界，批量导入只在全部校验成功后调用 replace。 */
public final class TerminalParametersSavedData extends SavedData {
    private static final String DATA_NAME = "piranport_parameters";
    private final Map<String, String> overrides = new HashMap<>();
    private long revision;

    public Map<String, String> overrides() { return Map.copyOf(overrides); }
    public long revision() { return revision; }

    public boolean put(String key, String raw) {
        Map<String, String> proposed = new HashMap<>(overrides);
        proposed.put(key, raw);
        return replace(proposed);
    }

    public boolean remove(String key) {
        if (!overrides.containsKey(key)) return false;
        Map<String, String> proposed = new HashMap<>(overrides);
        proposed.remove(key);
        return replace(proposed);
    }

    public boolean clearAll() {
        if (overrides.isEmpty()) return false;
        overrides.clear();
        changed();
        return true;
    }

    /** Removes only the overrides belonging to one terminal object. */
    public boolean clearTarget(String group, String target) {
        Map<String, String> proposed = new HashMap<>(overrides);
        Map<String, TerminalParameterSpec> specs = new HashMap<>();
        TerminalParameterCatalog.all().forEach(spec -> specs.put(spec.key(), spec));
        proposed.keySet().removeIf(key -> {
            TerminalParameterSpec spec = specs.get(key);
            return spec != null && spec.group().equals(group) && spec.target().equals(target);
        });
        return replace(proposed);
    }

    public boolean replace(Map<String, String> proposed) {
        // 先整批验证；这里也阻断绕过网络/CSV 调用本方法的非法值。
        Map<String, String> checked = TerminalParameterValidation.checked(proposed, TerminalParameterCatalog.all());
        if (overrides.equals(checked)) return false;
        overrides.clear();
        overrides.putAll(checked);
        changed();
        return true;
    }

    public void refreshCatalog() {
        var metadata = TerminalParameterCatalog.all();
        Map<String, TerminalParameterSpec> specs = new HashMap<>();
        metadata.forEach(spec -> specs.put(spec.key(), spec));
        Map<String, String> valid = new HashMap<>();
        for (var entry : overrides.entrySet()) {
            TerminalParameterSpec spec = specs.get(entry.getKey());
            if (spec == null) continue;
            try {
                String value = spec.canonical(entry.getValue());
                if (!value.equals(spec.canonical(spec.baseValue()))) valid.put(entry.getKey(), value);
            } catch (RuntimeException e) {
                PiranPort.LOGGER.warn("Skipping invalid reloaded terminal override {}: {}", entry.getKey(), e.getMessage());
            }
        }
        if (!overrides.equals(valid)) {
            overrides.clear();
            overrides.putAll(valid);
            changed();
        } else {
            TerminalParameters.apply(overrides, revision);
        }
    }

    private void changed() {
        revision++;
        setDirty();
        TerminalParameters.apply(overrides, revision);
        AircraftStatsService.clear();
        BallisticSolver.clearCache();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        CompoundTag values = new CompoundTag();
        overrides.forEach(values::putString);
        tag.put("values", values);
        tag.putLong("revision", revision);
        return tag;
    }

    public static TerminalParametersSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        TerminalParametersSavedData data = new TerminalParametersSavedData();
        Map<String, String> candidates = new HashMap<>();
        if (tag.contains("values", Tag.TAG_COMPOUND)) {
            CompoundTag values = tag.getCompound("values");
            for (String key : values.getAllKeys()) {
                if (values.getTagType(key) != Tag.TAG_STRING) continue;
                TerminalParameterSpec spec = TerminalParameterCatalog.find(key);
                if (spec != null) {
                    try {
                        String value = spec.canonical(values.getString(key));
                        if (!value.equals(spec.canonical(spec.baseValue()))) candidates.put(key, value);
                    } catch (RuntimeException e) {
                        PiranPort.LOGGER.warn("Skipping invalid terminal parameter {}: {}", key, e.getMessage());
                    }
                }
            }
        }
        try {
            data.overrides.putAll(TerminalParameterValidation.checked(candidates, TerminalParameterCatalog.all()));
        } catch (RuntimeException e) {
            // 成对约束失败时不激活半批旧值；原始文件仍保留供人工修复。
            PiranPort.LOGGER.warn("Ignoring invalid coupled terminal overrides: {}", e.getMessage());
        }
        data.revision = Math.max(0L, tag.getLong("revision"));
        return data;
    }

    public static TerminalParametersSavedData get(ServerLevel level) {
        TerminalParametersSavedData data = level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TerminalParametersSavedData::new, TerminalParametersSavedData::load, null),
                DATA_NAME);
        TerminalParameters.apply(data.overrides, data.revision);
        return data;
    }

}
