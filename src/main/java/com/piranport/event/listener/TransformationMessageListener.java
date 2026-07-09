package com.piranport.event.listener;

import com.piranport.event.EventBus;
import com.piranport.event.TransformationEvent;
import net.minecraft.network.chat.Component;

/**
 * 变身消息监听器 — 响应变身事件向玩家显示提示消息。
 *
 * <p>职责:
 * <ul>
 *   <li>变身激活时: 显示 "已变身为舰娘形态" 动作栏消息</li>
 *   <li>变身解除时: 显示 "已解除变身" 动作栏消息</li>
 * </ul>
 *
 * <h2>消息国际化</h2>
 * <p>消息文本通过 {@link Component#translatable} 从语言文件加载:
 * <ul>
 *   <li>{@code message.piranport.transformed} — 变身消息</li>
 *   <li>{@code message.piranport.untransformed} — 解除变身消息</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class TransformationMessageListener {

    private TransformationMessageListener() {}

    /**
     * 注册监听器到事件总线。应在模组初始化时调用一次。
     */
    public static void register() {
        EventBus.getInstance().subscribe(TransformationEvent.class,
                TransformationMessageListener::onTransformation);
    }

    private static void onTransformation(TransformationEvent event) {
        Component message = event.activated()
                ? Component.translatable("message.piranport.transformed")
                : Component.translatable("message.piranport.untransformed");

        event.player().displayClientMessage(message, true);
    }
}
