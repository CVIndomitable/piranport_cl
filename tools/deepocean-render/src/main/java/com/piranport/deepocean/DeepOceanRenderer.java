package com.piranport.deepocean;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线贴图渲染器 —— 把带 UV 的模型渲染成 PNG, 供无 MC 环境下快速预览迭代.
 *
 * 流程: collect quads (world 坐标 + UV) → 自适应取景 → 按 centerZ 排序 (画家算法)
 *       → 透视投影 → 逐像素按 UV 采样贴图 → 光照着色.
 */
public class DeepOceanRenderer {

    public final int width, height;
    private final BufferedImage img;
    public BufferedImage texture;

    public final float fovDeg = 30f;
    public float camRotX = (float) Math.toRadians(10);
    public float camRotY = (float) Math.toRadians(-24);
    public float camRotZ = 0f;

    public float focusY;
    public float cameraDist = 80f;
    public float zoom = 0.60f;

    public boolean showGround = true;
    public boolean lightFromFront = true;

    public DeepOceanRenderer(int width, int height, BufferedImage texture) {
        this.width = width;
        this.height = height;
        this.texture = texture;
        this.img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public void render(ModelPart model) {
        model.resetPose();
        clearCanvas();

        List<ModelPart.Quad> quads = new ArrayList<>();
        model.collect(quads, new MatrixStack(), 0);
        if (quads.isEmpty()) return;

        // 先用相机矩阵把模型变换到相机空间, 再据此取景 —— 否则绕模型转视角时会出画.
        MatrixStack cam = new MatrixStack();
        if (camRotZ != 0) cam.rotateZ(camRotZ);
        if (camRotY != 0) cam.rotateY(camRotY);
        if (camRotX != 0) cam.rotateX(camRotX);
        for (ModelPart.Quad q : quads) {
            q.a = cam.transform(q.a);
            q.b = cam.transform(q.b);
            q.c = cam.transform(q.c);
            q.d = cam.transform(q.d);
            q.normal = cam.rotateNormal(q.normal);
        }

        float[] b = computeBounds(quads);
        focusY = (b[2] + b[3]) * 0.5f;
        float modelH = b[3] - b[2];
        float modelW = b[1] - b[0];
        float modelD = b[5] - b[4];
        // 取景只按"画面上实际占的宽高"算, 深度不参与, 否则侧视角会把模型缩得过小.
        float maxHalf = Math.max(modelW, modelH) * 0.5f;
        float maxDepth = modelD * 0.5f;
        cameraDist = maxHalf / (float) Math.tan(Math.toRadians(fovDeg) * 0.5f) / zoom;
        cameraDist = Math.max(cameraDist, maxDepth + 8f);

        if (showGround) drawGroundShadow(b[2], modelW, modelD, cam);

        quads.sort((a, q) -> Float.compare(q.centerZ(), a.centerZ()));
        for (ModelPart.Quad q : quads) drawQuad(q);
    }

    private float[] computeBounds(List<ModelPart.Quad> quads) {
        float[] b = {Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (ModelPart.Quad q : quads) {
            for (Vec3 v : new Vec3[]{q.a, q.b, q.c, q.d}) {
                if (v.x < b[0]) b[0] = v.x;
                if (v.x > b[1]) b[1] = v.x;
                if (v.y < b[2]) b[2] = v.y;
                if (v.y > b[3]) b[3] = v.y;
                if (v.z < b[4]) b[4] = v.z;
                if (v.z > b[5]) b[5] = v.z;
            }
        }
        return b;
    }

    private void drawQuad(ModelPart.Quad q) {
        float[] sx = new float[4], sy = new float[4];
        Vec3[] verts = {q.a, q.b, q.c, q.d};
        for (int i = 0; i < 4; i++) {
            float[] s = project(verts[i]);
            sx[i] = s[0];
            sy[i] = s[1];
        }

        int minX = Math.max(0, (int) Math.floor(min(sx)));
        int maxX = Math.min(width - 1, (int) Math.ceil(max(sx)));
        int minY = Math.max(0, (int) Math.floor(min(sy)));
        int maxY = Math.min(height - 1, (int) Math.ceil(max(sy)));
        if (minX > maxX || minY > maxY) return;

        float[] uu = {q.u0, q.u1, q.u1, q.u0};
        float[] vv = {q.v0, q.v0, q.v1, q.v1};

        int tw = texture.getWidth(), th = texture.getHeight();
        int[] px = img.getRGB(0, 0, width, height, null, 0, width);

        for (int py = minY; py <= maxY; py++) {
            for (int pix = minX; pix <= maxX; pix++) {
                float[] uv = baryUV(sx, sy, uu, vv, pix + 0.5f, py + 0.5f);
                if (uv == null) continue;
                float fu = uv[0] / tw, fv = uv[1] / th;
                if (fu < 0 || fu >= 1 || fv < 0 || fv >= 1) continue;

                int tx = clampI((int) Math.floor(fu * tw), 0, tw - 1);
                int ty = clampI((int) Math.floor(fv * th), 0, th - 1);
                int argb = texture.getRGB(tx, ty);
                if (((argb >>> 24) & 0xFF) < 8) continue;
                px[py * width + pix] = shade(argb, q.shade, q.tint, q.normal);
            }
        }
        img.setRGB(0, 0, width, height, px, 0, width);
    }

    private int shade(int argb, float shade, int tint, Vec3 n) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF, g = (argb >>> 8) & 0xFF, b = argb & 0xFF;

        if (tint != 0) {
            r = r * ((tint >>> 16) & 0xFF) / 255;
            g = g * ((tint >>> 8) & 0xFF) / 255;
            b = b * (tint & 0xFF) / 255;
            a = a * ((tint >>> 24) & 0xFF) / 255;
        }

        // 面向相机的面更亮
        float facing = lightFromFront ? (n.z * 0.5f + 0.5f) : 0.6f;
        float f = (0.55f + shade * 0.45f) * (0.72f + facing * 0.38f);
        r = Math.min(255, Math.round(r * f));
        g = Math.min(255, Math.round(g * f));
        b = Math.min(255, Math.round(b * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** 输入已是相机空间坐标, 这里只做透视投影. */
    private float[] project(Vec3 v) {
        float aspect = (float) width / height;
        float tanFov = (float) Math.tan(Math.toRadians(fovDeg) * 0.5f);
        float y = v.y - focusY;
        float z = v.z + cameraDist;
        if (z <= 0.1f) z = 0.1f;

        float px = (v.x / z) / tanFov / aspect;
        float py = (y / z) / tanFov;
        return new float[]{(px * 0.5f + 0.5f) * width,
                (1f - (py * 0.5f + 0.5f)) * height};
    }

    private static float min(float[] a) { float m = a[0]; for (float v : a) if (v < m) m = v; return m; }
    private static float max(float[] a) { float m = a[0]; for (float v : a) if (v > m) m = v; return m; }
    private static int clampI(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }

    private static float[] baryUV(float[] qx, float[] qy, float[] qu, float[] qv, float x, float y) {
        for (int tri = 0; tri < 2; tri++) {
            int i0 = 0, i1 = tri == 0 ? 1 : 2, i2 = tri == 0 ? 2 : 3;
            float x0 = qx[i0], y0 = qy[i0], x1 = qx[i1], y1 = qy[i1], x2 = qx[i2], y2 = qy[i2];
            float denom = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2);
            if (Math.abs(denom) < 1e-6) continue;
            float a = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) / denom;
            float b = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) / denom;
            float c = 1 - a - b;
            if (a >= -0.0001 && b >= -0.0001 && c >= -0.0001) {
                return new float[]{a * qu[i0] + b * qu[i1] + c * qu[i2],
                        a * qv[i0] + b * qv[i1] + c * qv[i2]};
            }
        }
        return null;
    }

    public BufferedImage getImage() { return img; }

    private void clearCanvas() {
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(244, 248, 252), 0, height,
                new Color(214, 226, 238)));
        g.fillRect(0, 0, width, height);
        g.dispose();
    }

    private void drawGroundShadow(float minY, float modelW, float modelD, MatrixStack cam) {
        float[] c = project(cam.transform(new Vec3(0, minY + 0.1f, 0)));
        float[] xe = project(cam.transform(new Vec3(modelW * 0.45f, minY + 0.1f, 0)));
        float[] ze = project(cam.transform(new Vec3(0, minY + 0.1f, modelD * 0.45f)));
        float sw = Math.max(90f, Math.abs(xe[0] - c[0]) * 2.1f);
        float sh = Math.max(26f, Math.abs(ze[1] - c[1]) * 1.5f);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(46, 62, 84, 60));
        g.fillOval(Math.round(c[0] - sw * 0.5f), Math.round(c[1] - sh * 0.25f),
                Math.round(sw), Math.round(sh));
        g.dispose();
    }
}
