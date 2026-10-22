package com.piranport.dungeon.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * 出口传送门的客户端演出：走进门面后先演一段"下界门同款"的传送效果，约一秒后才真正传送。
 *
 * <p><b>为什么不用反胃效果：</b>原版的紫色屏幕遮罩挂在 {@code MobEffects.CONFUSION} 上
 * （{@code GameRenderer#renderConfusionOverlay}），但反胃会往 HUD 增益列表插一个漩涡图标
 * （原版下界门没有），而且 {@code LocalPlayer#removeEffectNoUpdate} 在效果到期时会把
 * {@code spinningEffectIntensity} 硬清零，镜头会猛地归位。所以这里不碰效果，改为自己画遮罩，
 * 参数与 {@code GameRenderer#renderConfusionOverlay} 完全一致。</p>
 *
 * <p>视角扭曲本身仍走原版那条路径：{@code GameRenderer} 的 view bob 无门槛地读
 * {@code LocalPlayer#spinningEffectIntensity}，把这个字段抬上去就能拿到同款镜头晃动。</p>
 */
public final class ClientPortalTransit {
    /** 与服务端 {@code DungeonPortalEntity.TRANSIT_TICKS} 一致：20 tick = 1 秒。 */
    private static final int TRANSIT_TICKS = 20;
    /** 原版反胃遮罩用的同一张贴图，绘制方式照抄 GameRenderer。 */
    private static final ResourceLocation NAUSEA_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/misc/nausea.png");

    private static boolean active = false;
    private static int remainingTicks = 0;
    /**
     * 自己这条遮罩的强度（0..1），独立于 {@code LocalPlayer.spinningEffectIntensity}。
     *
     * <p><b>为什么不能直接读那个字段：</b>玩家真的喝了反胃药水时原版会自己画遮罩，
     * 我们再按同一个字段画一遍就会叠成两层紫色。这里只跟踪本演出自己的曲线：
     * 演出中按 1/20 上升，结束后按原版 0.05/tick 回落。</p>
     */
    private static float overlayStrength = 0.0F;

    private ClientPortalTransit() {}

    public static void start() {
        active = true;
        remainingTicks = TRANSIT_TICKS;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        // 与 LocalPlayer#handleConfusionTransitionEffect 进原版传送门时的音效参数一致。
        mc.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(
                SoundEvents.PORTAL_TRIGGER, mc.player.getRandom().nextFloat() * 0.4F + 0.8F, 0.25F));
    }

    public static void stop() {
        active = false;
        remainingTicks = 0;
    }

    /** 断线/换维度时清干净，避免镜头扭曲跨局残留。 */
    public static void clear() {
        stop();
        overlayStrength = 0.0F;
    }

    /**
     * 每 tick 在玩家自身 tick <b>之后</b>调用（{@code ClientTickEvent.Post}）。
     *
     * <p><b>为什么必须放在 Post：</b>{@code LocalPlayer#handleConfusionTransitionEffect}
     * 在玩家 tick 里会把 {@code spinningEffectIntensity} 按 -0.05 衰减；写在 Pre 的值
     * 当 tick 就会被扣掉，放在 Post 才能拿到完整的上升曲线。</p>
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { stop(); return; }

        if (active) {
            float step = 1.0F / TRANSIT_TICKS;
            mc.player.spinningEffectIntensity = Mth.clamp(
                    mc.player.spinningEffectIntensity + step, 0.0F, 1.0F);
            overlayStrength = Mth.clamp(overlayStrength + step, 0.0F, 1.0F);
            if (--remainingTicks <= 0) {
                // 服务端在真正传送的同一 tick 会发 stop；这里只是防丢包兜底，
                // 让镜头开始按原版速度回落，不至于卡在满强度。
                active = false;
            }
            return;
        }

        // 演出结束后遮罩按原版反胃的衰减速度淡出；镜头扭曲交给原版自己 -0.05/tick 回落。
        if (overlayStrength > 0.0F) {
            overlayStrength = Math.max(0.0F, overlayStrength - 0.05F);
        }
    }

    /** 遮罩强度沿用原版门槛公式，尊重玩家的"屏幕效果"设置。 */
    public static void renderOverlay(GuiGraphics guiGraphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || overlayStrength <= 0.0F) return;
        float screenEffectScale = mc.options.screenEffectScale().get().floatValue();
        if (screenEffectScale >= 1.0F) return;
        renderConfusionLike(guiGraphics, overlayStrength * (1.0F - screenEffectScale));
    }

    /** {@code GameRenderer#renderConfusionOverlay} 的同款绘制（同贴图、同缩放、同混合）。 */
    private static void renderConfusionLike(GuiGraphics guiGraphics, float scalar) {
        int width = guiGraphics.guiWidth();
        int height = guiGraphics.guiHeight();
        guiGraphics.pose().pushPose();
        float scale = Mth.lerp(scalar, 2.0F, 1.0F);
        guiGraphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, scale);
        guiGraphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        guiGraphics.setColor(0.2F * scalar, 0.4F * scalar, 0.2F * scalar, 1.0F);
        guiGraphics.blit(NAUSEA_LOCATION, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        guiGraphics.pose().popPose();
    }
}
