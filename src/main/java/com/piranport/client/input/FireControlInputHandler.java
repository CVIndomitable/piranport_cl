package com.piranport.client.input;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.ToggleAutoModePayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.ManualReloadPayload;
import com.piranport.network.ToggleFighterGroundAttackPayload;
import com.piranport.client.ClientGameEvents;
import com.piranport.client.ModKeyMappings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 火控按键处理(鼠标中键)和战斗机对地/升空/装填功能键(U/H/R键)。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 */
public class FireControlInputHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireControlInputHandler.class);
    private static final double FIRE_CONTROL_RANGE = 80.0;

    private FireControlInputHandler() {}

    /**
     * 处理火控选择键（鼠标中键）：
     * 不蹲下=加选，蹲下=单选，蹲下且准心无实体=清空列表。
     *
     * <p>注意：中键同时是原版 keyPickItem（选取方块），本方法会在变身/侦察状态下
     * 顺带清空 keyPickItem 的点击队列，避免创造模式误替换快捷栏物品；两个键被改绑
     * 到不同物理键时不吞，见 {@code ClientGameEvents#onClientTickPre}。
     */
    public static void handleFireControlKeys(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;

        // 先累计本 tick 的中键点击次数：卡顿时可能一次 tick 内累积多次点击，
        // 保留旧 while(consumeClick()) 的「一次点击=一次操作」语义。
        int clicks = 0;
        while (ModKeyMappings.FIRE_CONTROL_SELECT.consumeClick()) {
            clicks++;
        }
        if (clicks == 0) return;

        // 解锁前置条件保持不变：仅变身态或侦察模式生效。
        // 放行前先吞掉原版「选取方块」队列，避免占用中键时误触发 pick block。
        // 仅在两键确实指向同一物理键时才吞：玩家把任一绑定改走时，原版行为必须保留。
        if (!transformed && !inReconMode) return;
        if (ClientGameEvents.keysCollide(mc.options.keyPickItem, ModKeyMappings.FIRE_CONTROL_SELECT)) {
            while (mc.options.keyPickItem.consumeClick()) { /* discard vanilla pick block */ }
        }

        // 蹲下判定只读一次；不同点击之间玩家可能转视角，故每次点击重新取准心实体
        boolean crouching = mc.player.isShiftKeyDown();

        for (int i = 0; i < clicks; i++) {
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);

            if (crouching) {
                if (target != null) {
                    // 蹲下 + 有目标 → 单选（替换整个列表）
                    PacketDistributor.sendToServer(new FireControlPayload(
                            FireControlPayload.FireAction.LOCK, target.getUUID()));
                } else {
                    // 蹲下 + 准心无实体 → 清空列表（与旧 I 键行为一致）
                    PacketDistributor.sendToServer(FireControlPayload.cancel());
                    ClientFireControlData.clear();
                }
            } else if (target != null) {
                // 不蹲下 + 有目标 → 加选（追加，服务端上限 4）
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.ADD, target.getUUID()));
            }
            // 不蹲下 + 准心无实体 → 静默，不做任何事（避免误清空）
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

    /** 处理 H 键 — 切换自动模式总开关（OFF ↔ ON）。 */
    public static void handleAutoLaunchKey(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;
        while (ModKeyMappings.TOGGLE_AUTO_LAUNCH.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new ToggleAutoModePayload());
        }
    }

    /** 处理 R 键 — 手动装填（鱼雷/导弹）。 */
    public static void handleManualReloadKey(Minecraft mc, boolean transformed, boolean inReconMode) {
        if (mc.player == null) return;
        while (ModKeyMappings.MANUAL_RELOAD.consumeClick()) {
            LOGGER.info("[CLIENT] R key pressed - transformed: {}, inReconMode: {}", transformed, inReconMode);
            if (!transformed || inReconMode) {
                LOGGER.info("[CLIENT] R key ignored - not in combat mode");
                continue;
            }
            LOGGER.info("[CLIENT] Sending ManualReloadPayload to server");
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
