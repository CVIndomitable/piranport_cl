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
import com.piranport.network.CycleWeaponPayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.OpenFlightGroupPayload;
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

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // V key — exits recon mode, or cycles weapon in GUI mode only
        while (ClientEvents.CYCLE_WEAPON_KEY.consumeClick()) {
            if (ClientReconData.isInReconMode()) {
                PacketDistributor.sendToServer(new ReconExitPayload());
            } else if (com.piranport.config.ModCommonConfig.SHIP_CORE_GUI_ENABLED.get()) {
                ItemStack hand = mc.player.getMainHandItem();
                if (hand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(hand)) {
                    PacketDistributor.sendToServer(new CycleWeaponPayload());
                }
            }
            // No-GUI mode: weapon is the other-hand item, V key does nothing here
        }

        // Phase 32: recon aircraft WASD control
        if (ClientReconData.isInReconMode()) {
            // Mirror player mouse rotation to the recon entity so the camera rotates with mouse input
            if (mc.level != null) {
                Entity reconEntity = mc.level.getEntity(ClientReconData.getReconEntityId());
                if (reconEntity != null) {
                    reconEntity.setXRot(mc.player.getXRot());
                    reconEntity.setYRot(mc.player.getYRot());
                }
            }
            handleReconInput(mc);
            return;  // skip all other key handling while in recon mode
        }

        // GUI mode: core must be in main hand.
        // No-GUI mode: core can be anywhere in inventory/offhand — scan the full inventory.
        boolean transformed = false;
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(mainHand)) {
            transformed = true;
        } else {
            for (ItemStack s : mc.player.getInventory().items) {
                if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                    transformed = true;
                    break;
                }
            }
            if (!transformed) {
                ItemStack offhand = mc.player.getInventory().offhand.get(0);
                if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) {
                    transformed = true;
                }
            }
        }

        // Fire control — only while transformed
        while (ModKeyMappings.FIRE_CONTROL_LOCK.consumeClick()) {
            if (!transformed) continue;
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);
            if (target != null) {
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.LOCK, target.getUUID()));
            }
        }

        while (ModKeyMappings.FIRE_CONTROL_ADD.consumeClick()) {
            if (!transformed) continue;
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

        // Open flight group GUI (U key) — only while transformed
        while (ModKeyMappings.OPEN_FLIGHT_GROUP.consumeClick()) {
            if (!transformed) continue;
            // Find the actual core slot (may be in offhand or elsewhere in no-GUI mode)
            int coreSlot = mc.player.getInventory().selected;
            ItemStack mh = mc.player.getMainHandItem();
            if (!(mh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(mh))) {
                // Core not in main hand — scan inventory
                coreSlot = -1;
                for (int i = 0; i < mc.player.getInventory().items.size(); i++) {
                    ItemStack s = mc.player.getInventory().items.get(i);
                    if (s.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(s)) {
                        coreSlot = i;
                        break;
                    }
                }
                if (coreSlot == -1) {
                    ItemStack oh = mc.player.getInventory().offhand.get(0);
                    if (oh.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(oh)) {
                        coreSlot = 40;
                    }
                }
            }
            if (coreSlot >= 0) {
                PacketDistributor.sendToServer(new OpenFlightGroupPayload(coreSlot));
            }
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
                // Remove glow from all entities we highlighted
                if (mc.level != null) {
                    for (int id : highlightedEntityIds) {
                        Entity e = mc.level.getEntity(id);
                        if (e != null) e.setGlowingTag(false);
                    }
                }
                highlightedEntityIds.clear();
            }
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(
                            highlightEnabled ? "message.piranport.highlight_on"
                                             : "message.piranport.highlight_off"),
                    true);
        }

        // Apply/maintain highlight effect each tick
        if (highlightEnabled && mc.level != null) {
            Player localPlayer = mc.player;
            List<UUID> lockedTargets = ClientFireControlData.getTargets();

            for (Entity entity : mc.level.entitiesForRendering()) {
                boolean shouldGlow = isHighlightTarget(entity, localPlayer, lockedTargets);
                if (shouldGlow) {
                    // Set every tick — server data sync may reset the flag
                    entity.setGlowingTag(true);
                    highlightedEntityIds.add(entity.getId());
                } else if (highlightedEntityIds.contains(entity.getId())) {
                    entity.setGlowingTag(false);
                    highlightedEntityIds.remove(entity.getId());
                }
            }
            // Clean up IDs for despawned entities
            highlightedEntityIds.removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    /**
     * Returns true if the entity should have setGlowingTag applied.
     * AircraftEntity is excluded — its outline is handled via AircraftRenderer.shouldShowOutline().
     */
    private static boolean isHighlightTarget(Entity entity, Player localPlayer, List<UUID> lockedTargets) {
        // Torpedoes, bombs, bullets (use ThrownItemRenderer, no shouldShowOutline override available)
        if (entity instanceof TorpedoEntity te && te.getOwner() == localPlayer) return true;
        if (entity instanceof AerialBombEntity be && be.getOwner() == localPlayer) return true;
        if (entity instanceof BulletEntity bl && bl.getOwner() == localPlayer) return true;
        // Fire control locked targets (any entity type)
        if (lockedTargets.contains(entity.getUUID())) return true;
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

    @Nullable
    private static Entity getTargetInCrosshair(Minecraft mc, double range) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookDir = mc.player.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(range));

        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, mc.player, eyePos, end, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player,
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }
}
