package com.piranport.client;

/**
 * 客户端屏幕震动状态管理。
 * 发射/爆炸时触发震动，由 ClientGameEvents 在渲染阶段应用偏移。
 */
public final class CameraShakeHandler {
    private static float shakeIntensity = 0;
    private static int shakeDuration = 0;

    public static void trigger(float intensity, int durationTicks) {
        if (intensity > shakeIntensity) {
            shakeIntensity = intensity;
            shakeDuration = durationTicks;
        }
    }

    public static void tick() {
        if (shakeDuration > 0) {
            shakeDuration--;
            if (shakeDuration <= 0) {
                shakeIntensity = 0;
            }
        }
    }

    public static float getShakeIntensity() { return shakeIntensity; }
    public static boolean isShaking() { return shakeDuration > 0; }
}
