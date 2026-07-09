package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.PiranPort;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import com.piranport.npc.deepocean.DeepOceanArchivistEntity;
import com.piranport.npc.deepocean.DeepOceanBattleCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanBattleshipEntity;
import com.piranport.npc.deepocean.DeepOceanCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanDestroyerEntity;
import com.piranport.npc.deepocean.DeepOceanEngineerEntity;
import com.piranport.npc.deepocean.DeepOceanFlagshipEntity;
import com.piranport.npc.deepocean.DeepOceanHeavyCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanLightCarrierEntity;
import com.piranport.npc.deepocean.DeepOceanLightCruiserEntity;
import com.piranport.npc.deepocean.DeepOceanNavigatorEntity;
import com.piranport.npc.deepocean.DeepOceanQuartermasterEntity;
import com.piranport.npc.deepocean.DeepOceanSubmarineEntity;
import com.piranport.npc.deepocean.DeepOceanSupplyEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renderer for deep ocean entities. It still uses the vanilla humanoid rig as a
 * fallback skeleton, but each ship type now has a distinct texture, scale and
 * rigging silhouette until hand-authored models are available.
 */
public class DeepOceanRenderer extends HumanoidMobRenderer<AbstractDeepOceanEntity, HumanoidModel<AbstractDeepOceanEntity>> {

    public DeepOceanRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5f);
        addLayer(new DeepOceanRiggingLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractDeepOceanEntity entity) {
        if (entity instanceof DeepOceanFlagshipEntity flagship) {
            int phase = flagship.getPhase();
            if (phase >= 3) {
                return deepOceanTexture("flagship_phase3");
            }
            if (phase >= 2) {
                return deepOceanTexture("flagship_phase2");
            }
        }
        return profileFor(entity).texture();
    }

    @Override
    protected void scale(AbstractDeepOceanEntity entity, PoseStack poseStack, float partialTickTime) {
        float scale = profileFor(entity).scale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected float getShadowRadius(AbstractDeepOceanEntity entity) {
        return profileFor(entity).shadowRadius();
    }

    static VisualProfile profileFor(AbstractDeepOceanEntity entity) {
        if (entity instanceof DeepOceanFlagshipEntity) {
            return profile("flagship", HullKind.FLAGSHIP, 1.35f, 0.88f,
                    0.42f, 0.22f, 0.72f, 0.86f);
        }
        if (entity instanceof DeepOceanCarrierEntity) {
            return profile("carrier", HullKind.CARRIER, 1.22f, 0.78f,
                    0.10f, 0.45f, 0.58f, 0.78f);
        }
        if (entity instanceof DeepOceanLightCarrierEntity) {
            return profile("light_carrier", HullKind.CARRIER, 1.08f, 0.68f,
                    0.18f, 0.55f, 0.42f, 0.76f);
        }
        if (entity instanceof DeepOceanBattleshipEntity) {
            return profile("battleship", HullKind.BATTLESHIP, 1.28f, 0.80f,
                    0.52f, 0.53f, 0.62f, 0.82f);
        }
        if (entity instanceof DeepOceanBattleCruiserEntity) {
            return profile("battle_cruiser", HullKind.BATTLESHIP, 1.18f, 0.74f,
                    0.30f, 0.48f, 0.68f, 0.80f);
        }
        if (entity instanceof DeepOceanHeavyCruiserEntity) {
            return profile("heavy_cruiser", HullKind.CRUISER, 1.12f, 0.68f,
                    0.55f, 0.28f, 0.58f, 0.78f);
        }
        if (entity instanceof DeepOceanLightCruiserEntity) {
            return profile("light_cruiser", HullKind.CRUISER, 1.02f, 0.60f,
                    0.65f, 0.42f, 0.18f, 0.74f);
        }
        if (entity instanceof DeepOceanDestroyerEntity) {
            return profile("destroyer", HullKind.DESTROYER, 0.92f, 0.50f,
                    0.62f, 0.24f, 0.24f, 0.72f);
        }
        if (entity instanceof DeepOceanSubmarineEntity) {
            return profile("submarine", HullKind.SUBMARINE, 0.88f, 0.48f,
                    0.16f, 0.28f, 0.62f, 0.76f);
        }
        if (entity instanceof DeepOceanSupplyEntity) {
            return profile("supply", HullKind.SUPPLY, 0.86f, 0.48f,
                    0.42f, 0.55f, 0.62f, 0.72f);
        }
        if (entity instanceof DeepOceanArchivistEntity) {
            return profile("archivist", HullKind.SUPPLY, 0.90f, 0.50f,
                    0.46f, 0.38f, 0.78f, 0.76f);
        }
        if (entity instanceof DeepOceanEngineerEntity) {
            return profile("engineer", HullKind.SUPPLY, 0.94f, 0.54f,
                    0.30f, 0.72f, 0.68f, 0.78f);
        }
        if (entity instanceof DeepOceanNavigatorEntity) {
            return profile("navigator", HullKind.SUPPLY, 0.88f, 0.50f,
                    0.26f, 0.54f, 0.86f, 0.78f);
        }
        if (entity instanceof DeepOceanQuartermasterEntity) {
            return profile("quartermaster", HullKind.SUPPLY, 0.92f, 0.52f,
                    0.78f, 0.54f, 0.24f, 0.78f);
        }
        return profile("destroyer", HullKind.DESTROYER, 1.0f, 0.5f,
                0.45f, 0.45f, 0.55f, 0.72f);
    }

    private static VisualProfile profile(String textureName, HullKind hullKind, float scale,
                                         float shadowRadius, float r, float g, float b, float alpha) {
        return new VisualProfile(
                deepOceanTexture(textureName),
                hullKind, scale, shadowRadius, r, g, b, alpha);
    }

    private static ResourceLocation deepOceanTexture(String textureName) {
        return ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID,
                "textures/entity/deep_ocean/" + textureName + ".png");
    }

    enum HullKind {
        SUPPLY,
        DESTROYER,
        CRUISER,
        BATTLESHIP,
        CARRIER,
        SUBMARINE,
        FLAGSHIP
    }

    record VisualProfile(ResourceLocation texture, HullKind hullKind, float scale,
                         float shadowRadius, float r, float g, float b, float alpha) {
    }
}
