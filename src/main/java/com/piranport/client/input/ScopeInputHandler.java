package com.piranport.client.input;

import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.ClientScopeHandler;
import com.piranport.network.ScopeEnterPayload;
import com.piranport.network.ScopeFirePayload;
import net.minecraft.client.Minecraft;
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

    private ScopeInputHandler() {}

    public static void reset() {
        useWasDown = false;
        attackWasDown = false;
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

        // 左键：开火
        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !attackWasDown && holdingCannon) {
            if (mc.getConnection() != null) {
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
            }
        }
        attackWasDown = attackDown;

        if (isScoping) {
            ClientScopeHandler.tick(mc.player, mc.player.getMainHandItem());
        }
    }
}
