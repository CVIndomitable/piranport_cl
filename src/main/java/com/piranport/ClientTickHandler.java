package com.piranport;

import com.piranport.item.ShipType;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.client.CameraShakeHandler;
import com.piranport.client.EntityUuidCache;
import com.piranport.combat.ClientTorpedoGuidance;
import com.piranport.combat.TransformationManager;
import com.piranport.debug.PiranPortDebug;
import com.piranport.network.DebugCooldownOverridePayload;
import com.piranport.network.DebugTogglePayload;
import com.piranport.network.HitDisplayTogglePayload;
import com.piranport.network.SnapshotRequestPayload;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.BulletEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AutoLaunchTogglePayload;
import com.piranport.network.CycleWeaponPayload;
import com.piranport.network.FireControlPayload;
import com.piranport.client.AmmoSelectOverlay;
import com.piranport.network.ManualReloadPayload;
import com.piranport.network.OpenFlightGroupPayload;
import com.piranport.network.ReconControlPayload;
import com.piranport.network.SwitchAmmoPayload;
import com.piranport.network.ReconExitPayload;
import com.piranport.network.TorpedoGuidanceExitPayload;
import com.piranport.network.TorpedoGuidanceInputPayload;
import com.piranport.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {

    private static final double FIRE_CONTROL_RANGE = 80.0;

    private static boolean highlightEnabled = false;
    private static boolean cooldownOverrideClientState = false;
    private static boolean hitDisplayEnabled = true;

    public static boolean isHighlightEnabled() { return highlightEnabled; }
    private static final Set<Integer> highlightedEntityIds = new HashSet<>();

    private static final String FC_TEAM_NAME = "pp_fc_target";
    private static final Set<String> fcTeamMembers = new HashSet<>();
    private static final String ASW_TEAM_NAME = "pp_asw_sonar";
    private static final Set<String> aswTeamMembers = new HashSet<>();
    private static final Set<Integer> aswHighlightedEntityIds = new HashSet<>();
    /** 仅高亮（非火控）激活时，每 N tick 进行一次完整实体扫描。 */
    private static final int ENTITY_SCAN_INTERVAL = 4;
    private static int entityScanCooldown = 0;

    /** UUID→实体查找缓存，避免遍历所有实体来查找已知目标。 */
    private static final EntityUuidCache entityCache = new EntityUuidCache();

    /**
     * 如果实体拥有原版级发光效果（例如发光药水效果、光灵箭），返回 true。
     * 原版发光拥有最高优先级，绝不能被本模组移除。
     */
    private static boolean hasVanillaGlow(Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living.hasEffect(net.minecraft.world.effect.MobEffects.GLOWING);
        }
        return false;
    }

    /** 重置所有客户端静态状态（断开连接时调用）。 */
    public static void resetClientState() {
        highlightEnabled = false;
        cooldownOverrideClientState = false;
        highlightedEntityIds.clear();
        entityCache.clear();
        clearFcTeam(Minecraft.getInstance());
        clearAswTeam(Minecraft.getInstance());
        com.piranport.aviation.ClientAswSonarData.resetClientState();
        ClientTorpedoGuidance.resetClientState();
        com.piranport.client.ClientScopeHandler.clear();
        useWasDown = false;
        attackWasDown = false;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        CameraShakeHandler.tick();

        // V 键 — 丢弃鱼雷导线、退出侦察模式，或仅在 GUI 模式下切换武器
        while (ModKeyMappings.CYCLE_WEAPON.consumeClick()) {
            if (ClientTorpedoGuidance.isActive()) {
                if (mc.getConnection() != null) {
                    PacketDistributor.sendToServer(new TorpedoGuidanceExitPayload());
                }
            } else if (ClientReconData.isInReconMode()) {
                if (mc.getConnection() != null) {
                    PacketDistributor.sendToServer(new ReconExitPayload());
                }
            } else {
                // 让服务端判断 GUI 模式是否激活 — 避免客户端读取 Common 配置
                ItemStack hand = mc.player.getMainHandItem();
                if (hand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(hand)) {
                    PacketDistributor.sendToServer(new CycleWeaponPayload());
                }
            }
            // 无 GUI 模式：武器为副手物品，V 键在此不执行任何操作
        }

        // 鱼雷制导模式：将玩家旋转镜像到鱼雷，并将视线方向流式发送到服务端
        boolean inTorpedoGuidance = ClientTorpedoGuidance.isActive();
        if (inTorpedoGuidance) {
            if (mc.level != null) {
                Entity torpedoEntity = mc.level.getEntity(ClientTorpedoGuidance.getTorpedoEntityId());
                if (torpedoEntity != null) {
                    if (mc.getCameraEntity() != torpedoEntity) {
                        mc.setCameraEntity(torpedoEntity);
                    }
                    torpedoEntity.setXRot(mc.player.getXRot());
                    torpedoEntity.setYRot(mc.player.getYRot());
                    torpedoEntity.xRotO = mc.player.xRotO;
                    torpedoEntity.yRotO = mc.player.yRotO;
                } else {
                    // 鱼雷在客户端已消失 — 退出制导
                    ClientTorpedoGuidance.handleEnd();
                    inTorpedoGuidance = false;
                }
            }
            if (inTorpedoGuidance && mc.player.tickCount % 2 == 0) {
                Vec3 look = mc.player.getLookAngle();
                PacketDistributor.sendToServer(new TorpedoGuidanceInputPayload(
                        (float) look.x, (float) look.y, (float) look.z));
            }
        }

        // Phase 32：侦察机 WASD 操控（每 2 tick 发送一次以减少网络流量）
        boolean inReconMode = ClientReconData.isInReconMode();
        if (inReconMode) {
            // 将玩家鼠标旋转镜像到侦察实体，使镜头随鼠标输入旋转
            if (mc.level != null) {
                Entity reconEntity = mc.level.getEntity(ClientReconData.getReconEntityId());
                if (reconEntity != null) {
                    // 维持镜头绑定 — Minecraft 可能会重置它（例如实体重新同步）
                    if (mc.getCameraEntity() != reconEntity) {
                        mc.setCameraEntity(reconEntity);
                    }
                    // 同时设置当前和上一 tick 的旋转，防止 partialTick 插值闪烁
                    reconEntity.setXRot(mc.player.getXRot());
                    reconEntity.setYRot(mc.player.getYRot());
                    reconEntity.xRotO = mc.player.xRotO;
                    reconEntity.yRotO = mc.player.yRotO;
                }
            }
            if (mc.player.tickCount % 2 == 0) {
                handleReconInput(mc);
            }
            // 不返回 — 火控和高亮同步在侦察模式下仍需运行
        }

        boolean transformed = TransformationManager.isPlayerTransformed(mc.player);

        // 大型船变身态：本地客户端持续压平 hurtTime/hurtDuration，移除受击视野抖动
        if (transformed) {
            ItemStack coreStack = TransformationManager.findTransformedCore(mc.player);
            if (!coreStack.isEmpty() && coreStack.getItem() instanceof ShipCoreItem sci
                    && sci.getShipType() == ShipType.LARGE) {
                if (mc.player.hurtTime > 0) {
                    mc.player.hurtTime = 0;
                    mc.player.hurtDuration = 0;
                }
            }
        }

        // 火控 — 变身态或侦察模式下
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

        // 打开飞行编队 GUI（U 键） — 仅变身态下，非侦察模式
        while (ModKeyMappings.OPEN_FLIGHT_GROUP.consumeClick()) {
            if (!transformed || inReconMode) continue;
            int coreSlot = findCoreSlot(mc.player);
            if (coreSlot >= 0) {
                PacketDistributor.sendToServer(new OpenFlightGroupPayload(coreSlot));
            }
        }

        // H 键 — 切换战斗机自动升空（无 GUI 模式）
        while (ModKeyMappings.TOGGLE_AUTO_LAUNCH.consumeClick()) {
            if (!transformed || inReconMode) continue;
            int autoSlot = findCoreSlot(mc.player);
            if (autoSlot >= 0) {
                PacketDistributor.sendToServer(new AutoLaunchTogglePayload(autoSlot));
            }
        }

        // R 键 — 手动装填（仅鱼雷/导弹，Phase 4 火炮不再响应）
        while (ModKeyMappings.MANUAL_RELOAD.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new ManualReloadPayload());
        }

        // Tab 键 — 切换火炮偏好弹种
        while (ModKeyMappings.SWITCH_AMMO.consumeClick()) {
            if (!transformed || inReconMode) continue;
            ItemStack hand = mc.player.getMainHandItem();
            if (hand.getItem() instanceof com.piranport.artillery.ArtilleryItem
                    || hand.getItem() instanceof com.piranport.item.CannonItem) {
                PacketDistributor.sendToServer(new SwitchAmmoPayload());
                AmmoSelectOverlay.bumpShow();
            }
        }

        // F8 / Shift+F8：调试开关 / 快照
        while (ModKeyMappings.DEBUG_TOGGLE.consumeClick()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                // Shift+F8：请求服务端快照（无需启用调试）
                PacketDistributor.sendToServer(new SnapshotRequestPayload());
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("[PP] Snapshot written to logs/piranport-debug.log"), true);
            } else {
                // F8：切换客户端水印 + 通知服务端
                boolean nowEnabled = PiranPortDebug.toggleClient();
                PacketDistributor.sendToServer(new DebugTogglePayload(nowEnabled));
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                nowEnabled ? "[PP DEBUG] ON" : "[PP DEBUG] OFF"), true);
            }
        }

        // 调试：切换全局冷却覆盖（将所有冷却压缩到 5 秒）
        while (ModKeyMappings.DEBUG_COOLDOWN_OVERRIDE.consumeClick()) {
            cooldownOverrideClientState = !cooldownOverrideClientState;
            boolean nowEnabled = cooldownOverrideClientState;
            PacketDistributor.sendToServer(new DebugCooldownOverridePayload(nowEnabled));
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            nowEnabled ? "message.piranport.debug_cooldown_override_on"
                                       : "message.piranport.debug_cooldown_override_off"),
                    true);
        }

        // J 键 — 切换武器命中/击杀/未命中聊天通知
        while (ModKeyMappings.HIT_DISPLAY_TOGGLE.consumeClick()) {
            hitDisplayEnabled = !hitDisplayEnabled;
            PacketDistributor.sendToServer(new HitDisplayTogglePayload(hitDisplayEnabled));
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            hitDisplayEnabled ? "message.piranport.hit_display_on"
                                              : "message.piranport.hit_display_off"),
                    true);
        }

        // 切换实体高亮（Y 键）
        while (ModKeyMappings.HIGHLIGHT_ENTITIES.consumeClick()) {
            highlightEnabled = !highlightEnabled;
            if (!highlightEnabled) {
                // 移除仅高亮的实体的发光效果；保留火控目标和原版发光
                if (mc.level != null) {
                    java.util.Set<UUID> fcTargets = new java.util.HashSet<>(ClientFireControlData.getTargets());
                    for (int id : List.copyOf(highlightedEntityIds)) {
                        Entity e = mc.level.getEntity(id);
                        if (e != null && !fcTargets.contains(e.getUUID()) && !hasVanillaGlow(e)) {
                            e.setGlowingTag(false);
                            highlightedEntityIds.remove(id);
                        }
                    }
                }
            }
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            highlightEnabled ? "message.piranport.highlight_on"
                                             : "message.piranport.highlight_off"),
                    true);
        }

        // 每 tick 应用/维持高亮和火控发光效果
        if (mc.level != null) {
            Player localPlayer = mc.player;
            java.util.Set<UUID> lockedTargets = ClientFireControlData.getTargets().isEmpty()
                ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(ClientFireControlData.getTargets());
            boolean hasFcTargets = !lockedTargets.isEmpty();

            Set<String> currentFcMembers = new HashSet<>();

            // 阶段 1：火控目标 — 通过 UUID 缓存进行 O(k) 定向查找，无需完整实体扫描
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

            // 阶段 2：Y 键战场高亮 — 完整扫描按 ENTITY_SCAN_INTERVAL 节流
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

            // 阶段 3：移除不再处于任何活跃高亮集合中的实体的发光效果。
            // 仅遍历之前高亮过的 ID（O(k)），而不是所有实体。
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
            // 同步客户端计分板队伍，为火控目标显示红色轮廓
            syncFcTeam(mc, currentFcMembers);
        }

        // 反潜声呐高亮 — 独立于 Y 键，使用黄色轮廓
        com.piranport.aviation.ClientAswSonarData.tick();
        if (mc.level != null) {
            Set<Integer> aswDetected = com.piranport.aviation.ClientAswSonarData.getAllDetected();
            Set<String> currentAswMembers = new HashSet<>();
            java.util.Set<UUID> lockedTargets2 = ClientFireControlData.getTargets().isEmpty()
                ? java.util.Collections.emptySet()
                : new java.util.HashSet<>(ClientFireControlData.getTargets());

            // 为新检测到的实体应用发光效果
            for (int eid : aswDetected) {
                Entity entity = mc.level.getEntity(eid);
                if (entity == null || !entity.isAlive()) continue;
                entity.setGlowingTag(true);
                aswHighlightedEntityIds.add(eid);
                // 火控队伍优先级更高 — 如果已是火控目标，不加入反潜队伍
                if (!lockedTargets2.contains(entity.getUUID())) {
                    currentAswMembers.add(entity.getStringUUID());
                }
            }
            // 移除不再被声呐检测到的实体的发光效果
            aswHighlightedEntityIds.removeIf(id -> {
                if (aswDetected.contains(id)) return false;
                Entity entity = mc.level.getEntity(id);
                if (entity == null) return true;
                // 如果实体被火控、Y 键或原版高亮，不移除发光效果
                if (highlightedEntityIds.contains(id)) return true;
                if (hasVanillaGlow(entity)) return true;
                entity.setGlowingTag(false);
                return true;
            });
            syncAswTeam(mc, currentAswMembers);
        }

        // ===== Phase 5: Scope mode right-click hold detection =====
        handleScopeInput(mc);
    }

    /** Phase 5: 右键切换瞄准镜，左键发射。 */
    private static boolean attackWasDown = false;
    private static boolean useWasDown = false;

    private static void handleScopeInput(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        boolean holdingCannon = com.piranport.client.ClientScopeHandler.isHoldingCannon(mc.player);
        boolean isScoping = com.piranport.client.ClientScopeHandler.isScoping();

        if (!holdingCannon && isScoping) {
            com.piranport.client.ClientScopeHandler.exitScope();
            useWasDown = false;
            attackWasDown = false;
            return;
        }

        // 右键：由 ArtilleryItem.use() 切换瞄准镜，此处只发网络包
        boolean useDown = mc.options.keyUse.isDown();
        if (useDown && !useWasDown && holdingCannon) {
            if (mc.getConnection() != null) {
                PacketDistributor.sendToServer(new com.piranport.network.ScopeEnterPayload(
                        com.piranport.client.ClientScopeHandler.isScoping()));
            }
        }
        useWasDown = useDown;

        // 左键：开火
        boolean attackDown = mc.options.keyAttack.isDown();
        if (attackDown && !attackWasDown && holdingCannon) {
            if (mc.getConnection() != null) {
                if (isScoping) {
                    net.minecraft.world.phys.Vec3 target = com.piranport.client.ClientScopeHandler.getAimedPosition();
                    if (target != null) {
                        PacketDistributor.sendToServer(
                                com.piranport.network.ScopeFirePayload.aimedFire(target.x, target.y, target.z));
                    } else {
                        PacketDistributor.sendToServer(com.piranport.network.ScopeFirePayload.quickFire());
                    }
                } else {
                    PacketDistributor.sendToServer(com.piranport.network.ScopeFirePayload.quickFire());
                }
            }
        }
        attackWasDown = attackDown;

        if (isScoping) {
            com.piranport.client.ClientScopeHandler.tick(mc.player, mc.player.getMainHandItem());
        }
    }

    /**
     * 当 Y 键高亮启用时，判断实体是否应该发光。
     * 不包含火控目标 — 火控目标单独处理。
     */
    private static boolean isHighlightTarget(Entity entity, Player localPlayer) {
        // 子弹使用 setGlowingTag；鱼雷和炸弹使用 isCurrentlyGlowing() 覆写
        if (entity instanceof BulletEntity bl && bl.getOwner() == localPlayer) return true;

        // 优先高亮敌对实体，其次中立/友好实体
        if (entity instanceof LivingEntity living && living != localPlayer) {
            double distSq = entity.distanceToSqr(localPlayer);
            // 仅高亮合理范围内的实体（32 格）
            if (distSq <= 32 * 32) {
                // 优先级：敌对 > 中立 > 友好
                if (living.getLastHurtByMob() == localPlayer ||
                    (living instanceof net.minecraft.world.entity.Mob mob
                            && mob.getTarget() != null && mob.getTarget() == localPlayer)) {
                    return true; // 敌对 - 最高优先级
                }
                if (living instanceof net.minecraft.world.entity.monster.Monster) {
                    return true; // 怪物 - 高优先级
                }
                if (living instanceof net.minecraft.world.entity.animal.Animal) {
                    return distSq <= 16 * 16; // 动物 - 较低优先级，更近距离
                }
            }
        }

        return false;
    }

    /**
     * Phase 32：将 WASD/空格/潜行输入发送到服务端，用于侦察机操控。
     * 移动方向相对于玩家当前视线方向。
     */
    private static void handleReconInput(Minecraft mc) {
        if (mc.player == null) return;

        // 验证客户端状态
        if (!ClientReconData.isInReconMode()) {
            return;
        }

        // 验证实体存在
        int entityId = ClientReconData.getReconEntityId();
        if (mc.level != null) {
            Entity reconEntity = mc.level.getEntity(entityId);
            if (reconEntity == null) {
                com.piranport.PiranPort.LOGGER.warn("ClientReconInput ENTITY_MISSING | entityId={}", entityId);
                return;
            }
        }

        Options opts = mc.options;
        boolean anyKey = opts.keyUp.isDown() || opts.keyDown.isDown()
                || opts.keyLeft.isDown() || opts.keyRight.isDown()
                || opts.keyJump.isDown() || opts.keyShift.isDown();
        if (!anyKey) return;

        Vec3 look = mc.player.getLookAngle();
        // 前向 = 水平视线方向，右向 = 水平垂直方向
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        double fwdLen = fwd.length();
        if (fwdLen < 0.001) fwd = new Vec3(0, 0, 1);
        else fwd = fwd.scale(1.0 / fwdLen);
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);  // 前向顺时针旋转 90°

        float dx = 0, dy = 0, dz = 0;
        if (opts.keyUp.isDown())  { dx += (float) fwd.x;   dz += (float) fwd.z; }
        if (opts.keyDown.isDown())     { dx -= (float) fwd.x;   dz -= (float) fwd.z; }
        if (opts.keyLeft.isDown())     { dx -= (float) right.x; dz -= (float) right.z; }
        if (opts.keyRight.isDown())    { dx += (float) right.x; dz += (float) right.z; }
        if (opts.keyJump.isDown())     dy += 1f;
        if (opts.keyShift.isDown())    dy -= 1f;

        // 归一化水平分量，防止斜向加速
        float hLen = (float) Math.sqrt(dx * dx + dz * dz);
        if (hLen > 1f) { dx /= hLen; dz /= hLen; }

        PacketDistributor.sendToServer(new ReconControlPayload(dx, dy, dz));
    }

    /** 同步客户端 FC 目标队伍：添加新成员，移除过时成员 */
    private static void syncFcTeam(Minecraft mc, Set<String> currentMembers) {
        if (mc.level == null) return;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(FC_TEAM_NAME);
        net.minecraft.world.scores.PlayerTeam aswTeam = scoreboard.getPlayerTeam(ASW_TEAM_NAME);

        if (team == null && !currentMembers.isEmpty()) {
            team = scoreboard.addPlayerTeam(FC_TEAM_NAME);
            team.setColor(net.minecraft.ChatFormatting.RED);
        }
        if (team == null) {
            fcTeamMembers.clear();
            return;
        }
        // 移除不再锁定的成员
        for (String name : new HashSet<>(fcTeamMembers)) {
            if (!currentMembers.contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        // 添加新成员并从 ASW 队伍中移除（FC 优先级高于 ASW）
        for (String name : currentMembers) {
            if (!fcTeamMembers.contains(name)) {
                // 如果存在于 ASW 队伍中则移除（FC 优先级 > ASW 优先级）
                if (aswTeam != null && aswTeamMembers.contains(name)) {
                    scoreboard.removePlayerFromTeam(name, aswTeam);
                    aswTeamMembers.remove(name);
                    // 同时从 ASW 高亮跟踪中移除
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

    /** 移除所有 FC 队伍成员并删除队伍 */
    private static void clearFcTeam(Minecraft mc) {
        if (mc.level == null || fcTeamMembers.isEmpty()) return;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(FC_TEAM_NAME);
        if (team != null) {
            for (String name : fcTeamMembers) {
                scoreboard.removePlayerFromTeam(name, team);
            }
            scoreboard.removePlayerTeam(team);
        }
        fcTeamMembers.clear();
    }

    /** 同步客户端 ASW 声呐队伍：已检测水下实体的黄色轮廓 */
    private static void syncAswTeam(Minecraft mc, Set<String> currentMembers) {
        if (mc.level == null) return;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(ASW_TEAM_NAME);
        if (team == null && !currentMembers.isEmpty()) {
            team = scoreboard.addPlayerTeam(ASW_TEAM_NAME);
            team.setColor(net.minecraft.ChatFormatting.YELLOW);
        }
        if (team == null) {
            aswTeamMembers.clear();
            return;
        }
        // 移除不再检测到的成员
        for (String name : new HashSet<>(aswTeamMembers)) {
            if (!currentMembers.contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        // 添加新成员
        for (String name : currentMembers) {
            if (!aswTeamMembers.contains(name)) {
                scoreboard.addPlayerToTeam(name, team);
            }
        }
        aswTeamMembers.clear();
        aswTeamMembers.addAll(currentMembers);
    }

    /** 移除所有 ASW 队伍成员并删除队伍 */
    private static void clearAswTeam(Minecraft mc) {
        if (mc.level == null || aswTeamMembers.isEmpty()) return;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(ASW_TEAM_NAME);
        if (team != null) {
            for (String name : aswTeamMembers) {
                scoreboard.removePlayerFromTeam(name, team);
            }
            scoreboard.removePlayerTeam(team);
        }
        aswTeamMembers.clear();
        aswHighlightedEntityIds.clear();
    }

    /** 查找活跃变身核心所在的背包槽位，未找到返回 -1 */
    private static int findCoreSlot(Player player) {
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
    private static Entity getTargetInCrosshair(Minecraft mc, double range) {
        if (mc.player == null || mc.level == null) return null;
        // 侦察模式下，从摄像机实体（侦察机）位置射
        Entity cameraEntity = mc.getCameraEntity();
        if (cameraEntity == null) cameraEntity = mc.player;
        Vec3 eyePos = cameraEntity.getEyePosition();
        Vec3 lookDir = mc.player.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(range));

        AABB searchBox = cameraEntity.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(1.0);

        final Entity cam = cameraEntity;
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, mc.player, eyePos, end, searchBox,
                e -> (e instanceof LivingEntity || e instanceof AircraftEntity)
                        && e.isAlive() && e != mc.player && e != cam
                        && !(e instanceof net.minecraft.world.Container),
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }
}
