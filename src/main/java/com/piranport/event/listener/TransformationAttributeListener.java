package com.piranport.event.listener;

import com.piranport.combat.TransformationManager;
import com.piranport.event.EventBus;
import com.piranport.event.TransformationEvent;

/**
 * 变身属性监听器 — 响应变身事件应用/移除属性修饰器。
 *
 * <p>职责:
 * <ul>
 *   <li>变身激活时: 应用护甲/速度/血量/韧性修饰器</li>
 *   <li>变身解除时: 移除所有变身相关属性修饰器</li>
 * </ul>
 *
 * <h2>属性计算</h2>
 * <p>委托给 {@link TransformationManager} 计算:
 * <ul>
 *   <li>基础属性(由舰装类型决定)</li>
 *   <li>装甲板加成</li>
 *   <li>引擎速度加成</li>
 *   <li>负重速度惩罚</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class TransformationAttributeListener {

    private TransformationAttributeListener() {}

    /**
     * 注册监听器到事件总线。应在模组初始化时调用一次。
     */
    public static void register() {
        EventBus.getInstance().subscribe(TransformationEvent.class,
                TransformationAttributeListener::onTransformation);
    }

    private static void onTransformation(TransformationEvent event) {
        if (event.activated()) {
            // 激活变身: 应用属性
            TransformationManager.applyTransformationAttributes(event.player(), event.core());
        } else {
            // 解除变身: 移除属性和惩罚
            TransformationManager.removeTransformationAttributes(event.player());
            TransformationManager.removeOverweightPenalty(event.player());
        }
    }
}
