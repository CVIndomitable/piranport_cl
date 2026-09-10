package com.piranport.npc.deepocean;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 深海 NPC 属性数据加载器。依据：策划决策/架构/03-数据驱动vs硬编码.md
 *
 * <p>路径：{@code data/piranport/npc/deep_ocean/*.json}</p>
 *
 * <p>每条 JSON 形如：</p>
 * <pre>
 * {
 *   "entity_type": "piranport:deep_ocean_destroyer",
 *   "max_health": 30.0,
 *   "movement_speed": 0.35,
 *   "follow_range": 24.0,
 *   "attack_damage": 3.0,
 *   "knockback_resistance": 0.3,
 *   "armor": 4.0,
 *   "shell_damage": 4.0,
 *   "explosion_power": 1.2,
 *   "fire_interval": 100,
 *   "orbit_distance": 12.0,
 *   "can_use_torpedoes": true,
 *   "torpedo_damage": 8.0,
 *   "tracking_interval_min": 3,
 *   "tracking_interval_max": 6,
 *   "can_launch_aircraft": false,
 *   "max_aircraft": 0
 * }
 * </pre>
 *
 * <p>loader 在服务端启动时通过 {@code SimpleJsonResourceReloadListener} 扫描该路径，
 * 将字段写入 {@link #DATA} 静态 Map，由对应 Entity 子类按需查询。
 * 缺失字段或文件不存在时回退到子类原硬编码值。</p>
 */
public class DeepOceanDataLoader extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    public static final String FOLDER = "npc/deep_ocean";

    /** id -> DeepOceanEntityData。运行时全局只读缓存。 */
    public static final Map<ResourceLocation, DeepOceanEntityData> DATA = new HashMap<>();

    public DeepOceanDataLoader() {
        super(GSON, FOLDER);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        DATA.clear();
        for (Map.Entry<ResourceLocation, JsonElement> e : resources.entrySet()) {
            try {
                if (!e.getValue().isJsonObject()) continue;
                DeepOceanEntityData data = DeepOceanEntityData.fromJson(e.getValue().getAsJsonObject());
                DATA.put(e.getKey(), data);
                LOGGER.debug("Loaded deep-ocean NPC data: {}", e.getKey());
            } catch (Exception ex) {
                LOGGER.warn("Failed to load deep-ocean NPC data {}: {}", e.getKey(), ex.getMessage());
            }
        }
        LOGGER.info("DeepOceanDataLoader: {} entries loaded", DATA.size());
    }

    /** 按 id 查询；找不到返回 null（调用方应回退到硬编码默认）。 */
    public static DeepOceanEntityData get(ResourceLocation id) {
        return DATA.get(id);
    }
}