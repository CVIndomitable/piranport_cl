package com.piranport.network;

import com.piranport.PiranPort;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ModPackets {
    public static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(
                ApplyModificationPayload.TYPE,
                ApplyModificationPayload.STREAM_CODEC,
                ApplyModificationPayload::handle
        );
        registrar.playToServer(
                ToggleFighterGroundAttackPayload.TYPE,
                ToggleFighterGroundAttackPayload.STREAM_CODEC,
                ToggleFighterGroundAttackPayload::handle
        );
        // Phase 20: fire control
        registrar.playToServer(
                FireControlPayload.TYPE,
                FireControlPayload.STREAM_CODEC,
                FireControlPayload::handle
        );
        registrar.playToClient(
                FireControlSyncPayload.TYPE,
                FireControlSyncPayload.STREAM_CODEC,
                FireControlSyncPayload::handle
        );
        // Phase 32: recon aircraft
        registrar.playToServer(
                ReconControlPayload.TYPE,
                ReconControlPayload.STREAM_CODEC,
                ReconControlPayload::handle
        );
        registrar.playToServer(
                ReconExitPayload.TYPE,
                ReconExitPayload.STREAM_CODEC,
                ReconExitPayload::handle
        );
        registrar.playToClient(
                ReconStatePayload.TYPE,
                ReconStatePayload.STREAM_CODEC,
                ReconStatePayload::handle
        );
        // Phase 36: ship config
        registrar.playToServer(
                AutoLaunchTogglePayload.TYPE,
                AutoLaunchTogglePayload.STREAM_CODEC,
                AutoLaunchTogglePayload::handle
        );
        // Debug system
        registrar.playToServer(
                DebugTogglePayload.TYPE,
                DebugTogglePayload.STREAM_CODEC,
                DebugTogglePayload::handle
        );
        // S2C: 服务端确认调试开关结果
        registrar.playToClient(
                DebugToggleAckPayload.TYPE,
                DebugToggleAckPayload.STREAM_CODEC,
                DebugToggleAckPayload::handle
        );
        registrar.playToServer(
                SnapshotRequestPayload.TYPE,
                SnapshotRequestPayload.STREAM_CODEC,
                SnapshotRequestPayload::handle
        );
        registrar.playToServer(
                DebugCooldownOverridePayload.TYPE,
                DebugCooldownOverridePayload.STREAM_CODEC,
                DebugCooldownOverridePayload::handle
        );
        // S2C: 测试模式水印（与调试隔离的独立测试工具）
        registrar.playToClient(
                TestModeWatermarkPayload.TYPE,
                TestModeWatermarkPayload.STREAM_CODEC,
                TestModeWatermarkPayload::handle
        );
        registrar.playToServer(
                HitDisplayTogglePayload.TYPE,
                HitDisplayTogglePayload.STREAM_CODEC,
                HitDisplayTogglePayload::handle
        );

        // Empty-hand recall all aircraft
        registrar.playToServer(
                RecallAllAircraftPayload.TYPE,
                RecallAllAircraftPayload.STREAM_CODEC,
                RecallAllAircraftPayload::handle
        );

        // Manual reload (R key) — 仅鱼雷/导弹
        registrar.playToServer(
                ManualReloadPayload.TYPE,
                ManualReloadPayload.STREAM_CODEC,
                ManualReloadPayload::handle
        );

        // Phase 4: ammo type switching (Tab key)
        registrar.playToServer(
                SwitchAmmoPayload.TYPE,
                SwitchAmmoPayload.STREAM_CODEC,
                SwitchAmmoPayload::handle
        );

        // Wire-guided torpedo guidance (view-control mode)
        registrar.playToServer(
                TorpedoGuidanceInputPayload.TYPE,
                TorpedoGuidanceInputPayload.STREAM_CODEC,
                TorpedoGuidanceInputPayload::handle
        );
        registrar.playToServer(
                TorpedoGuidanceExitPayload.TYPE,
                TorpedoGuidanceExitPayload.STREAM_CODEC,
                TorpedoGuidanceExitPayload::handle
        );
        registrar.playToClient(
                TorpedoGuidanceStatePayload.TYPE,
                TorpedoGuidanceStatePayload.STREAM_CODEC,
                TorpedoGuidanceStatePayload::handle
        );

        // ===== ASW Sonar =====
        registrar.playToClient(
                AswSonarSyncPayload.TYPE,
                AswSonarSyncPayload.STREAM_CODEC,
                AswSonarSyncPayload::handle
        );

        // ===== Skin System =====
        registrar.playToClient(
                SkinSyncPayload.TYPE,
                SkinSyncPayload.STREAM_CODEC,
                SkinSyncPayload::handle
        );
        registrar.playToServer(
                SkinRevertPayload.TYPE,
                SkinRevertPayload.STREAM_CODEC,
                SkinRevertPayload::handle
        );

        // ===== Entity Core System =====
        registrar.playToClient(
                EntityCoreSyncPayload.TYPE,
                EntityCoreSyncPayload.STREAM_CODEC,
                EntityCoreSyncPayload::handle
        );
        registrar.playToServer(
                EntityCoreRevertPayload.TYPE,
                EntityCoreRevertPayload.STREAM_CODEC,
                EntityCoreRevertPayload::handle
        );

        // ===== Ammo Workbench =====
        registrar.playToServer(
                AmmoWorkbenchCraftPayload.TYPE,
                AmmoWorkbenchCraftPayload.STREAM_CODEC,
                AmmoWorkbenchCraftPayload::handle
        );
        registrar.playToServer(
                AmmoWorkbenchCancelPayload.TYPE,
                AmmoWorkbenchCancelPayload.STREAM_CODEC,
                AmmoWorkbenchCancelPayload::handle
        );

        // ===== Dungeon System =====（整合版副本系统总体设计 2026-09-07：副本/10 联机大厅已作废）
        // C2S
        registrar.playToServer(
                com.piranport.dungeon.network.SelectNodePayload.TYPE,
                com.piranport.dungeon.network.SelectNodePayload.STREAM_CODEC,
                com.piranport.dungeon.network.SelectNodePayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.ReviveRequestPayload.TYPE,
                com.piranport.dungeon.network.ReviveRequestPayload.STREAM_CODEC,
                com.piranport.dungeon.network.ReviveRequestPayload::handle
        );
        registrar.playToServer(
                com.piranport.dungeon.network.TownScrollUsePayload.TYPE,
                com.piranport.dungeon.network.TownScrollUsePayload.STREAM_CODEC,
                com.piranport.dungeon.network.TownScrollUsePayload::handle
        );
        // S2C
        registrar.playToClient(
                com.piranport.dungeon.network.DungeonStatePayload.TYPE,
                com.piranport.dungeon.network.DungeonStatePayload.STREAM_CODEC,
                com.piranport.dungeon.network.DungeonStatePayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.NodeEnteredPayload.TYPE,
                com.piranport.dungeon.network.NodeEnteredPayload.STREAM_CODEC,
                com.piranport.dungeon.network.NodeEnteredPayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.DungeonResultPayload.TYPE,
                com.piranport.dungeon.network.DungeonResultPayload.STREAM_CODEC,
                com.piranport.dungeon.network.DungeonResultPayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload.TYPE,
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload.STREAM_CODEC,
                com.piranport.dungeon.network.PlayerDiedInDungeonPayload::handle
        );
        registrar.playToClient(
                com.piranport.dungeon.network.DungeonRegistrySyncPayload.TYPE,
                com.piranport.dungeon.network.DungeonRegistrySyncPayload.STREAM_CODEC,
                com.piranport.dungeon.network.DungeonRegistrySyncPayload::handle
        );

        // ===== Phase 5: Fire Control System (Scope) =====
        registrar.playToServer(
                ScopeEnterPayload.TYPE,
                ScopeEnterPayload.STREAM_CODEC,
                ScopeEnterPayload::handle
        );
        registrar.playToServer(
                ScopeFirePayload.TYPE,
                ScopeFirePayload.STREAM_CODEC,
                ScopeFirePayload::handle
        );

        // ===== Salvo Fire (double-click) =====
        registrar.playToServer(
                SalvoFirePayload.TYPE,
                SalvoFirePayload.STREAM_CODEC,
                SalvoFirePayload::handle
        );

        // ===== Phase 10: Screen Shake =====
        registrar.playToClient(
                ShakeEffectPayload.TYPE,
                ShakeEffectPayload.STREAM_CODEC,
                ShakeEffectPayload::handle
        );
        registrar.playToClient(
                AircraftLaunchPosePayload.TYPE,
                AircraftLaunchPosePayload.STREAM_CODEC,
                AircraftLaunchPosePayload::handle
        );
        // 弹道解算统计（服务端→客户端）
        registrar.playToClient(
                SolverStatsPayload.TYPE,
                SolverStatsPayload.STREAM_CODEC,
                SolverStatsPayload::handle
        );
        registrar.playToClient(
                CannonImpactEffectPayload.TYPE,
                CannonImpactEffectPayload.STREAM_CODEC,
                CannonImpactEffectPayload::handle
        );

        // ===== Artillery Config Tool =====
        registrar.playToServer(
                UpdateConfigOverridePayload.TYPE,
                UpdateConfigOverridePayload.STREAM_CODEC,
                UpdateConfigOverridePayload::handle
        );
        registrar.playToServer(
                ExportConfigPayload.TYPE,
                ExportConfigPayload.STREAM_CODEC,
                ExportConfigPayload::handle
        );
        registrar.playToServer(
                ImportConfigPayload.TYPE,
                ImportConfigPayload.STREAM_CODEC,
                ImportConfigPayload::handle
        );
        registrar.playToServer(
                ResetConfigPayload.TYPE,
                ResetConfigPayload.STREAM_CODEC,
                ResetConfigPayload::handle
        );
        registrar.playToServer(
                ResetSingleConfigPayload.TYPE,
                ResetSingleConfigPayload.STREAM_CODEC,
                ResetSingleConfigPayload::handle
        );
        registrar.playToClient(
                SyncConfigOverridesPayload.TYPE,
                SyncConfigOverridesPayload.STREAM_CODEC,
                SyncConfigOverridesPayload::handle
        );
    }
}
