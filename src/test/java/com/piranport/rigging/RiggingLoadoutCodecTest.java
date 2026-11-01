package com.piranport.rigging;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RiggingLoadout#STREAM_CODEC} 的往返测试。
 *
 * <p>ItemStack 的 StreamCodec 需要真实物品注册表，所以按仓库既有约定打
 * {@code mc-bootstrap} 标签，由独立的 {@code mcBootstrapTest} JVM 执行。
 *
 * <p>断言不能直接 {@code assertEquals(original, decoded)}：记录里的 ItemStack 没有重写
 * equals，按引用比较，必须逐项比 socket/category 并走 {@link ItemStack#matches}。
 */
@Tag("mc-bootstrap")
class RiggingLoadoutCodecTest {

    private static boolean bootstrapped;

    @BeforeAll
    static void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        SharedConstants.tryDetectVersion();
        // 纯 JUnit 环境没有 FML 启动流程；FeatureFlags.<clinit> 会读 LoadingModList。
        // 塞一个空列表进去，加载器就会认为「没有模组声明特性开关」，bootstrap 得以继续。
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        Bootstrap.bootStrap();
        // 纯 JUnit 环境没有 NeoForge 的注册表同步阶段，item 注册表默认 doesSync=false，
        // ItemStack.STREAM_CODEC 会拒绝按 ID 编解码。这里只给测试 JVM 打上同步标记。
        markItemRegistrySynced();
    }

    private static void markItemRegistrySynced() {
        try {
            var setSync = Class.forName("net.neoforged.neoforge.registries.BaseMappedRegistry")
                    .getDeclaredMethod("setSync", boolean.class);
            setSync.setAccessible(true);
            setSync.invoke(BuiltInRegistries.ITEM, true);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法把 item 注册表标记为同步", e);
        }
    }

    @Test
    void roundTripsFullLoadout() {
        RiggingLoadout original = new RiggingLoadout(
                List.of(
                        new RiggingMount(RiggingSocket.LEG_LEFT, RiggingCategory.TORPEDO,
                                new ItemStack(Items.DIAMOND_SWORD)),
                        new RiggingMount(RiggingSocket.SIDE_LEFT_1, RiggingCategory.CANNON,
                                new ItemStack(Items.IRON_AXE)),
                        new RiggingMount(RiggingSocket.DECK_1, RiggingCategory.AIRCRAFT,
                                new ItemStack(Items.ELYTRA))),
                true,
                RiggingLoadout.HULL_CARRIER,
                3);

        RegistryFriendlyByteBuf buf = newBuffer();
        RiggingLoadout.STREAM_CODEC.encode(buf, original);
        RiggingLoadout decoded = RiggingLoadout.STREAM_CODEC.decode(buf);

        assertLoadoutMatches(original, decoded);
        assertEquals(3, decoded.mounts().size());
        assertEquals(0, buf.readableBytes(), "decode 应正好消费完 encode 写出的全部字节");
    }

    @Test
    void roundTripsEmptySentinel() {
        RegistryFriendlyByteBuf buf = newBuffer();
        RiggingLoadout.STREAM_CODEC.encode(buf, RiggingLoadout.EMPTY);
        RiggingLoadout decoded = RiggingLoadout.STREAM_CODEC.decode(buf);

        assertTrue(decoded.isEmpty(), "EMPTY 的 selectedSlot 哨兵必须原样往返");
        assertEquals(-1, decoded.selectedSlot());
        assertEquals(0, decoded.mounts().size());
        assertEquals(0, decoded.hullProfile());
        assertFalse(decoded.hasAircraft());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void keepsDisplayStackCountAtOne() {
        ItemStack stacked = new ItemStack(Items.IRON_INGOT, 16);
        RiggingMount mount = new RiggingMount(RiggingSocket.BACK_1, RiggingCategory.DEPTH_CHARGE, stacked);

        assertEquals(1, mount.stack().getCount(), "挂点展示副本不应携带真实数量");
        assertEquals(16, stacked.getCount(), "原物品栈不能被展示副本改动");
    }

    private static void assertLoadoutMatches(RiggingLoadout expected, RiggingLoadout actual) {
        assertEquals(expected.hasAircraft(), actual.hasAircraft());
        assertEquals(expected.hullProfile(), actual.hullProfile());
        assertEquals(expected.selectedSlot(), actual.selectedSlot());
        assertEquals(expected.mounts().size(), actual.mounts().size());
        for (int i = 0; i < expected.mounts().size(); i++) {
            RiggingMount expectedMount = expected.mounts().get(i);
            RiggingMount actualMount = actual.mounts().get(i);
            assertEquals(expectedMount.socket(), actualMount.socket());
            assertEquals(expectedMount.category(), actualMount.category());
            assertEquals(1, actualMount.stack().getCount());
            assertTrue(ItemStack.matches(expectedMount.stack(), actualMount.stack()),
                    "物品栈应逐项一致: " + expectedMount.stack() + " vs " + actualMount.stack());
        }
    }

    private static RegistryFriendlyByteBuf newBuffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(),
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
    }
}
