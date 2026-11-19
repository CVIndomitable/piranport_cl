package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.PiranPort;
import com.piranport.item.UnicornHarpItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public final class UnicornHarpPerformance {
    static final int DURATION = 16;
    private static final int[] BEATS = {4, 8, 12};
    private static final Map<AbstractClientPlayer, Performance> ACTIVE = new WeakHashMap<>();

    private static final class Performance {
        final InteractionHand hand;
        final int start;
        int nextBeat;
        Performance(InteractionHand hand, int start) { this.hand = hand; this.start = start; }
    }

    private UnicornHarpPerformance() {}

    static void start(int id, InteractionHand hand) {
        var level = Minecraft.getInstance().level;
        if (level != null && level.getEntity(id) instanceof AbstractClientPlayer player) {
            ACTIVE.put(player, new Performance(hand, player.tickCount));
        }
    }

    private static boolean playing(AbstractClientPlayer player, Performance p) {
        return p != null && player.isAlive() && !player.isRemoved() && !player.isSpectator()
                && player.level() == Minecraft.getInstance().level && player.tickCount - p.start < DURATION
                && player.getItemInHand(p.hand).getItem() instanceof UnicornHarpItem;
    }

    public static boolean isPerforming(AbstractClientPlayer player) {
        return playing(player, ACTIVE.get(player));
    }

    // 平滑抬琴、落琴；手背往返扫过弦面。三个过弦节拍与粒子共用时钟。
    static float envelope(float t) {
        float f = Mth.clamp(Math.min(t / 3, (DURATION - t) / 3), 0, 1);
        return f * f * (3 - 2 * f);
    }

    static float sweep(float t) { return Mth.sin((t - BEATS[0]) * Mth.PI / 4); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (ACTIVE.isEmpty() || Minecraft.getInstance().isPaused()) return;
        for (Iterator<Map.Entry<AbstractClientPlayer, Performance>> it = ACTIVE.entrySet().iterator(); it.hasNext();) {
            var entry = it.next(); var player = entry.getKey(); var p = entry.getValue();
            if (player == null || !playing(player, p)) { it.remove(); continue; }
            while (p.nextBeat < BEATS.length && player.tickCount - p.start >= BEATS[p.nextBeat]) {
                emitNote(player, p.nextBeat++);
            }
        }
    }

    private static void emitNote(AbstractClientPlayer player, int beat) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 position;
        if (mc.getCameraEntity() == player && mc.options.getCameraType().isFirstPerson()) {
            var camera = mc.gameRenderer.getMainCamera();
            position = camera.getPosition().add(new Vec3(camera.getLookVector()).scale(.94))
                    .add(new Vec3(camera.getLeftVector()).scale(.22 + .055 * beat))
                    .add(new Vec3(camera.getUpVector()).scale(-.12 + .045 * beat));
        } else {
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            double x = .17 + .055 * beat;
            position = player.position().add(x * Mth.cos(yaw) - .52 * Mth.sin(yaw),
                    player.getEyeHeight() - .34 + .045 * beat, x * Mth.sin(yaw) + .52 * Mth.cos(yaw));
        }
        player.level().addParticle(ParticleTypes.NOTE, position.x, position.y, position.z, (3 + beat * 5) / 24.0, 0, 0);
    }

    /** 在原版人物姿势之后叠加；手和袖子一起动，离开演出后恢复原版。 */
    public static void pose(PlayerModel<?> model, AbstractClientPlayer player, float ageInTicks) {
        Performance p = ACTIVE.get(player);
        if (!playing(player, p) || ageInTicks < player.tickCount - 1) return;
        float t = ageInTicks - p.start, weight = envelope(t), stroke = sweep(t);
        if (weight == 0) return;
        ModelPart holder = model.leftArm;
        ModelPart picker = model.rightArm;
        float tilt = -1.13F + stroke * .035F, turn = 1.04F;
        // 弦在握点的外侧。用托琴臂的局部坐标求另一个肩膀到弦面的方向，避免两手平行挥空。
        Vector3f target = new Vector3f(2.6F + 1.4F * stroke, 7.1F, -2.6F)
                .rotateX(tilt).rotateY(turn).add(holder.x - picker.x, holder.y - picker.y, holder.z - picker.z);
        holder.xRot = Mth.lerp(weight, holder.xRot, tilt);
        holder.yRot = Mth.lerp(weight, holder.yRot, turn);
        holder.zRot *= 1 - weight;
        picker.xRot = Mth.lerp(weight, picker.xRot, (float) -Math.atan2(Math.hypot(target.x, target.z), target.y));
        picker.yRot = Mth.lerp(weight, picker.yRot, (float) Math.atan2(-target.x, -target.z));
        picker.zRot *= 1 - weight;
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void alignIdleArms(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player) || player.isSpectator()) return;
        HarpHandLayout layout = HarpHandLayout.of(player);
        if (layout == HarpHandLayout.VANILLA) return;
        var model = event.getRenderer().getModel();
        var mainPose = player.getMainArm() == HumanoidArm.RIGHT ? model.rightArmPose : model.leftArmPose;
        model.leftArmPose = HumanoidModel.ArmPose.ITEM;
        model.rightArmPose = layout == HarpHandLayout.MAIN_HARP || isPerforming(player) ? HumanoidModel.ArmPose.EMPTY : mainPose;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void renderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance(); var player = mc.player;
        if (player == null || player.isScoping()) return;
        HarpHandLayout layout = HarpHandLayout.of(player);
        if (layout == HarpHandLayout.VANILLA) {
            // 原版换物有数帧缓存；不要让上一帧的琴闪回右手。
            if (event.getItemStack().getItem() instanceof UnicornHarpItem) event.setCanceled(true);
            return;
        }
        Performance p = ACTIVE.get(player);
        boolean performing = playing(player, p);
        if (layout.hides(event.getHand(), performing)) { event.setCanceled(true); return; }
        var stack = player.getItemInHand(event.getHand());
        if (!(stack.getItem() instanceof UnicornHarpItem)) {
            if (event.getItemStack().getItem() instanceof UnicornHarpItem) event.setCanceled(true);
            return;
        }
        event.setCanceled(true);
        float t = performing ? player.tickCount - p.start + event.getPartialTick() : 0;
        float weight = envelope(t), stroke = sweep(t);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-.56 + .16 * weight, -.52 + .07 * weight, -.72);
        pose.mulPose(Axis.ZP.rotationDegrees(-weight * (7 + 1.6F * stroke)));
        mc.gameRenderer.itemInHandRenderer.renderItem(player, stack, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                true, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
        if (player.isInvisible() || weight <= 0) return;

        // 把拨弦手的手端定位在弦面前；身后半截沿视线退到画面下方。
        var renderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(player);
        pose.pushPose();
        pose.translate(-Mth.lerp(weight, -.46F, .28F + .085F * stroke),
                Mth.lerp(weight, -.96F, -.16F + .045F * stroke), -.90);
        pose.mulPose(Axis.ZP.rotationDegrees(40));
        pose.mulPose(Axis.XP.rotationDegrees(-125));
        pose.scale(.78F, .78F, .78F);
        pose.translate(.375, -.75, 0);
        renderer.renderRightHand(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        pose.popPose();
    }
}
