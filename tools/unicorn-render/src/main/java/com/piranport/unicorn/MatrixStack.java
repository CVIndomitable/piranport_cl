package com.piranport.unicorn;

/**
 * 4x4 matrix stack, 仅支持 translate/rotate/scale, 列主序.
 * 顺序: rotateX(α) 把向量绕 X 轴转 α 弧度 (右手系).
 */
public class MatrixStack {

    private static final int MAX = 32;
    private final float[][] stack = new float[MAX][16];
    private int top = 0;

    public MatrixStack() {
        identity(stack[0]);
    }

    private static void identity(float[] m) {
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = m[5] = m[10] = m[15] = 1;
    }

    public void push() {
        System.arraycopy(stack[top], 0, stack[top + 1], 0, 16);
        top++;
    }

    public void pop() {
        top--;
    }

    public void translate(float x, float y, float z) {
        apply(makeTranslate(x, y, z));
    }

    public void scale(float x, float y, float z) {
        apply(makeScale(x, y, z));
    }

    public void rotateX(float a) { apply(makeRotateX(a)); }
    public void rotateY(float a) { apply(makeRotateY(a)); }
    public void rotateZ(float a) { apply(makeRotateZ(a)); }

    /**
     * 不修改 top: 直接把 stack[top] = n * stack[top]
     * (累积变换, 后乘 = 局部坐标系下变换叠加)
     */
    private void apply(float[] n) {
        float[] m = stack[top];
        float[] r = stack[MAX - 1]; // 用底部 scratch, 防止污染
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += n[k * 4 + row] * m[col * 4 + k];
                }
                r[col * 4 + row] = sum;
            }
        }
        // 复制回 stack[top]
        System.arraycopy(r, 0, m, 0, 16);
    }

    private void mul(float[] n) {
        if (top + 1 >= MAX) throw new RuntimeException("MatrixStack overflow");
        float[] m = stack[top];
        float[] r = stack[top + 1];
        // r = n * m
        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                float sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += n[k * 4 + row] * m[col * 4 + k];
                }
                r[col * 4 + row] = sum;
            }
        }
        top++;
    }

    public Vec3 transform(Vec3 v) {
        float[] m = stack[top];
        float x = v.x * m[0] + v.y * m[4] + v.z * m[8]  + m[12];
        float y = v.x * m[1] + v.y * m[5] + v.z * m[9]  + m[13];
        float z = v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14];
        return new Vec3(x, y, z);
    }

    public Vec3 rotateNormal(Vec3 v) {
        float[] m = stack[top];
        float x = v.x * m[0] + v.y * m[4] + v.z * m[8];
        float y = v.x * m[1] + v.y * m[5] + v.z * m[9];
        float z = v.x * m[2] + v.y * m[6] + v.z * m[10];
        float len = (float) Math.sqrt(x*x + y*y + z*z);
        if (len < 1e-6f) return new Vec3(0, 0, 0);
        return new Vec3(x / len, y / len, z / len);
    }

    // ===== matrix builders =====

    private static float[] makeTranslate(float x, float y, float z) {
        float[] r = new float[16];
        identity(r);
        r[12] = x; r[13] = y; r[14] = z;
        return r;
    }

    private static float[] makeScale(float x, float y, float z) {
        float[] r = new float[16];
        for (int i = 0; i < 16; i++) r[i] = 0;
        r[0] = x; r[5] = y; r[10] = z; r[15] = 1;
        return r;
    }

    private static float[] makeRotateX(float a) {
        float[] r = new float[16];
        identity(r);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        r[5] = c;  r[6] = s;
        r[9] = -s; r[10] = c;
        return r;
    }

    private static float[] makeRotateY(float a) {
        float[] r = new float[16];
        identity(r);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        r[0] = c;  r[2] = -s;
        r[8] = s;  r[10] = c;
        return r;
    }

    private static float[] makeRotateZ(float a) {
        float[] r = new float[16];
        identity(r);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        r[0] = c;  r[1] = s;
        r[4] = -s; r[5] = c;
        return r;
    }
}
