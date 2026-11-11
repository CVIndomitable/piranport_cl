package com.piranport.client.input;

import com.piranport.client.AmmoSelectOverlay;
import com.piranport.client.ClientScopeHandler;
import com.piranport.network.CannonHoldFirePayload;
import com.piranport.network.ScopeEnterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/** 火炮瞄准与按住开火输入；选炮和射击节奏由服务端负责。 */
public class ScopeInputHandler {
    private static boolean attackWasDown;
    private static boolean useWasDown;
    private static Item activeWeapon;
    private static int activeSlot = -1;

    private ScopeInputHandler() {}

    public static void reset() {
        stopHold(Minecraft.getInstance());
        useWasDown = false;
        attackWasDown = false;
    }

    /** 保留左键边沿，避免关闭界面时把按住误判为新点击。 */
    public static void resetUseEdge() { useWasDown = false; }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        boolean holdingCannon = ClientScopeHandler.isHoldingCannon(mc.player);
        boolean isScoping = ClientScopeHandler.isScoping();
        if (!holdingCannon && isScoping) {
            ClientScopeHandler.exitScope();
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new ScopeEnterPayload(false));
            useWasDown = false;
            stopHold(mc);
            attackWasDown = mc.options.keyAttack.isDown();
            return;
        }
        if (!holdingCannon) ClientScopeHandler.clearQuickAim();
        if (isScoping && AmmoSelectOverlay.isOpen()) AmmoSelectOverlay.close();

        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !useWasDown && holdingCannon) {
            if (!ClientScopeHandler.isScoping()) ClientScopeHandler.enterScope(mc.player, mc.player.getMainHandItem());
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new ScopeEnterPayload(true));
        } else if (!useDown && useWasDown && isScoping) {
            ClientScopeHandler.exitScope();
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new ScopeEnterPayload(false));
        }
        useWasDown = useDown;

        if (holdingCannon) {
            if (isScoping) ClientScopeHandler.tick(mc.player, mc.player.getMainHandItem());
            else ClientScopeHandler.tickQuickAim(mc.player, mc.player.getMainHandItem());
        }

        boolean attackDown = mc.options.keyAttack.isDown();
        boolean canFire = holdingCannon && mc.screen == null && !AmmoSelectOverlay.isOpen();
        if (attackDown && !attackWasDown && canFire && mc.getConnection() != null) {
            activeWeapon = mc.player.getMainHandItem().getItem();
            activeSlot = mc.player.getInventory().selected;
            send(CannonHoldFirePayload.START, aimTargetOrNull());
            suppressCannonHandAnimation(mc);
        } else if (attackDown && attackWasDown && canFire && activeWeapon != null
                && activeSlot == mc.player.getInventory().selected
                && activeWeapon == mc.player.getMainHandItem().getItem() && mc.getConnection() != null) {
            send(CannonHoldFirePayload.UPDATE, aimTargetOrNull());
        } else if (activeWeapon != null) {
            stopHold(mc);
        }
        attackWasDown = attackDown;
    }

    @Nullable
    private static Vec3 aimTargetOrNull() {
        return ClientScopeHandler.hasValidTarget() ? ClientScopeHandler.getAimedPosition() : null;
    }

    private static void send(byte action, @Nullable Vec3 target) {
        PacketDistributor.sendToServer(CannonHoldFirePayload.of(action, target));
    }

    public static void onAmmoSelectionChanged() { stopHold(Minecraft.getInstance()); }

    private static void stopHold(Minecraft mc) {
        if (activeWeapon != null && mc.getConnection() != null) send(CannonHoldFirePayload.STOP, null);
        activeWeapon = null;
        activeSlot = -1;
    }

    private static void suppressCannonHandAnimation(Minecraft mc) {
        if (mc.player == null) return;
        mc.player.attackAnim = 0.0f;
        mc.player.oAttackAnim = 0.0f;
        mc.player.swinging = false;
        mc.player.swingTime = 0;
    }
}
