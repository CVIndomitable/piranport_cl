package com.piranport.artillery.config.override;

import com.piranport.PiranPort;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 存档级别的火炮和弹药配置覆盖数据
 *
 * <p>存储位置: world/data/piranport_artillery_overrides.dat
 * <p>用途: 创造模式配置工具的数值覆盖存储
 * <p>线程模型: 服务端主线程
 */
public class ArtilleryConfigOverrideSavedData extends SavedData {
    private static final String DATA_NAME = "piranport_artillery_overrides";

    // 火炮覆盖: cannonName → (fieldName → value)
    private final Map<String, Map<String, Object>> cannonOverrides = new HashMap<>();

    // 弹药配置覆盖: configKey → value
    private final Map<String, Object> projectileOverrides = new HashMap<>();

    public ArtilleryConfigOverrideSavedData() {
    }

    // ==================== 火炮覆盖方法 ====================

    /**
     * 设置火炮字段覆盖
     * @param cannonName 火炮注册ID（如 "medium_gun"）
     * @param field 字段名（如 "damage", "reloadTime"）
     * @param value 覆盖值
     */
    public void setCannonOverride(String cannonName, String field, Object value) {
        cannonOverrides.computeIfAbsent(cannonName, k -> new HashMap<>()).put(field, value);
        setDirty();
    }

    /**
     * 获取火炮字段覆盖
     * @param cannonName 火炮注册ID
     * @param field 字段名
     * @return 覆盖值（如果存在）
     */
    public Optional<Object> getCannonOverride(String cannonName, String field) {
        Map<String, Object> fieldMap = cannonOverrides.get(cannonName);
        if (fieldMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(fieldMap.get(field));
    }

    /**
     * 移除火炮字段覆盖
     */
    public void removeCannonOverride(String cannonName, String field) {
        Map<String, Object> fieldMap = cannonOverrides.get(cannonName);
        if (fieldMap != null) {
            fieldMap.remove(field);
            if (fieldMap.isEmpty()) {
                cannonOverrides.remove(cannonName);
            }
            setDirty();
        }
    }

    /**
     * 移除火炮的所有覆盖
     */
    public void removeAllCannonOverrides(String cannonName) {
        if (cannonOverrides.remove(cannonName) != null) {
            setDirty();
        }
    }

    /**
     * 获取所有火炮覆盖（用于GUI显示和CSV导出）
     */
    public Map<String, Map<String, Object>> getAllCannonOverrides() {
        return new HashMap<>(cannonOverrides);
    }

    /**
     * 获取有覆盖的火炮名称集合
     */
    public Set<String> getOverriddenCannonNames() {
        return cannonOverrides.keySet();
    }

    // ==================== 弹药配置覆盖方法 ====================

    /**
     * 设置弹药配置覆盖
     * @param configKey 配置键（如 "HE_ARMOR_PENETRATION"）
     * @param value 覆盖值
     */
    public void setProjectileOverride(String configKey, Object value) {
        projectileOverrides.put(configKey, value);
        setDirty();
    }

    /**
     * 获取弹药配置覆盖
     */
    public Optional<Object> getProjectileOverride(String configKey) {
        return Optional.ofNullable(projectileOverrides.get(configKey));
    }

    /**
     * 移除弹药配置覆盖
     */
    public void removeProjectileOverride(String configKey) {
        if (projectileOverrides.remove(configKey) != null) {
            setDirty();
        }
    }

    /**
     * 获取所有弹药配置覆盖
     */
    public Map<String, Object> getAllProjectileOverrides() {
        return new HashMap<>(projectileOverrides);
    }

    // ==================== 批量操作 ====================

    /**
     * 清除所有覆盖（重置功能）
     */
    public void clearAllOverrides() {
        boolean changed = false;
        if (!cannonOverrides.isEmpty()) {
            cannonOverrides.clear();
            changed = true;
        }
        if (!projectileOverrides.isEmpty()) {
            projectileOverrides.clear();
            changed = true;
        }
        if (changed) {
            setDirty();
        }
    }

    /**
     * 检查是否有任何覆盖
     */
    public boolean hasAnyOverrides() {
        return !cannonOverrides.isEmpty() || !projectileOverrides.isEmpty();
    }

    // ==================== NBT序列化 ====================

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        // 保存火炮覆盖
        CompoundTag cannonsTag = new CompoundTag();
        for (Map.Entry<String, Map<String, Object>> entry : cannonOverrides.entrySet()) {
            String cannonName = entry.getKey();
            Map<String, Object> fields = entry.getValue();

            CompoundTag fieldsTag = new CompoundTag();
            for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
                String fieldName = fieldEntry.getKey();
                Object value = fieldEntry.getValue();
                saveValue(fieldsTag, fieldName, value);
            }

            cannonsTag.put(cannonName, fieldsTag);
        }
        tag.put("cannons", cannonsTag);

        // 保存弹药配置覆盖
        CompoundTag projectilesTag = new CompoundTag();
        for (Map.Entry<String, Object> entry : projectileOverrides.entrySet()) {
            saveValue(projectilesTag, entry.getKey(), entry.getValue());
        }
        tag.put("projectiles", projectilesTag);

        return tag;
    }

    /**
     * 保存单个值到NBT（根据类型）
     */
    private void saveValue(CompoundTag tag, String key, Object value) {
        if (value instanceof Float f) {
            tag.putFloat(key, f);
        } else if (value instanceof Double d) {
            tag.putDouble(key, d);
        } else if (value instanceof Integer i) {
            tag.putInt(key, i);
        } else if (value instanceof Boolean b) {
            tag.putBoolean(key, b);
        } else if (value instanceof String s) {
            tag.putString(key, s);
        } else {
            PiranPort.LOGGER.warn("Unsupported value type for key {}: {}", key, value.getClass());
        }
    }

    /**
     * 从NBT加载数据
     */
    public static ArtilleryConfigOverrideSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ArtilleryConfigOverrideSavedData data = new ArtilleryConfigOverrideSavedData();

        // 加载火炮覆盖
        if (tag.contains("cannons")) {
            CompoundTag cannonsTag = tag.getCompound("cannons");
            for (String cannonName : cannonsTag.getAllKeys()) {
                CompoundTag fieldsTag = cannonsTag.getCompound(cannonName);
                Map<String, Object> fields = new HashMap<>();

                for (String fieldName : fieldsTag.getAllKeys()) {
                    Object value = loadValue(fieldsTag, fieldName);
                    if (value != null) {
                        fields.put(fieldName, value);
                    }
                }

                if (!fields.isEmpty()) {
                    data.cannonOverrides.put(cannonName, fields);
                }
            }
        }

        // 加载弹药配置覆盖
        if (tag.contains("projectiles")) {
            CompoundTag projectilesTag = tag.getCompound("projectiles");
            for (String configKey : projectilesTag.getAllKeys()) {
                Object value = loadValue(projectilesTag, configKey);
                if (value != null) {
                    data.projectileOverrides.put(configKey, value);
                }
            }
        }

        return data;
    }

    /**
     * 从NBT加载单个值（自动检测类型）
     */
    private static Object loadValue(CompoundTag tag, String key) {
        byte type = tag.getTagType(key);
        return switch (type) {
            case net.minecraft.nbt.Tag.TAG_FLOAT -> tag.getFloat(key);
            case net.minecraft.nbt.Tag.TAG_DOUBLE -> tag.getDouble(key);
            case net.minecraft.nbt.Tag.TAG_INT -> tag.getInt(key);
            case net.minecraft.nbt.Tag.TAG_BYTE -> tag.getBoolean(key);
            case net.minecraft.nbt.Tag.TAG_STRING -> tag.getString(key);
            default -> {
                PiranPort.LOGGER.warn("Unknown NBT type {} for key {}", type, key);
                yield null;
            }
        };
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 获取或创建SavedData实例
     * @param level 服务端世界（通常使用主世界）
     */
    public static ArtilleryConfigOverrideSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(
                ArtilleryConfigOverrideSavedData::new,
                ArtilleryConfigOverrideSavedData::load,
                null
            ),
            DATA_NAME
        );
    }
}
