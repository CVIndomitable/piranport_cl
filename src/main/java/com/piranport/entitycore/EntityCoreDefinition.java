package com.piranport.entitycore;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * 实体核心定义。服务端只同步 coreId，客户端按这里的实体类型创建临时渲染替身。
 */
public record EntityCoreDefinition(
        int id,
        String entityPath,
        Supplier<? extends EntityType<? extends Entity>> entityType
) {
    public Component displayName() {
        return Component.translatable("entity.piranport." + entityPath);
    }

    public Entity create(Level level) {
        return entityType.get().create(level);
    }
}
