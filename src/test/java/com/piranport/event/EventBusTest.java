package com.piranport.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventBus 单元测试 — 验证发布/订阅机制的正确性。
 */
class EventBusTest {

    private EventBus eventBus;

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clearAll(); // 清空上次测试的监听器
    }

    @AfterEach
    void tearDown() {
        eventBus.clearAll();
    }

    @Test
    void subscribeShouldRegisterListener() {
        AtomicInteger callCount = new AtomicInteger(0);
        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet());

        eventBus.post(new TestEvent("test"));

        assertEquals(1, callCount.get(), "监听器应被调用1次");
    }

    @Test
    void postShouldInvokeAllListeners() {
        List<String> results = new ArrayList<>();

        eventBus.subscribe(TestEvent.class, event -> results.add("listener1-" + event.message()));
        eventBus.subscribe(TestEvent.class, event -> results.add("listener2-" + event.message()));

        eventBus.post(new TestEvent("test"));

        assertEquals(2, results.size(), "应调用2个监听器");
        assertTrue(results.contains("listener1-test"));
        assertTrue(results.contains("listener2-test"));
    }

    @Test
    void postShouldInvokeListenersInOrderForSameSubscription() {
        List<Integer> order = new ArrayList<>();

        eventBus.subscribe(TestEvent.class, event -> order.add(1));
        eventBus.subscribe(TestEvent.class, event -> order.add(2));
        eventBus.subscribe(TestEvent.class, event -> order.add(3));

        eventBus.post(new TestEvent("test"));

        assertEquals(List.of(1, 2, 3), order, "监听器应按注册顺序调用");
    }

    @Test
    void postShouldNotInvokeListenersOfOtherEventTypes() {
        AtomicInteger callCount = new AtomicInteger(0);

        eventBus.subscribe(TestEvent.class, event -> callCount.incrementAndGet());
        eventBus.post(new OtherEvent(123));

        assertEquals(0, callCount.get(), "不应调用其他事件类型的监听器");
    }

    @Test
    void unsubscribeShouldRemoveListener() {
        AtomicInteger callCount = new AtomicInteger(0);
        java.util.function.Consumer<TestEvent> listener = event -> callCount.incrementAndGet();

        eventBus.subscribe(TestEvent.class, listener);
        eventBus.post(new TestEvent("test1"));
        assertEquals(1, callCount.get());

        boolean removed = eventBus.unsubscribe(TestEvent.class, listener);
        assertTrue(removed, "应成功移除监听器");

        eventBus.post(new TestEvent("test2"));
        assertEquals(1, callCount.get(), "移除后不应再调用");
    }

    @Test
    void unsubscribeShouldReturnFalseIfListenerNotFound() {
        java.util.function.Consumer<TestEvent> listener = event -> {};
        boolean removed = eventBus.unsubscribe(TestEvent.class, listener);
        assertFalse(removed, "移除不存在的监听器应返回false");
    }

    @Test
    void getListenerCountShouldReturnCorrectCount() {
        assertEquals(0, eventBus.getListenerCount(TestEvent.class));

        eventBus.subscribe(TestEvent.class, event -> {});
        assertEquals(1, eventBus.getListenerCount(TestEvent.class));

        eventBus.subscribe(TestEvent.class, event -> {});
        assertEquals(2, eventBus.getListenerCount(TestEvent.class));
    }

    @Test
    void clearAllShouldRemoveAllListeners() {
        eventBus.subscribe(TestEvent.class, event -> {});
        eventBus.subscribe(OtherEvent.class, event -> {});

        eventBus.clearAll();

        assertEquals(0, eventBus.getListenerCount(TestEvent.class));
        assertEquals(0, eventBus.getListenerCount(OtherEvent.class));
    }

    @Test
    void postShouldPropagateExceptionsFromListeners() {
        eventBus.subscribe(TestEvent.class, event -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(RuntimeException.class, () -> {
            eventBus.post(new TestEvent("test"));
        }, "监听器抛出的异常应传播给调用方");
    }

    // 测试事件类
    private record TestEvent(String message) {}
    private record OtherEvent(int value) {}
}
