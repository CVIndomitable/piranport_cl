package com.piranport.client.input;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AutoLaunchTogglePayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.ManualReloadPayload;
import com.piranport.network.ToggleFighterGroundAttackPayload;
import com.piranport.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * 火控按键处理（P/O/I键）和战斗机对地/升空/装填功能键（U/H/R键）。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 */
public class FireControlInputHandler {

    private static final double FIRE_CONTROL_RANGE = 80.0;

    private FireControlInputHandler() {}

    /** 处理火控按键：锁定(P)/追加(O)/取消(I)。 */
    public static void handleFireControlKeys(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;

        while (ModKeyMappings.FIRE_CONTROL_LOCK.consumeClick()) {
            if (!transformed && !inReconMode) continue;
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);
            if (target != null) {
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.LOCK, target.getUUID()));
            }
        }

        while (ModKeyMappings.FIRE_CONTROL_ADD.consumeClick()) {
            if (!transformed && !inReconMode) continue;
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);
            if (target != null) {
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.ADD, target.getUUID()));
            }
        }

        while (ModKeyMappings.FIRE_CONTROL_CANCEL.consumeClick()) {
            PacketDistributor.sendToServer(FireControlPayload.cancel());
            ClientFireControlData.clear();
        }
    }

    /** 处理 U 键 — 切换战斗机是否攻击地面目标。 */
    public static void handleFighterGroundAttackKey(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;
        while (ModKeyMappings.TOGGLE_FIGHTER_GROUND_ATTACK.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new ToggleFighterGroundAttackPayload());
        }
    }

    /** 处理 H 键 — 切换战斗机自动升空。 */
    public static void handleAutoLaunchKey(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;
        while (ModKeyMappings.TOGGLE_AUTO_LAUNCH.consumeClick()) {
            if (!transformed || inReconMode) continue;
            int autoSlot = findCoreSlot(mc.player);
            if (autoSlot >= 0) {
                PacketDistributor.sendToServer(new AutoLaunchTogglePayload(autoSlot));
            }
        }
    }

    /** 处理 R 键 — 手动装填（鱼雷/导弹）。 */
    public static void handleManualReloadKey(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;
        while (ModKeyMappings.MANUAL_RELOAD.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new ManualReloadPayload());
        }
    }

    /** 查找活跃变身核心所在的背包槽位，未找到返回 -1。 */
    public static int findCoreSlot(Player player) {
        int slot = player.getInventory().selected;
        ItemStack mh = player.getMainHandItem();
        if (mh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(mh)) {
            return slot;
        }
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack s = player.getInventory().items.get(i);
            if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                return i;
            }
        }
        ItemStack oh = player.getInventory().offhand.get(0);
        if (oh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(oh)) {
            return 40;
        }
        return -1;
    }

    @Nullable
    public static Entity getTargetInCrosshair(Minecraft mc, double range) {
        if (mc.player == null || mc.level == null) return null;
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) cameraEntity = mc.player;
        Vec3 eyePos = cameraEntity.getEyePosition();
        // 在侦察模式下使用侦察机的视线方向，而非玩家身体的朝向
        Vec3 lookDir = cameraEntity.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(range));

        AABB searchBox = cameraEntity.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(1.0);

        final Entity cam = cameraEntity;
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, mc.player, eyePos, end, searchBox,
                e -> (e instanceof LivingEntity || e instanceof com.piranport.entity.AircraftEntity)
                        && e.isAlive() && e != mc.player && e != cam
                        && !(e instanceof net.minecraft.world.Container),
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }
}
