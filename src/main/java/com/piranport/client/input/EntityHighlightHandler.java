package com.piranport.client.input;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.client.EntityUuidCache;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.BulletEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 实体高亮逻辑（Y键+扫描）和火控/ASW计分板队伍同步。
 *
 * <p>管理三个高亮层级：
 * <ol>
 *   <li>火控目标（红色轮廓）— 通过 UUID 缓存 O(k) 定向查找</li>
 *   <li>Y 键战场高亮（白色轮廓）— 完整扫描按 ENTITY_SCAN_INTERVAL 节流</li>
 *   <li>反潜声呐（黄色轮廓）— 独立于 Y 键</li>
 * </ol>
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>生命周期</b>: 在 {@link com.piranport.ClientGameEvents#onClientDisconnect} 中通过 {@link #reset()} 清理。
 * <p><b>高亮优先级</b>: 原版发光 > 火控(玩法高亮) > Y键战场高亮
 */
public class EntityHighlightHandler {

    private static boolean highlightEnabled = false;
    private static final Set<Integer> highlightedEntityIds = new HashSet<>();
    private static final Set<Integer> aswHighlightedEntityIds = new HashSet<>();

    private static final String FC_TEAM_NAME = "pp_fc_target";
    private static final Set<String> fcTeamMembers = new HashSet<>();
    private static final String ASW_TEAM_NAME = "pp_asw_sonar";
    private static final Set<String> aswTeamMembers = new HashSet<>();

    private static final int ENTITY_SCAN_INTERVAL = 4;
    private static int entityScanCooldown = 0;
    private static final EntityUuidCache entityCache = new EntityUuidCache();

    private EntityHighlightHandler() {}

    // ========== Public API ==========

    public static boolean isHighlightEnabled() { return highlightEnabled; }

    /** 断开连接时重置所有高亮状态。 */
    public static void reset() {
        highlightEnabled = false;
        highlightedEntityIds.clear();
        entityCache.clear();
        clearFcTeam(Minecraft.getInstance());
        clearAswTeam(Minecraft.getInstance());
    }

    // ========== Y 键切换 ==========

    /** 处理 Y 键切换：启用/禁用战场高亮。 */
    public static void toggleHighlight(Minecraft mc) {
        if (mc.player == null) return;
        highlightEnabled = !highlightEnabled;
        if (!highlightEnabled && mc.level != null) {
            Set<UUID> fcTargets = new HashSet<>(ClientFireControlData.getTargets());
            for (int id : List.copyOf(highlightedEntityIds)) {
                Entity e = mc.level.getEntity(id);
                if (e != null && !fcTargets.contains(e.getUUID()) && !hasVanillaGlow(e)) {
                    e.setGlowingTag(false);
                    highlightedEntityIds.remove(id);
                }
            }
        }
        mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable(
                        highlightEnabled ? "message.piranport.highlight_on"
                                         : "message.piranport.highlight_off"),
                true);
    }

    // ========== 主 tick 方法 ==========

    /**
     * 每 tick 应用/维持高亮发光效果（火控 + Y键）。
     *
     * @param lockedTargets 当前火控锁定的 UUID 集合
     */
    public static void tick(Minecraft mc, Set<UUID> lockedTargets, boolean hasFcTargets) {
        if (mc.level == null || mc.player == null) return;
        Player localPlayer = mc.player;
        Set<String> currentFcMembers = new HashSet<>();

        // Phase 1：火控目标 — 通过 UUID 缓存进行 O(k) 定向查找
        if (hasFcTargets) {
            for (UUID uuid : lockedTargets) {
                Entity entity = entityCache.get(mc.level, uuid);
                if (entity != null && entity.isAlive()) {
                    entity.setGlowingTag(true);
                    highlightedEntityIds.add(entity.getId());
                    if (!(entity instanceof AircraftEntity)) {
                        currentFcMembers.add(entity.getStringUUID());
                    }
                }
            }
        }

        // Phase 2：Y 键战场高亮 — 完整扫描按 ENTITY_SCAN_INTERVAL 节流
        boolean doFullScan = highlightEnabled && (entityScanCooldown <= 0);
        if (doFullScan) {
            entityScanCooldown = ENTITY_SCAN_INTERVAL;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (!lockedTargets.contains(entity.getUUID()) && isHighlightTarget(entity, localPlayer)) {
                    entity.setGlowingTag(true);
                    highlightedEntityIds.add(entity.getId());
                }
            }
        }
        if (highlightEnabled) entityScanCooldown--;

        // Phase 3：移除不再处于任何活跃高亮集合中的实体的发光效果
        if (!highlightedEntityIds.isEmpty()) {
            highlightedEntityIds.removeIf(id -> {
                Entity entity = mc.level.getEntity(id);
                if (entity == null) return true;
                UUID uuid = entity.getUUID();
                if (lockedTargets.contains(uuid)) return false;
                if (highlightEnabled && isHighlightTarget(entity, localPlayer)) return false;
                if (hasVanillaGlow(entity)) return false;
                entity.setGlowingTag(false);
                return true;
            });
        }

        // 同步火控计分板队伍（红色轮廓）
        syncFcTeam(mc, currentFcMembers);
    }

    /** 反潜声呐高亮 — 独立于 Y 键，使用黄色轮廓。 */
    public static void tickAswSonar(Minecraft mc, Set<UUID> lockedTargets) {
        if (mc.level == null) return;

        com.piranport.aviation.ClientAswSonarData.tick();
        Set<Integer> aswDetected = com.piranport.aviation.ClientAswSonarData.getAllDetected();
        Set<String> currentAswMembers = new HashSet<>();
        Set<UUID> fcTargets = hasFcTargets(lockedTargets)
                ? new HashSet<>(lockedTargets)
                : Collections.emptySet();

        // 为新检测到的实体应用发光效果
        for (int eid : aswDetected) {
            Entity entity = mc.level.getEntity(eid);
            if (entity == null || !entity.isAlive()) continue;
            entity.setGlowingTag(true);
            aswHighlightedEntityIds.add(eid);
            if (!fcTargets.contains(entity.getUUID())) {
                currentAswMembers.add(entity.getStringUUID());
            }
        }

        // 移除不再被声呐检测到的实体的发光效果
        aswHighlightedEntityIds.removeIf(id -> {
            if (aswDetected.contains(id)) return false;
            Entity entity = mc.level.getEntity(id);
            if (entity == null) return true;
            if (highlightedEntityIds.contains(id)) return true;
            if (hasVanillaGlow(entity)) return true;
            entity.setGlowingTag(false);
            return true;
        });

        syncAswTeam(mc, currentAswMembers);
    }

    // ========== 高亮判断辅助 ==========

    /**
     * 如果实体拥有原版级发光效果（如发光药水、光灵箭），返回 true。
     * 原版发光拥有最高优先级，绝不能被本模组移除。
     */
    private static boolean hasVanillaGlow(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.hasEffect(MobEffects.GLOWING);
        }
        return false;
    }

    /**
     * 当 Y 键高亮启用时，判断实体是否应该发光。
     * 不包含火控目标 — 火控目标单独处理。
     */
    private static boolean isHighlightTarget(Entity entity, Player localPlayer) {
        if (entity instanceof BulletEntity bl && bl.getOwner() == localPlayer) return true;

        if (entity instanceof LivingEntity living && living != localPlayer) {
            double distSq = entity.distanceToSqr(localPlayer);
            if (distSq <= 32 * 32) {
                if (living.getLastHurtByMob() == localPlayer ||
                        (living instanceof Mob mob
                                && mob.getTarget() != null && mob.getTarget() == localPlayer)) {
                    return true;
                }
                if (living instanceof Monster) return true;
                if (living instanceof Animal) return distSq <= 16 * 16;
            }
        }
        return false;
    }

    // ========== 计分板队伍同步 ==========

    private static void syncFcTeam(Minecraft mc, Set<String> currentMembers) {
        if (mc.level == null) return;
        Scoreboard scoreboard = mc.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(FC_TEAM_NAME);
        PlayerTeam aswTeam = scoreboard.getPlayerTeam(ASW_TEAM_NAME);

        if (team == null && !currentMembers.isEmpty()) {
            team = scoreboard.addPlayerTeam(FC_TEAM_NAME);
            team.setColor(net.minecraft.ChatFormatting.RED);
        }
        if (team == null) {
            fcTeamMembers.clear();
            return;
        }
        for (String name : new HashSet<>(fcTeamMembers)) {
            if (!currentMembers.contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        for (String name : currentMembers) {
            if (!fcTeamMembers.contains(name)) {
                if (aswTeam != null && aswTeamMembers.contains(name)) {
                    scoreboard.removePlayerFromTeam(name, aswTeam);
                    aswTeamMembers.remove(name);
                    if (mc.level != null) {
                        aswHighlightedEntityIds.removeIf(id -> {
                            Entity e = mc.level.getEntity(id);
                            return e != null && e.getStringUUID().equals(name);
                        });
                    }
                }
                scoreboard.addPlayerToTeam(name, team);
            }
        }
        fcTeamMembers.clear();
        fcTeamMembers.addAll(currentMembers);
    }

    private static void clearFcTeam(Minecraft mc) {
        if (mc.level == null || fcTeamMembers.isEmpty()) return;
        Scoreboard scoreboard = mc.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(FC_TEAM_NAME);
        if (team != null) {
            for (String name : fcTeamMembers) {
                scoreboard.removePlayerFromTeam(name, team);
            }
            scoreboard.removePlayerTeam(team);
        }
        fcTeamMembers.clear();
    }

    private static void syncAswTeam(Minecraft mc, Set<String> currentMembers) {
        if (mc.level == null) return;
        Scoreboard scoreboard = mc.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(ASW_TEAM_NAME);
        if (team == null && !currentMembers.isEmpty()) {
            team = scoreboard.addPlayerTeam(ASW_TEAM_NAME);
            team.setColor(net.minecraft.ChatFormatting.YELLOW);
        }
        if (team == null) {
            aswTeamMembers.clear();
            return;
        }
        for (String name : new HashSet<>(aswTeamMembers)) {
            if (!currentMembers.contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        for (String name : currentMembers) {
            if (!aswTeamMembers.contains(name)) {
                scoreboard.addPlayerToTeam(name, team);
            }
        }
        aswTeamMembers.clear();
        aswTeamMembers.addAll(currentMembers);
    }

    private static void clearAswTeam(Minecraft mc) {
        if (mc.level == null || aswTeamMembers.isEmpty()) return;
        Scoreboard scoreboard = mc.level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(ASW_TEAM_NAME);
        if (team != null) {
            for (String name : aswTeamMembers) {
                scoreboard.removePlayerFromTeam(name, team);
            }
            scoreboard.removePlayerTeam(team);
        }
        aswTeamMembers.clear();
        aswHighlightedEntityIds.clear();
    }

    /** 检查 Set 是否包含火控目标（避免 null）。 */
    private static boolean hasFcTargets(Set<UUID> lockedTargets) {
        return lockedTargets != null && !lockedTargets.isEmpty();
    }
}
