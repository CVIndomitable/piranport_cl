package com.piranport;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AerialBombEntity;
import com.piranport.entity.AircraftEntity;
import com.piranport.entity.BulletEntity;
import com.piranport.entity.TorpedoEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.CycleWeaponPayload;
import com.piranport.network.FireControlPayload;
import com.piranport.network.OpenFlightGroupPayload;
import com.piranport.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
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
    private static final Set<Integer> highlightedEntityIds = new HashSet<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Weapon cycling
        while (ClientEvents.CYCLE_WEAPON_KEY.consumeClick()) {
            ItemStack hand = mc.player.getMainHandItem();
            if (hand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(hand)) {
                PacketDistributor.sendToServer(new CycleWeaponPayload());
            }
        }

        boolean transformed = mc.player.getMainHandItem().getItem() instanceof ShipCoreItem sci
                && TransformationManager.isTransformed(mc.player.getMainHandItem());

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
            int coreSlot = mc.player.getInventory().selected;
            PacketDistributor.sendToServer(new OpenFlightGroupPayload(coreSlot));
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
        }

        // Apply/maintain highlight effect each tick
        if (highlightEnabled && mc.level != null) {
            Player localPlayer = mc.player;
            List<UUID> lockedTargets = ClientFireControlData.getTargets();

            for (Entity entity : mc.level.entitiesForRendering()) {
                boolean shouldGlow = isHighlightTarget(entity, localPlayer, lockedTargets);
                if (shouldGlow) {
                    if (!highlightedEntityIds.contains(entity.getId())) {
                        entity.setGlowingTag(true);
                        highlightedEntityIds.add(entity.getId());
                    }
                } else if (highlightedEntityIds.contains(entity.getId())) {
                    entity.setGlowingTag(false);
                    highlightedEntityIds.remove(entity.getId());
                }
            }
            // Clean up IDs for despawned entities
            highlightedEntityIds.removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    /** Returns true if the entity should be highlighted. */
    private static boolean isHighlightTarget(Entity entity, Player localPlayer, List<UUID> lockedTargets) {
        // Own aircraft
        if (entity instanceof AircraftEntity ae && ae.isOwnedByPlayer(localPlayer)) return true;
        // Own torpedoes and bombs
        if (entity instanceof TorpedoEntity te && te.getOwner() == localPlayer) return true;
        if (entity instanceof AerialBombEntity be && be.getOwner() == localPlayer) return true;
        if (entity instanceof BulletEntity bl && bl.getOwner() == localPlayer) return true;
        // Other players' aircraft, torpedoes, bombs (entities of same type not owned by us)
        if (entity instanceof AircraftEntity ae && !ae.isOwnedByPlayer(localPlayer)) return true;
        if (entity instanceof TorpedoEntity te2 && !(te2.getOwner() == localPlayer)) return true;
        if (entity instanceof AerialBombEntity be2 && !(be2.getOwner() == localPlayer)) return true;
        // Fire control locked targets
        if (lockedTargets.contains(entity.getUUID())) return true;
        return false;
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
