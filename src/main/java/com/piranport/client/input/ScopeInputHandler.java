package com.piranport.client.input;

import com.piranport.artillery.ArtilleryItem;
import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.ClientScopeHandler;
import com.piranport.component.LoadedAmmo;
import com.piranport.config.ModArtilleryConfig;
import com.piranport.network.SalvoFirePayload;
import com.piranport.network.ScopeEnterPayload;
import com.piranport.network.ScopeFirePayload;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 瞄准镜输入处理：按住右键进入瞄准镜，左键发射，松开右键退出。
 * 状态管理委托给 {@link ClientScopeHandler}。
 *
 * <p><b>开镜延迟</b>: 开镜阈值恒为 1 tick，右键按下当 tick 即完全开镜，闭镜退镜同样即时。
 *
 * <p><b>落点算法</b>: 只要手持火炮就每 tick 维护准星射线，闭镜与开镜使用同一套弹道解算落点，
 * 差别只在服务端散布 ×1.1（策划决策/武器/13 方案 B）。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 */
public class ScopeInputHandler {

    private static boolean attackWasDown = false;
    private static boolean useWasDown = false;

    // 双击检测状态
    private static long lastAttackClickTick = -1;
    private static Item lastAttackClickItemType = null;
    private static final int DOUBLE_CLICK_WINDOW_TICKS = 15;

    // 自动装填火炮按住连射的「等一次空膛」状态，详见 tickAutoFireHold。
    private static Item autoFireWaitItem = null;
    private static boolean autoFireWaitingForReload = false;
    /** 进入「等空膛」时的 gameTime，用于超时重试（三式弹撞实体上限时服务端不清膛）。 */
    private static long autoFireWaitingSinceTick = -1;

    private ScopeInputHandler() {}

    public static void reset() {
        useWasDown = false;
        attackWasDown = false;
        lastAttackClickTick = -1;
        lastAttackClickItemType = null;
        clearAutoFireHold();
    }

    /**
     * 只清右键边沿，保留左键边沿与双击检测状态。
     * 打开 GUI 时用：右键边沿必须清（否则关界面后残留按住状态会立刻重新开镜），
     * 但左键边沿不能清（否则关界面时仍按着的左键会被当成一次新点击而走火）。
     */
    public static void resetUseEdge() {
        useWasDown = false;
    }

    /** 每 tick 处理瞄准镜的右键/左键输入。 */
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        boolean holdingCannon = ClientScopeHandler.isHoldingCannon(mc.player);
        boolean isScoping = ClientScopeHandler.isScoping();

        if (!holdingCannon && isScoping) {
            ClientScopeHandler.exitScope();
            if (mc.getConnection() != null) {
                PacketDistributor.sendToServer(new ScopeEnterPayload(false));
            }
            useWasDown = false;
            attackWasDown = false;
            return;
        }
        // 不再持有火炮：清理闭镜准星射线，避免残留落点被下次开火使用。
        if (!holdingCannon) {
            ClientScopeHandler.clearQuickAim();
            clearAutoFireHold();
        }

        // 进入瞄准镜时关闭弹药轮盘
        if (isScoping && AmmoSelectOverlay.isOpen()) {
            AmmoSelectOverlay.close();
        }

        // 右键：按住进入瞄准镜，松开退出；客户端状态由 ArtilleryItem.use() 和这里共同兜底。
        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !useWasDown && holdingCannon) {
            if (!ClientScopeHandler.isScoping()) {
                ClientScopeHandler.enterScope(mc.player, mc.player.getMainHandItem());
            }
            if (mc.getConnection() != null) {
                PacketDistributor.sendToServer(new ScopeEnterPayload(true));
            }
        } else if (!useDown && useWasDown && isScoping) {
            ClientScopeHandler.exitScope();
            if (mc.getConnection() != null) {
                PacketDistributor.sendToServer(new ScopeEnterPayload(false));
            }
        }
        useWasDown = useDown;

        // 只要手持火炮就维护准星射线：闭镜与开镜走同一套落点解算（策划决策/武器/13 方案 B）。
        if (holdingCannon) {
            if (isScoping) {
                ClientScopeHandler.tick(mc.player, mc.player.getMainHandItem());
            } else {
                ClientScopeHandler.tickQuickAim(mc.player, mc.player.getMainHandItem());
            }
        }

        // 左键：开火（单击 / 双击）。弹药轮盘或任意界面打开时禁止开炮，避免误触发射。
        boolean attackDown = mc.options.keyAttack.isDown();
        boolean canFire = holdingCannon && mc.screen == null && !AmmoSelectOverlay.isOpen();
        if (attackDown && !attackWasDown && canFire) {
            // 按下的第一发仍走单击/双击语义；自动炮此后的连射由 tickAutoFireHold 接管。
            handleAttackPress(mc);
        } else if (attackDown && attackWasDown && canFire) {
            tickAutoFireHold(mc);
        }
        if (!attackDown) {
            // 松键结束本轮连射：再按下的第一发回到边沿逻辑。
            clearAutoFireHold();
        }
        attackWasDown = attackDown;
    }

    /** 左键按下的第一发：双击齐射，否则单击射一发。 */
    private static void handleAttackPress(Minecraft mc) {
        if (mc.getConnection() == null) return;

        ItemStack heldStack = mc.player.getMainHandItem();
        boolean isArtillery = heldStack.getItem() instanceof ArtilleryItem;
        // 记录按下瞬间客户端看到的装填状态，供连射的「等一次空膛」判断（见 tickAutoFireHold）。
        boolean hadAmmo = isArtillery && hasLoadedAmmo(heldStack);

        // 双击检测：同类型火炮，且在窗口内
        if (isArtillery && lastAttackClickTick >= 0
                && (mc.level.getGameTime() - lastAttackClickTick) <= DOUBLE_CLICK_WINDOW_TICKS
                && lastAttackClickItemType == heldStack.getItem()) {
            // 双击：齐射所有同型炮
            Vec3 target = aimTargetOrNull();
            if (target != null) {
                PacketDistributor.sendToServer(SalvoFirePayload.aimedFire(target.x, target.y, target.z));
            } else {
                PacketDistributor.sendToServer(SalvoFirePayload.maxRangeFire());
            }
            // 重置，防三击被当作新一轮双击
            lastAttackClickTick = -1;
            lastAttackClickItemType = null;
        } else {
            // 单击
            fireAimedOrMaxRange(mc);
            // 记录供双击检测
            if (isArtillery) {
                lastAttackClickTick = mc.level.getGameTime();
                lastAttackClickItemType = heldStack.getItem();
            } else {
                lastAttackClickTick = -1;
                lastAttackClickItemType = null;
            }
        }

        suppressCannonHandAnimation(mc);
        if (isArtillery && ((ArtilleryItem) heldStack.getItem()).isAutoLoading()) {
            // 自动炮按住连射：按下时若客户端确实看见膛内有弹，就要等一次空膛再发下一发；
            // 若按下时就看见空膛，装填完成即是新的一发，无需再等。
            autoFireWaitItem = heldStack.getItem();
            autoFireWaitingForReload = hadAmmo;
            if (hadAmmo) autoFireWaitingSinceTick = mc.level.getGameTime();
        }
    }

    /**
     * 自动装填火炮按住开火键连射：每个 tick 检查膛内状态，装好一发就再打一发。
     *
     * <p>未装填时不发包，避免每 tick 空包；手动装填火炮不走这里，仍是单击/双击一次。
     *
     * <p>WHY 要等一次「空膛」观测才允许下一发：服务端清空 LOADED_AMMO 的同步比客户端发包
     * 晚一 tick，只按 hasAmmo 判断会在同一轮里重复发包，服务端回「未装填」提示。
     *
     * <p>WHY 还要有超时重试：三式弹一次生成几十颗弹丸，撞上实体上限（perf_shrapnel_limit 等）时
     * 服务端放弃这次射击、<b>不清空膛内弹药</b>。死等空膛就会永远等不到，表现为三式弹不能连射。
     * 等满 {@link ModArtilleryConfig#AUTO_FIRE_RETRY_TICKS} 仍未见空膛，就把上一发当作被服务端
     * 拒绝，允许重发；上限释放后自动接上。
     */
    private static void tickAutoFireHold(Minecraft mc) {
        ItemStack heldStack = mc.player.getMainHandItem();
        if (!(heldStack.getItem() instanceof ArtilleryItem artillery) || !artillery.isAutoLoading()) {
            // 换成手动炮/非火炮：旧炮的连射状态不能留着。
            clearAutoFireHold();
            return;
        }
        // 换炮后旧炮的「等空膛」不能继承到新炮，否则新炮装好弹也永远等不到空膛。
        if (autoFireWaitItem != heldStack.getItem()) {
            autoFireWaitItem = heldStack.getItem();
            autoFireWaitingForReload = false;
        }
        if (!hasLoadedAmmo(heldStack)) {
            // 已经看到空膛，下一次装填完成才是新的一发。
            autoFireWaitingForReload = false;
            return;
        }
        if (autoFireWaitingForReload) {
            long now = mc.level.getGameTime();
            if (now - autoFireWaitingSinceTick < ModArtilleryConfig.AUTO_FIRE_RETRY_TICKS.get()) {
                return;
            }
            // 超过重试间隔膛内仍有弹：上一发大概率被实体上限等条件拒绝，按未发出处理再试一次。
            autoFireWaitingForReload = false;
        }
        if (!fireAimedOrMaxRange(mc)) return;
        autoFireWaitingForReload = true;
        autoFireWaitingSinceTick = mc.level.getGameTime();
        suppressCannonHandAnimation(mc);
    }

    /** 当前准星落点；没有有效目标时返回 null（调用方回退最大射程）。 */
    @Nullable
    private static Vec3 aimTargetOrNull() {
        if (!ClientScopeHandler.hasValidTarget()) return null;
        return ClientScopeHandler.getAimedPosition();
    }

    /**
     * 单击与连射共用：有准星落点走弹道解算，否则按最大射程仰角开火。
     * 开镜/闭镜同一套算法，差别只在服务端散布 ×1.1（策划决策/武器/13 方案 B）。
     *
     * @return 是否已把开火请求发给服务端
     */
    private static boolean fireAimedOrMaxRange(Minecraft mc) {
        if (mc.getConnection() == null) return false;
        Vec3 target = aimTargetOrNull();
        if (target != null) {
            PacketDistributor.sendToServer(ScopeFirePayload.aimedFire(target.x, target.y, target.z));
        } else {
            PacketDistributor.sendToServer(ScopeFirePayload.maxRangeFire());
        }
        return true;
    }

    /**
     * 客户端看到的膛内状态。判据与 ArtilleryItem/WeaponState 一致：LOADED_AMMO 是否有弹。
     * 该组件已 networkSynchronized，客户端渲染器本来就读它判断空膛/装填两态。
     */
    private static boolean hasLoadedAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY).hasAmmo();
    }

    /** 换弹种会让服务端重启读条，客户端连射的「等空膛/重试」状态一并作废。 */
    public static void onAmmoSelectionChanged() {
        clearAutoFireHold();
    }

    private static void clearAutoFireHold() {
        autoFireWaitItem = null;
        autoFireWaitingForReload = false;
        autoFireWaitingSinceTick = -1;
    }

    private static void suppressCannonHandAnimation(Minecraft mc) {
        if (mc.player == null) return;
        mc.player.attackAnim = 0.0f;
        mc.player.oAttackAnim = 0.0f;
        mc.player.swinging = false;
        mc.player.swingTime = 0;
    }
}
