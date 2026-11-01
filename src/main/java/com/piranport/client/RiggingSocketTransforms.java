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
     * <p><b>2026-11-01 起挂点画的是 3D 模型，不是平面图标</b>：
     * 舷侧与腰部挂点的 yaw/pitch/roll 全部为 0，让模型底座落在舰装平台上、
     * 炮口/管口朝前（+Z）。不要再按平面图标那套「朝外 90°」调角度。
     *
     * <p>GeckoLib 模型沿用同一套挂点偏移，只在缩放和模型前后朝向（yawOffset）上有自己的参数。
     */
    public static MountPose mountPose(RiggingSocket socket, float launchPose) {
        return switch (socket) {
            // 腿部：占位历史值。2026-11-01 起鱼雷改挂腰部，这两个挂点保留仅为网络序号兼容，
            // 不再由 RiggingLoadoutResolver 分配。
            case LEG_LEFT -> new MountPose(2.6f, 6.0f, 0.0f, 90.0f, 0.0f, 90.0f, 0.40f);
            case LEG_RIGHT -> new MountPose(-2.6f, 6.0f, 0.0f, -90.0f, 0.0f, -90.0f, 0.40f);
            // 舷侧：L 型扶手的最终坐标（hull 48122f10），前后各一个炮位，炮口朝前。
            // 朝向：挂点 yaw=180。renderMount 原来的有效旋转是 XP(180)×FIXED Y(180)=Rz(180)，
            // 实机看到炮口与角色面向相反（朝镜头/身后），说明 3D 武器模型相对玩家还需再转 180°。
            // 加上挂点 Y(180) 后总旋转 = X(180)：炮口转到与角色同向、+Y 翻到玩家上方、+X 不再镜像。
            // 底座下沉补偿：物品管线的 translate(-0.5) 是 0.5 格，经挂点 scale 后
            // 下沉 0.5*scale 格 = 8*scale 个 mountPose 单位。scale 0.42 → 3.36 单位；
            // 平台顶面 player y=1.8，所以 mount y = 1.8 - 3.36 = -1.56。
            // x 保持 ±9.5；模型空间 -Z 是前，玩家空间 +Z 是前，所以 _1（前）= +6.0、_2（后）= -6.0。
            case SIDE_LEFT_1 -> new MountPose(9.5f, -1.56f, 6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_LEFT_2 -> new MountPose(9.5f, -1.56f, -6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_RIGHT_1 -> new MountPose(-9.5f, -1.56f, 6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            case SIDE_RIGHT_2 -> new MountPose(-9.5f, -1.56f, -6.0f, 180.0f, 0.0f, 0.0f, 0.42f);
            // 后背：深弹 / 防空导弹现在也是 3D 模型，必须正放（roll=0），不能沿用平面图标那套 roll±90 横躺。
            // 两具分别朝左/右，避免模型互相穿插，也符合背部挂点的视觉：
            // BACK_1 在左（x +2.2）朝左/+X（yaw 90），BACK_2 在右（x -2.2）朝右/-X（yaw -90）。
            // y 含底座下沉补偿：8*scale=8*0.38=3.04，背平台顶面 player y=2.2 → 挂点 y=-0.84。
            // x ±2.2、z -3.4 保持不变。
            case BACK_1 -> new MountPose(2.2f, -0.84f, -3.4f, 90.0f, 0.0f, 0.0f, 0.38f);
            case BACK_2 -> new MountPose(-2.2f, -0.84f, -3.4f, -90.0f, 0.0f, 0.0f, 0.38f);
            // 甲板：右侧甲板前后各一，面朝上平放。
            // carrier_deck 已 X 镜像到模型 x +11.5..+18.5（554e1308）。GeckoLib 导入会镜像 X，
            // 所以渲染到玩家空间是 x -18.5..-11.5（中心 -15）＝角色右侧（玩家模型 +X 是左）。
            // 挂点必须写渲染后的 -15，不能按模型 JSON 的 +15 写：写 +15 会跑到甲板对面约 1.9 格。
            // z：模型 z +1.0 / +6.0，船体层不翻 Z → 挂点 z 仍为 +1.0 / +6.0。
            // y：甲板托面 player y=+0.8，挂点取 0.4（托面上方 0.4）。
            case DECK_1 -> new MountPose(-15.0f, 0.4f, 1.0f, 0.0f, -90.0f, 0.0f, 0.34f);
            case DECK_2 -> new MountPose(-15.0f, 0.4f, 6.0f, 0.0f, -90.0f, 0.0f, 0.34f);
            // 腰部：左右两具鱼雷发射器必须和玩家平行——同一高度、同一前后位置、管口一致朝前。
            // 朝向同样取 yaw=180（与舷侧同一推导：XP(180)×FIXED Y(180)×Y(180) = X(180)），
            // 管口转到与角色同向、左右不镜像。
            // 托盘（705a2784）：模型 x 8..12 / -12..-8、顶面 y=-11、z -2..6。GeckoLib 导入镜像 X，
            // 渲染到玩家空间后仍是 x ±8..±12（中心 ±10）；Y/Z 不变，船体层 Y 反射也不翻 Z，
            // 所以 player z = model z：托盘 z 中心 = +2.0、顶面 player y = +11.0。
            // 鱼雷模型 X 中心正好在原点，托盘中心 x=±10，所以挂点 x=±10；
            // 底座下沉补偿 8*0.36=2.88 → mount y = 11.0 - 2.88 = 8.12；
            // 挂点 z 取托盘中心 +2.0（yaw 180 时 quad 底座板落在 z -1.56..+1.24，完整压在板上）。
            case WAIST_LEFT -> new MountPose(10.0f, 8.12f, 2.0f, 180.0f, 0.0f, 0.0f, 0.36f);
            case WAIST_RIGHT -> new MountPose(-10.0f, 8.12f, 2.0f, 180.0f, 0.0f, 0.0f, 0.36f);
        };
    }

    public record MountPose(float x, float y, float z, float yaw, float pitch, float roll, float scale) {}
}
