package com.piranport.unicorn;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * 离线 Java2D 渲染器 —— 贴图版.
 *
 * 流程:
 *   1) ModelPart.collect() 收集所有 quad (含 world 坐标 + 法线 + material)
 *   2) 自动计算模型 bounding box, 自适应 focusY
 *   3) 按 centerZ 排序 (painter algorithm)
 *   4) 透视投影 quad 4 顶点 → 按 (u, v) 在材质贴图里采像素 → 写到屏幕
 *
 * 不依赖 MC, 任何 JDK 1.8+ 都能跑.
 */
public class UnicornRenderer {

    public final int width, height;
    private final BufferedImage img;
    public final float fovDeg = 30f;
    public float camRotX = (float) Math.toRadians(8);
    public float camRotY = (float) Math.toRadians(-12);

    public float focusY;       // 自适应
    public float scale;        // 自适应: 每 MC 像素 -> 屏幕像素 / 透视系数
    public float cameraDist = 80f; // 自适应

    private final Map<PixelTextureGenerator.Material, BufferedImage[]> textures;

    public UnicornRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.textures = PixelTextureGenerator.createAll();
    }

    public void render(UnicornModel model) {
        model.resetAll();
        clearCanvas();

        java.util.List<ModelPart.Quad> quads = new java.util.ArrayList<>();
        MatrixStack m = new MatrixStack();

        // 相机变换
        if (camRotY != 0) m.rotateY(camRotY);
        if (camRotX != 0) m.rotateX(camRotX);

        model.collect(quads, m);

        // 自适应 focusY / scale
        if (quads.isEmpty()) return;
        float minX = Float.POSITIVE_INFINITY, maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
        for (ModelPart.Quad q : quads) {
            for (Vec3 v : new Vec3[]{q.a, q.b, q.c, q.d}) {
                if (v.x < minX) minX = v.x; if (v.x > maxX) maxX = v.x;
                if (v.y < minY) minY = v.y; if (v.y > maxY) maxY = v.y;
                if (v.z < minZ) minZ = v.z; if (v.z > maxZ) maxZ = v.z;
            }
        }
        focusY = (minY + maxY) * 0.5f;
        float modelH = maxY - minY;
        float modelW = maxX - minX;
        float modelD = maxZ - minZ;

        // 相机距离: 让模型在屏幕占 ~60%
        // 透视公式: screen_normalized_y = (y - focusY) / z / tanFov
        // 半高占 0.55 -> maxHalf / z / tanFov = 0.55 -> z = maxHalf / tanFov / 0.55
        float maxHalf = Math.max(modelW, Math.max(modelH, modelD)) * 0.5f;
        cameraDist = maxHalf / (float) Math.tan(Math.toRadians(fovDeg) * 0.5f) / 0.55f;
        // 防近裁: 至少留 maxHalf+5 距离
        cameraDist = Math.max(cameraDist, maxHalf + 5f);
        // scale = 1 (用 world 单位的原大小)
        scale = 1f;
        drawGroundShadow(minY, modelW, modelD);

        // 排序
        quads.sort((a, b) -> Float.compare(b.centerZ, a.centerZ));

        for (ModelPart.Quad q : quads) {
            drawQuad(q);
        }
    }

    private void drawQuad(ModelPart.Quad q) {
        // 4 顶点投影
        float[] sx = new float[4];
        float[] sy = new float[4];
        float[] sz = new float[4];
        Vec3[] verts = {q.a, q.b, q.c, q.d};

        for (int i = 0; i < 4; i++) {
            float[] s = project(verts[i]);
            sx[i] = s[0];
            sy[i] = s[1];
            sz[i] = s[2];
        }

        // backface cull: 屏幕空间 quad 叉积
        // 暂禁掉: quad 顶点顺序与屏幕坐标方向不一致, 简单 cull 反而剔错.
        // 改为通过 normal.z 判定 (法线 z 分量 < 0 = 背朝相机, 剔除)
        // 由于我们 z-sort 已经远先画, 多画一个 quad 不会显著影响 (画家算法会遮挡)
        // TODO: 修复 quad 顶点顺序使其与 MC 一致
        float ex1 = sx[1] - sx[0], ey1 = sy[1] - sy[0];
        float ex2 = sx[2] - sx[0], ey2 = sy[2] - sy[0];
        float cross = ex1 * ey2 - ey1 * ex2;
        // (cross > 0 时为背面, 不剔除, 反正 z-sort 兜底)
        // if (cross > 0) return; // 背面

        // 按 UV 把 quad 分成像素小块 (u, v 都从 0 到 1)
        // 我们按 quad 在屏幕的 bbox 网格扫描
        int minX = (int) Math.floor(min(sx));
        int maxX = (int) Math.ceil(max(sx));
        int minXp = Math.max(0, minX);
        int maxXp = Math.min(width - 1, maxX);
        int minYp = Math.max(0, (int) Math.floor(min(sy)));
        int maxYp = Math.min(height - 1, (int) Math.ceil(max(sy)));
        if (minXp > maxXp || minYp > maxYp) return;

        BufferedImage tex = pickTexture(q.material);

        // 用 quad 4 顶点的 (u, v) 通过重心插值求每像素 UV
        float[] u = {0, 1, 1, 0};
        float[] v = {0, 0, 1, 1};

        // 写像素
        int[] px = img.getRGB(0, 0, width, height, null, 0, width);
        for (int py = minYp; py <= maxYp; py++) {
            for (int pix = minXp; pix <= maxXp; pix++) {
                float[] uv = baryUV(sx, sy, u, v, pix + 0.5f, py + 0.5f);
                if (uv == null) continue;
                if (uv[0] < 0 || uv[0] > 1 || uv[1] < 0 || uv[1] > 1) continue;

                int tx = clampI((int) (uv[0] * (tex.getWidth() - 1)), 0, tex.getWidth() - 1);
                int ty = clampI((int) (uv[1] * (tex.getHeight() - 1)), 0, tex.getHeight() - 1);
                int rgb = shadeRgb(tex.getRGB(tx, ty), q.shade);
                if (((rgb >>> 24) & 0xFF) > 0) {
                    px[py * width + pix] = rgb;
                }
            }
        }
        img.setRGB(0, 0, width, height, px, 0, width);
    }

    private BufferedImage pickTexture(int materialIdx) {
        PixelTextureGenerator.Material[] mats = PixelTextureGenerator.Material.values();
        if (materialIdx < 0 || materialIdx >= mats.length) {
            return textures.get(PixelTextureGenerator.Material.GENERAL)[0];
        }
        BufferedImage[] faces = textures.get(mats[materialIdx]);
        return faces != null && faces.length > 0 ? faces[0] : null;
    }

    private float[] project(Vec3 v) {
        float aspect = (float) width / height;
        float tanFov = (float) Math.tan(Math.toRadians(fovDeg) * 0.5f);
        float d = cameraDist;

        float x = v.x * scale;
        float y = (v.y - focusY) * scale;
        float z = v.z * scale + d;

        if (z <= 0.1f) return new float[]{-1, -1, -1};

        float px = (x / z) / tanFov / aspect;
        float py = (y / z) / tanFov;

        float screenX = (px * 0.5f + 0.5f) * width;
        float screenY = (1f - (py * 0.5f + 0.5f)) * height;

        return new float[]{screenX, screenY, z};
    }

    private static float min(float[] a) {
        float m = a[0]; for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }
    private static float max(float[] a) {
        float m = a[0]; for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }

    private static int clampI(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /**
     * 重心插值: 求屏幕点 (x, y) 在 quad 内的 (u, v).
     * 输入 quad 4 顶点坐标 (sx, sy) 和 UV (u, v).
     * 返回 null 表示点在 quad 外.
     */
    private static float[] baryUV(float[] qx, float[] qy, float[] qu, float[] qv, float x, float y) {
        // 双三角形: (0,1,2) 和 (0,2,3)
        for (int tri = 0; tri < 2; tri++) {
            int i0 = 0, i1 = tri == 0 ? 1 : 2, i2 = tri == 0 ? 2 : 3;
            float x0 = qx[i0], y0 = qy[i0];
            float x1 = qx[i1], y1 = qy[i1];
            float x2 = qx[i2], y2 = qy[i2];
            float denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
            if (Math.abs(denom) < 1e-6) continue;
            float a = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / denom;
            float b = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / denom;
            float c = 1 - a - b;
            if (a >= 0 && b >= 0 && c >= 0) {
                float u = a * qu[i0] + b * qu[i1] + c * qu[i2];
                float v = a * qv[i0] + b * qv[i1] + c * qv[i2];
                return new float[]{u, v};
            }
        }
        return null;
    }

    public BufferedImage getImage() {
        return img;
    }

    private void clearCanvas() {
        Graphics2D graphics = img.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        graphics.setPaint(new GradientPaint(0, 0, new Color(246, 249, 252), 0, height,
                new Color(218, 228, 239)));
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
    }

    private void drawGroundShadow(float minY, float modelW, float modelD) {
        float[] center = project(new Vec3(0.0F, minY + 0.12F, 0.0F));
        float[] xEdge = project(new Vec3(modelW * 0.48F, minY + 0.12F, 0.0F));
        float[] zEdge = project(new Vec3(0.0F, minY + 0.12F, modelD * 0.48F));
        float shadowWidth = Math.max(80.0F, Math.abs(xEdge[0] - center[0]) * 2.2F);
        float shadowHeight = Math.max(22.0F, Math.abs(zEdge[1] - center[1]) * 1.4F);

        Graphics2D graphics = img.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(47, 63, 84, 68));
        graphics.fillOval(Math.round(center[0] - shadowWidth * 0.5F),
                Math.round(center[1] - shadowHeight * 0.25F),
                Math.round(shadowWidth), Math.round(shadowHeight));
        graphics.dispose();
    }

    private static int shadeRgb(int argb, float shade) {
        int alpha = (argb >>> 24) & 0xFF;
        float factor = 0.78F + shade * 0.22F;
        int red = Math.min(255, Math.round(((argb >>> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((argb >>> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((argb & 0xFF) * factor));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
