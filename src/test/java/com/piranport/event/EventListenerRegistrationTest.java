package com.piranport.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 事件监听器注册测试 — 验证监听器能正确注册到事件总线。
 *
 * <p>注意: 由于 Minecraft 类在单元测试环境中不可用,
 * 本测试使用简单的测试事件代替实际的领域事件。
 * 实际监听器(TransformationAttributeListener等)的功能测试
 * 应通过集成测试(GameTest)完成。
 */
class EventListenerRegistrationTest {

    private EventBus eventBus;

    // 测试用事件
    private record TestTransformationEvent(String playerName, boolean activated) {}

    @BeforeEach
    void setUp() {
        eventBus = EventBus.getInstance();
        eventBus.clearAll();
    }

    @AfterEach
    void tearDown() {
        eventBus.clearAll();
    }

    @Test
    void shouldRegisterMultipleListenersForSameEvent() {
        AtomicInteger callCount = new AtomicInteger(0);

        // 注册3个监听器模拟实际场景(属性/消息/粒子)
        eventBus.subscribe(TestTransformationEvent.class, e -> callCount.incrementAndGet());
        eventBus.subscribe(TestTransformationEvent.class, e -> callCount.incrementAndGet());
        eventBus.subscribe(TestTransformationEvent.class, e -> callCount.incrementAndGet());

        assertEquals(3, eventBus.getListenerCount(TestTransformationEvent.class),
                "Should have 3 listeners registered");

        // 发布事件,所有监听器应被调用
        eventBus.post(new TestTransformationEvent("testPlayer", true));

        assertEquals(3, callCount.get(),
                "All 3 listeners should be invoked");
    }

    @Test
    void shouldAllowDuplicateRegistrationsCurrently() {
        // 注册相同的监听器2次
        java.util.function.Consumer<TestTransformationEvent> listener = e -> {};

        eventBus.subscribe(TestTransformationEvent.class, listener);
        int countAfterFirst = eventBus.getListenerCount(TestTransformationEvent.class);

        eventBus.subscribe(TestTransformationEvent.class, listener);
        int countAfterSecond = eventBus.getListenerCount(TestTransformationEvent.class);

        // 当前实现允许重复注册(未来可优化为防重)
        assertEquals(countAfterFirst + 1, countAfterSecond,
                "Current implementation allows duplicate registrations");
    }

    @Test
    void clearAllShouldRemoveAllRegisteredListeners() {
        // 注册多个监听器
        eventBus.subscribe(TestTransformationEvent.class, e -> {});
        eventBus.subscribe(TestTransformationEvent.class, e -> {});
        eventBus.subscribe(TestTransformationEvent.class, e -> {});

        assertTrue(eventBus.getListenerCount(TestTransformationEvent.class) > 0,
                "Should have listeners after registration");

        // 清空
        eventBus.clearAll();

        assertEquals(0, eventBus.getListenerCount(TestTransformationEvent.class),
                "Should have no listeners after clearAll");
    }

    @Test
    void shouldIsolateListenersByEventType() {
        record EventA(String data) {}
        record EventB(int value) {}

        eventBus.subscribe(EventA.class, e -> {});
        eventBus.subscribe(EventA.class, e -> {});
        eventBus.subscribe(EventB.class, e -> {});

        assertEquals(2, eventBus.getListenerCount(EventA.class),
                "EventA should have 2 listeners");
        assertEquals(1, eventBus.getListenerCount(EventB.class),
                "EventB should have 1 listener");
    }
}
