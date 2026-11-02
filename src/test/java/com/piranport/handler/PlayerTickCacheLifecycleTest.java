package com.piranport.handler;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 拆分后仍通过原生命周期入口清理，避免重连沿用旧位置或漏掉另一位玩家的缓存。 */
class PlayerTickCacheLifecycleTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @BeforeEach
    void seed() throws Exception {
        PlayerTickHandler.clearCaches();
        loads().put(first, -999);
        loads().put(second, 42);
        positions().put(first, new Vec3(100, 64, 100));
        positions().put(second, new Vec3(200, 64, 200));
        distances().put(first, 4.5);
        distances().put(second, 8.5);
    }

    @AfterEach
    void cleanup() {
        PlayerTickHandler.clearCaches();
    }

    @Test
    void logoutClearsAllStateForOnlyTheLeavingPlayer() throws Exception {
        PlayerTickHandler.onPlayerLogout(first);
        PlayerTickHandler.onPlayerLogout(first);

        assertEquals(Set.of(second), loads().keySet());
        assertEquals(Set.of(second), positions().keySet());
        assertEquals(Map.of(second, 8.5), distances());
    }

    @Test
    void serverStopClearsEveryModuleIncludingNoFuelSentinel() throws Exception {
        PlayerTickHandler.clearCaches();

        assertTrue(loads().isEmpty());
        assertTrue(positions().isEmpty());
        assertTrue(distances().isEmpty());
    }

    @Test
    void clearingTravelOnUntransformedTicksKeepsNoFuelMessageSentinel() throws Exception {
        PlayerFuelTick.forget(first);

        assertEquals(-999, loads().get(first));
        assertFalse(positions().containsKey(first));
        assertFalse(distances().containsKey(first));
        assertTrue(positions().containsKey(second));
    }

    @Test
    void invalidatingLoadDoesNotResetUnspentTravel() throws Exception {
        PlayerLoadoutTick.forget(first);

        assertFalse(loads().containsKey(first));
        assertEquals(4.5, distances().get(first));
        assertEquals(new Vec3(100, 64, 100), positions().get(first));
    }

    @Test
    void offlineSweepPreservesOnlinePlayersAcrossAllModules() throws Exception {
        Set<UUID> online = Set.of(second);
        PlayerLoadoutTick.retainOnline(online);
        PlayerFuelTick.retainOnline(online);

        assertEquals(Map.of(second, 42), loads());
        assertEquals(Set.of(second), positions().keySet());
        assertEquals(Map.of(second, 8.5), distances());

        PlayerLoadoutTick.retainOnline(Set.of());
        PlayerFuelTick.retainOnline(Set.of());
        assertTrue(loads().isEmpty());
        assertTrue(positions().isEmpty());
        assertTrue(distances().isEmpty());
    }

    // 测试通过反射预置真实缓存，避免为测试向生产模块暴露可变集合或依赖启动世界。
    private static Map<UUID, Integer> loads() throws Exception {
        return cache(PlayerLoadoutTick.class, "lastWeaponLoad");
    }

    private static Map<UUID, Vec3> positions() throws Exception {
        return cache(PlayerFuelTick.class, "lastPlayerPos");
    }

    private static Map<UUID, Double> distances() throws Exception {
        return cache(PlayerFuelTick.class, "accumulatedDistance");
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<UUID, T> cache(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<UUID, T>) field.get(null);
    }
}
