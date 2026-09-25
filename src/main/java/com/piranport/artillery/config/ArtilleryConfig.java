package com.piranport.artillery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.piranport.PiranPort;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.HashSet;

/** 加载 data/piranport/artillery/cannons/ 下的火炮 JSON 配置 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ArtilleryConfig extends SimplePreparableReloadListener<Map<String, ArtilleryCannonData>> {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String PATH_PREFIX = "data/piranport/artillery/cannons/";

    private static final Map<String, ArtilleryCannonData> CANNON_DATA = new HashMap<>();
    private static final Map<String, CannonDefinition> DEFINITIONS = new HashMap<>();
    private static final Set<String> MISSING_WARNINGS = new HashSet<>();

    @Override
    protected Map<String, ArtilleryCannonData> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ArtilleryCannonData> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> entry : manager.listResources("artillery/cannons",
                loc -> loc.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation loc = entry.getKey();
            String name = loc.getPath().replaceFirst(".*artillery/cannons/", "").replace(".json", "");
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                ArtilleryCannonData data = GSON.fromJson(reader, ArtilleryCannonData.class);
                if (data != null) {
                    CannonDefinition definition = new CannonDefinition(loc, data);
                    if (!definition.isValid()) {
                        PiranPort.LOGGER.error("Invalid artillery definition {}: {}", loc,
                                String.join("; ", definition.validationErrors()));
                        continue;
                    }
                    result.put(name, data);
                }
            } catch (Exception e) {
                PiranPort.LOGGER.warn("Failed to load artillery config: {}", loc, e);
            }
        }
        return result;
    }

    @Override
    protected void apply(Map<String, ArtilleryCannonData> data, ResourceManager manager, ProfilerFiller profiler) {
        CANNON_DATA.clear();
        CANNON_DATA.putAll(data);
        DEFINITIONS.clear();
        data.forEach((name, cannon) -> {
            // 定义 ID 与物品/覆盖使用的 cannonName 对齐；JSON 所在目录是资源路径，不是逻辑 ID 的一部分。
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, name);
            DEFINITIONS.put(name, new CannonDefinition(id, cannon));
        });
        MISSING_WARNINGS.clear();
        PiranPort.LOGGER.info("Loaded {} artillery cannon config(s)", data.size());
    }

    public static ArtilleryCannonData get(String name) {
        ArtilleryCannonData data = CANNON_DATA.get(name);
        if (data != null) return data;
        if (MISSING_WARNINGS.add(name)) {
            PiranPort.LOGGER.error("Missing artillery cannon definition '{}'; using migration fallback", name);
        }
        return ArtilleryCannonData.DEFAULT;
    }

    /** 查询规范定义；新代码应优先使用此入口而不是把配置名与数据拆开传递。 */
    public static Optional<CannonDefinition> findDefinition(String name) {
        return Optional.ofNullable(DEFINITIONS.get(name));
    }

    /** 缺失时返回空，供启动检查和兼容层区分“缺配置”和“合法默认值”。 */
    public static Optional<ArtilleryCannonData> find(String name) {
        return Optional.ofNullable(CANNON_DATA.get(name));
    }

    /**
     * 获取所有已加载的火炮名称（用于CSV导出等）
     */
    public static Set<String> getAllCannonNames() {
        return Collections.unmodifiableSet(CANNON_DATA.keySet());
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ArtilleryConfig());
    }
}
