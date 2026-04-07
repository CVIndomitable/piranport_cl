package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.npc.ai.FleetGroup;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Placeholder renderer for all deep ocean entities.
 * The entity uses isCurrentlyGlowing()=true for visibility.
 * Renders fleet state particles: ALERT=yellow, COMBAT=red.
 */
public class DeepOceanRenderer extends EntityRenderer<AbstractDeepOceanEntity> {

    public DeepOceanRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AbstractDeepOceanEntity entity, float entityYaw, float partialTick,
                        PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        // Fleet state particles (client-side visual only)
        if (entity.tickCount % 20 == 0) {
            FleetGroup.State state = entity.getFleetState();
            if (state == FleetGroup.State.ALERT) {
                entity.level().addParticle(ParticleTypes.WAX_ON,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.5,
                        entity.getZ(), 0, 0.02, 0);
            } else if (state == FleetGroup.State.COMBAT) {
                entity.level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                        entity.getX(), entity.getY() + entity.getBbHeight() + 0.5,
                        entity.getZ(), 0, 0.02, 0);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractDeepOceanEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/zombie/zombie.png");
    }
}
