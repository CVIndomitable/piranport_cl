package com.piranport.testsupport;

import com.piranport.registry.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** 独立测试 JVM 没有 FML 注册事件；按模组注册顺序执行 supplier，绑定真实方块与物品。 */
public final class ModRegistryFixture {
    private ModRegistryFixture() {}
    private static boolean ready;

    public static void bootstrap() {
        if (ready) return;
        SharedConstants.tryDetectVersion();
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        bindAll(ModDataComponents.DATA_COMPONENTS);
        bindAll(ModBlocks.BLOCKS);
        bindAll(ModBlockEntityTypes.BLOCK_ENTITY_TYPES);
        bindAll(ModItems.ITEMS);
        bindAll(ModMenuTypes.MENU_TYPES);
        ready = true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindAll(DeferredRegister<?> deferred) {
        Registry registry = BuiltInRegistries.REGISTRY.get(deferred.getRegistryKey().location());
        if (registry instanceof MappedRegistry<?> mapped) mapped.unfreeze();
        try {
            var field = DeferredRegister.class.getDeclaredField("entries");
            field.setAccessible(true);
            var entries = (Map<DeferredHolder<?, ?>, Supplier<?>>) field.get(deferred);
            entries.forEach((holder, supplier) -> {
                if (!registry.containsKey(holder.getId())) Registry.register(registry, holder.getId(), supplier.get());
            });
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取测试注册项", e);
        }
    }
}
