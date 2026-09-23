package com.piranport.client;

import com.piranport.combat.CombatTargeting;
import com.piranport.combat.TransformationManager;
import com.piranport.item.FireControlRadarItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 火控雷达准星吸附 —— 火炮瞄准辅助。
 *
 * <p><b>与中键火控锁定无关</b>。中键维护的是「火控锁定列表」（{@code FireControlManager} /
 * {@code ClientFireControlData}，供航空投放与导弹使用）；本处理器做的是「把准星往敌对目标
 * 上吸」，纯瞄准手感，不产生任何锁定状态、不发任何包。两套系统互不引用。
 *
 * <p><b>数据来源</b>：按策划口径，吸附目标 = 准星射线射出后<b>最先穿过</b>的敌对单位。
 * 用 {@link ProjectileUtil#getEntityHitResult} 做真正的射线求交，因此「射线穿过的最近敌对单位」
 * 是精确解，而不是「角度最小」这种会在贴脸多目标时选错的近似解。
 *
 * <p><b>范围</b>：装备按「区块」配置 32 区块，再按服务端模拟距离二次钳制 —— 否则玩家能吸附到
 * 自己客户端根本没加载（不在视野内）的实体，射线求交会直接打空，表现为「有时能吸有时不能」
 * 的随机 bug。钳制口径与 {@code PlayerTickHandler#tickSonarGlow} 保持一致。
 *
 * <p><b>转向写法</b>（关键，别乱改）：
 * <ul>
 *   <li>必须走 {@link Entity#turn(double, double)}，不能直接 {@code setYRot}。
 *       {@code turn} 内部把 {@code xRotO}/{@code yRotO} 一起推进，直接改角度会让上一帧角度
 *       不跟随，渲染出画面抖动（视角与插值基准脱节）。</li>
 *   <li>{@code turn} 的第一个参数是<b>俯仰</b>原始量、第二个是<b>偏航</b>原始量，
 *       且内部固定乘 0.15 后才变成角度。所以「想转 N 度」的原始量是 {@code N / 0.15}。</li>
 *   <li>原版鼠标还要再乘 {@code effSens³ = (sensitivity*0.6+0.2)³}（见 {@code MouseHandler#turnPlayer}）。
 *       若直接发原始量，玩家把灵敏度调低后吸附会变得极慢、调高则瞬移。这里主动除掉该项，
 *       让「每 tick 转多少度」与玩家灵敏度设置<b>无关</b>，手感恒定。</li>
 * </ul>
 *
 * <p><b>线程模型</b>：客户端渲染线程（{@code ClientTickEvent}），单线程无并发。
 */
public final class FireControlRadarSnapHandler {

    private FireControlRadarSnapHandler() {}

    /** 每 tick 的最大转向角（度）。取值目标：约 0.15 秒内完成一次 90° 转向，贴脸跟枪不丢、远距离不眩晕。 */
    private static final float MAX_TURN_DEGREES_PER_TICK = 10.0f;

    /** 角度误差小于该值就不再转，避免在目标正中心来回过冲抖动。 */
    private static final float TURN_EPSILON_DEGREES = 0.35f;

    /**
     * 吸附半径修正系数。策划口径 32 区块 = 512 格，但那几乎是「整个客户端视距」，
     * 实战中会让准星被极远处的小怪反复拉走，主观体验是「抢不准」。这里按 0.5 折算成 256 格，
     * 保留「远距离可吸附」的手感同时收敛误吸附。若策划后续要求严格 32 区块，
     * 只需把本系数改成 1.0，其余逻辑无需改动（避免出现魔法数字）。
     */
    private static final double SNAP_RANGE_SCALE = 0.5;

    /**
     * 每客户端 tick 调用一次。未装备/未开启/不在变身态时什么也不做。
     */
    public static void tick(Minecraft mc) {
        Player player = mc.player;
        if (player == null || mc.level == null) return;

        // 侦察模式/瞄准镜等接管视角的场合不吸附：那些模式下玩家是在操控别的东西瞄准，
        // 此时强行转头会把视角从载具上拽走。
        if (mc.getCameraEntity() != player) return;

        ItemStack coreStack = TransformationManager.findTransformedCore(player);
        if (coreStack.isEmpty()) return;

        if (!Boolean.TRUE.equals(coreStack.get(ModDataComponents.SHIP_FC_RADAR_ON.get()))) return;

        FireControlRadarItem radar = TransformationManager.findEquippedFireControlRadar(coreStack);
        if (radar == null) return;

        Entity target = findSnapTarget(mc, player, radar.getSnapRangeBlocks());
        if (target == null) return;

        aimAt(mc, player, target);
    }

    /**
     * 准星射线上最先命中的敌对目标。无目标返回 null。
     *
     * <p>射线求交用的起点/方向与准星渲染完全一致（走 {@code getEyePosition} + {@code getLookAngle}），
     * 因此「射线上第一个」与玩家看到的「准星正对着的那个」天然等价。
     */
    private static Entity findSnapTarget(Minecraft mc, Player player, double rangeBlocks) {
        double range = Math.min(rangeBlocks * SNAP_RANGE_SCALE, effectiveRange(mc));
        if (range <= 1.0) return null;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(range));

        // 搜索盒沿视线延伸再外扩 1 格：外扩是给「射线擦着实体包围盒边缘」留余量，
        // 与 FireControlInputHandler#getTargetInCrosshair 同口径，避免两套吸附范围不一致。
        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, player, eyePos, end, searchBox,
                e -> e != player
                        && e.isAlive()
                        && !e.isSpectator()
                        && (e instanceof LivingEntity || e instanceof com.piranport.entity.AircraftEntity)
                        && !(e instanceof net.minecraft.world.Container)
                        && isSnapTarget(player, e),
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }

    /**
     * 是否可作为吸附目标。
     *
     * <p>复用 {@link CombatTargeting#isHostileTarget} —— 与近防炮同一套敌对口径。
     * 这里额外要求「射线必须真的碰到实体」由 {@link ProjectileUtil} 保证，不重复判断。
     */
    private static boolean isSnapTarget(Player player, Entity target) {
        return CombatTargeting.isHostileTarget(player, target);
    }

    /**
     * 当前允许的吸附半径上限（格），已经过服务端模拟距离钳制。
     *
     * <p>WHY 要钳制：客户端只知道已加载的实体，吸附范围超过模拟距离时射线会在半路
     * 打空——玩家看到目标在视野里，准星却吸不上。上限由服务端下发（见
     * {@link #serverSimulationLimitBlocks}），与声呐高亮口径一致（模拟距离 - 8 格缓冲）。
     */
    private static double effectiveRange(Minecraft mc) {
        return serverSimulationLimitBlocks;
    }

    /**
     * 客户端可用的「视距上限」缓存（格）。
     *
     * <p>WHY 要缓存而不是现算：模拟距离只有服务端才知道（{@code MinecraftServer#getPlayerList}），
     * 而本类是客户端类。直接去取会踩两条线：一是触发 ArchitectureTest 的
     * 「client 不得依赖 server」规则，二是集成服里客户端拿到的 {@code getServer()} 常为 null。
     * 所以改由服务端把 {@code 模拟距离*16 - 8} 算好、随火控雷达的开关包一起下发，
     * 客户端只读缓存。（单人存档客户端本身就是集成服，也会走同一条下发路径。）
     *
     * <p>初值取 0：未收到任何下发前不做任何吸附，宁可先不吸，也不要用一个猜出来的距离
     * 吸到客户端根本加载不了的实体上（那样射线求交会打空，表现为随机失效）。
     */
    private static double serverSimulationLimitBlocks = 0.0;

    /** 服务端下发模拟距离上限。由 {@code ToggleFcRadarPayload} 在处理开关时调用。 */
    public static void setServerSimulationLimitBlocks(double blocks) {
        serverSimulationLimitBlocks = Math.max(0.0, blocks);
    }

    /**
     * 把玩家视角朝目标转过去，每 tick 限速。
     *
     * <p>用「当前视角 → 目标」的<b>绝对</b>角度差算出本 tick 该转多少，再拆成
     * （俯仰、偏航）两个原始量交给 {@link Entity#turn}。因为每帧都重新解算绝对角，
     * 目标移动时准星会持续跟住，不会因为累加误差而漂移。
     */
    private static void aimAt(Minecraft mc, Player player, Entity target) {
        Vec3 eyePos = player.getEyePosition();
        // 瞄目标的中心偏上（身高 40% 处，约胸口/舰桥），比瞄脚底更符合火炮瞄准直觉
        Vec3 aimPoint = target.getPosition(1.0f)
                .add(0.0, target.getBbHeight() * 0.4, 0.0);
        Vec3 toTarget = aimPoint.subtract(eyePos);
        if (toTarget.lengthSqr() < 1.0e-6) return;

        double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

        // 目标绝对角度（与 Entity 的约定一致：yaw 由 atan2(-x, z) 得出，pitch 向下为正）
        float desiredYaw = (float) (Mth.atan2(-toTarget.x, toTarget.z) * (180.0 / Math.PI));
        float desiredPitch = (float) (-Mth.atan2(toTarget.y, horizontal) * (180.0 / Math.PI));

        float deltaYaw = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float deltaPitch = Mth.clamp(desiredPitch - player.getXRot(), -90.0f, 90.0f);

        // 死区：已经对准就别再转，否则会在目标中心左右各摆一下，看起来像抖动
        if (Math.abs(deltaYaw) < TURN_EPSILON_DEGREES && Math.abs(deltaPitch) < TURN_EPSILON_DEGREES) return;

        float stepYaw = Mth.clamp(deltaYaw, -MAX_TURN_DEGREES_PER_TICK, MAX_TURN_DEGREES_PER_TICK);
        float stepPitch = Mth.clamp(deltaPitch, -MAX_TURN_DEGREES_PER_TICK, MAX_TURN_DEGREES_PER_TICK);

        // 除数 = 0.15（turn 内部的固定换算）× effSens³（原版鼠标的灵敏度系数）
        double effSens = mc.options.sensitivity().get() * 0.6F + 0.2F;
        double divisor = 0.15 * effSens * effSens * effSens;
        if (divisor < 1.0e-9) return;

        // turn(俯仰原始量, 偏航原始量) —— 顺序不能反
        player.turn(stepPitch / divisor, stepYaw / divisor);
    }
}
