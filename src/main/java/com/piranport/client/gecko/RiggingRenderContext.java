package com.piranport.client.gecko;

/**
 * 舰装 3D 渲染标记：只在 {@code ShipRiggingLayer} 摆挂点模型时为 true。
 *
 * <p>BEWLR 的 {@code renderByItem} 无法从 {@code ItemDisplayContext} 区分
 * 「物品展示框的 FIXED」与「舰装挂点的 FIXED」——两者上下文相同，但展示框要图标、
 * 挂点要 3D。所以在挂点调用 {@code ItemRenderer.renderStatic} 前后设置本标记，
 * BEWLR 只在 {@code FIXED && isRiggingPass()} 时走 3D 分支。
 *
 * <p>渲染是客户端渲染线程单线程执行，静态 boolean 足够；前后用 try/finally 恢复，
 * 避免异常时把标记漏成 true 影响后续图标渲染。
 */
public final class RiggingRenderContext {

    private static boolean riggingPass;

    private RiggingRenderContext() {}

    public static boolean isRiggingPass() {
        return riggingPass;
    }

    public static void setRiggingPass(boolean value) {
        riggingPass = value;
    }
}
