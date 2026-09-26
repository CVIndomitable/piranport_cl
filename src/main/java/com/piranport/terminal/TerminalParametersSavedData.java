package com.piranport.terminal;

import com.piranport.PiranPort;
import com.piranport.artillery.config.override.ArtilleryConfigOverrideSavedData;
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
    private boolean migratedLegacy;

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
        // 新基准可能使旧仰角对倒置，只舍弃相关旧覆盖，不连带删除其他有效设置。
        for (TerminalParameterSpec spec : metadata) {
            if (!"cannon".equals(spec.group()) || !"min_elevation".equals(spec.property())) continue;
            TerminalParameterSpec max = specs.get("cannon." + spec.target() + ".max_elevation");
            if (max == null) continue;
            double minimum = Double.parseDouble(valid.getOrDefault(spec.key(), spec.baseValue()));
            double maximum = Double.parseDouble(valid.getOrDefault(max.key(), max.baseValue()));
            if (minimum >= maximum) {
                valid.remove(spec.key());
                valid.remove(max.key());
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
        tag.putBoolean("migratedLegacy", migratedLegacy);
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
        data.migratedLegacy = tag.getBoolean("migratedLegacy");
        return data;
    }

    public static TerminalParametersSavedData get(ServerLevel level) {
        TerminalParametersSavedData data = level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(TerminalParametersSavedData::new, TerminalParametersSavedData::load, null),
                DATA_NAME);
        if (!data.migratedLegacy && !TerminalParameterCatalog.all().isEmpty()) {
            data.migrateLegacy(level);
        }
        TerminalParameters.apply(data.overrides, data.revision);
        return data;
    }

    private void migrateLegacy(ServerLevel level) {
        Map<String, String> previous = new HashMap<>(overrides);
        // 旧火炮工具按维度保存；合并各维度的数据，以主世界同键优先。
        migrateArtillery(level.getServer().overworld());
        for (ServerLevel sourceLevel : level.getServer().getAllLevels()) {
            if (sourceLevel != level.getServer().overworld()) migrateArtillery(sourceLevel);
        }
        TerminalOverridesSavedData oldSpeeds = TerminalOverridesSavedData.get(level);
        oldSpeeds.getAllTorpedoDeltas().forEach((key, delta) ->
                migrateDelta("torpedo." + key + ".speed", delta));
        oldSpeeds.getAllCoreDeltas().forEach((key, delta) -> {
            for (String property : new String[] { "speed_bonus", "speed_multiplier", "speed" }) {
                if (TerminalParameterCatalog.find("core." + key + "." + property) != null) {
                    migrateDelta("core." + key + "." + property, delta);
                    break;
                }
            }
        });
        try {
            TerminalParameterValidation.checked(overrides, TerminalParameterCatalog.all());
        } catch (RuntimeException e) {
            overrides.clear();
            overrides.putAll(previous);
            PiranPort.LOGGER.warn("Ignoring invalid coupled legacy terminal overrides: {}", e.getMessage());
        }
        migratedLegacy = true;
        revision++;
        setDirty();
        PiranPort.LOGGER.info("Migrated {} legacy terminal parameter overrides", overrides.size());
    }

    private void migrateArtillery(ServerLevel sourceLevel) {
        ArtilleryConfigOverrideSavedData old = ArtilleryConfigOverrideSavedData.get(sourceLevel);
        old.getAllCannonOverrides().forEach((name, fields) -> fields.forEach((field, value) ->
                migrate("cannon." + name + "." + snakeCase(field), value)));
        old.getAllProjectileOverrides().forEach((key, value) ->
                migrate("global.projectiles." + key.toLowerCase(java.util.Locale.ROOT), value));
    }

    private void migrateDelta(String key, double delta) {
        TerminalParameterSpec spec = TerminalParameterCatalog.find(key);
        if (spec == null) return;
        try {
            migrate(key, Double.parseDouble(spec.baseValue()) + delta);
        } catch (NumberFormatException ignored) {
            // 非数值项没有速度偏移语义。
        }
    }

    private void migrate(String key, Object value) {
        if (overrides.containsKey(key) || value == null) return;
        TerminalParameterSpec spec = TerminalParameterCatalog.find(key);
        if (spec == null) return;
        try {
            String raw = value.toString();
            // 旧配置以浮点数存储整型参数，只有精确整数才可迁移。
            if (spec.type() == TerminalParameterSpec.ValueType.INTEGER && value instanceof Number number) {
                double numeric = number.doubleValue();
                if (Double.isFinite(numeric) && numeric == Math.rint(numeric)
                        && numeric >= Integer.MIN_VALUE && numeric <= Integer.MAX_VALUE) {
                    raw = Integer.toString((int) numeric);
                }
            }
            String canonical = spec.canonical(raw);
            if (!canonical.equals(spec.canonical(spec.baseValue()))) overrides.put(key, canonical);
        } catch (RuntimeException e) {
            PiranPort.LOGGER.warn("Skipping legacy override {}: {}", key, e.getMessage());
        }
    }

    private static String snakeCase(String field) {
        return field.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(java.util.Locale.ROOT);
    }

}
