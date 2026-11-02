package com.piranport.handler;

import com.piranport.combat.CombatTargeting;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.RadarItem;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 舰装声呐/雷达扫描，仅在服务端主线程执行。
 * 保留40 tick错峰、模拟距离钳制与目标分层；不负责自动开火。
 */
final class PlayerDetectionTick {
    private PlayerDetectionTick() {}

    private static final int SONAR_SCAN_INTERVAL = 40;

    /**
     * 索敌高亮：核心被动声呐 + 已装备雷达，统一在本次扫描里处理（每40tick，错峰执行）。
     *
     * <p><b>为什么合并成一次扫描</b>：声呐和雷达都是「加 GLOWING 效果」，而 addEffect 会覆盖
     * 同名效果的持续时间。若两者各扫一次，后执行的一方会把先执行的一方的效果时长重置，
     * 造成高频重扫。这里一次取完实体再按各自条件打标，只发一次效果包。
     *
     * <p><b>为什么范围内实体只扫一次</b>：多台雷达同时装备时，扫描范围取其中最大值，
     * 逐台各扫一遍会重复遍历同一批实体。取并集后逐实体判断，性能按最大范围算一次。
     */
    static void tickSonarGlow(Player player, ItemStack transformedCore) {
        // 错峰执行：使用UUID哈希避免玩家ID连续分配导致的碰撞
        if ((player.tickCount + player.getUUID().hashCode()) % SONAR_SCAN_INTERVAL != 0) return;

        boolean hasSonar = TransformationManager.hasSonarEquipped(player, transformedCore);
        List<RadarItem> radars = TransformationManager.getEquippedRadars(transformedCore);
        if (!hasSonar && radars.isEmpty()) return;

        // 动态限制扫描范围，避免超出服务器模拟距离
        int simDist = ((ServerLevel) player.level()).getServer().getPlayerList().getSimulationDistance();
        double simLimit = simDist * 16.0 - 8.0;

        // 实际扫描半径取「所有已装备索敌设备里的最大值」，再统一受模拟距离钳制。
        // 逐台各扫一遍会重复遍历同一批实体，取并集后逐实体判断只需扫一次。
        double scanRange = 0.0;
        if (hasSonar) {
            // Phase 27：策划 §3.6 - 声呐半径按 SonarItem.radius 决定 (标准 24/改进 32/先进 40)
            scanRange = Math.max(scanRange,
                    TransformationManager.getEquippedSonarRadius(player, transformedCore));
        }
        for (RadarItem radar : radars) {
            scanRange = Math.max(scanRange, radar.getRange());
        }
        double maxRange = Math.min(scanRange, simLimit);

        // 扫描盒三轴都用 maxRange，而不是把垂直方向压成 ±8。
        //
        // 为什么必须各向同性：雷达是球面索敌，逐实体判定用的是 player.distanceTo(entity)（三维距离），
        // 而扫描盒只负责「把候选实体捞出来」。若垂直方向只给 ±8，正上方 20 格的飞机、正下方 20 格的
        // 潜艇会因为压根不在盒子里而永远进不了候选列表 —— 三维距离判定根本没机会执行，
        // 于是「索敌范围: 32 格」在垂直方向上被静默缩水成 8 格，恰恰是雷达最该看到的两个方向。
        // 三轴同为 maxRange 才能保证「盒内 ⊇ 球内」，球面判定才是唯一的判定口径。
        //
        // 注意这条对「舰装被动声呐」同样是行为变更：策划决策/火控/02 记载的垂直 ±8 是 2026-09-09
        // 按当时代码现状定稿的描述性记录，而非主动决策；而声呐半径标准档就有 24 格，±8 的盒子
        // 连自身半径都装不下，正下方 20 格的潜艇会被静默漏掉。故一并放宽为 24 格档
        // 垂直可达 ±24，与该决策同文件里「半径 24」的参数不再自相矛盾。
        // 若后续要恢复声呐的浅层扫描手感，应给声呐单独一个垂直范围参数，而不是回退整个盒子。
        AABB scanBox = player.getBoundingBox().inflate(maxRange);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class, scanBox,
                e -> e.isAlive() && e != player && !(e instanceof Player));
        for (LivingEntity entity : nearby) {
            if (isDetectedBySonarOrRadar(player, entity, hasSonar, radars)) {
                entity.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING, SONAR_SCAN_INTERVAL, 0, false, false, false));
            }
        }

        // 飞机不是 LivingEntity，上面的扫描捞不到；只有对空雷达能标空中目标，声呐不标飞机
        if (radars.isEmpty()) return;
        List<AircraftEntity> aircraft = player.level().getEntitiesOfClass(
                AircraftEntity.class, scanBox,
                a -> CombatTargeting.isHostileTarget(player, a));
        for (AircraftEntity a : aircraft) {
            if (isDetectedByAirRadar(player, a, radars)) {
                a.markRadarGlow(SONAR_SCAN_INTERVAL);
            }
        }
    }

    private static boolean isDetectedByAirRadar(Player player, AircraftEntity aircraft, List<RadarItem> radars) {
        double distance = player.distanceTo(aircraft);
        for (RadarItem radar : radars) {
            if (radar.getTarget() == RadarItem.RadarTarget.AIR && radar.getRange() >= distance) return true;
        }
        return false;
    }

    /**
     * 判定单个实体是否被核心被动声呐或任一已装备雷达探测到。
     * 被动声呐沿用策划决策/火控/02 的口径：范围内一切非玩家生物（含友方水生生物）都标记；
     * 雷达则严格按各自「索敌目标」分层，三种分层互斥。
     */
    private static boolean isDetectedBySonarOrRadar(Player player, LivingEntity entity,
                                                    boolean hasSonar, List<RadarItem> radars) {
        // 被动声呐：全目标
        if (hasSonar) return true;
        if (radars.isEmpty()) return false;

        // 先算出目标的分层归属，避免每台雷达重复调用 isUnderWater / onGround 等
        boolean underwater = isUnderwaterTarget(entity);
        boolean airborne = isAirborneTarget(entity);
        for (RadarItem radar : radars) {
            // 各雷达索敌范围独立：装了大范围雷达也要能让小范围雷达的高亮不越界
            if (radar.getRange() < player.distanceTo(entity)) continue;
            switch (radar.getTarget()) {
                case SUBMARINE -> {
                    if (underwater) return true;
                }
                case AIR -> {
                    if (airborne) return true;
                }
                // 对海：既不在水下、也不在空中（水面舰船、岸上单位）
                case SURFACE -> {
                    if (!underwater && !airborne) return true;
                }
            }
        }
        return false;
    }

    /**
     * 水下目标判定 —— 雷达的「声纳」只在目标真正处于水下时才高亮（策划：声纳只能高亮水下目标）。
     *
     * <p>为什么不能只看 {@code EntityTypeTags.AQUATIC}：该标签包含海龟、美西螈等两栖生物，
     * 它们爬上岸、{@code isUnderWater()} 为 false 时仍会被误判为水下目标，于是声纳雷达高亮一只
     * 沙滩上的海龟、而对海雷达又因为它「属于水下」而跳过它 —— 分层互相挤占，与策划的互斥设计冲突。
     *
     * <p><b>本判据只服务雷达，不服务被动声呐</b>：被动声呐在 {@link #isDetectedBySonarOrRadar}
     * 里第一行就无条件返回 true 了，根本不经过这里。所以「声呐标记所有生物（含水陆不分）」
     * 的既定玩法（策划决策/火控/02）不受任何影响，这里收紧的只是雷达声纳分层。
     * 反过来说，若哪天想让被动声呐也过滤水陆，改的是那一处 early return，不是这里。
     *
     * <p>{@code DeepOceanSubmarineEntity} 单独判：深海潜艇是自定义实体，不保证 {@code isUnderWater()}
     * 在贴底航行时为真，而它正是声纳的首要目标，必须先于状态判定返回。
     */
    private static boolean isUnderwaterTarget(LivingEntity entity) {
        if (entity instanceof com.piranport.npc.deepocean.DeepOceanSubmarineEntity) return true;
        return entity.isUnderWater();
    }

    /**
     * 飞行目标判定 —— 与防空导弹的判定（MissileEntity.isValidTarget）保持一致：
     * 离地且不在水中即视为空中，或正在上升（避免把下落中的地面实体误判为空中目标）。
     *
     * <p>飞机（AircraftEntity 继承 Entity 而非 LivingEntity）不经过本方法，由 tickSonarGlow 的飞机分支
     * 直接判定为空中目标，所以这里不需要为它单独分支。
     */
    private static boolean isAirborneTarget(LivingEntity entity) {
        return (!entity.onGround() && !entity.isInWater()) || entity.getDeltaMovement().y > 0.1;
    }
}
