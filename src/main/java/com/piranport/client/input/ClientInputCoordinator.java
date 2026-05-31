package com.piranport.client.input;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.CameraShakeHandler;
import com.piranport.client.CannonImpactEffects;
import com.piranport.client.ClientTorpedoGuidance;
import com.piranport.client.EntityUuidCache;
import com.piranport.client.ModKeyMappings;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 客户端输入主协调器 — 负责调度各个子系统 Handler。
 * 自身不包含业务逻辑，仅按顺序调用各 Handler 的静态方法。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 *
 * @see FireControlInputHandler
 * @see TorpedoGuidanceInputHandler
 * @see ReconInputHandler
 * @see EntityHighlightHandler
 * @see AmmoSelectionHandler
 * @see DebugInputHandler
 * @see ScopeInputHandler
 */
@EventBusSubscriber(modid = com.piranport.PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientInputCoordinator {

    private ClientInputCoordinator() {}

    /** 重置所有客户端静态状态（断开连接时调用）。 */
    public static void resetClientState() {
        if (AmmoSelectOverlay.isOpen()) {
            AmmoSelectOverlay.close();
        }
        EntityHighlightHandler.reset();
        com.piranport.aviation.ClientAswSonarData.resetClientState();
        ClientTorpedoGuidance.resetClientState();
        com.piranport.client.ClientScopeHandler.clear();
        CannonImpactEffects.clear();
        DebugInputHandler.reset();
        ScopeInputHandler.reset();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 检测轮盘打开时玩家打开了其他 Screen
        if (AmmoSelectOverlay.isOpen() && mc.screen != null) {
            AmmoSelectOverlay.close();
        }

        CameraShakeHandler.tick();
        CannonImpactEffects.tick();

        // 1) 鱼雷制导模式
        boolean inTorpedoGuidance = TorpedoGuidanceInputHandler.tick(mc);

        // 2) 侦察模式
        boolean inReconMode = ReconInputHandler.tick(mc);

        boolean transformed = TransformationManager.isPlayerTransformed(mc.player);

        // 3) 大型船变身态：压平 hurtTime
        if (transformed) {
            ItemStack coreStack = TransformationManager.findTransformedCore(mc.player);
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ShipCoreItem sci
                    && sci.getShipType() == ShipType.LARGE) {
                if (mc.player.hurtTime > 0) {
                    mc.player.hurtTime = 0;
                    mc.player.hurtDuration = 0;
                }
            }
        }

        // 4) 火控按键 (P/O/I)
        FireControlInputHandler.handleFireControlKeys(mc, transformed, inReconMode);

        // 5) 功能键 (U/H/R)
        FireControlInputHandler.handleFighterGroundAttackKey(mc, transformed, inReconMode);
        FireControlInputHandler.handleAutoLaunchKey(mc, transformed, inReconMode);
        FireControlInputHandler.handleManualReloadKey(mc, transformed, inReconMode);

        // 6) 弹药选择轮盘 (Tab)
        AmmoSelectionHandler.handleAmmoWheel(mc, mc.player, transformed, inReconMode);

        // 7) 调试快捷键 (F8/N/J)
        DebugInputHandler.handleDebugKeys(mc);

        // 8) 实体高亮 (Y键 + 火控 + 声呐)
        if (mc.level != null) {
            Player localPlayer = mc.player;
            List<UUID> fcTargets = ClientFireControlData.getTargets();
            Set<UUID> lockedTargets = fcTargets.isEmpty()
                    ? java.util.Collections.emptySet()
                    : new HashSet<>(fcTargets);
            boolean hasFcTargets = !lockedTargets.isEmpty();

            // 处理 Y 键切换
            while (ModKeyMappings.HIGHLIGHT_ENTITIES.consumeClick()) {
                EntityHighlightHandler.toggleHighlight(mc);
            }

            // 应用/维持高亮
            EntityHighlightHandler.tick(mc, lockedTargets, hasFcTargets);

            // 反潜声呐高亮
            EntityHighlightHandler.tickAswSonar(mc, lockedTargets);
        }

        // 9) 瞄准镜输入
        ScopeInputHandler.tick(mc);
    }
}
