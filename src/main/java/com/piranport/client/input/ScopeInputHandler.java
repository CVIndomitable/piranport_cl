package com.piranport.client.input;

import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.ClientScopeHandler;
import com.piranport.network.SalvoFirePayload;
import com.piranport.network.ScopeEnterPayload;
import com.piranport.network.ScopeFirePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 瞄准镜输入处理：右键切换瞄准镜，左键发射。
 * 状态管理委托给 {@link ClientScopeHandler}。
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

    private ScopeInputHandler() {}

    public static void reset() {
        useWasDown = false;
        attackWasDown = false;
        lastAttackClickTick = -1;
        lastAttackClickItemType = null;
    }

    /** 每 tick 处理瞄准镜的右键/左键输入。 */
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;

        boolean holdingCannon = ClientScopeHandler.isHoldingCannon(mc.player);
        boolean isScoping = ClientScopeHandler.isScoping();

        if (!holdingCannon && isScoping) {
            ClientScopeHandler.exitScope();
            useWasDown = false;
            attackWasDown = false;
            return;
        }

        // 进入瞄准镜时关闭弹药轮盘
        if (isScoping && AmmoSelectOverlay.isOpen()) {
            AmmoSelectOverlay.close();
        }

        // 右键：由 ArtilleryItem.use() 切换瞄准镜，此处只发网络包
        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !useWasDown && holdingCannon) {
            if (mc.getConnection() != null) {
                PacketDistributor.sendToServer(new ScopeEnterPayload(ClientScopeHandler.isScoping()));
            }
        }
        useWasDown = useDown;

        // 左键：开火（单击 / 双击）
        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !attackWasDown && holdingCannon) {
            boolean fired = false;
            ItemStack heldStack = mc.player.getMainHandItem();
            boolean isArtillery = heldStack.getItem() instanceof com.piranport.artillery.ArtilleryItem;

            // 双击检测：同类型火炮，且在窗口内
            if (isArtillery && lastAttackClickTick >= 0
                    && (mc.level.getGameTime() - lastAttackClickTick) <= DOUBLE_CLICK_WINDOW_TICKS
                    && lastAttackClickItemType == heldStack.getItem()
                    && mc.getConnection() != null) {
                // 双击：齐射所有同型炮
                fired = true;
                if (isScoping) {
                    if (ClientScopeHandler.hasValidTarget() && ClientScopeHandler.getAimedPosition() != null) {
                        Vec3 target = ClientScopeHandler.getAimedPosition();
                        PacketDistributor.sendToServer(SalvoFirePayload.aimedFire(target.x, target.y, target.z));
                    } else {
                        PacketDistributor.sendToServer(SalvoFirePayload.maxRangeFire());
                    }
                } else {
                    PacketDistributor.sendToServer(SalvoFirePayload.quickFire());
                }
                // 重置，防三击被当作新一轮双击
                lastAttackClickTick = -1;
                lastAttackClickItemType = null;
            } else if (mc.getConnection() != null) {
                // 单击
                fired = true;
                if (isScoping) {
                    if (ClientScopeHandler.hasValidTarget() && ClientScopeHandler.getAimedPosition() != null) {
                        Vec3 target = ClientScopeHandler.getAimedPosition();
                        PacketDistributor.sendToServer(ScopeFirePayload.aimedFire(target.x, target.y, target.z));
                    } else {
                        PacketDistributor.sendToServer(ScopeFirePayload.maxRangeFire());
                    }
                } else {
                    PacketDistributor.sendToServer(ScopeFirePayload.quickFire());
                }
                // 记录供双击检测
                if (isArtillery) {
                    lastAttackClickTick = mc.level.getGameTime();
                    lastAttackClickItemType = heldStack.getItem();
                } else {
                    lastAttackClickTick = -1;
                    lastAttackClickItemType = null;
                }
            }
        }
        attackWasDown = attackDown;

        if (isScoping) {
            ClientScopeHandler.tick(mc.player, mc.player.getMainHandItem());
        }
    }
}
