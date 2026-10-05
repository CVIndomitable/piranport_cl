package com.piranport.client.input;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.ToggleAutoModePayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.ManualReloadPayload;
import net.minecraft.world.InteractionHand;
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

        // 火控锁定仅限变身态、侦察态和创造模式。
        // 放行前先吞掉原版「选取方块」队列，避免占用中键时误触发 pick block。
        // 仅在两键确实指向同一物理键时才吞：玩家把任一绑定改走时，原版行为必须保留。
        boolean creativeAim = mc.player.getAbilities().instabuild
                && mc.player.getMainHandItem().getItem() instanceof com.piranport.artillery.ArtilleryItem;
        if (!transformed && !inReconMode && !creativeAim) return;
        if (ClientGameEvents.keysCollide(mc.options.keyPickItem, ModKeyMappings.FIRE_CONTROL_SELECT)) {
            while (mc.options.keyPickItem.consumeClick()) { /* discard vanilla pick block */ }
        }

        // 蹲下判定只读一次；不同点击之间玩家可能转视角，故每次点击重新取准心实体
        boolean crouching = mc.player.isShiftKeyDown();

        for (int i = 0; i < clicks; i++) {
            // 火控范围取调试终端参数（客户端镜像由 SyncTerminalParametersPayload 同步；
            // 未同步时回退默认值）。原先写死 80 与终端参数完全脱节，策划改终端无效。
            Entity target = getTargetInCrosshair(mc, ModEquipmentConfig.FIRE_CONTROL_RANGE.get());

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

    /**
     * 处理 R 键 — 手动装填（火炮/舰载机/鱼雷/导弹）。
     *
     * <p>只发一次「请求装填」意图：读条计时与到期结算全在服务端（与火炮共用
     * WEAPON_COOLDOWN 组件）。曾经这里对舰载机做过客户端 startUsingItem 的长按读条，
     * 已废弃 —— 松开 R 就取消读条、且客户端持有一段可被任意方式打断的计时状态，
     * 与《武器/09》"按下 R 启动读条、到期结算"的语义不符。
     */
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
        // 必须用「按实体 getPickRadius() 膨胀」的原版拾取重载（与准心选取方块同一套口径），
        // 不能传给带 float inflation 的那个重载：后者对所有实体用同一个固定膨胀值，
        // 而 AbstractDeepOceanEntity 把 getPickRadius() 覆写成 0.85 就是为了让准心能选中
        // "看得见的舰船索具"而不是人物碰撞箱。传 0.0f 等于把这个覆写静默作废 ——
        // 深海舰船（浮在/潜在水里）就表现为「火控选不中」，而海面上的怪物一切正常。
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player, eyePos, end, searchBox,
                e -> (e instanceof LivingEntity || e instanceof com.piranport.entity.AircraftEntity)
                        && e.isAlive() && e != mc.player && e != cam
                        && !(e instanceof net.minecraft.world.Container),
                range * range);

        return hit != null ? hit.getEntity() : null;
    }
}
