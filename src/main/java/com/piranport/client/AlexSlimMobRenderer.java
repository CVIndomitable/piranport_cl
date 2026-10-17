package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * 深海舰与低级驱逐舰的临时人形外观：艾利克斯（Alex / slim）玩家模型 + 原版 Alex 皮肤占位。
 *
 * <p>皮肤目前统一使用原版 {@code textures/entity/player/slim/alex.png}。后续按舰种补
 * 皮肤时，只需把 {@link #getTextureLocation} 改成按实体类型选择贴图，模型和缩放不用动。</p>
 */
public class AlexSlimMobRenderer<T extends Mob> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    /** 原版 slim Alex 皮肤；深海舰专属皮肤未制作前作为占位。 */
    public static final ResourceLocation ALEX_PLACEHOLDER =
            ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png");

    /** 原版玩家模型的设计高度；用于把模型等比缩放到实体碰撞箱。 */
    private static final float PLAYER_MODEL_HEIGHT = 1.8f;

    public AlexSlimMobRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        // TODO 深海舰专属皮肤：按 BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) 拼贴图路径。
        return ALEX_PLACEHOLDER;
    }

    @Override
    protected void scale(T entity, PoseStack poseStack, float partialTickTime) {
        // 玩家模型约 1.8 格高；按碰撞箱高度等比放大，避免大船看起来只有一小截。
        float scale = Math.max(0.1f, entity.getBbHeight()) / PLAYER_MODEL_HEIGHT;
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected float getShadowRadius(T entity) {
        return Math.max(0.3f, entity.getBbWidth() * 0.5f);
    }
}
