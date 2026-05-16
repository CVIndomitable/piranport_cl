package com.piranport.client;

import com.piranport.artillery.ArtilleryItem;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端瞄准镜状态管理。
 * 跟踪玩家是否处于瞄准模式、长按 tick 数、当前武器 scopeZoom。
 * 状态存储在静态字段中（与 ClientFireControlData 模式一致）。
 */
public final class ClientScopeHandler {

    private static boolean scoping = false;
    private static int holdTicks = 0;
    private static float zoomLevel = 2.0f;
    /** 当前瞄准的目标位置（方块/实体命中点） */
    @Nullable
    private static Vec3 aimedPosition = null;
    /** 与目标的水平距离（格） */
    private static double targetDistance = 0;
    /** 与目标的高度差（格） */
    private static double targetVertical = 0;

    /** 进入瞄准模式之前是否持有火炮 */
    private static boolean heldCannonBeforeScope = false;

    private static final int SCOPE_THRESHOLD_TICKS = 5;

    private ClientScopeHandler() {}

    /** 进入瞄准模式 */
    public static void enterScope(Player player, ItemStack weapon) {
        if (scoping) return;
        zoomLevel = getZoomFromWeapon(weapon);
        if (zoomLevel <= 0) return;
        scoping = true;
        holdTicks = 0;
        heldCannonBeforeScope = true;
    }

    /** 退出瞄准模式 */
    public static void exitScope() {
        scoping = false;
        holdTicks = 0;
        aimedPosition = null;
        targetDistance = 0;
        targetVertical = 0;
        heldCannonBeforeScope = false;
    }

    /** 每客户端 tick 调用，更新长按计数和射线检测 */
    public static void tick(Player player, ItemStack weapon) {
        if (!scoping) return;
        holdTicks++;

        // 每 tick 更新射线检测，获取目标位置
        updateAimedPosition(player);
    }

    /** 从玩家视角发射射线，获取准星指向的命中点坐标 */
    private static void updateAimedPosition(Player player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getCameraEntity() == null) return;

        Entity camera = mc.getCameraEntity();
        Vec3 eyePos = camera.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        double range = 256.0; // 火控最大射程

        Vec3 end = eyePos.add(lookDir.scale(range));

        // 1. 方块碰撞检测
        ClipContext clipCtx = new ClipContext(eyePos, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, camera);
        BlockHitResult blockHit = mc.level.clip(clipCtx);

        // 2. 实体碰撞检测（活体实体 + 飞机）
        AABB searchBox = camera.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(2.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                mc.level, player, eyePos, end, searchBox,
                e -> (e instanceof LivingEntity || e instanceof com.piranport.entity.AircraftEntity)
                        && e.isAlive() && e != player && e != camera,
                0.0f);

        // 3. 取距离最近的命中点
        Vec3 hitPos;
        if (entityHit != null) {
            double entityDist = entityHit.getLocation().distanceToSqr(eyePos);
            double blockDist = blockHit.getType() == HitResult.Type.BLOCK
                    ? blockHit.getLocation().distanceToSqr(eyePos) : Double.MAX_VALUE;

            if (blockHit.getType() == HitResult.Type.BLOCK && blockDist < entityDist) {
                hitPos = blockHit.getLocation();
            } else {
                hitPos = entityHit.getLocation();
            }
        } else if (blockHit.getType() == HitResult.Type.BLOCK) {
            hitPos = blockHit.getLocation();
        } else {
            // 什么都没打到：取射线远端
            hitPos = end;
        }

        aimedPosition = hitPos;
        targetDistance = Math.sqrt(
                (hitPos.x - eyePos.x) * (hitPos.x - eyePos.x) +
                (hitPos.z - eyePos.z) * (hitPos.z - eyePos.z));
        targetVertical = hitPos.y - eyePos.y;
    }

    // ===== Getters =====

    public static boolean isScoping() { return scoping; }

    /** 是否已长按达到瞄准阈值（完全进入瞄准模式） */
    public static boolean isFullyScoped() {
        return scoping && holdTicks >= SCOPE_THRESHOLD_TICKS;
    }

    /** 是否为快速点击（短按未达到瞄准阈值） */
    public static boolean isQuickRelease() {
        return scoping && holdTicks < SCOPE_THRESHOLD_TICKS;
    }

    public static int getHoldTicks() { return holdTicks; }

    public static int getScopeThreshold() { return SCOPE_THRESHOLD_TICKS; }

    public static float getZoomLevel() { return zoomLevel; }

    @Nullable
    public static Vec3 getAimedPosition() { return aimedPosition; }

    public static double getTargetDistance() { return targetDistance; }

    public static double getTargetVertical() { return targetVertical; }

    /** 从武器 ItemStack 读取 scopeZoom */
    private static float getZoomFromWeapon(ItemStack weapon) {
        if (weapon.getItem() instanceof ArtilleryItem ai) {
            return ai.getData().scopeZoom();
        }
        return 2.0f;
    }

    /** 判断玩家是否手持火炮 */
    public static boolean isHoldingCannon(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof ArtilleryItem;
    }

    /** 断开连接等场景清理状态 */
    public static void clear() {
        scoping = false;
        holdTicks = 0;
        zoomLevel = 2.0f;
        aimedPosition = null;
        targetDistance = 0;
        targetVertical = 0;
        heldCannonBeforeScope = false;
    }
}
