package com.piranport.compat.maid.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.blaze3d.vertex.PoseStack;
import com.piranport.compat.maid.combat.MaidCombatStats;
import com.piranport.compat.maid.combat.MaidWeaponFirer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class MaidWeaponIconRenderer {

    public static void render(EntityMaid maid, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ItemStack weapon = maid.getMainHandItem();
        if (weapon.isEmpty() || !MaidWeaponFirer.isSupported(weapon)) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        Font font = mc.font;

        poseStack.pushPose();

        poseStack.translate(0.0, maid.getBbHeight() + 0.5, 0.0);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float scale = 0.5f;
        poseStack.scale(scale, scale, scale);

        poseStack.pushPose();
        poseStack.translate(0, 0, 0.01);
        itemRenderer.renderStatic(weapon, ItemDisplayContext.GUI, packedLight,
                LightTexture.FULL_BRIGHT, poseStack, buffer, maid.level(), 0);
        poseStack.popPose();

        MaidCombatStats.Stats stats = MaidCombatStats.get(maid);
        if (stats.kills > 0 || stats.shotsFired > 0) {
            poseStack.pushPose();
            poseStack.translate(0, -12, 0);
            poseStack.scale(0.5f, 0.5f, 0.5f);

            String statsText = String.format("§6%d§r/§c%.0f", stats.kills, stats.totalDamage);
            Component text = Component.literal(statsText);

            float textX = -font.width(text) / 2f;
            Matrix4f matrix = poseStack.last().pose();

            font.drawInBatch(text, textX, 0, 0xFFFFFF, false, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, packedLight);

            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
