package com.piranport.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 领域事件总线 — 解耦跨系统通信的发布/订阅机制。
 *
 * <h2>设计目标</h2>
 * <ul>
 *   <li>解耦发布者和订阅者(避免静态方法调用链)</li>
 *   <li>支持多监听器(一个事件可被多个系统监听)</li>
 *   <li>线程安全(服务端主线程访问为主,使用 ConcurrentHashMap 防御并发)</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 定义事件(使用 record 保持简洁)
 * public record TransformationEvent(Player player, ItemStack core, boolean activated) {}
 *
 * // 订阅事件
 * EventBus.getInstance().subscribe(TransformationEvent.class, event -> {
 *     if (event.activated()) {
 *         event.player().displayClientMessage(Component.literal("变身成功!"), true);
 *     }
 * });
 *
 * // 发布事件
 * EventBus.getInstance().post(new TransformationEvent(player, coreStack, true));
 * }</pre>
 *
 * <h2>性能考量</h2>
 * <ul>
 *   <li>使用 {@link CopyOnWriteArrayList} 避免迭代时的并发修改异常</li>
 *   <li>读多写少场景(订阅在初始化时完成,运行时只发布事件)</li>
 *   <li>事件处理是同步的,长时间操作应异步执行或使用 tick 延迟</li>
 * </ul>
 *
 * @since 1.1.0
 */
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return INSTANCE;
    }

    /**
     * 订阅特定类型的事件。可以多次订阅同一事件类型以注册多个监听器。
     *
     * @param eventType 事件类型
     * @param listener 监听器回调(在事件发布时同步调用)
     * @param <T> 事件类型参数
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * 发布事件到所有已注册的监听器。
     *
     * <p>监听器按注册顺序依次同步调用。如果某个监听器抛出异常,
     * 该异常会传播给调用方并中断后续监听器执行。
     *
     * @param event 事件对象
     * @param <T> 事件类型参数
     */
    public <T> void post(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> consumer : list) {
                @SuppressWarnings("unchecked")
                Consumer<T> typedConsumer = (Consumer<T>) consumer;
                typedConsumer.accept(event);
            }
        }
    }

    /**
     * 取消订阅特定监听器。
     *
     * @param eventType 事件类型
     * @param listener 要移除的监听器
     * @param <T> 事件类型参数
     * @return 是否成功移除(如果监听器不存在则返回 false)
     */
    public <T> boolean unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventType);
        return list != null && list.remove(listener);
    }

    /**
     * 清空所有监听器(用于测试或服务器关闭时清理)。
     */
    public void clearAll() {
        listeners.clear();
    }

    /**
     * 获取特定事件类型的监听器数量(用于调试/测试)。
     *
     * @param eventType 事件类型
     * @return 监听器数量
     */
    public int getListenerCount(Class<?> eventType) {
        List<Consumer<?>> list = listeners.get(eventType);
        return list != null ? list.size() : 0;
    }
}
