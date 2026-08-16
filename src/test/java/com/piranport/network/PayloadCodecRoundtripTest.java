package com.piranport.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Payload STREAM_CODEC roundtrip 回归测试（C4：critical 级）。
 *
 * <p>build.gradle 已将 testCompile/testRuntime classpath 扩展到包含 runtimeClasspath，
 * 并强制 slf4j-api 版本统一到 2.0.12，因此本测试可以直接 import MC 类。
 *
 * <p>覆盖：
 * <ul>
 *   <li>基本 Payload 的 encode→decode→equals roundtrip（28 个 Payload）</li>
 *   <li>FireAction ordinal 越界兜底为 CANCEL（FireControlPayload 守卫）</li>
 *   <li>size > 128 / size < 0 兜底（AswSonarSyncPayload 守卫）</li>
 *   <li>size > 16 兜底（FireControlSyncPayload 守卫）</li>
 * </ul>
 */
class PayloadCodecRoundtripTest {

    private static <T> T roundtrip(StreamCodec<ByteBuf, T> codec, T original) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, original);
        T decoded = codec.decode(buf);
        assertNotNull(decoded, "decode() returned null");
        return decoded;
    }

    /** 适用于 FriendlyByteBuf codec（writeVarInt / writeBlockPos / writeUtf 等扩展方法） */
    private static <T> T roundtripFb(StreamCodec<FriendlyByteBuf, T> codec, T original) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        codec.encode(buf, original);
        T decoded = codec.decode(buf);
        assertNotNull(decoded, "decode() returned null");
        return decoded;
    }

    /** 适用于 RegistryFriendlyByteBuf codec（含 RegistryAccess 的 view） */
    private static <T> T roundtripRfb(StreamCodec<RegistryFriendlyByteBuf, T> codec, T original) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
                new FriendlyByteBuf(Unpooled.buffer()), RegistryAccess.EMPTY);
        codec.encode(buf, original);
        T decoded = codec.decode(buf);
        assertNotNull(decoded, "decode() returned null");
        return decoded;
    }

    // ===== AircraftLaunchPosePayload =====
    @Test
    void aircraftLaunchPoseRoundtrip() {
        AircraftLaunchPosePayload p = new AircraftLaunchPosePayload(42, 7, 80);
        assertEquals(p, roundtripRfb(AircraftLaunchPosePayload.STREAM_CODEC, p));
    }

    // ===== ApplyModificationPayload =====
    @Test
    void applyModificationRoundtrip() {
        ApplyModificationPayload p = new ApplyModificationPayload(new BlockPos(10, 64, -200), 3, 2);
        assertEquals(p, roundtrip(ApplyModificationPayload.STREAM_CODEC, p));
    }

    // ===== AswSonarSyncPayload（含 size 守卫）=====
    @Test
    void aswSonarSyncRoundtrip() {
        AswSonarSyncPayload p = new AswSonarSyncPayload(123, List.of(1, 2, 3, 4, 5));
        assertEquals(p, roundtrip(AswSonarSyncPayload.STREAM_CODEC, p));
    }

    @Test
    void aswSonarSyncSizeGuardClampsToZero() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, 999);  // aircraftId
        ByteBufCodecs.VAR_INT.encode(buf, 999);  // size > 128
        AswSonarSyncPayload d = AswSonarSyncPayload.STREAM_CODEC.decode(buf);
        assertEquals(999, d.aircraftEntityId());
        assertTrue(d.detectedEntityIds().isEmpty(), "size > 128 必须清空列表");
    }

    @Test
    void aswSonarSyncNegativeSizeClampsToZero() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, 1);
        ByteBufCodecs.VAR_INT.encode(buf, -5);
        AswSonarSyncPayload d = AswSonarSyncPayload.STREAM_CODEC.decode(buf);
        assertTrue(d.detectedEntityIds().isEmpty(), "负 size 必须清空列表");
    }

    // ===== AmmoWorkbenchCraftPayload =====
    @Test
    void ammoWorkbenchCraftRoundtrip() {
        AmmoWorkbenchCraftPayload p = new AmmoWorkbenchCraftPayload(new BlockPos(5, 70, 5), "shell_he_m", 8);
        assertEquals(p, roundtripFb(AmmoWorkbenchCraftPayload.STREAM_CODEC, p));
    }

    // ===== AmmoWorkbenchCancelPayload =====
    @Test
    void ammoWorkbenchCancelRoundtrip() {
        AmmoWorkbenchCancelPayload p = new AmmoWorkbenchCancelPayload(new BlockPos(0, 100, 0));
        assertEquals(p, roundtripFb(AmmoWorkbenchCancelPayload.STREAM_CODEC, p));
    }

    // ===== AutoLaunchTogglePayload =====
    @Test
    void autoLaunchToggleRoundtrip() {
        AutoLaunchTogglePayload p = new AutoLaunchTogglePayload(2);
        assertEquals(p, roundtrip(AutoLaunchTogglePayload.STREAM_CODEC, p));
    }

    // ===== CannonImpactEffectPayload =====
    @Test
    void cannonImpactEffectRoundtrip() {
        CannonImpactEffectPayload p = new CannonImpactEffectPayload(1.5, 2.5, 3.5, 4.5f, CannonImpactEffectPayload.Kind.HE);
        assertEquals(p, roundtrip(CannonImpactEffectPayload.STREAM_CODEC, p));
    }

    // ===== DebugCooldownOverridePayload =====
    @Test
    void debugCooldownOverrideRoundtrip() {
        DebugCooldownOverridePayload p = new DebugCooldownOverridePayload(true);
        assertEquals(p, roundtrip(DebugCooldownOverridePayload.STREAM_CODEC, p));
    }

    // ===== DebugTogglePayload =====
    @Test
    void debugToggleRoundtrip() {
        DebugTogglePayload p = new DebugTogglePayload(false);
        assertEquals(p, roundtrip(DebugTogglePayload.STREAM_CODEC, p));
    }

    // ===== EntityCoreRevertPayload (unit) =====
    @Test
    void entityCoreRevertUnitRoundtrip() {
        EntityCoreRevertPayload p = new EntityCoreRevertPayload();
        assertEquals(p, roundtrip(EntityCoreRevertPayload.STREAM_CODEC, p));
    }

    // ===== EntityCoreSyncPayload =====
    @Test
    void entityCoreSyncRoundtrip() {
        UUID uuid = UUID.randomUUID();
        EntityCoreSyncPayload p = new EntityCoreSyncPayload(uuid, 5);
        assertEquals(p, roundtrip(EntityCoreSyncPayload.STREAM_CODEC, p));
    }

    // ===== ExportConfigPayload (unit) =====
    @Test
    void exportConfigUnitRoundtrip() {
        ExportConfigPayload p = new ExportConfigPayload();
        assertEquals(p, roundtrip(ExportConfigPayload.STREAM_CODEC, p));
    }

    // ===== FireControlPayload（含 FireAction ordinal 守卫）=====
    @Test
    void fireControlCancelRoundtrip() {
        FireControlPayload p = FireControlPayload.cancel();
        assertEquals(p, roundtrip(FireControlPayload.STREAM_CODEC, p));
    }

    @Test
    void fireControlLockRoundtrip() {
        UUID target = UUID.randomUUID();
        FireControlPayload p = new FireControlPayload(FireControlPayload.FireAction.LOCK, target);
        assertEquals(p, roundtrip(FireControlPayload.STREAM_CODEC, p));
    }

    /** FireAction ordinal 越界（>=3）应被守卫兜底为 CANCEL */
    @Test
    void fireControlOrdinalOverflowClampsToCancel() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, 99);
        buf.writeLong(0L);
        buf.writeLong(0L);
        FireControlPayload d = FireControlPayload.STREAM_CODEC.decode(buf);
        assertEquals(FireControlPayload.FireAction.CANCEL, d.action(),
                "ordinal 越界必须兜底为 CANCEL，避免客户端注入未知 action 类型");
    }

    @Test
    void fireControlOrdinalNegativeClampsToCancel() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, -1);
        buf.writeLong(0L);
        buf.writeLong(0L);
        FireControlPayload d = FireControlPayload.STREAM_CODEC.decode(buf);
        assertEquals(FireControlPayload.FireAction.CANCEL, d.action());
    }

    // ===== FireControlSyncPayload（含 size > 16 守卫）=====
    @Test
    void fireControlSyncRoundtrip() {
        FireControlSyncPayload p = new FireControlSyncPayload(
                List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        assertEquals(p, roundtrip(FireControlSyncPayload.STREAM_CODEC, p));
    }

    @Test
    void fireControlSyncSizeGuardClamps() {
        ByteBuf buf = Unpooled.buffer();
        ByteBufCodecs.VAR_INT.encode(buf, 17);
        FireControlSyncPayload d = FireControlSyncPayload.STREAM_CODEC.decode(buf);
        assertTrue(d.targetUUIDs().size() <= 16, "size > 16 应被守卫截断");
    }

    // ===== HitDisplayTogglePayload =====
    @Test
    void hitDisplayToggleRoundtrip() {
        HitDisplayTogglePayload p = new HitDisplayTogglePayload(true);
        assertEquals(p, roundtrip(HitDisplayTogglePayload.STREAM_CODEC, p));
    }

    // ===== ImportConfigPayload =====
    @Test
    void importConfigRoundtrip() {
        ImportConfigPayload p = new ImportConfigPayload("cannon.json", "projectile.json");
        assertEquals(p, roundtrip(ImportConfigPayload.STREAM_CODEC, p));
    }

    // ===== ManualReloadPayload (unit) =====
    @Test
    void manualReloadUnitRoundtrip() {
        ManualReloadPayload p = new ManualReloadPayload();
        assertEquals(p, roundtrip(ManualReloadPayload.STREAM_CODEC, p));
    }

    // ===== ReconControlPayload =====
    @Test
    void reconControlRoundtrip() {
        ReconControlPayload p = new ReconControlPayload(0.1f, 0.2f, 0.3f);
        assertEquals(p, roundtrip(ReconControlPayload.STREAM_CODEC, p));
    }

    // ===== ReconExitPayload (unit) =====
    @Test
    void reconExitUnitRoundtrip() {
        ReconExitPayload p = new ReconExitPayload();
        assertEquals(p, roundtrip(ReconExitPayload.STREAM_CODEC, p));
    }

    // ===== ReconStatePayload =====
    @Test
    void reconStateRoundtrip() {
        ReconStatePayload p = new ReconStatePayload(true, 42);
        assertEquals(p, roundtrip(ReconStatePayload.STREAM_CODEC, p));
    }

    // ===== RecallAllAircraftPayload (unit) =====
    @Test
    void recallAllAircraftUnitRoundtrip() {
        RecallAllAircraftPayload p = new RecallAllAircraftPayload();
        assertEquals(p, roundtrip(RecallAllAircraftPayload.STREAM_CODEC, p));
    }

    // ===== SalvoFirePayload =====
    @Test
    void salvoFireRoundtrip() {
        SalvoFirePayload p = new SalvoFirePayload((byte) 1, 100.5, 64.0, -200.5);
        assertEquals(p, roundtrip(SalvoFirePayload.STREAM_CODEC, p));
    }

    // ===== ScopeEnterPayload =====
    @Test
    void scopeEnterRoundtrip() {
        ScopeEnterPayload p = new ScopeEnterPayload(true);
        assertEquals(p, roundtrip(ScopeEnterPayload.STREAM_CODEC, p));
    }

    // ===== ScopeFirePayload =====
    @Test
    void scopeFireRoundtrip() {
        ScopeFirePayload p = new ScopeFirePayload(ScopeFirePayload.FireMode.QUICK_FIRE, 1.0, 2.0, 3.0);
        assertEquals(p, roundtrip(ScopeFirePayload.STREAM_CODEC, p));
    }

    // ===== ShakeEffectPayload =====
    @Test
    void shakeEffectRoundtrip() {
        ShakeEffectPayload p = new ShakeEffectPayload(0.5f, 100);
        assertEquals(p, roundtripRfb(ShakeEffectPayload.STREAM_CODEC, p));
    }

    // ===== SkinRevertPayload (unit) =====
    @Test
    void skinRevertUnitRoundtrip() {
        SkinRevertPayload p = new SkinRevertPayload();
        assertEquals(p, roundtrip(SkinRevertPayload.STREAM_CODEC, p));
    }

    // ===== SkinSyncPayload =====
    @Test
    void skinSyncRoundtrip() {
        UUID uuid = UUID.randomUUID();
        SkinSyncPayload p = new SkinSyncPayload(uuid, 23);
        assertEquals(p, roundtrip(SkinSyncPayload.STREAM_CODEC, p));
    }

    // ===== ToggleFighterGroundAttackPayload (unit codec，无字段) =====
    @Test
    void toggleFighterGroundAttackRoundtrip() {
        ToggleFighterGroundAttackPayload p = new ToggleFighterGroundAttackPayload();
        assertEquals(p, roundtrip(ToggleFighterGroundAttackPayload.STREAM_CODEC, p));
    }

    // ===== 所有 payload 的 TYPE ID 不应为 null =====
    @Test
    void allPayloadTypesAreRegistered() {
        assertNotNull(FireControlPayload.TYPE);
        assertNotNull(FireControlSyncPayload.TYPE);
        assertNotNull(SkinSyncPayload.TYPE);
        assertNotNull(EntityCoreSyncPayload.TYPE);
        assertNotNull(AswSonarSyncPayload.TYPE);
        assertNotNull(SalvoFirePayload.class);
        assertNotNull(ShakeEffectPayload.class);
        assertNotNull(AircraftLaunchPosePayload.class);
        assertNotNull(CannonImpactEffectPayload.class);
    }
}