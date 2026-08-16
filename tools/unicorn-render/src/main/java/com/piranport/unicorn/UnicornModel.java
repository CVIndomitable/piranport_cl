package com.piranport.unicorn;

import java.util.List;

/**
 * 战舰少女R 独角兽 (CV-8) box 模型 —— YSM 标准 A-pose, 带 material.
 *
 * 比例: 8 头身, 总高 32 (= MC 玩家标准).
 *   y=0     脚底
 *   y=12    腰
 *   y=20    颈底
 *   y=22    颈顶 (head pivot)
 *   y=30    头顶
 *   y=32    角尖
 */
public class UnicornModel {

    public final ModelPart bipedHead;
    public final ModelPart bipedBody;
    public final ModelPart bipedLegL;
    public final ModelPart bipedLegR;
    public final ModelPart bipedArmL;
    public final ModelPart bipedArmR;
    public ModelPart root;

    public UnicornModel() {
        // ============== head ==============
        bipedHead = new ModelPart();
        bipedHead.setPos(0, 22, 0);

        // 头主体 (face 贴图: 肤色 + 红瞳)
        bipedHead.addChild(faceBox(-4, -8, -4, 8, 8, 8, true));

        // 后发
        bipedHead.addChild(hairBox(-4, -8, 1, 8, 8, 2));
        bipedHead.addChild(hairBox(-3.5f, -10, 1, 7, 2, 2));
        // 刘海
        bipedHead.addChild(hairBox(1, -4, -4.2f, 4, 4, 0.3f));
        // 耳侧发
        bipedHead.addChild(hairBox(-4.4f, -7, -3.5f, 1, 5, 5));
        bipedHead.addChild(hairBox(3.4f, -7, -3.5f, 1, 5, 5));

        // 独角
        bipedHead.addChild(hornBox(-1, 0, -4.5f, 2, 1.5f, 2));
        bipedHead.addChild(hornBox(-0.5f, 1.5f, -4, 1, 4, 1));
        bipedHead.addChild(hornBox(-0.3f, 5, -3.7f, 0.6f, 1, 0.6f));

        // 花环
        bipedHead.addChild(flowerBox(-5, -3, -3, 1, 1.5f, 6));
        bipedHead.addChild(flowerBox(-4, -2, -4, 8, 1.5f, 8));
        bipedHead.addChild(flowerBox(4, -3, -3, 1, 1.5f, 6));

        // 双马尾
        bipedHead.addChild(hairBox(3.5f, -8, 1, 2, 4, 4));
        bipedHead.addChild(hairBox(4, -12, 2, 2, 4, 3.5f));
        bipedHead.addChild(hairBox(4.5f, -16, 2.5f, 2, 5, 3));
        bipedHead.addChild(hairBox(5, -21, 3, 2, 5, 3));
        bipedHead.addChild(hairBox(-5.5f, -8, 1, 2, 4, 4));
        bipedHead.addChild(hairBox(-6, -12, 2, 2, 4, 3.5f));
        bipedHead.addChild(hairBox(-6.5f, -16, 2.5f, 2, 5, 3));
        bipedHead.addChild(hairBox(-7, -21, 3, 2, 5, 3));

        // ============== body ==============
        bipedBody = new ModelPart();
        bipedBody.setPos(0, 12, 0);

        // 上胸腔 (皮肤)
        bipedBody.addChild(skinBox(-4, -8, -2.5f, 8, 8, 5));
        // 胸凸
        bipedBody.addChild(skinBox(-4, -7, -3, 8, 5, 1));
        // 腰
        bipedBody.addChild(skinBox(-3, -1, -2, 6, 2, 4));

        // 蓝色缎带 (蝴蝶结 + 双尾)
        bipedBody.addChild(ribbonBox(-1.5f, -6, -3.2f, 3, 1.5f, 0.3f));
        bipedBody.addChild(ribbonBox(-0.5f, -7.5f, -3.2f, 1, 2, 0.3f));
        bipedBody.addChild(ribbonBox(-0.5f, -9, -3.2f, 1, 2, 0.3f));

        // 连衣裙
        bipedBody.addChild(dressBox(-4, -2, -3, 8, 2, 6));
        bipedBody.addChild(dressBox(-6, 0, -3.5f, 12, 8, 7));
        bipedBody.addChild(dressBox(-5, 1, -2.5f, 10, 6, 5));
        bipedBody.addChild(dressBox(-6.5f, -0.5f, -4, 13, 1.5f, 8));

        // ============== 双腿 ==============
        bipedLegL = new ModelPart();
        bipedLegL.setPos(2, 12, 0);
        bipedLegL.setMaterial(PixelTextureGenerator.Material.STOCKING.ordinal());
        bipedLegL.addChild(box(-2, -12, -2, 4, 12, 4, 0));

        bipedLegR = new ModelPart();
        bipedLegR.setPos(-2, 12, 0);
        bipedLegR.setMaterial(PixelTextureGenerator.Material.STOCKING.ordinal());
        bipedLegR.addChild(box(-2, -12, -2, 4, 12, 4, 0));

        // ============== 双臂 (皮肤) ==============
        bipedArmL = new ModelPart();
        bipedArmL.setPos(6, 20, 0);
        bipedArmL.setMaterial(PixelTextureGenerator.Material.SKIN.ordinal());
        bipedArmL.addChild(box(-1, -10, -1, 2, 10, 2, 0));

        bipedArmR = new ModelPart();
        bipedArmR.setPos(-6, 20, 0);
        bipedArmR.setMaterial(PixelTextureGenerator.Material.SKIN.ordinal());
        bipedArmR.addChild(box(-1, -10, -1, 2, 10, 2, 0));

        // 挂载
        bipedBody.addChild(bipedHead);
        bipedBody.addChild(bipedLegL);
        bipedBody.addChild(bipedLegR);
        bipedBody.addChild(bipedArmL);
        bipedBody.addChild(bipedArmR);

        root = bipedBody;
    }

    public void collect(List<ModelPart.Quad> out, MatrixStack m) {
        bipedBody.collect(out, m);
    }

    public void resetAll() {
        bipedBody.resetPose();
    }

    // ===== box helpers (带 material) =====
    private static ModelPart box(float xOff, float yOff, float zOff,
                                 float w, float h, float d, float inflate) {
        ModelPart p = new ModelPart();
        p.addBox(xOff, yOff, zOff, w, h, d, inflate);
        return p;
    }

    private static ModelPart hairBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.HAIR.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart faceBox(float x, float y, float z, float w, float h, float d, boolean left) {
        ModelPart p = new ModelPart();
        p.setMaterial(left ? PixelTextureGenerator.Material.FACE_L.ordinal()
                           : PixelTextureGenerator.Material.FACE_R.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart hornBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.HORN.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart flowerBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.FLOWER.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart skinBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.SKIN.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart dressBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.DRESS.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }

    private static ModelPart ribbonBox(float x, float y, float z, float w, float h, float d) {
        ModelPart p = new ModelPart();
        p.setMaterial(PixelTextureGenerator.Material.RIBBON.ordinal());
        p.addBox(x, y, z, w, h, d, 0);
        return p;
    }
}