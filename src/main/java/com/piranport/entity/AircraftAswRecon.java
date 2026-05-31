package com.piranport.entity;

import com.piranport.aviation.FireControlManager;
import com.piranport.component.AircraftAttackMode;
import com.piranport.aviation.ReconManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.network.AswSonarSyncPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 反潜声呐 + 侦察机子系统 — 从 AircraftEntity 提取的静态辅助方法。
 *
 * <p><b>设计</b>: 静态方法以 AircraftEntity 为首参数，直接读写其实例字段（同包可见性）。
 * <p><b>线程模型</b>: 服务端主线程。
 */
public class AircraftAswRecon {

    private AircraftAswRecon() {}

    // ====================================================================
    // ASW 攻击
    // ====================================================================

    /** @see AircraftEntity#tickASWAttack(Player, LivingEntity) */
    public static void tickASWAttack(AircraftEntity craft, @Nullable Player owner, LivingEntity target) {
        if (craft.remainingAmmo <= 0) {
            if (owner != null && ModCommonConfig.AUTO_RESUPPLY_ENABLED.get() && craft.tryAutoResupplyAmmo(owner)) {
                // Ammo restored — continue attacking
            } else {
                craft.startReturning("asw_ammo_depleted");
                return;
            }
        }

        double seaLevel = craft.level().getSeaLevel();
        double bombAltitude = target.isUnderWater()
            ? seaLevel + 15.0
            : target.getY() + 20.0;

        if (craft.getY() < bombAltitude - 1.5) {
            Vec3 toPoint = new Vec3(target.getX() - craft.getX(), bombAltitude - craft.getY(), target.getZ() - craft.getZ());
            double dist = toPoint.length();
            craft.setDeltaMovement(toPoint.normalize().scale(Math.min(craft.panelSpeed * 0.4, dist)));
        } else {
            double dx = target.getX() - craft.getX();
            double dz = target.getZ() - craft.getZ();
            double horizDist = Math.sqrt(dx * dx + dz * dz);

            double targetDepth = Math.max(0, seaLevel - target.getY());
            double leadDistance = targetDepth * 0.15;

            if (horizDist < (4.0 + leadDistance) && craft.attackCooldown <= 0) {
                float dcDamage = craft.panelDamage * 1.5f;
                int toFire = craft.computeSalvoSize(target, dcDamage);
                com.piranport.debug.PiranPortDebug.event(
                        "Aircraft ASW_SALVO | entityId={} capacity={} remaining={} firing={} targetHP={}",
                        craft.getId(), craft.ammoCapacity, craft.remainingAmmo, toFire, target.getHealth());
                Vec3 hv = craft.getDeltaMovement();
                double hvLen = hv.horizontalDistance();
                Vec3 forward = hvLen > 0.01 ? new Vec3(hv.x / hvLen, 0, hv.z / hvLen) : new Vec3(1, 0, 0);
                for (int i = 0; i < toFire; i++) {
                    double offset = (i - (toFire - 1) / 2.0) * 0.8;
                    double spawnX = craft.getX() + forward.x * offset;
                    double spawnZ = craft.getZ() + forward.z * offset;
                    DepthChargeEntity dc = new DepthChargeEntity(craft.level(), dcDamage, 3.0f);
                    dc.moveTo(spawnX, craft.getY(), spawnZ, dc.getYRot(), dc.getXRot());
                    dc.setDeltaMovement(craft.getDeltaMovement().x * 0.1, -0.1, craft.getDeltaMovement().z * 0.1);
                    dc.setOwner(owner);
                    craft.level().addFreshEntity(dc);
                }
                craft.remainingAmmo -= toFire;
                craft.attackCooldown = 30;
            }

            Vec3 horizontal = new Vec3(dx, 0, dz).normalize().scale(Math.min(craft.panelSpeed * 0.4, horizDist));
            double yCorrect = (bombAltitude - craft.getY()) * 0.15;
            craft.setDeltaMovement(horizontal.x, yCorrect, horizontal.z);
        }
    }

    // ====================================================================
    // ASW 目标解析
    // ====================================================================

    /** @see AircraftEntity#resolveASWTarget(Player) */
    @Nullable
    public static LivingEntity resolveASWTarget(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel sl)) return null;

        List<UUID> locks = FireControlManager.getTargets(owner.getUUID());
        if (!locks.isEmpty()) {
            if (craft.attackMode == AircraftAttackMode.SPREAD) {
                return locks.stream()
                        .map(sl::getEntity)
                        .filter(e -> e instanceof LivingEntity le && le.isAlive() && isAswTarget(le))
                        .map(e -> (LivingEntity) e)
                        .min(Comparator.comparingDouble(craft::distanceTo))
                        .orElse(null);
            } else {
                for (UUID uuid : locks) {
                    Entity e = sl.getEntity(uuid);
                    if (e instanceof LivingEntity le && le.isAlive() && isAswTarget(le)) return le;
                }
                return null;
            }
        }

        if (craft.hasEverHadFireControl || craft.autoSeekDone) return null;
        if (craft.autoSeekCooldown > 0) { craft.autoSeekCooldown--; return null; }
        craft.autoSeekDone = true;
        AABB box = craft.getBoundingBox().inflate(32.0);
        return sl.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && e != owner && isAswTarget(e))
                .stream()
                .min(Comparator.comparingDouble(craft::distanceTo))
                .orElse(null);
    }

    /** Returns true if the entity qualifies as an ASW target. */
    public static boolean isAswTarget(Entity e) {
        if (e instanceof com.piranport.npc.deepocean.DeepOceanSubmarineEntity) return true;
        if (e.getType().is(net.minecraft.tags.EntityTypeTags.AQUATIC)) return true;
        if (e instanceof net.minecraft.world.entity.monster.Guardian) return true;
        if (e instanceof net.minecraft.world.entity.monster.Monster && e.isUnderWater()) return true;
        return false;
    }

    // ====================================================================
    // ASW 声呐
    // ====================================================================

    /** @see AircraftEntity#tickAswSonar(Player) */
    public static void tickAswSonar(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel sl)) return;
        if (!(owner instanceof ServerPlayer sp)) return;

        AABB sonarBox = craft.getBoundingBox().inflate(16.0);
        java.util.List<Integer> detectedIds = new java.util.ArrayList<>();
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, sonarBox,
                le -> le.isAlive() && le != owner && (le.isInWater() || isAswTarget(le)))) {
            detectedIds.add(e.getId());
            if (detectedIds.size() >= 128) break;
        }
        if (!detectedIds.isEmpty()) {
            PacketDistributor.sendToPlayer(sp, new AswSonarSyncPayload(craft.getId(), detectedIds));
        }
    }

    // ====================================================================
    // 侦察机
    // ====================================================================

    /** @see AircraftEntity#tickReconActive(Player) */
    public static void tickReconActive(AircraftEntity craft, Player owner) {
        if (!ReconManager.isInRecon(owner.getUUID())) {
            com.piranport.PiranPort.LOGGER.warn(
                "Aircraft RECON_DESYNC | entityId={} ownerNotInRecon, forcing state sync", craft.getId());
            ReconManager.startRecon(owner.getUUID(), craft.getUUID());
        }

        int cx = craft.getBlockX() >> 4;
        int cz = craft.getBlockZ() >> 4;
        if (cx != craft.lastForcedChunkX || cz != craft.lastForcedChunkZ) {
            updateReconChunkLoading(craft, cx, cz);
            craft.lastForcedChunkX = cx;
            craft.lastForcedChunkZ = cz;
        }
        if (owner instanceof ServerPlayer sp) {
            sendPendingChunks(craft, sp);
        }

        float[] input = ReconManager.consumeInput(owner.getUUID());
        boolean hasInput = input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0);
        double speed = craft.panelSpeed * 0.5;
        Vec3 target;
        if (hasInput) {
            target = new Vec3(input[0] * speed, input[1] * speed, input[2] * speed);
        } else {
            double minSpeed = speed * 0.15;
            float yawRad = (float) Math.toRadians(craft.getYRot());
            target = new Vec3(-Math.sin(yawRad) * minSpeed, 0, Math.cos(yawRad) * minSpeed);
        }
        Vec3 current = craft.getDeltaMovement();
        double factor = hasInput ? 0.12 : 0.25;
        craft.setDeltaMovement(current.lerp(target, factor));

        if (!craft.level().isClientSide && craft.tickCount % 20 == 0) {
            if (craft.cachedMapSlots == null || craft.tickCount - craft.mapSlotsRefreshTick >= 100) {
                refreshCachedMapSlots(craft, owner);
                craft.mapSlotsRefreshTick = craft.tickCount;
            }
            if (craft.cachedMapSlots != null) {
                for (int slot : craft.cachedMapSlots) {
                    ItemStack stack = owner.getInventory().getItem(slot);
                    if (stack.getItem() instanceof MapItem mapItem) {
                        MapItemSavedData mapData = MapItem.getSavedData(stack, craft.level());
                        if (mapData != null && !mapData.locked) {
                            mapData.tickCarriedBy(owner, stack);
                            mapItem.update(craft.level(), owner, mapData);
                        }
                    }
                }
            }
        }
    }

    private static void refreshCachedMapSlots(AircraftEntity craft, Player owner) {
        int size = owner.getInventory().getContainerSize();
        int[] buf = new int[size];
        int n = 0;
        for (int i = 0; i < size; i++) {
            if (owner.getInventory().getItem(i).getItem() instanceof MapItem) {
                buf[n++] = i;
            }
        }
        if (n == 0) { craft.cachedMapSlots = null; return; }
        int[] out = new int[n];
        System.arraycopy(buf, 0, out, 0, n);
        craft.cachedMapSlots = out;
    }

    // ===== Chunk forcing =====

    private static void updateReconChunkLoading(AircraftEntity craft, int cx, int cz) {
        if (!(craft.level() instanceof ServerLevel sl)) return;
        int radius = Math.min(sl.getServer().getPlayerList().getViewDistance(), 5);

        java.util.Set<Long> desired = new java.util.HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                desired.add(ChunkPos.asLong(cx + dx, cz + dz));
            }
        }

        java.util.Iterator<Long> it = craft.reconForcedChunks.iterator();
        while (it.hasNext()) {
            long key = it.next();
            if (!desired.contains(key)) {
                sl.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
                it.remove();
            }
        }

        for (long key : desired) {
            if (!craft.reconForcedChunks.contains(key)) {
                int x = ChunkPos.getX(key);
                int z = ChunkPos.getZ(key);
                sl.setChunkForced(x, z, true);
                craft.reconForcedChunks.add(key);
                craft.reconPendingSend.add(key);
            }
        }
    }

    private static void sendPendingChunks(AircraftEntity craft, ServerPlayer player) {
        if (craft.reconPendingSend.isEmpty()) return;
        if (!(craft.level() instanceof ServerLevel sl)) return;

        java.util.Iterator<Long> it = craft.reconPendingSend.iterator();
        int sent = 0;
        while (it.hasNext() && sent < 16) {
            long key = it.next();
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            net.minecraft.world.level.chunk.LevelChunk chunk = sl.getChunkSource().getChunkNow(x, z);
            if (chunk != null) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                        chunk, sl.getLightEngine(), null, null));
                it.remove();
                sent++;
            }
        }
    }

    /** @see AircraftEntity#releaseAllForcedChunks() */
    public static void releaseAllForcedChunks(AircraftEntity craft) {
        if (!(craft.level() instanceof ServerLevel sl)) {
            craft.reconForcedChunks.clear();
            craft.reconPendingSend.clear();
            craft.lastForcedChunkX = Integer.MIN_VALUE;
            craft.lastForcedChunkZ = Integer.MIN_VALUE;
            return;
        }
        for (long key : craft.reconForcedChunks) {
            sl.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        craft.reconForcedChunks.clear();
        craft.reconPendingSend.clear();
        craft.lastForcedChunkX = Integer.MIN_VALUE;
        craft.lastForcedChunkZ = Integer.MIN_VALUE;
    }
}
