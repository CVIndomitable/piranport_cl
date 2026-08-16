package com.piranport.unicorn;

import java.util.ArrayList;
import java.util.List;

/**
 * 独立版本的 ModelRenderer 替身 —— 故意保留与 MC 1.21.1 net.minecraft.client.model.geom.ModelPart
 * 一致的可见方法签名 (addBox / setRotation / setPos / xRot/yRot/zRot/yScale),
 * 这样离线渲染器和 mod 端能共用同一份模型代码。
 *
 * 坐标: 单位 = MC 像素 (1/16 格), +Y 上, +X 右, +Z 前 (朝玩家).
 *
 * 实现策略:
 *   - 每个 Part = 1 个根节点 (rootPos) + 旋转轴 + 子节点
 *   - addBox 创建带 Pivot 的 Cube, 通过 cubes 列表提供几何
 *   - 渲染时由 Renderer 深度优先遍历, 用 parent.xRot/yRot/zRot 累计 transform
 *
 * 为什么单文件: 让整个模型描述和 box 拼装都在一个 Java 类里,
 * 复制进 mod 项目只需换包名。
 */
public class ModelPart {

    public final List<ModelPart> children = new ArrayList<>();
    public final List<Cube> cubes = new ArrayList<>();

    /** 此 part 的默认 material (子 cube 可以单独覆盖) */
    public int material = 0;

    // 旋转点(相对 parent, MC 坐标系)
    public float x, y, z;           // 当前 offset (setPos 设置)
    public float xRot, yRot, zRot;  // 旋转角, 弧度
    public float xScale = 1, yScale = 1, zScale = 1;

    // 原始设定(供 reset)
    private float ox, oy, oz;
    private float oxr, oyr, ozr;
    private float oxs = 1, oys = 1, ozs = 1;

    public ModelPart() {}

    // ===== MC API 兼容签名 =====

    public ModelPart addChild(ModelPart child) {
        children.add(child);
        return this;
    }

    /** MC 风格: setPos(pivotX, pivotY, pivotZ), 子件偏移量 */
    public ModelPart setPos(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        this.ox = x; this.oy = y; this.oz = z;
        return this;
    }

    public ModelPart setRotation(float x, float y, float z) {
        this.xRot = x; this.yRot = y; this.zRot = z;
        this.oxr = x; this.oyr = y; this.ozr = z;
        return this;
    }

    public void setXRot(float v) { this.xRot = v; }
    public void setYRot(float v) { this.yRot = v; }
    public void setZRot(float v) { this.zRot = v; }

    /** MC 风格: addBox(xOff, yOff, zOff, w, h, d, optional inflate) */
    public Cube addBox(float xOff, float yOff, float zOff, float w, float h, float d, float inflate) {
        Cube c = new Cube(xOff, yOff, zOff, w + inflate*2, h + inflate*2, d + inflate*2);
        c.material = this.material;
        cubes.add(c);
        return c;
    }

    public Cube addBox(float xOff, float yOff, float zOff, float w, float h, float d) {
        return addBox(xOff, yOff, zOff, w, h, d, 0);
    }

    /** 设置此 part 的材质 (影响后续 addBox) */
    public ModelPart setMaterial(int mat) {
        this.material = mat;
        return this;
    }

    /** reset: 还原所有 setPos / setRotation 设置 */
    public void resetPose() {
        x = ox; y = oy; z = oz;
        xRot = oxr; yRot = oyr; zRot = ozr;
        xScale = oxs; yScale = oys; zScale = ozs;
        for (ModelPart c : children) c.resetPose();
    }

    // ===== 几何收集 (供 Renderer 用) =====

    /**
     * 递归把当前节点的所有 cube 投影到 world space.
     * out.add 进去的 Quad 已经包含 world 坐标 + 着色信息.
     */
    public void collect(List<Quad> out, MatrixStack m) {
        m.push();
        m.translate(x, y, z);
        if (xRot != 0) m.rotateX(xRot);
        if (yRot != 0) m.rotateY(yRot);
        if (zRot != 0) m.rotateZ(zRot);
        m.scale(xScale, yScale, zScale);

        for (Cube cube : cubes) {
            cube.emit(out, m);
        }
        for (ModelPart child : children) {
            child.collect(out, m);
        }

        m.pop();
    }

    // ============================================================
    // 数据结构
    // ============================================================

    /**
     * 一个轴对齐 box (相对于 part origin).
     * MC 习惯: addBox 的 xOff/yOff/zOff 是 box 左下后角偏移, w/h/d 是 +X/+Y/+Z 方向尺寸.
     */
    public static class Cube {
        // 8 个角点 (本地坐标)
        public final float x0, y0, z0, x1, y1, z1;

        /** 此 cube 的材质 (PixelTextureGenerator.Material 整数值) */
        public int material = 0;

        public Cube(float xOff, float yOff, float zOff, float w, float h, float d) {
            this.x0 = xOff; this.y0 = yOff; this.z0 = zOff;
            this.x1 = xOff + w; this.y1 = yOff + h; this.z1 = zOff + d;
        }

        public void emit(List<Quad> out, MatrixStack m) {
            // 6 个面, 每面 1 个 quad, 朝外法线
            // 顺序: -X, +X, -Y, +Y, -Z, +Z
            addFace(out, m,
                    x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0,
                    1, 0, 0, 0.65f);
            addFace(out, m,
                    x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1,
                    1, 0, 0, 0.85f);
            // top
            addFace(out, m,
                    x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,
                    0, 1, 0, 1.00f);
            // bottom (略暗)
            addFace(out, m,
                    x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1,
                    0, 1, 0, 0.45f);
            // -Z (后)
            addFace(out, m,
                    x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0,
                    0, 0, 1, 0.70f);
            // +Z (前)
            addFace(out, m,
                    x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,
                    0, 0, 1, 0.90f);
        }

        private void addFace(List<Quad> out, MatrixStack m,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float nx, float ny, float nz,
                             float shade) {
            Vec3 A = m.transform(new Vec3(ax, ay, az));
            Vec3 B = m.transform(new Vec3(bx, by, bz));
            Vec3 C = m.transform(new Vec3(cx, cy, cz));
            Vec3 D = m.transform(new Vec3(dx, dy, dz));
            // 法线经过 transformMatrix 旋转
            Vec3 N = m.rotateNormal(new Vec3(nx, ny, nz));
            out.add(new Quad(A, B, C, D, N, shade, material));
        }
    }

    /**
     * 一个 quad 在 world space, 4 顶点 + 法线 + 亮度系数.
     * 离线渲染器 painter 时根据 z 排序.
     */
    public static class Quad {
        public final Vec3 a, b, c, d;
        public final Vec3 normal;
        public final float shade;
        public final float centerZ;
        public final int material;

        public Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, float shade, int material) {
            this.a = a; this.b = b; this.c = c; this.d = d;
            this.normal = normal;
            this.shade = shade;
            this.material = material;
            this.centerZ = (a.z + b.z + c.z + d.z) * 0.25f;
        }
    }
}
