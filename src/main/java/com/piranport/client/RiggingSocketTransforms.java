package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.model.PlayerModel;
import software.bernie.geckolib.util.RenderUtil;

/** 舰体、支架和武器共用的挂载参考系；模型单位为 1/16 格。 */
public final class RiggingSocketTransforms {
    private RiggingSocketTransforms() {}

    public static void applyBase(PoseStack poseStack, PlayerModel<?> parentModel,
                                 RiggingSocket socket, float launchPose) {
        switch (socket) {
            case LEG_LEFT -> parentModel.leftLeg.translateAndRotate(poseStack);
            case LEG_RIGHT -> parentModel.rightLeg.translateAndRotate(poseStack);
            default -> applyHullBase(poseStack, parentModel, launchPose);
        }
    }

    public static void applyHullBase(PoseStack poseStack, PlayerModel<?> parentModel,
                                     float launchPose) {
        parentModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.28 + launchPose * 0.035, 0.34 - launchPose * 0.035);
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0f * launchPose));
    }

    // 舰体的骨骼动画不再额外移动挂架；统一在外层采样同一时钟，避免各控制器起播相位不同。
    public static void applyThirdPersonBreathing(PoseStack poseStack, RiggingSocket socket) {
        poseStack.translate(0.0, -breathingOffset(RenderUtil.getCurrentTick()), 0.0);
    }

    public static void applyFirstPersonBreathing(PoseStack poseStack, RiggingSocket socket) {
        poseStack.translate(0.0, breathingOffset(RenderUtil.getCurrentTick()), 0.0);
    }

    public static float breathingOffset(double ticks) {
        double period = ModEquipmentConfig.RIGGING_BREATH_PERIOD_TICKS.get();
        float phase = (float) (((ticks % period) + period) % period / period * 4);
        float rise = ModEquipmentConfig.RIGGING_BREATH_RISE.get().floatValue();
        float fall = ModEquipmentConfig.RIGGING_BREATH_FALL.get().floatValue();
        float modelUnits;
        if (phase <= 1) {
            modelUnits = rise * phase;
        } else if (phase <= 2) {
            modelUnits = rise * (2 - phase);
        } else if (phase <= 3) {
            modelUnits = -fall * (phase - 2);
        } else {
            modelUnits = -fall * (4 - phase);
        }
        return modelUnits / 16.0f;
    }

    /** 甲板以靠身侧的纵向铰链翻折，收起时顶面朝身体右外侧。坐标系为 +Y 朝上。 */
    public static void applyDeckFold(PoseStack poseStack, float deployment) {
        poseStack.translate(-RiggingDeckPose.HINGE_X / 16.0f, RiggingDeckPose.HINGE_Y / 16.0f, 0.0);
        poseStack.mulPose(Axis.ZP.rotationDegrees(RiggingDeckPose.foldDegrees(deployment)));
        poseStack.translate(RiggingDeckPose.HINGE_X / 16.0f, -RiggingDeckPose.HINGE_Y / 16.0f, 0.0);
    }

    public static void applyMountOffset(PoseStack poseStack, MountPose pose) {
        poseStack.translate(pose.x() / 16.0f, pose.y() / 16.0f, pose.z() / 16.0f);
    }

    public static void applyMountRotations(PoseStack poseStack, MountPose pose) {
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
    }

    public static void applyMountFrame(PoseStack poseStack, MountPose pose) {
        applyMountOffset(poseStack, pose);
        applyMountRotations(poseStack, pose);
    }

    /** 玩家模型空间：+X 朝左，+Y 朝下，-Z 朝前。物品 FIXED 会再绕 Y 转 180°。 */
    public static MountPose mountPose(RiggingSocket socket, float launchPose) {
        return switch (socket) {
            // 旧腿挂点仅保留网络序号兼容，新快照分配 WAIST_*。
            case LEG_LEFT -> new MountPose(2.6f, 6.0f, 0.0f, 90.0f, 0.0f, 90.0f, 0.40f);
            case LEG_RIGHT -> new MountPose(-2.6f, 6.0f, 0.0f, -90.0f, 0.0f, -90.0f, 0.40f);
            // 底座补偿为 8 × scale；平台顶面 y=1.8，故挂点 y=1.8-3.36。
            case SIDE_LEFT_1 -> new MountPose(9.5f, -1.56f, 6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_LEFT_2 -> new MountPose(9.5f, -1.56f, -6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_RIGHT_1 -> new MountPose(-9.5f, -1.56f, 6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_RIGHT_2 -> new MountPose(-9.5f, -1.56f, -6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case BACK_1 -> new MountPose(2.2f, -0.84f, 3.4f, 90.0f, 0.0f, 0.0f, 0.38f);
            case BACK_2 -> new MountPose(-2.2f, -0.84f, 3.4f, -90.0f, 0.0f, 0.0f, 0.38f);
            case DECK_1 -> new MountPose(RiggingDeckPose.CENTER_X, 0.4f, RiggingDeckPose.aircraftZ(true), 0.0f, -90.0f, 0.0f, 0.34f);
            case DECK_2 -> new MountPose(RiggingDeckPose.CENTER_X, 0.4f, RiggingDeckPose.aircraftZ(false), 0.0f, -90.0f, 0.0f, 0.34f);
            // 底面转向腿侧，原模型上方转向舷外；补偿随侧翻改沿 X，底座贴支架 x=±8。
            case WAIST_LEFT -> new MountPose(10.88f, 9.0f, 2.0f, 180.0f, 0.0f, -90.0f, 0.36f);
            case WAIST_RIGHT -> new MountPose(-10.88f, 9.0f, 2.0f, 180.0f, 0.0f, 90.0f, 0.36f);
        };
    }

    public record MountPose(float x, float y, float z, float yaw, float pitch, float roll, float scale) {}
}
