package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.rigging.RiggingGeometry;
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
        poseStack.translate(0.0, RiggingGeometry.HULL_DOWN + launchPose * 0.035,
                RiggingGeometry.HULL_BACK - launchPose * 0.035);
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

    /** +Y 朝上；正俯仰表示甲板前端抬高。两个视角的前向 Z 符号相反。 */
    public static void applyDeckPose(PoseStack poseStack, float pitch, boolean firstPerson) {
        poseStack.translate(-RiggingDeckPose.HINGE_X / 16.0f, RiggingDeckPose.HINGE_Y / 16.0f, 0.0);
        poseStack.mulPose(Axis.XP.rotationDegrees(firstPerson ? -pitch : pitch));
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

    /** X 朝左、Y 朝下；第一人称取正 Z 为前，第三人称取负 Z 为前。 */
    public static MountPose mountPose(RiggingSocket socket, boolean firstPerson) {
        float forward = firstPerson ? 1.0f : -1.0f;
        return switch (socket) {
            // 旧腿挂点仅保留网络序号兼容，新快照分配 WAIST_*。
            case LEG_LEFT -> new MountPose(2.6f, 6.0f, 0.0f, 90.0f, 0.0f, 90.0f, 0.40f);
            case LEG_RIGHT -> new MountPose(-2.6f, 6.0f, 0.0f, -90.0f, 0.0f, -90.0f, 0.40f);
            // 底座补偿为 8 × scale；平台顶面 y=1.8，故挂点 y=1.8-3.36。
            case SIDE_LEFT_1, SIDE_LEFT_2, SIDE_RIGHT_1, SIDE_RIGHT_2 -> {
                var mount = RiggingGeometry.sideMount(socket);
                yield new MountPose(mount.left(), RiggingGeometry.SideMount.DOWN, mount.forward() * forward,
                        180.0f, 0.0f, 0.0f, RiggingGeometry.SIDE_ITEM_SCALE);
            }
            case BACK_1 -> new MountPose(2.2f, -0.84f, 3.4f, 90.0f, 0.0f, 0.0f, 0.38f);
            case BACK_2 -> new MountPose(-2.2f, -0.84f, 3.4f, -90.0f, 0.0f, 0.0f, 0.38f);
            case DECK_1, DECK_2 -> new MountPose(RiggingDeckPose.CENTER_X, 0.4f,
                    RiggingDeckPose.aircraftZ(socket, firstPerson), 0.0f, -90.0f, 0.0f, 0.34f);
            // 底面转向腿侧，原模型上方转向舷外；补偿随侧翻改沿 X，底座贴支架 x=±8。
            case WAIST_LEFT -> new MountPose(10.88f, 9.0f, 2.0f, 180.0f, 0.0f, -90.0f, 0.36f);
            case WAIST_RIGHT -> new MountPose(-10.88f, 9.0f, 2.0f, 180.0f, 0.0f, 90.0f, 0.36f);
        };
    }

    public record MountPose(float x, float y, float z, float yaw, float pitch, float roll, float scale) {}
}
