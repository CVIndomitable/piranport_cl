package com.piranport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.piranport.rigging.RiggingSocket;
import net.minecraft.client.model.PlayerModel;

/**
 * 舰装挂点的位姿计算，占位层与 GeckoLib 层共用。
 *
 * <p>如果两层各写一份坐标，任何一次调整都会让武器物品模型和 GeckoLib 模型错位；
 * 这里集中维护「躯干/腿 → 挂点偏移 → yaw/pitch/roll」的前半段，
 * 调用方只负责最后的缩放与具体模型绘制。
 */
public final class RiggingSocketTransforms {

    private RiggingSocketTransforms() {}

    /**
     * 进入挂点所在骨骼的参考系。
     *
     * <p>腿挂点必须先 {@code translateAndRotate} 腿部模型，偏移才是相对大腿的；
     * 其余挂点以躯干为基准，并与舰体 hull 用同一套偏移 + 放飞后仰，
     * 否则挂点坐标和画出来的舷侧/甲板对不上。
     *
     * <p>2026-11-01 起鱼雷挂点在腰部（{@code WAIST_*}），走躯干默认分支，
     * 与 GeckoLib 船体共用 {@link #applyHullBase} 的共享偏移。
     */
    public static void applyBase(PoseStack poseStack, PlayerModel<?> parentModel,
                                 RiggingSocket socket, float launchPose) {
        switch (socket) {
            case LEG_LEFT -> parentModel.leftLeg.translateAndRotate(poseStack);
            case LEG_RIGHT -> parentModel.rightLeg.translateAndRotate(poseStack);
            default -> applyHullBase(poseStack, parentModel, launchPose);
        }
    }

    /**
     * 船体 / 躯干挂点共用的基准变换。
     *
     * <p>船体层的整个模型和侧舷、背部、甲板、腰部挂点都必须用这一个方法，
     * 切换渲染实现时位置才不会跳。改这里的偏移会同时影响所有挂点。
     */
    public static void applyHullBase(PoseStack poseStack, PlayerModel<?> parentModel,
                                     float launchPose) {
        parentModel.body.translateAndRotate(poseStack);
        poseStack.translate(0.0, 0.28 + launchPose * 0.035, 0.34 - launchPose * 0.035);
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0f * launchPose));
    }

    /**
     * 挂点偏移（只平移）。GeckoLib 层需要在平移与 socket 旋转之间插入模型自身的
     * 坐标系转换（Y 轴翻转 + yawOffset），所以把平移和旋转拆开。
     */
    public static void applyMountOffset(PoseStack poseStack, MountPose pose) {
        poseStack.translate(pose.x() / 16.0f, pose.y() / 16.0f, pose.z() / 16.0f);
    }

    /** 挂点朝向（yaw → pitch → roll，顺序与占位层原有实现一致）。 */
    public static void applyMountRotations(PoseStack poseStack, MountPose pose) {
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.yaw()));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.roll()));
    }

    /** 平移 + 朝向，占位物品模型用这一套即可。 */
    public static void applyMountFrame(PoseStack poseStack, MountPose pose) {
        applyMountOffset(poseStack, pose);
        applyMountRotations(poseStack, pose);
    }

    /**
     * 各挂点的占位位姿。模型空间里 1 单位 = 1/16 格，+y 朝下。
     *
     * <p>这些数值最初是给武器物品模型用的占位观感；GeckoLib 模型沿用同一套挂点偏移，
     * 只在缩放和模型前后朝向（yawOffset）上有自己的参数。
     */
    public static MountPose mountPose(RiggingSocket socket, float launchPose) {
        return switch (socket) {
            // 腿部：占位历史值。2026-11-01 起鱼雷改挂腰部，这两个挂点保留仅为网络序号兼容，
            // 不再由 RiggingLoadoutResolver 分配。
            case LEG_LEFT -> new MountPose(2.6f, 6.0f, 0.0f, 90.0f, 0.0f, 90.0f, 0.40f);
            case LEG_RIGHT -> new MountPose(-2.6f, 6.0f, 0.0f, -90.0f, 0.0f, -90.0f, 0.40f);
            // 舷侧：上下两个平台，炮口朝外（舰体后仰已由共享变换处理）
            case SIDE_LEFT_1 -> new MountPose(4.0f, 0.2f, 0.4f, 90.0f, -4.0f * launchPose, 90.0f, 0.42f);
            case SIDE_LEFT_2 -> new MountPose(4.0f, 3.6f, -0.4f, 90.0f, -4.0f * launchPose, 90.0f, 0.42f);
            case SIDE_RIGHT_1 -> new MountPose(-4.0f, 0.2f, 0.4f, -90.0f, -4.0f * launchPose, -90.0f, 0.42f);
            case SIDE_RIGHT_2 -> new MountPose(-4.0f, 3.6f, -0.4f, -90.0f, -4.0f * launchPose, -90.0f, 0.42f);
            // 后背：烟囱左右各一，朝后
            case BACK_1 -> new MountPose(2.2f, 2.2f, -3.4f, 180.0f, 0.0f, 90.0f, 0.38f);
            case BACK_2 -> new MountPose(-2.2f, 2.2f, -3.4f, 180.0f, 0.0f, -90.0f, 0.38f);
            // 甲板：右舷甲板前后各一，面朝上平放
            case DECK_1 -> new MountPose(-5.6f, 0.4f, -2.2f, 0.0f, -90.0f, 0.0f, 0.34f);
            case DECK_2 -> new MountPose(-5.6f, 0.4f, 2.2f, 0.0f, -90.0f, 0.0f, 0.34f);
            // 腰部：鱼雷发射器挂在腰带两侧，与腰部鱼雷架模型对齐。
            // 右舷 z 前移 3.6 是为了补偿鱼雷发射器物品模型原点不居中；
            // roll 保持 +90（不能照抄 LEG_RIGHT 的 -90，那会让右舷炮管上下颠倒）。
            case WAIST_LEFT -> new MountPose(4.3f, 5.1f, 0.2f, 90.0f, 0.0f, 90.0f, 0.36f);
            case WAIST_RIGHT -> new MountPose(-4.3f, 5.1f, 3.8f, -90.0f, 0.0f, 90.0f, 0.36f);
        };
    }

    public record MountPose(float x, float y, float z, float yaw, float pitch, float roll, float scale) {}
}
