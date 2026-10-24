package com.piranport.client.model.bedrock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 用 Bedrock 几何 + 动画驱动的实体模型。
 *
 * <p>动画状态按实体（弱引用）保存：同一个模型实例会被所有同类型实体共享，
 * 播放进度不能放在模型字段里。状态切换做 0.2 秒交叉淡入，避免走/跑/跳切换时抽帧。</p>
 */
public class BedrockEntityModel<T extends LivingEntity> extends EntityModel<T> {
    private static final float FADE_SECONDS = 0.2F;

    private final BedrockMesh mesh;
    private final Map<String, BedrockAnimation> animations;
    private final Map<T, AnimationState> states = new WeakHashMap<>();
    private final BedrockMolang.Context molang = new BedrockMolang.Context();

    private final float[] poses;
    private final float[] currentPoses;
    private final float[] previousPoses;

    public BedrockEntityModel(BedrockGeometry geometry, Map<String, BedrockAnimation> animations, float scale) {
        super();
        this.mesh = new BedrockMesh(geometry, scale);
        this.animations = animations;
        int size = geometry.bones.size() * 9;
        this.poses = new float[size];
        this.currentPoses = new float[size];
        this.previousPoses = new float[size];
        reset(this.poses);
        reset(this.currentPoses);
        reset(this.previousPoses);
    }

    private static void reset(float[] poses) {
        Arrays.fill(poses, 0.0F);
        for (int i = 6; i < poses.length; i += 9) {
            poses[i] = 1.0F;
            poses[i + 1] = 1.0F;
            poses[i + 2] = 1.0F;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        if (animations.isEmpty()) return;

        AnimationState state = this.states.computeIfAbsent(entity, ignored -> new AnimationState());
        float delta = state.lastAge < 0.0F ? 0.0F : Mth.clamp(ageInTicks - state.lastAge, 0.0F, 0.1F);
        state.lastAge = ageInTicks;

        boolean moving = limbSwingAmount > 0.02F;
        String target = pickAnimation(entity, limbSwingAmount, moving);
        if (!animations.containsKey(target)) target = "idle";
        if (!animations.containsKey(target)) {
            target = animations.keySet().iterator().next();
        }

        if (!target.equals(state.current)) {
            state.previous = state.current;
            state.previousTime = state.time;
            state.fade = FADE_SECONDS;
            state.current = target;
            state.time = 0.0F;
        }

        float rate = animationRate(target, limbSwingAmount);
        state.time += delta * rate;
        state.previousTime += delta * rate;
        wrap(state, animations.get(state.current));
        if (state.previous != null) {
            wrapPrevious(state, animations.get(state.previous));
        }

        reset(this.currentPoses);
        reset(this.previousPoses);
        BedrockAnimation current = animations.get(state.current);
        if (current != null) {
            this.molang.animTime = state.time;
            current.sample(state.time, this.molang, this.currentPoses, this.mesh.boneIndex());
        }

        BedrockAnimation previous = state.previous == null ? null : animations.get(state.previous);
        float blend = state.fade > 0.0F ? 1.0F - state.fade / FADE_SECONDS : 1.0F;
        if (previous != null && blend < 1.0F) {
            this.molang.animTime = state.previousTime;
            previous.sample(state.previousTime, this.molang, this.previousPoses, this.mesh.boneIndex());
            for (int i = 0; i < this.poses.length; i++) {
                this.poses[i] = this.previousPoses[i] + (this.currentPoses[i] - this.previousPoses[i]) * blend;
            }
        } else {
            System.arraycopy(this.currentPoses, 0, this.poses, 0, this.poses.length);
            state.previous = null;
        }
        state.fade = Math.max(0.0F, state.fade - delta);

        // 头部跟随视线：叠加在动画之后，单位是度
        Integer head = this.mesh.boneIndex().get("Head");
        if (head != null) {
            int base = head * 9;
            this.poses[base] += headPitch;
            this.poses[base + 1] += netHeadYaw;
        }
    }

    private void wrap(AnimationState state, BedrockAnimation animation) {
        if (animation != null && animation.loop && animation.length > 0.0F) {
            state.time = state.time % animation.length;
        }
    }

    private void wrapPrevious(AnimationState state, BedrockAnimation animation) {
        if (animation != null && animation.loop && animation.length > 0.0F) {
            state.previousTime = state.previousTime % animation.length;
        }
    }

    private static String pickAnimation(LivingEntity entity, float limbSwingAmount, boolean moving) {
        if (entity.isSwimming()) return "swim";
        if (entity.isCrouching()) return moving ? "sneaking" : "sneak";
        if (!entity.onGround() && !entity.isInWater()) return "jump";
        if (moving) return limbSwingAmount > 0.65F ? "run" : "walk";
        return "idle";
    }

    /**
     * 动画播放速率。walk/run 的帧长分别是 1s / 0.667s，
     * 用行走速度把脚步频率拉近原版，避免“滑步”。
     */
    private static float animationRate(String animation, float limbSwingAmount) {
        return switch (animation) {
            case "walk" -> 0.6F + limbSwingAmount;
            case "run" -> 1.0F + 0.6F * limbSwingAmount;
            case "sneaking" -> 0.8F;
            default -> 1.0F;
        };
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.mesh.render(poseStack, buffer, packedLight, packedOverlay, color, this.poses);
    }

    private static final class AnimationState {
        private String current = "idle";
        private String previous;
        private float time;
        private float previousTime;
        private float fade;
        private float lastAge = -1.0F;
    }
}
