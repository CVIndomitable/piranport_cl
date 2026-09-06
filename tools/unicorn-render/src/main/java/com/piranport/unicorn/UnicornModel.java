package com.piranport.unicorn;

import java.util.List;

/**
 * 独角兽的离线几何预览模型。
 *
 * <p>离线坐标采用正 Y 向上的展示空间：脚底约为 y=0，头顶约为 y=30。
 * 视觉结构与游戏端一致，专门用于检查角色剪影、前后层次和装饰挂点。</p>
 */
public class UnicornModel {
    public final ModelPart bipedHead;
    public final ModelPart bipedBody;
    public final ModelPart bipedLegL;
    public final ModelPart bipedLegR;
    public final ModelPart bipedArmL;
    public final ModelPart bipedArmR;
    public final ModelPart root;

    private static final int HAIR = PixelTextureGenerator.Material.HAIR.ordinal();
    private static final int HAIR_FRONT = PixelTextureGenerator.Material.HAIR_FRONT.ordinal();
    private static final int HAIR_HIGHLIGHT = PixelTextureGenerator.Material.HAIR_HIGHLIGHT.ordinal();
    private static final int FACE = PixelTextureGenerator.Material.FACE.ordinal();
    private static final int EYE_DARK = PixelTextureGenerator.Material.EYE_DARK.ordinal();
    private static final int EYE_BLUE = PixelTextureGenerator.Material.EYE_BLUE.ordinal();
    private static final int EYE_WHITE = PixelTextureGenerator.Material.EYE_WHITE.ordinal();
    private static final int BLUSH = PixelTextureGenerator.Material.BLUSH.ordinal();
    private static final int MOUTH = PixelTextureGenerator.Material.MOUTH.ordinal();
    private static final int FLOWER = PixelTextureGenerator.Material.FLOWER.ordinal();
    private static final int FLOWER_PINK = PixelTextureGenerator.Material.FLOWER_PINK.ordinal();
    private static final int FLOWER_YELLOW = PixelTextureGenerator.Material.FLOWER_YELLOW.ordinal();
    private static final int FLOWER_MINT = PixelTextureGenerator.Material.FLOWER_MINT.ordinal();
    private static final int FLOWER_CORAL = PixelTextureGenerator.Material.FLOWER_CORAL.ordinal();
    private static final int DRESS = PixelTextureGenerator.Material.DRESS.ordinal();
    private static final int RIBBON = PixelTextureGenerator.Material.RIBBON.ordinal();
    private static final int STOCKING = PixelTextureGenerator.Material.STOCKING.ordinal();
    private static final int SHOE = PixelTextureGenerator.Material.SHOE.ordinal();
    private static final int METAL = PixelTextureGenerator.Material.METAL.ordinal();
    private static final int GOLD = PixelTextureGenerator.Material.GOLD.ordinal();
    private static final int HARP_RED = PixelTextureGenerator.Material.HARP_RED.ordinal();
    private static final int HARP_BLUE = PixelTextureGenerator.Material.HARP_BLUE.ordinal();
    private static final int BIRD = PixelTextureGenerator.Material.BIRD.ordinal();

    public UnicornModel() {
        this.root = new ModelPart();
        this.bipedBody = root;
        this.bipedHead = node(0.0F, 29.2F, 0.0F);
        this.bipedLegL = node(1.18F, 11.7F, 0.0F);
        this.bipedLegR = node(-1.18F, 11.7F, 0.0F);
        this.bipedArmL = node(3.15F, 20.0F, 0.0F);
        this.bipedArmR = node(-3.15F, 20.0F, 0.0F);

        root.addChild(bipedHead);
        root.addChild(bipedLegL);
        root.addChild(bipedLegR);
        root.addChild(bipedArmL);
        root.addChild(bipedArmR);

        buildBody();
        buildHead();
        buildArms();
        buildLegs();
        buildHarp();
        buildBirds();
    }

    public void collect(List<ModelPart.Quad> out, MatrixStack matrix) {
        root.collect(out, matrix);
    }

    public void resetAll() {
        root.resetPose();
    }

    private void buildBody() {
        addBox(root, DRESS, -2.35F, 11.2F, -1.05F, 4.7F, 9.0F, 2.1F);
        addBox(root, DRESS, -2.0F, 19.75F, -1.28F, 4.0F, 0.85F, 0.3F);
        addBox(root, RIBBON, -1.12F, 12.0F, -1.55F, 2.24F, 6.9F, 0.28F);
        addBox(root, HARP_BLUE, -2.65F, 10.65F, -1.5F, 5.3F, 0.58F, 2.95F);

        for (int i = 0; i < 4; i++) {
            addBox(root, GOLD, -0.15F, 13.0F + i * 1.28F, -1.82F, 0.3F, 0.3F, 0.22F);
        }
        addFlower(root, -1.55F, 11.0F, -1.85F, "pink", 0.46F);
        addFlower(root, 0.0F, 10.96F, -1.87F, "yellow", 0.52F);
        addFlower(root, 1.55F, 11.0F, -1.85F, "mint", 0.46F);

        ModelPart frontDress = node(0.0F, 11.0F, -1.55F);
        frontDress.setRotation(0.035F, 0.0F, 0.0F);
        root.addChild(frontDress);
        addBox(frontDress, DRESS, -1.85F, -4.8F, -0.25F, 3.7F, 4.8F, 0.5F);
        addBox(frontDress, DRESS, -2.65F, -6.65F, -0.3F, 5.3F, 2.45F, 0.62F);
        addBox(frontDress, DRESS, -3.55F, -7.72F, -0.34F, 7.1F, 1.2F, 0.7F);
        addBox(frontDress, RIBBON, -2.35F, -5.55F, -0.42F, 4.7F, 0.34F, 0.66F);
        addBox(frontDress, RIBBON, -3.7F, -7.96F, -0.46F, 7.4F, 0.56F, 0.82F);
        String[] hemColors = {"pink", "yellow", "mint", "coral"};
        float[] hemX = {-2.45F, -0.85F, 0.85F, 2.45F};
        for (int i = 0; i < hemColors.length; i++) {
            addFlower(frontDress, hemX[i], -7.7F + (i % 2) * 0.12F, -0.78F,
                    hemColors[i], 0.5F + (i % 2) * 0.05F);
        }

        ModelPart leftDress = node(-2.18F, 11.0F, -0.05F);
        leftDress.setRotation(0.0F, -0.06F, 0.16F);
        root.addChild(leftDress);
        addBox(leftDress, DRESS, -2.25F, -6.2F, -1.48F, 3.05F, 6.2F, 0.46F);
        addBox(leftDress, DRESS, -3.35F, -7.45F, -1.34F, 4.35F, 1.95F, 0.6F);
        addBox(leftDress, DRESS, -4.0F, -8.1F, -1.16F, 5.25F, 1.0F, 0.72F);
        addBox(leftDress, RIBBON, -3.55F, -8.35F, -1.62F, 4.8F, 0.58F, 0.78F);
        addFlower(leftDress, -2.35F, -2.2F, -1.82F, "yellow", 0.54F);
        addFlower(leftDress, -2.95F, -7.65F, -1.92F, "pink", 0.7F);

        ModelPart rightDress = node(2.18F, 11.0F, -0.05F);
        rightDress.setRotation(0.0F, 0.06F, -0.16F);
        root.addChild(rightDress);
        addBox(rightDress, DRESS, -0.8F, -6.0F, -1.48F, 3.05F, 6.0F, 0.46F);
        addBox(rightDress, DRESS, -0.95F, -7.1F, -1.34F, 4.0F, 1.75F, 0.6F);
        addBox(rightDress, DRESS, -0.95F, -7.75F, -1.16F, 4.9F, 0.95F, 0.72F);
        addBox(rightDress, RIBBON, -0.7F, -8.0F, -1.62F, 4.7F, 0.58F, 0.78F);
        addFlower(rightDress, 2.35F, -2.2F, -1.82F, "mint", 0.54F);
        addFlower(rightDress, 2.9F, -7.25F, -1.92F, "coral", 0.7F);

        ModelPart leftRibbon = node(-3.15F, 11.35F, 1.2F);
        leftRibbon.setRotation(0.08F, 0.0F, 0.2F);
        root.addChild(leftRibbon);
        addRibbon(leftRibbon, 0.0F, -7.4F, 0.0F, 0.72F, 7.4F, -0.08F);
        addRibbon(leftRibbon, -0.28F, -12.3F, 0.0F, 0.54F, 5.1F, 0.14F);

        ModelPart rightRibbon = node(3.15F, 11.35F, 1.2F);
        rightRibbon.setRotation(0.08F, 0.0F, -0.2F);
        root.addChild(rightRibbon);
        addRibbon(rightRibbon, 0.0F, -7.4F, 0.0F, 0.72F, 7.4F, 0.08F);
        addRibbon(rightRibbon, 0.28F, -12.3F, 0.0F, 0.54F, 5.1F, -0.14F);

        ModelPart backDress = node(0.0F, 11.25F, 1.48F);
        backDress.setRotation(-0.06F, 0.0F, 0.0F);
        root.addChild(backDress);
        addBox(backDress, DRESS, -2.9F, -6.7F, -0.18F, 5.8F, 6.7F, 0.42F);
        addBox(backDress, RIBBON, -3.35F, -7.1F, -0.3F, 6.7F, 0.68F, 0.66F);
        addBox(backDress, DRESS, -3.35F, -7.45F, -1.48F, 6.7F, 0.62F, 2.9F);
    }

    private void buildHead() {
        addBox(bipedHead, FACE, -3.25F, -7.2F, -3.28F, 6.5F, 6.9F, 6.55F);
        addBox(bipedHead, FACE, -2.98F, -6.62F, -3.55F, 5.96F, 6.0F, 0.14F);

        addBox(bipedHead, HAIR_HIGHLIGHT, -3.35F, -0.15F, -3.0F, 6.7F, 1.35F, 6.4F);
        addBox(bipedHead, HAIR, -3.35F, -7.45F, 2.45F, 6.7F, 7.0F, 0.82F);
        addBox(bipedHead, HAIR, -3.5F, -7.4F, -2.35F, 0.68F, 7.0F, 4.95F);
        addBox(bipedHead, HAIR, 2.82F, -7.4F, -2.35F, 0.68F, 7.0F, 4.95F);
        addBox(bipedHead, HAIR_FRONT, -3.02F, -7.58F, -3.58F, 6.04F, 0.92F, 0.34F);
        addBox(bipedHead, HAIR_FRONT, -3.45F, -6.78F, -3.5F, 0.46F, 4.0F, 0.34F);
        addBox(bipedHead, HAIR_FRONT, 2.99F, -6.78F, -3.5F, 0.46F, 4.0F, 0.34F);

        float[] eyeX = {-2.08F, 0.62F};
        for (float x : eyeX) {
            addBox(bipedHead, EYE_DARK, x, -4.68F, -3.7F, 1.42F, 1.38F, 0.16F);
            addBox(bipedHead, EYE_BLUE, x + 0.18F, -4.48F, -3.8F, 0.98F, 1.16F, 0.1F);
            addBox(bipedHead, EYE_WHITE, x + 0.32F, -4.28F, -3.87F, 0.28F, 0.3F, 0.08F);
            addBox(bipedHead, EYE_DARK, x - 0.04F, -4.78F, -3.77F, 1.56F, 0.2F, 0.1F);
        }
        addBox(bipedHead, BLUSH, -2.38F, -2.55F, -3.62F, 1.05F, 0.32F, 0.08F);
        addBox(bipedHead, BLUSH, 1.32F, -2.55F, -3.62F, 1.05F, 0.32F, 0.08F);
        addBox(bipedHead, MOUTH, -0.3F, -1.95F, -3.66F, 0.6F, 0.2F, 0.08F);

        float[] bangX = {-2.65F, -1.78F, -0.88F, 0.02F, 0.92F, 1.8F, 2.68F};
        float[] bangLength = {2.1F, 2.85F, 3.55F, 3.9F, 3.45F, 2.75F, 2.05F};
        float[] bangTilt = {0.22F, 0.13F, 0.06F, 0.0F, -0.06F, -0.13F, -0.22F};
        for (int i = 0; i < bangX.length; i++) {
            ModelPart bang = node(bangX[i], -1.18F, -3.72F);
            bang.setRotation(-0.045F, 0.0F, bangTilt[i]);
            bipedHead.addChild(bang);
            addBox(bang, i % 2 == 0 ? HAIR : HAIR_FRONT, -0.38F, -bangLength[i], -0.2F,
                    0.76F, bangLength[i], 0.44F);
        }

        ModelPart leftLock = node(-3.48F, -1.8F, -2.9F);
        leftLock.setRotation(0.04F, 0.0F, 0.1F);
        bipedHead.addChild(leftLock);
        addDownHairSegment(leftLock, -0.05F, 0.0F, 0.0F, 1.0F, 5.2F, 0.76F, 0.1F, HAIR_FRONT);
        addDownHairSegment(leftLock, 0.65F, -4.55F, 0.18F, 0.96F, 5.8F, 0.78F, 0.22F, HAIR);
        ModelPart rightLock = node(3.48F, -1.8F, -2.9F);
        rightLock.setRotation(0.04F, 0.0F, -0.1F);
        bipedHead.addChild(rightLock);
        addDownHairSegment(rightLock, 0.05F, 0.0F, 0.0F, 1.0F, 4.9F, 0.76F, -0.1F, HAIR_FRONT);
        addDownHairSegment(rightLock, -0.65F, -4.25F, 0.18F, 0.9F, 5.2F, 0.74F, -0.2F, HAIR);

        ModelPart wreath = node(0.0F, 0.0F, 0.0F);
        bipedHead.addChild(wreath);
        addBox(wreath, HARP_BLUE, -3.15F, -0.05F, -3.0F, 6.3F, 0.26F, 0.34F);
        addFlower(wreath, -2.5F, 0.18F, -3.18F, "pink", 0.82F);
        addFlower(wreath, -1.2F, 0.03F, -3.2F, "mint", 0.62F);
        addFlower(wreath, 0.05F, -0.06F, -3.22F, "yellow", 0.74F);
        addFlower(wreath, 1.3F, 0.04F, -3.19F, "coral", 0.62F);
        addFlower(wreath, 2.52F, 0.18F, -3.16F, "pink", 0.82F);

        ModelPart hairMotion = node(0.0F, 0.0F, 0.0F);
        bipedHead.addChild(hairMotion);
        ModelPart leftHair = node(-0.02F, -5.85F, 0.0F);
        leftHair.setRotation(0.035F, -0.02F, 0.0F);
        hairMotion.addChild(leftHair);
        ModelPart centerHair = node(0.0F, -5.8F, 0.0F);
        centerHair.setRotation(0.045F, 0.0F, 0.0F);
        hairMotion.addChild(centerHair);
        ModelPart rightHair = node(0.02F, -5.85F, 0.0F);
        rightHair.setRotation(0.035F, 0.02F, 0.0F);
        hairMotion.addChild(rightHair);

        addDownHairSegment(leftHair, -3.15F, 0.0F, -0.82F, 1.3F, 9.2F, 0.96F, 0.16F, HAIR_HIGHLIGHT);
        addDownHairSegment(leftHair, -2.18F, -0.16F, -0.94F, 1.55F, 11.0F, 1.08F, 0.1F, HAIR);
        addDownHairSegment(leftHair, -1.12F, -0.3F, -1.02F, 1.24F, 10.4F, 0.9F, 0.04F, HAIR_FRONT);
        addDownHairSegment(centerHair, -0.55F, 0.0F, -0.75F, 0.92F, 6.6F, 0.72F, 0.02F, HAIR);
        addDownHairSegment(centerHair, 0.35F, -0.1F, -0.7F, 0.96F, 7.0F, 0.76F, -0.02F, HAIR_HIGHLIGHT);
        addDownHairSegment(rightHair, 1.3F, -0.28F, -0.72F, 1.08F, 7.1F, 0.82F, -0.05F, HAIR_FRONT);
        addDownHairSegment(rightHair, 2.28F, -0.14F, -0.68F, 1.32F, 8.2F, 0.94F, -0.12F, HAIR);

        ModelPart backHair = node(0.0F, 23.1F, 1.55F);
        root.addChild(backHair);
        addBackHairPanel(backHair, -3.15F, 0.0F, 0.0F, 1.25F, 13.4F, 0.16F, HAIR);
        addBackHairPanel(backHair, -2.0F, 0.0F, 0.16F, 1.4F, 16.0F, 0.09F, HAIR_HIGHLIGHT);
        addBackHairPanel(backHair, -0.55F, 0.0F, 0.24F, 1.65F, 17.2F, 0.0F, HAIR);
        addBackHairPanel(backHair, 1.05F, 0.0F, 0.16F, 1.4F, 16.0F, -0.09F, HAIR_HIGHLIGHT);
        addBackHairPanel(backHair, 2.35F, 0.0F, 0.0F, 1.25F, 13.4F, -0.16F, HAIR);

        ModelPart leftSideFlow = node(-3.55F, 22.85F, -1.05F);
        leftSideFlow.setRotation(0.035F, 0.0F, 0.08F);
        root.addChild(leftSideFlow);
        addDownHairSegment(leftSideFlow, 0.0F, 0.0F, 0.0F, 1.3F, 8.2F, 1.0F, 0.12F, HAIR);
        addDownHairSegment(leftSideFlow, -0.45F, -0.2F, 0.1F, 1.55F, 9.2F, 1.12F, 0.22F, HAIR_HIGHLIGHT);
        addDownHairSegment(leftSideFlow, -0.88F, -0.36F, 0.16F, 1.1F, 6.5F, 0.92F, 0.32F, HAIR_FRONT);
        ModelPart rightSideFlow = node(3.45F, 22.85F, -1.0F);
        rightSideFlow.setRotation(0.035F, 0.0F, -0.08F);
        root.addChild(rightSideFlow);
        addDownHairSegment(rightSideFlow, 0.0F, 0.0F, 0.0F, 1.25F, 7.5F, 0.98F, -0.1F, HAIR);
        addDownHairSegment(rightSideFlow, 0.42F, -0.2F, 0.1F, 1.35F, 8.2F, 1.02F, -0.2F, HAIR_HIGHLIGHT);
    }

    private void buildArms() {
        buildArm(bipedArmL, false);
        buildArm(bipedArmR, true);
    }

    private void buildArm(ModelPart arm, boolean right) {
        arm.setRotation(0.0F, 0.0F, right ? 0.42F : 0.56F);
        addBox(arm, FACE, -0.68F, -4.6F, -0.82F, 1.36F, 4.7F, 1.64F);
        addBox(arm, DRESS, -0.82F, -1.35F, -0.96F, 1.64F, 1.12F, 1.92F);
        ModelPart forearm = node(0.0F, -4.35F, 0.0F);
        forearm.setRotation(-0.12F, 0.0F, right ? 0.62F : 0.72F);
        arm.addChild(forearm);
        addBox(forearm, FACE, -0.64F, -3.75F, -0.78F, 1.28F, 3.8F, 1.56F);
        addBox(forearm, DRESS, -0.78F, -3.28F, -0.92F, 1.56F, 3.1F, 1.84F);
        addFlower(forearm, right ? -0.76F : 0.76F, -0.9F, -1.1F, right ? "yellow" : "mint", 0.38F);
    }

    private void buildLegs() {
        buildLeg(bipedLegL, false);
        buildLeg(bipedLegR, true);
    }

    private void buildLeg(ModelPart leg, boolean right) {
        leg.setRotation(0.0F, 0.0F, right ? -0.16F : 0.12F);
        addBox(leg, FACE, -0.9F, -9.5F, -2.05F, 1.8F, 9.2F, 1.92F);
        addBox(leg, STOCKING, -0.94F, -11.7F, -2.08F, 1.88F, 3.2F, 2.0F);
        for (int i = 0; i < 4; i++) {
            addBox(leg, i % 2 == 0 ? RIBBON : HARP_BLUE, -1.02F, -10.9F + i * 1.75F,
                    -2.22F, 2.04F, 0.22F, 0.24F);
        }
        addBox(leg, SHOE, -1.16F, -11.7F, -2.4F, 2.32F, 2.8F, 2.75F);
        addBox(leg, RIBBON, -1.25F, -9.15F, -2.35F, 2.5F, 0.52F, 2.64F);
        addBox(leg, HARP_BLUE, -0.78F, -10.28F, -2.55F, 1.56F, 0.32F, 0.42F);
        addFlower(leg, right ? -0.98F : 0.98F, -9.0F, -2.3F, right ? "coral" : "pink", 0.46F);
    }

    private void buildHarp() {
        ModelPart harp = node(5.25F, 14.1F, -2.0F);
        harp.setRotation(0.02F, -0.08F, -0.05F);
        root.addChild(harp);
        addBox(harp, HARP_BLUE, -0.42F, 0.0F, -0.48F, 0.84F, 14.2F, 0.96F);
        addBox(harp, METAL, -0.56F, -0.18F, -0.58F, 1.12F, 0.78F, 1.16F);
        addBox(harp, GOLD, -0.55F, 13.55F, -0.58F, 1.1F, 0.85F, 1.16F);
        addBeam(harp, -0.05F, 1.35F, 0.0F, 4.25F, -0.14F);
        addBeam(harp, -2.7F, 3.25F, 0.0F, 3.15F, -0.38F);
        addBeam(harp, -4.9F, 5.75F, 0.0F, 2.35F, -0.72F);
        addBeam(harp, -5.85F, 8.25F, 0.0F, 1.55F, -1.02F);

        ModelPart base = node(-5.85F, 0.8F, 0.0F);
        base.setRotation(0.0F, 0.0F, 0.22F);
        harp.addChild(base);
        addBox(base, HARP_RED, -0.25F, -0.66F, -0.55F, 6.35F, 1.32F, 1.1F);
        addBox(base, METAL, -0.4F, -0.86F, -0.65F, 6.6F, 0.26F, 1.3F);
        addBox(base, HARP_BLUE, 0.95F, 0.42F, -0.6F, 5.0F, 0.9F, 1.2F);

        int[] stringMaterials = {GOLD, FLOWER_PINK, HARP_BLUE, FLOWER_YELLOW, GOLD, FLOWER_MINT, HARP_BLUE, FLOWER_CORAL};
        for (int i = 0; i < stringMaterials.length; i++) {
            ModelPart string = node(-5.35F + i * 0.72F, 1.45F + i * 0.5F, -0.08F);
            string.setRotation(0.0F, 0.0F, -0.018F);
            harp.addChild(string);
            addBox(string, stringMaterials[i], -0.065F, 0.0F, -0.1F,
                    0.13F, 7.8F + i * 0.42F, 0.2F);
        }

        ModelPart ornament = node(0.0F, 14.0F, 0.0F);
        ornament.setRotation(0.0F, 0.0F, -0.08F);
        harp.addChild(ornament);
        addBox(ornament, DRESS, -0.34F, -0.6F, -0.5F, 1.5F, 1.2F, 1.0F);
        addBox(ornament, GOLD, 0.95F, -0.28F, -0.38F, 0.75F, 0.64F, 0.76F);
        ModelPart horn = node(1.0F, 0.0F, -0.16F);
        horn.setRotation(0.0F, 0.0F, 0.5F);
        ornament.addChild(horn);
        addBox(horn, GOLD, -0.1F, 0.0F, -0.1F, 0.2F, 1.6F, 0.2F);
        for (int i = 0; i < 4; i++) {
            ModelPart wing = node(-0.15F, 0.0F + i * 0.3F, 0.0F);
            wing.setRotation(0.0F, 0.0F, -0.35F - i * 0.15F);
            ornament.addChild(wing);
            addBox(wing, HARP_BLUE, -1.55F - i * 0.18F, -0.16F, -0.32F,
                    1.7F + i * 0.12F, 0.32F, 0.64F);
        }
    }

    private void buildBirds() {
        addBird(-7.1F, 24.0F, -1.65F, 1.0F);
        addBird(8.1F, 22.8F, -1.7F, -1.0F);
    }

    private void addBird(float x, float y, float z, float facing) {
        ModelPart bird = node(x, y, z);
        bird.setRotation(0.0F, facing > 0 ? -0.2F : 0.2F, 0.0F);
        root.addChild(bird);
        addBox(bird, BIRD, -0.68F, -0.32F, -0.36F, 1.36F, 0.64F, 0.72F);
        addBox(bird, FLOWER_MINT, facing > 0 ? 0.3F : -0.78F, -0.52F, -0.3F,
                0.5F, 0.48F, 0.6F);
        addBox(bird, GOLD, facing > 0 ? 0.78F : -1.38F, -0.34F, -0.1F,
                0.6F, 0.14F, 0.2F);
        ModelPart wings = node(0.0F, 0.0F, 0.0F);
        bird.addChild(wings);
        addBox(wings, HAIR_HIGHLIGHT, -0.16F, -0.12F, -1.55F, 0.58F, 0.24F, 1.45F);
        addBox(wings, HAIR_HIGHLIGHT, -0.16F, -0.12F, 0.1F, 0.58F, 0.24F, 1.45F);
    }

    private static void addDownHairSegment(ModelPart parent, float x, float y, float z,
                                            float width, float length, float depth, float zRot, int material) {
        ModelPart segment = node(x, y, z);
        segment.setRotation(-0.02F, 0.0F, zRot);
        parent.addChild(segment);
        addBox(segment, material, -width * 0.5F, -length, -depth * 0.5F, width, length, depth);
        ModelPart tip = node(0.0F, -length + 0.18F, 0.0F);
        tip.setRotation(-0.05F, 0.0F, zRot * 0.45F);
        segment.addChild(tip);
        addBox(tip, material == HAIR_HIGHLIGHT ? HAIR : HAIR_HIGHLIGHT,
                -width * 0.38F, -length * 0.7F, -depth * 0.42F,
                width * 0.76F, length * 0.7F, depth * 0.84F);
    }

    private static void addBackHairPanel(ModelPart parent, float x, float y, float z,
                                         float width, float length, float zRot, int material) {
        ModelPart panel = node(x, y, z);
        panel.setRotation(-0.025F, 0.0F, zRot);
        parent.addChild(panel);
        addBox(panel, material, -width * 0.5F, -length, -0.38F, width, length, 0.76F);
        ModelPart tip = node(0.0F, -length + 0.25F, 0.0F);
        tip.setRotation(-0.06F, 0.0F, zRot * 0.45F);
        panel.addChild(tip);
        float tipLength = Math.max(3.0F, length * 0.38F);
        addBox(tip, material == HAIR ? HAIR_FRONT : HAIR,
                -width * 0.38F, -tipLength, -0.31F, width * 0.76F, tipLength, 0.62F);
    }

    private static void addFlower(ModelPart parent, float x, float y, float z,
                                  String color, float scale) {
        ModelPart flower = node(x, y, z);
        parent.addChild(flower);
        int petalMaterial = switch (color) {
            case "pink" -> FLOWER_PINK;
            case "yellow" -> FLOWER_YELLOW;
            case "mint" -> FLOWER_MINT;
            case "coral" -> FLOWER_CORAL;
            default -> FLOWER;
        };
        for (int i = 0; i < 4; i++) {
            ModelPart petal = node(0.0F, 0.0F, 0.0F);
            petal.setRotation(0.0F, 0.0F, i * (float) (Math.PI / 2.0));
            flower.addChild(petal);
            addBox(petal, petalMaterial, -0.58F * scale, -0.16F * scale, -0.15F,
                    1.16F * scale, 0.32F * scale, 0.3F);
        }
        addBox(flower, GOLD, -0.19F * scale, -0.19F * scale, -0.22F,
                0.38F * scale, 0.38F * scale, 0.44F);
    }

    private static void addRibbon(ModelPart parent, float x, float y, float z,
                                  float width, float length, float zRot) {
        ModelPart ribbon = node(x, y, z);
        ribbon.setRotation(-0.04F, 0.0F, zRot);
        parent.addChild(ribbon);
        addBox(ribbon, RIBBON, -width * 0.5F, 0.0F, -0.16F, width, length, 0.32F);
    }

    private static void addBeam(ModelPart parent, float x, float y, float z,
                                float length, float zRot) {
        ModelPart beam = node(x, y, z);
        beam.setRotation(0.0F, 0.0F, zRot);
        parent.addChild(beam);
        addBox(beam, HARP_BLUE, -length, -0.34F, -0.42F, length, 0.68F, 0.84F);
        addBox(beam, METAL, -length + 0.12F, -0.42F, -0.48F,
                length - 0.12F, 0.18F, 0.96F);
    }

    private static ModelPart node(float x, float y, float z) {
        return new ModelPart().setPos(x, y, z);
    }

    private static void addBox(ModelPart parent, int material,
                               float x, float y, float z,
                               float width, float height, float depth) {
        ModelPart cube = new ModelPart();
        cube.setMaterial(material);
        cube.addBox(x, y, z, width, height, depth, 0.0F);
        parent.addChild(cube);
    }
}
