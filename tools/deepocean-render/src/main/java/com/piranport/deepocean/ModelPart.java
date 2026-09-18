package com.piranport.deepocean;

import java.util.ArrayList;
import java.util.List;

/**
 * 独立版本的 ModelPart —— 与 MC 1.21.1 net.minecraft.client.model.geom.ModelPart 保持一致的可见
 * 方法签名 (addBox / setRotation / setPos / xRot/yRot/zRot), 这样离线渲染器和 mod 端能共用同一份模型代码.
 *
 * 与 unicorn-render 版本的区别: 本版本支持 texOffs + 真实 UV, 不是纯材质色块.
 * CubeListBuilder 风格的 API 让模型类可以逐字复制进 mod (只换 import).
 *
 * 坐标: 单位 = MC 像素 (1/16 格), +Y 上, +X 右, +Z 前 (朝玩家).
 */
public class ModelPart {

    public final List<ModelPart> children = new ArrayList<>();
    public final List<Cube> cubes = new ArrayList<>();

    public float x, y, z;
    public float xRot, yRot, zRot;

    private float ox, oy, oz;
    private float oxr, oyr, ozr;

    public ModelPart() {}

    public ModelPart addChild(ModelPart child) {
        children.add(child);
        return this;
    }

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

    public void resetPose() {
        x = ox; y = oy; z = oz;
        xRot = oxr; yRot = oyr; zRot = ozr;
        for (ModelPart c : children) c.resetPose();
    }

    /** CubeListBuilder 风格的链式加 box. */
    public Cube addBox(float xOff, float yOff, float zOff, float w, float h, float d,
                       float dilate, int texU, int texV, int texW, int texH, boolean mirrored) {
        Cube c = new Cube(xOff - dilate, yOff - dilate, zOff - dilate,
                w + dilate * 2, h + dilate * 2, d + dilate * 2,
                texU, texV, texW, texH, mirrored);
        cubes.add(c);
        return c;
    }

    // ===== 几何收集 =====

    public void collect(List<Quad> out, MatrixStack m, int inheritedMaterial) {
        m.push();
        m.translate(x, y, z);
        if (xRot != 0) m.rotateX(xRot);
        if (yRot != 0) m.rotateY(yRot);
        if (zRot != 0) m.rotateZ(zRot);

        for (Cube cube : cubes) {
            cube.emit(out, m);
        }
        for (ModelPart child : children) {
            child.collect(out, m, inheritedMaterial);
        }

        m.pop();
    }

    // ============================================================
    // 数据结构
    // ============================================================

    /**
     * 一个轴对齐 box, 带 MC 风格的 UV 映射.
     *
     * MC 的 UV 布局: 以 (texU, texV) 为左上角基准, 展开成一条
     * (2*w + 2*d) 宽, (d + h) 高的十字形.
     */
    public static class Cube {
        public final float x0, y0, z0, x1, y1, z1;
        public final int texU, texV, texW, texH;
        public final boolean mirrored;

        /** 每面的显式 UV 覆盖: null = 用 MC 展开算法. 顺序 -X,+X,-Y,+Y,-Z,+Z. */
        public int[][] faceUv = null;

        /** 整体颜色 tint (ARGB), 0 = 不 tint. 用于贴图着色 (如发光件). */
        public int tint = 0;

        public Cube(float xOff, float yOff, float zOff, float w, float h, float d,
                    int texU, int texV, int texW, int texH, boolean mirrored) {
            this.x0 = xOff; this.y0 = yOff; this.z0 = zOff;
            this.x1 = xOff + w; this.y1 = yOff + h; this.z1 = zOff + d;
            this.texU = texU; this.texV = texV;
            this.texW = texW; this.texH = texH;
            this.mirrored = mirrored;
        }

        /** 用整张 box 的 UV 区域, 按 MC 展开算法生成 6 面 UV. */
        public void emit(List<Quad> out, MatrixStack m) {
            float u = texU, v = texV;
            float w = x1 - x0, h = y1 - y0, d = z1 - z0;

            // 用整数像素尺寸做 UV (MC 按整数 box 尺寸展开)
            float bw = Math.max(1f, Math.round(w));
            float bh = Math.max(1f, Math.round(h));
            float bd = Math.max(1f, Math.round(d));

            // MC 展开: 整块纹理区域宽 2*(bw+bd), 高 bd+bh
            // 上/下: 各自 bw x bd; 前/后: bw x bh; 左右: bd x bh
            UvRect top    = new UvRect(u + bd,         v,          bw, bd);
            UvRect bottom = new UvRect(u + bd + bw,    v,          bw, bd);
            UvRect right  = new UvRect(u,              v + bd,     bd, bh);
            UvRect front  = new UvRect(u + bd,         v + bd,     bw, bh);
            UvRect left   = new UvRect(u + bd + bw,    v + bd,     bd, bh);
            UvRect back   = new UvRect(u + bd + bw + bd, v + bd,   bw, bh);

            if (faceUv != null) {
                addFace(out, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, 1,0,0, 0.62f,
                        rectFrom(faceUv[0], left, mirrored));
                addFace(out, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, 0.86f,
                        rectFrom(faceUv[1], right, mirrored));
                addFace(out, m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, 1.00f,
                        rectFrom(faceUv[2], top, mirrored));
                addFace(out, m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,1,0, 0.44f,
                        rectFrom(faceUv[3], bottom, mirrored));
                addFace(out, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,1, 0.72f,
                        rectFrom(faceUv[4], back, mirrored));
                addFace(out, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1, 0.92f,
                        rectFrom(faceUv[5], front, mirrored));
                return;
            }

            // left (-X)
            addFace(out, m, x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0, 1,0,0, 0.62f, left);
            // right (+X)
            addFace(out, m, x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1, 1,0,0, 0.86f, right);
            // top (+Y)
            addFace(out, m, x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0, 0,1,0, 1.00f, top);
            // bottom (-Y)
            addFace(out, m, x0,y0,z1, x0,y0,z0, x1,y0,z0, x1,y0,z1, 0,1,0, 0.44f, bottom);
            // back (-Z)
            addFace(out, m, x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0, 0,0,1, 0.72f, back);
            // front (+Z)
            addFace(out, m, x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1, 0,0,1, 0.92f, front);
        }

        private static UvRect rectFrom(int[] uv, UvRect fallback, boolean mirrored) {
            if (uv == null) return fallback;
            return new UvRect(uv[0], uv[1], uv[2], uv[3]);
        }

        private void addFace(List<Quad> out, MatrixStack m,
                             float ax, float ay, float az,
                             float bx, float by, float bz,
                             float cx, float cy, float cz,
                             float dx, float dy, float dz,
                             float nx, float ny, float nz,
                             float shade, UvRect uv) {
            Vec3 A = m.transform(new Vec3(ax, ay, az));
            Vec3 B = m.transform(new Vec3(bx, by, bz));
            Vec3 C = m.transform(new Vec3(cx, cy, cz));
            Vec3 D = m.transform(new Vec3(dx, dy, dz));
            Vec3 N = m.rotateNormal(new Vec3(nx, ny, nz));
            out.add(new Quad(A, B, C, D, N, shade,
                    uv.u0, uv.v0, uv.u1, uv.v1, tint));
        }
    }

    /** 贴图上的一个矩形 (像素坐标). */
    public static class UvRect {
        public final float u0, v0, u1, v1;
        public UvRect(float u, float v, float w, float h) {
            this.u0 = u; this.v0 = v; this.u1 = u + w; this.v1 = v + h;
        }
    }

    public static class Quad {
        public Vec3 a, b, c, d;
        public Vec3 normal;
        public final float shade;
        public final float u0, v0, u1, v1;
        public final int tint;

        public Quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Vec3 normal, float shade,
                    float u0, float v0, float u1, float v1, int tint) {
            this.a = a; this.b = b; this.c = c; this.d = d;
            this.normal = normal;
            this.shade = shade;
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1;
            this.tint = tint;
        }

        /** 相机空间下的深度 (用于画家算法排序). */
        public float centerZ() {
            return (a.z + b.z + c.z + d.z) * 0.25f;
        }
    }
}
