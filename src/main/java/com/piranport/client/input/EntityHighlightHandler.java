package com.piranport.client.input;

import com.piranport.client.EntityUuidCache;
import com.piranport.entity.AircraftEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 实体高亮逻辑（火控 + 反潜声呐）和火控/ASW计分板队伍同步。
 *
 * <p>管理两个高亮层级：
 * <ol>
 *   <li>火控目标（红色轮廓）— 通过 UUID 缓存 O(k) 定向查找</li>
 *   <li>反潜声呐（黄色轮廓）— 独立于火控</li>
 * </ol>
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 * <p><b>生命周期</b>: 在 {@link com.piranport.client.ClientGameEvents#onClientDisconnect} 中通过 {@link #reset()} 清理。
 * <p><b>高亮优先级</b>: 原版发光 > 火控 > 声呐
 */
public class EntityHighlightHandler {

    private static final Set<Integer> highlightedEntityIds = new HashSet<>();
    private static final Set<Integer> aswHighlightedEntityIds = new HashSet<>();

    private static final String FC_TEAM_NAME = "pp_fc_target";
    private static final Set<String> fcTeamMembers = new HashSet<>();
    private static final String ASW_TEAM_NAME = "pp_asw_sonar";
    private static final Set<String> aswTeamMembers = new HashSet<>();

    private static final EntityUuidCache entityCache = new EntityUuidCache();

    private EntityHighlightHandler() {}

    // ========== Public API ==========

    /** 断开连接时重置所有高亮状态。 */
    public static void reset() {
        highlightedEntityIds.clear();
        entityCache.clear();
        clearFcTeam(Minecraft.getInstance());
        clearAswTeam(Minecraft.getInstance());
    }

    // ========== 主 tick 方法 ==========

    /**
     * 每 tick 应用/维持高亮发光效果（火控）。
     *
     * @param lockedTargets 当前火控锁定的 UUID 集合
     */
    public static void tick(Minecraft mc, Set<UUID> lockedTargets, boolean hasFcTargets) {
        if (mc.level == null || mc.player == null) return;
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

        // Phase 3：移除不再处于任何活跃高亮集合中的实体的发光效果
        if (!highlightedEntityIds.isEmpty()) {
            highlightedEntityIds.removeIf(id -> {
                Entity entity = mc.level.getEntity(id);
                if (entity == null) return true;
                UUID uuid = entity.getUUID();
                if (lockedTargets.contains(uuid)) return false;
                if (hasVanillaGlow(entity)) return false;
                entity.setGlowingTag(false);
                return true;
            });
        }

        // 同步火控计分板队伍（红色轮廓）
        syncFcTeam(mc, currentFcMembers);
    }

    /** 反潜声呐高亮 — 独立于火控，使用黄色轮廓。 */
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
