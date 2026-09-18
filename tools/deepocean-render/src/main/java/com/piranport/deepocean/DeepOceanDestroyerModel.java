package com.piranport.deepocean;

import static com.piranport.deepocean.ModelPart.*;

/**
 * 深海驱逐 (No.901 敌驱逐舰) 实体模型 —— 按《YSM 舰娘模型美术风格规范》重建.
 *
 * 立绘依据: 立绘/No901_敌驱逐舰.png
 *
 * ── 骨架 (单位 = MC 像素, 根节点在脚底; root 偏移后 y 向上为负) ──
 *     头顶      y = -26.0
 *     下巴      y = -16.8
 *     肩        y = -15.6
 *     胸        y = -13.0
 *     腰        y = -11.0
 *     胯        y = -8.6
 *     膝        y = -4.4
 *     脚底      y =  0.0
 *   总高 26, 头高 4.7 → 5.5 头身 (规范 §3.1 推荐 5~6 头身).
 *
 * ── 识别锚点 → 三维对应 (规范 §10) ──
 *   头/发  : 银灰长发 —— 头顶 + 刘海 + 后发 + 两侧长束 (规范 §6.2 三级发束)
 *   脸     : 右眼粉色机械眼, 左眼黑色护目镜, 口鼻黑色带齿面罩
 *   躯干   : 白皮肤 + 黑白色胸甲
 *   腿     : 黑色六边形鳞甲束
 *   舰装核心: 蓝灰「黑鱼」(鲸) 盘绕左肩 —— 弧形身体 + 分叉尾鳍 +
 *             前端机械炮舱 + 粉色电路炮管 + 胸鳍
 *   身后   : 倾斜黑盾 + 粉色电路纹路, 环绕人物但不压过头部 (规范 §8.1)
 *   两侧   : 炮舱 + 圆舱 + 挂架, 从腰侧后方展开
 *   环绕   : 带粉色发光眼的黑色舱体
 *
 * ── 视觉优先级 (规范 §2) ──
 *   头/脸/发 > 服装主色块 > 舰装轮廓 > 饰品 > 小机械.
 *   舰装不得遮挡脸与躯干中心 (规范 §12.4).
 *
 * UV 布局见 tools/deepocean_uv_layout.py —— 与贴图生成器共用一份定义.
 */
public class DeepOceanDestroyerModel {

    public final ModelPart root = new ModelPart();

    // ---- 人物 ----
    public final ModelPart body = new ModelPart();
    public final ModelPart hips = new ModelPart();
    public final ModelPart head = new ModelPart();
    public final ModelPart hair = new ModelPart();
    public final ModelPart leftArm = new ModelPart();
    public final ModelPart rightArm = new ModelPart();
    public final ModelPart leftLeg = new ModelPart();
    public final ModelPart rightLeg = new ModelPart();

    // ---- 发光件 ----
    public final ModelPart chestCore = new ModelPart();
    public final ModelPart faceGlow = new ModelPart();

    // ---- 舰装 ----
    public final ModelPart blackFish = new ModelPart();
    public final ModelPart fishMount = new ModelPart();
    public final ModelPart tailStem = new ModelPart();
    public final ModelPart shield = new ModelPart();
    public final ModelPart shieldCircuit = new ModelPart();
    public final ModelPart leftRigging = new ModelPart();
    public final ModelPart rightRigging = new ModelPart();
    public final ModelPart glowPods = new ModelPart();

    // ---- 骨架常量 (见类注释) ----
    private static final float Y_HEAD   = -21.2f;  // 头 pivot (头中心偏高)
    private static final float Y_SHOULDER = -15.6f;
    private static final float Y_CHEST  = -13.0f;
    private static final float Y_WAIST  = -11.0f;
    private static final float Y_HIP    = -8.6f;
    private static final float Y_KNEE   = -4.4f;
    private static final float ARM_OUT = 0.14f;

    public DeepOceanDestroyerModel() {
        root.setPos(0, 24, 0);

        buildBody();
        buildHead();
        buildArms();
        buildLegs();
        buildFish();
        buildShield();
        buildSideRigging();
        buildGlowPods();

        for (ModelPart p : new ModelPart[]{
                body, hips, head, hair, leftArm, rightArm, leftLeg, rightLeg,
                chestCore, faceGlow,
                blackFish, shield, shieldCircuit, leftRigging, rightRigging, glowPods}) {
            root.addChild(p);
        }
    }

    // ================================================================ 人物
    private void buildBody() {
        // 躯干: 肩到胯 7.0 高, 收腰 (规范 §3.1)
        body.setPos(0, Y_SHOULDER, 0);
        addBox(body, -2.4f, 0f, -1.4f, 4.8f, 7.0f, 2.8f, 0, 0, 0);   // 躯干主体
        // 胸口粉色核心 (发光件, 贴在胸甲上)
        chestCore.setPos(0, Y_CHEST, 0);
        addBox(chestCore, -1f, -1f, -1.7f, 2f, 2f, 1f, 0, 64, 0);

        // 胯 / 短裤 (胯到腿根)
        hips.setPos(0, Y_HIP, 0);
        addBox(hips, -2.2f, 0f, -1.3f, 4.4f, 2.2f, 2.6f, 0, 18, 6);
    }

    private void buildHead() {
        head.setPos(0, Y_HEAD, 0);
        // 头 4.8 x 4.7 x 4.2 (pivot 在颈, 头顶 y=-4.8)
        addBox(head, -2.4f, -4.7f, -2.1f, 4.8f, 4.7f, 4.2f, 0, 0, 24);
        // 下巴 (薄片)
        addBox(head, -1.9f, 0f, -1.9f, 3.8f, 0.6f, 1.6f, 0, 0, 36);

        // 银灰长发: 头顶 / 刘海 / 后发 / 两侧长束
        hair.setPos(0, Y_HEAD, 0);
        addBox(hair, -2.55f, -5.1f, -2.2f, 5.1f, 0.9f, 4.4f, 0, 20, 24);  // 头顶
        addBox(hair, -1.9f, -4.4f, -2.6f, 3.8f, 3.4f, 0.8f, 0, 20, 32);   // 刘海
        addBox(hair, -2.3f, -4.6f, 1.6f, 4.6f, 5.6f, 1.4f, 0, 40, 24);    // 后发
        addBox(hair, 2.2f, -4.6f, -1.6f, 1.6f, 5.6f, 3.2f, 0, 56, 24);    // 左侧长束
        addBox(hair, -3.8f, -4.6f, -1.6f, 1.6f, 5.6f, 3.2f, 0, 56, 36);   // 右侧长束

        // 面部发光: 粉色眼带 + 护目镜
        faceGlow.setPos(0, Y_HEAD, 0);
        addBox(faceGlow, -1.5f, -3.4f, -2.55f, 3f, 1.1f, 0.6f, 0, 64, 8);  // 眼带
        addBox(faceGlow, 1.5f, -3.9f, -2.2f, 0.9f, 1.6f, 0.5f, 0, 74, 8);  // 护目镜灯
        addBox(faceGlow, -2.4f, -3.9f, -2.2f, 0.9f, 1.6f, 0.5f, 0, 78, 8); // 机械眼灯
    }

    private void buildArms() {
        // 细长手臂: 肩(y=-15.6) 到手 (y=-8.6 附近)
        leftArm.setPos(-2.7f, Y_SHOULDER, 0);
        addBox(leftArm, -1.9f, 0f, -1f, 1.9f, 3.8f, 2f, 0, 0, 42);    // 上臂
        addBox(leftArm, -1.9f, 3.8f, -0.9f, 1.9f, 3.2f, 1.8f, 0, 0, 50); // 前臂+手套
        leftArm.setRotation(0f, 0f, -ARM_OUT);

        rightArm.setPos(2.7f, Y_SHOULDER, 0);
        addBox(rightArm, 0f, 0f, -1f, 1.9f, 3.8f, 2f, 0, 10, 42);
        addBox(rightArm, 0f, 3.8f, -0.9f, 1.9f, 3.2f, 1.8f, 0, 10, 50);
        rightArm.setRotation(0f, 0f, ARM_OUT);
    }

    private void buildLegs() {
        // 腿: 胯(y=-8.6) 到脚底(y=0), 大腿 4.2 + 小腿 4.4
        leftLeg.setPos(-1.15f, Y_HIP, 0);
        addBox(leftLeg, -1.05f, 0f, -1.05f, 2.1f, 4.2f, 2.1f, 0, 20, 42);   // 大腿
        addBox(leftLeg, -1.0f, 4.2f, -1.0f, 2.0f, 4.4f, 2.0f, 0, 20, 50);   // 小腿

        rightLeg.setPos(1.15f, Y_HIP, 0);
        addBox(rightLeg, -1.05f, 0f, -1.05f, 2.1f, 4.2f, 2.1f, 0, 30, 42);
        addBox(rightLeg, -1.0f, 4.2f, -1.0f, 2.0f, 4.4f, 2.0f, 0, 30, 50);
    }

    // ================================================================ 舰装
    private void buildFish() {
        // 黑鱼 (鲸) 盘绕左肩: 鲸体在角色左侧, 前端(头)朝前下方.
        // 规范 §8.1: 舰装中心应在腰至肩之间, 中央留空不挡躯干.
        blackFish.setPos(-4.2f, -14.6f, 0.2f);
        addBox(blackFish, -1.5f, -2.6f, -1.1f, 3f, 5.2f, 2.2f, 0, 96, 0);   // 鱼身
        addBox(blackFish, -2.6f, 1.6f, -1.1f, 3f, 2f, 2.2f, 0, 96, 9);      // 胸鳍
        blackFish.setRotation(0.10f, 0.36f, -0.30f);

        // 前端机械炮舱 (挂在鱼身前下方)
        fishMount.setPos(0f, 3.4f, -0.4f);
        addBox(fishMount, -2f, 0f, -1.5f, 4f, 3f, 3f, 0, 108, 0);
        addBox(fishMount, -0.5f, 1f, -6.4f, 1f, 1f, 5f, 0, 108, 8);          // 炮管
        blackFish.addChild(fishMount);

        // 分叉尾鳍 (鱼身上方, 立绘左上)
        tailStem.setPos(-5.2f, -19.2f, 1.2f);
        addBox(tailStem, -0.5f, -4f, -0.5f, 1f, 4f, 1f, 0, 96, 16);
        addBox(tailStem, -3f, -5.6f, -0.5f, 6f, 2f, 1f, 0, 102, 16);         // 尾鳍
        tailStem.setRotation(0f, 0f, 0.34f);
    }

    private void buildShield() {
        // 倾斜黑盾: 从腰后展开环绕. 宽 8 / 高 8 —— 不压过头部 (规范 §8.1),
        // 中央留空, 分四块板错层 (规范 §4.3).
        shield.setPos(0.2f, -10.5f, 2.5f);
        backPlate(shield, -4.0f, -4.0f, 8f, 3.5f, -0.10f, 0, 64);   // 上板
        backPlate(shield, -4.0f, -0.5f, 3f, 7f, 0.34f, 18, 64);     // 左板
        backPlate(shield, 1.0f, -0.5f, 3f, 7f, -0.34f, 28, 64);     // 右板
        backPlate(shield, -3.0f, 6.5f, 6f, 3f, 0.16f, 38, 64);      // 下板

        ModelPart inner = new ModelPart();
        addBox(inner, -3f, -3f, -0.5f, 6f, 6f, 1f, 0, 0, 72);
        shield.addChild(inner);

        // 盾内粉色电路 (辐条 + 弧)
        shieldCircuit.setPos(0.2f, -10.5f, 2.32f);
        for (int a : new int[]{0, 34, -34, 68, -68}) {
            ModelPart spoke = new ModelPart();
            addBox(spoke, -0.5f, -3f, -0.5f, 1f, 3f, 1f, 0, 52, 64);
            spoke.setRotation(0f, 0f, (float) Math.toRadians(a));
            shieldCircuit.addChild(spoke);
        }
        ModelPart arcA = new ModelPart();
        addBox(arcA, -3f, 1f, -0.5f, 6f, 1f, 1f, 0, 52, 72);
        arcA.setRotation(0f, 0f, 0.12f);
        shieldCircuit.addChild(arcA);
        ModelPart arcB = new ModelPart();
        addBox(arcB, -3.5f, -1f, -0.5f, 7f, 1f, 1f, 0, 52, 76);
        shieldCircuit.addChild(arcB);
    }

    private static void backPlate(ModelPart parent, float x, float y, float w, float h,
                                  float zRot, int texU, int texV) {
        ModelPart p = new ModelPart();
        p.setPos(x, y, 0f);
        addBox(p, 0f, 0f, 0f, w, h, 1f, 0, texU, texV);
        p.setRotation(0f, 0f, zRot);
        parent.addChild(p);
    }

    private void buildSideRigging() {
        // 两侧炮舱: 从腰侧后方展开, 向外变细 (规范 §8.2)
        leftRigging.setPos(-3.0f, -10.0f, 0.6f);
        addBox(leftRigging, -3.4f, -1f, -1f, 3.4f, 2f, 2f, 0, 0, 80);    // 炮舱
        addBox(leftRigging, -7f, -0.5f, -0.5f, 3.6f, 1f, 1f, 0, 12, 80); // 炮管
        addBox(leftRigging, 0f, -1.5f, -1f, 3f, 3f, 2f, 0, 22, 80);      // 圆舱
        addBox(leftRigging, -3.4f, -1.5f, 0.6f, 3.4f, 1f, 3f, 0, 0, 92); // 挂架
        leftRigging.setRotation(0f, 0.36f, -0.05f);

        rightRigging.setPos(3.0f, -10.0f, 0.6f);
        addBox(rightRigging, 0f, -1f, -1f, 3.4f, 2f, 2f, 0, 0, 100);
        addBox(rightRigging, 3.4f, -0.5f, -0.5f, 3.6f, 1f, 1f, 0, 12, 100);
        addBox(rightRigging, -3f, -1.5f, -1f, 3f, 3f, 2f, 0, 22, 100);
        addBox(rightRigging, 0f, -1.5f, 0.6f, 3.4f, 1f, 3f, 0, 0, 112);
        rightRigging.setRotation(0f, -0.36f, 0.05f);
    }

    private void buildGlowPods() {
        // 环绕身体的黑舱 + 粉色发光眼 —— 立绘中贴身的数个舱体.
        // 贴身但不侵入躯干中心 (规范 §8.1).
        glowPods.setPos(0f, -10.0f, 0.5f);
        float[][] pods = {
                {-3.1f, -1.6f, -1.0f, 1.8f, 2f, 1f, 0.30f, -0.18f},
                {1.3f, -1.3f, -1.0f, 1.8f, 2f, 1f, -0.30f, 0.18f},
                {-2.6f, 1.4f, -1.0f, 1.8f, 2f, 1f, 0.22f, -0.12f},
                {0.8f, 1.6f, -1.0f, 1.8f, 2f, 1f, -0.22f, 0.12f},
        };
        for (float[] p : pods) {
            ModelPart pod = new ModelPart();
            pod.setPos(p[0], p[1], p[2]);
            addBox(pod, 0f, 0f, 0f, p[3], p[4], p[5], 0, 96, 64);
            pod.setRotation(0f, p[6], p[7]);
            glowPods.addChild(pod);

            ModelPart eye = new ModelPart();
            eye.setPos(p[0], p[1] + 0.5f, p[2] - 0.6f);
            addBox(eye, 0f, 0f, 0f, 1.8f, 1f, 1f, 0, 36, 92);
            eye.setRotation(0f, p[6], p[7]);
            glowPods.addChild(eye);
        }
    }

    /** 按 UV 布局表取 texOffs, 与 tools/deepocean_uv_layout.py 一一对应. */
    private static void addBox(ModelPart parent, float x, float y, float z,
                               float w, float h, float d, float dilate, int texU, int texV) {
        parent.addBox(x, y, z, w, h, d, dilate, texU, texV, 0, 0, false);
    }
}
