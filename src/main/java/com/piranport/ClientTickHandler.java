package com.piranport;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.aviation.ClientReconData;
import com.piranport.combat.TransformationManager;
import com.piranport.debug.PiranPortDebug;
import com.piranport.network.DebugTogglePayload;
import com.piranport.network.SnapshotRequestPayload;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.BulletEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.AutoLaunchTogglePayload;
import com.piranport.network.CycleWeaponPayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.ManualReloadPayload;
import com.piranport.network.OpenFlightGroupPayload;
import com.piranport.network.TorpedoSteerPayload;
import com.piranport.network.ReconControlPayload;
import com.piranport.network.ReconExitPayload;
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

    public static boolean isHighlightEnabled() { return highlightEnabled; }
    private static final Set<Integer> highlightedEntityIds = new HashSet<>();

    private static final String FC_TEAM_NAME = "pp_fc_target";
    private static final Set<String> fcTeamMembers = new HashSet<>();
    /** Throttle full entity scan to every 4 ticks when only highlight (not FC) is active. */
    private static int entityScanCooldown = 0;

    /** Reset all client-side static state (called on disconnect). */
    public static void resetClientState() {
        highlightEnabled = false;
        highlightedEntityIds.clear();
        clearFcTeam(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // V key — exits recon mode, or cycles weapon in GUI mode only
        while (ModKeyMappings.CYCLE_WEAPON.consumeClick()) {
            if (ClientReconData.isInReconMode()) {
                PacketDistributor.sendToServer(new ReconExitPayload());
            } else {
                // Let the server decide if GUI mode is active — avoids reading Common config on client
                ItemStack hand = mc.player.getMainHandItem();
                if (hand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(hand)) {
                    PacketDistributor.sendToServer(new CycleWeaponPayload());
                }
            }
            // No-GUI mode: weapon is the other-hand item, V key does nothing here
        }

        // Phase 32: recon aircraft WASD control (throttled to every 2 ticks to reduce network traffic)
        boolean inReconMode = ClientReconData.isInReconMode();
        if (inReconMode) {
            // Mirror player mouse rotation to the recon entity so the camera rotates with mouse input
            if (mc.level != null) {
                Entity reconEntity = mc.level.getEntity(ClientReconData.getReconEntityId());
                if (reconEntity != null) {
                    // Maintain camera binding — Minecraft may reset it (e.g. entity re-sync)
                    if (mc.getCameraEntity() != reconEntity) {
                        mc.setCameraEntity(reconEntity);
                    }
                    // Set both current and previous-tick rotation to prevent partialTick interpolation flicker
                    reconEntity.setXRot(mc.player.getXRot());
                    reconEntity.setYRot(mc.player.getYRot());
                    reconEntity.xRotO = mc.player.xRotO;
                    reconEntity.yRotO = mc.player.yRotO;
                }
            }
            if (mc.player.tickCount % 2 == 0) {
                handleReconInput(mc);
            }
            // Don't return — fire control and glow sync still need to run in recon mode
        }

        boolean transformed = TransformationManager.isPlayerTransformed(mc.player);

        // Fire control — while transformed or in recon mode
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

        // Open flight group GUI (U key) — only while transformed, not in recon mode
        while (ModKeyMappings.OPEN_FLIGHT_GROUP.consumeClick()) {
            if (!transformed || inReconMode) continue;
            int coreSlot = findCoreSlot(mc.player);
            if (coreSlot >= 0) {
                PacketDistributor.sendToServer(new OpenFlightGroupPayload(coreSlot));
            }
        }

        // H key — toggle fighter auto-launch (no-GUI mode)
        while (ModKeyMappings.TOGGLE_AUTO_LAUNCH.consumeClick()) {
            if (!transformed || inReconMode) continue;
            int autoSlot = findCoreSlot(mc.player);
            if (autoSlot >= 0) {
                PacketDistributor.sendToServer(new AutoLaunchTogglePayload(autoSlot));
            }
        }

        // R key — manual reload for medium/large cannons
        while (ModKeyMappings.MANUAL_RELOAD.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new ManualReloadPayload());
        }

        // 9/0 keys — wire-guided torpedo steering
        while (ModKeyMappings.TORPEDO_STEER_LEFT.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new TorpedoSteerPayload(-1));
        }
        while (ModKeyMappings.TORPEDO_STEER_RIGHT.consumeClick()) {
            if (!transformed || inReconMode) continue;
            PacketDistributor.sendToServer(new TorpedoSteerPayload(1));
        }

        // F8 / Shift+F8: debug toggle / snapshot
        while (ModKeyMappings.DEBUG_TOGGLE.consumeClick()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
                // Shift+F8: request server snapshot (no enabled required)
                PacketDistributor.sendToServer(new SnapshotRequestPayload());
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("[PP] Snapshot written to logs/piranport-debug.log"), true);
            } else {
                // F8: toggle client watermark + notify server
                boolean nowEnabled = PiranPortDebug.toggleClient();
                PacketDistributor.sendToServer(new DebugTogglePayload(nowEnabled));
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal(
                                nowEnabled ? "[PP DEBUG] ON" : "[PP DEBUG] OFF"), true);
            }
        }

        // Toggle entity highlight (Y key)
        while (ModKeyMappings.HIGHLIGHT_ENTITIES.consumeClick()) {
            highlightEnabled = !highlightEnabled;
            if (!highlightEnabled) {
                // Remove glow from highlight-only entities; keep FC targets glowing
                if (mc.level != null) {
                    java.util.Set<UUID> fcTargets = new java.util.HashSet<>(ClientFireControlData.getTargets());
                    for (int id : new HashSet<>(highlightedEntityIds)) {
                        Entity e = mc.level.getEntity(id);
                        if (e != null && !fcTargets.contains(e.getUUID())) {
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

        // Apply/maintain highlight and fire-control glow each tick
        if (mc.level != null) {
            Player localPlayer = mc.player;
            java.util.Set<UUID> lockedTargets = new java.util.HashSet<>(ClientFireControlData.getTargets());
            boolean hasFcTargets = !lockedTargets.isEmpty();

            Set<String> currentFcMembers = new HashSet<>();

            // Always process FC targets; process highlight targets only when Y-key enabled
            // Throttle full entity scan: FC targets use targeted lookup; highlight scans every 4 ticks
            if (hasFcTargets || highlightEnabled || !highlightedEntityIds.isEmpty()) {
                boolean doFullScan = highlightEnabled && (entityScanCooldown <= 0);
                if (doFullScan) entityScanCooldown = 4;

                if (doFullScan || hasFcTargets) {
                    for (Entity entity : mc.level.entitiesForRendering()) {
                        boolean isFcTarget = lockedTargets.contains(entity.getUUID());
                        boolean isHighlight = doFullScan && isHighlightTarget(entity, localPlayer);
                        boolean shouldGlow = isFcTarget || isHighlight;
                        if (shouldGlow) {
                            entity.setGlowingTag(true);
                            highlightedEntityIds.add(entity.getId());
                            if (isFcTarget && !(entity instanceof AircraftEntity)) {
                                currentFcMembers.add(entity.getStringUUID());
                            }
                        } else if (highlightedEntityIds.contains(entity.getId())) {
                            entity.setGlowingTag(false);
                            highlightedEntityIds.remove(entity.getId());
                        }
                    }
                }
                entityScanCooldown--;
            }
            // Sync client-side scoreboard team for red outline on FC targets
            syncFcTeam(mc, currentFcMembers);
            // Clean up IDs for despawned entities
            highlightedEntityIds.removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    /**
     * Returns true if the entity should glow when Y-key highlight is enabled.
     * Does NOT include fire control targets — those are handled separately.
     */
    private static boolean isHighlightTarget(Entity entity, Player localPlayer) {
        // Bullets use setGlowingTag; torpedoes & bombs use isCurrentlyGlowing() override
        if (entity instanceof BulletEntity bl && bl.getOwner() == localPlayer) return true;
        return false;
    }

    /**
     * Phase 32: send WASD/Space/Sneak input to server for recon aircraft control.
     * Movement direction is relative to the player's current look direction.
     */
    private static void handleReconInput(Minecraft mc) {
        if (mc.player == null) return;
        Options opts = mc.options;
        boolean anyKey = opts.keyUp.isDown() || opts.keyDown.isDown()
                || opts.keyLeft.isDown() || opts.keyRight.isDown()
                || opts.keyJump.isDown() || opts.keyShift.isDown();
        if (!anyKey) return;

        Vec3 look = mc.player.getLookAngle();
        // Forward = horizontal look direction, Right = perpendicular horizontal
        Vec3 fwd = new Vec3(look.x, 0, look.z);
        double fwdLen = fwd.length();
        if (fwdLen < 0.001) fwd = new Vec3(0, 0, 1);
        else fwd = fwd.scale(1.0 / fwdLen);
        Vec3 right = new Vec3(-fwd.z, 0, fwd.x);  // 90° clockwise rotation of forward

        float dx = 0, dy = 0, dz = 0;
        if (opts.keyUp.isDown())  { dx += (float) fwd.x;   dz += (float) fwd.z; }
        if (opts.keyDown.isDown())     { dx -= (float) fwd.x;   dz -= (float) fwd.z; }
        if (opts.keyLeft.isDown())     { dx -= (float) right.x; dz -= (float) right.z; }
        if (opts.keyRight.isDown())    { dx += (float) right.x; dz += (float) right.z; }
        if (opts.keyJump.isDown())     dy += 1f;
        if (opts.keyShift.isDown())    dy -= 1f;

        // Normalize horizontal to prevent diagonal speed boost
        float hLen = (float) Math.sqrt(dx * dx + dz * dz);
        if (hLen > 1f) { dx /= hLen; dz /= hLen; }

        PacketDistributor.sendToServer(new ReconControlPayload(dx, dy, dz));
    }

    /** Sync the client-side FC target team: add new members, remove stale ones. */
    private static void syncFcTeam(Minecraft mc, Set<String> currentMembers) {
        if (mc.level == null) return;
        net.minecraft.world.scores.Scoreboard scoreboard = mc.level.getScoreboard();
        net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(FC_TEAM_NAME);
        if (team == null && !currentMembers.isEmpty()) {
            team = scoreboard.addPlayerTeam(FC_TEAM_NAME);
            team.setColor(net.minecraft.ChatFormatting.RED);
        }
        if (team == null) {
            fcTeamMembers.clear();
            return;
        }
        // Remove members no longer targeted
        for (String name : new HashSet<>(fcTeamMembers)) {
            if (!currentMembers.contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
            }
        }
        // Add new members
        for (String name : currentMembers) {
            if (!fcTeamMembers.contains(name)) {
                scoreboard.addPlayerToTeam(name, team);
            }
        }
        fcTeamMembers.clear();
        fcTeamMembers.addAll(currentMembers);
    }

    /** Remove all FC team members and delete the team. */
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

    /** Find the inventory slot of the active transformed ship core. Returns -1 if not found. */
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
        // In recon mode, raycast from the camera entity (recon aircraft) position
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
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player && e != cam,
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }
}
