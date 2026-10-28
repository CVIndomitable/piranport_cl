package com.piranport.entity;

import com.piranport.aviation.ReconManager;
import com.piranport.combat.CombatTargeting;
import com.piranport.config.ModEquipmentConfig;
import com.piranport.network.AswSonarSyncPayload;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 反潜声呐 + 侦察机子系统。
 *
 * <p>反潜攻击与目标解析已迁到 {@code aviation.combat.strategy.DepthChargeAttackStrategy}；
 * 本类只保留声呐扫描、侦察机操控与区块强加载。
 */
public class AircraftAswRecon {
    private AircraftAswRecon() {}

    /**
     * 声呐检测判据 —— 只判「是不是水下类目标」，不做敌我过滤。
     *
     * <p>声呐链路共用本方法；自动索敌/自动进入攻击态必须改用
     * {@link #isHostileAswTarget(Player, Entity)}。
     */
    public static boolean isAswTarget(Entity entity) {
        if (entity instanceof com.piranport.npc.deepocean.DeepOceanSubmarineEntity) return true;
        if (entity.getType().is(net.minecraft.tags.EntityTypeTags.AQUATIC)) return true;
        if (entity instanceof net.minecraft.world.entity.monster.Guardian) return true;
        return entity instanceof net.minecraft.world.entity.monster.Monster && entity.isUnderWater();
    }

    /**
     * 自动索敌/自动进入攻击态专用判据 —— 确属反潜对象 AND 对 owner 敌对。
     */
    public static boolean isHostileAswTarget(Player owner, Entity entity) {
        return isAswTarget(entity) && CombatTargeting.isHostileTarget(owner, entity);
    }

    /** ASW 声呐：扫描半径内的水下目标并同步给客户端，声呐标记不做敌我过滤。 */
    public static void tickAswSonar(AircraftEntity craft, Player owner) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return;
        if (!(owner instanceof ServerPlayer serverPlayer)) return;

        AABB sonarBox = craft.getBoundingBox().inflate(ModEquipmentConfig.ASW_SONAR_RANGE.get());
        List<Integer> detectedIds = new ArrayList<>();
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, sonarBox,
                living -> living.isAlive() && living != owner
                        && (living.isInWater() || isAswTarget(living)))) {
            detectedIds.add(entity.getId());
            if (detectedIds.size() >= ModEquipmentConfig.ASW_SONAR_MAX_TARGETS.get()) break;
        }
        if (!detectedIds.isEmpty()) {
            PacketDistributor.sendToPlayer(serverPlayer,
                    new AswSonarSyncPayload(craft.getId(), detectedIds));
        }
    }

    /** 侦察机主动操控：读取客户端输入、更新地图、差量强加载区块。 */
    public static void tickReconActive(AircraftEntity craft, Player owner) {
        if (!ReconManager.isInRecon(owner.getUUID())) {
            com.piranport.PiranPort.LOGGER.warn(
                    "Aircraft RECON_DESYNC | entityId={} ownerNotInRecon, forcing state sync",
                    craft.getId());
            ReconManager.startRecon(owner.getUUID(), craft.getUUID());
        }

        int chunkX = craft.getBlockX() >> 4;
        int chunkZ = craft.getBlockZ() >> 4;
        // 集合为空也要重建：读档时旧票据会被解除并清空，只比较坐标会导致重启后不再强加载。
        if (chunkX != craft.lastForcedChunkX || chunkZ != craft.lastForcedChunkZ
                || craft.reconForcedChunks.isEmpty()) {
            updateForcedChunks(craft, chunkX, chunkZ, reconScanRadius(craft));
            craft.lastForcedChunkX = chunkX;
            craft.lastForcedChunkZ = chunkZ;
        }
        if (owner instanceof ServerPlayer serverPlayer) {
            sendPendingChunks(craft, serverPlayer);
            // owner 已飞出原版实体追踪范围：手工把飞机实体重新同步过去，否则客户端丢实体、镜头冻结。
            if (!craft.reconOwnerTracksEntity) {
                syncDetachedReconEntity(craft, serverPlayer);
            }
        }

        float[] input = ReconManager.consumeInput(owner.getUUID());
        boolean hasInput = input != null && (input[0] != 0 || input[1] != 0 || input[2] != 0);
        double speed = craft.panelSpeed * 0.5 * craft.phaseSpeedCoefficient();
        Vec3Target target;
        if (hasInput) {
            target = new Vec3Target(input[0] * speed, input[1] * speed, input[2] * speed);
        } else {
            double minimumSpeed = speed * 0.15;
            float yawRadians = (float) Math.toRadians(craft.getYRot());
            target = new Vec3Target(-Math.sin(yawRadians) * minimumSpeed, 0,
                    Math.cos(yawRadians) * minimumSpeed);
        }
        net.minecraft.world.phys.Vec3 current = craft.getDeltaMovement();
        double factor = hasInput ? 0.12 : 0.25;
        craft.setDeltaMovement(current.lerp(target.toVec3(), factor));

        if (!craft.level().isClientSide && craft.tickCount % 20 == 0) {
            if (craft.cachedMapSlots == null
                    || craft.tickCount - craft.mapSlotsRefreshTick
                    >= ModEquipmentConfig.RECON_MAP_REFRESH_TICKS.get()) {
                refreshCachedMapSlots(craft, owner);
                craft.mapSlotsRefreshTick = craft.tickCount;
            }
            if (craft.cachedMapSlots != null) {
                for (int slot : craft.cachedMapSlots) {
                    net.minecraft.world.item.ItemStack stack = owner.getInventory().getItem(slot);
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

    /** 轻量三元组，避免为了构造 Vec3 反复分配命名变量。 */
    private record Vec3Target(double x, double y, double z) {
        net.minecraft.world.phys.Vec3 toVec3() {
            return new net.minecraft.world.phys.Vec3(x, y, z);
        }
    }

    private static void refreshCachedMapSlots(AircraftEntity craft, Player owner) {
        int size = owner.getInventory().getContainerSize();
        int[] buffer = new int[size];
        int count = 0;
        for (int i = 0; i < size; i++) {
            if (owner.getInventory().getItem(i).getItem() instanceof MapItem) {
                buffer[count++] = i;
            }
        }
        if (count == 0) {
            craft.cachedMapSlots = null;
            return;
        }
        int[] slots = new int[count];
        System.arraycopy(buffer, 0, slots, 0, count);
        craft.cachedMapSlots = slots;
    }

    /** 返航票据的前瞻距离（格）：提前把即将进入的区块挂上，避免跨区块时掉出 tick 范围。 */
    private static final double RECON_RETURN_LOOKAHEAD_BLOCKS = 32.0;

    /**
     * 侦察扫描环半径 = 服务器视距，设置多少就加载多少。
     *
     * <p>侦察镜头需要看到和玩家自己站在该处时一样远的地形，因此直接跟随游戏设置，
     * 不再叠加固定上限。视距越大的服务器，单架侦察机的加载成本越高。
     */
    private static int reconScanRadius(AircraftEntity craft) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return 3;
        return serverLevel.getServer().getPlayerList().getViewDistance();
    }

    private static void updateForcedChunks(AircraftEntity craft, int chunkX, int chunkZ, int radius) {
        java.util.Set<Long> desired = new java.util.HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                desired.add(ChunkPos.asLong(chunkX + dx, chunkZ + dz));
            }
        }
        applyForcedChunks(craft, desired, true);
    }

    /** 把强制加载集合收敛到 desired；queueForClient 只对侦察态为 true（返航不再推区块）。 */
    private static void applyForcedChunks(AircraftEntity craft, java.util.Set<Long> desired,
                                          boolean queueForClient) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) return;

        java.util.Iterator<Long> iterator = craft.reconForcedChunks.iterator();
        while (iterator.hasNext()) {
            long key = iterator.next();
            if (!desired.contains(key)) {
                serverLevel.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
                iterator.remove();
            }
        }

        for (long key : desired) {
            if (!craft.reconForcedChunks.contains(key)) {
                int x = ChunkPos.getX(key);
                int z = ChunkPos.getZ(key);
                serverLevel.setChunkForced(x, z, true);
                craft.reconForcedChunks.add(key);
                if (queueForClient) {
                    craft.reconPendingSend.add(key);
                }
            }
        }
    }

    /**
     * 返航票据：当前区块 + 前进方向 32 格内的区块。
     *
     * <p>只保留覆盖当前与即将进入区块的最小矩形，最多 2×2=4 个区块；方向每 tick 更新，
     * 斜向跨区块时两个方向都会提前挂上，不会掉出实体 tick 范围。
     */
    private static java.util.Set<Long> returnForcedChunkSet(AircraftEntity craft) {
        int chunkX = craft.getBlockX() >> 4;
        int chunkZ = craft.getBlockZ() >> 4;
        int aheadX = chunkX;
        int aheadZ = chunkZ;
        net.minecraft.world.phys.Vec3 velocity = craft.getDeltaMovement();
        if (velocity.lengthSqr() > 1.0e-6) {
            net.minecraft.world.phys.Vec3 ahead = craft.position()
                    .add(velocity.normalize().scale(RECON_RETURN_LOOKAHEAD_BLOCKS));
            net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(ahead);
            aheadX = pos.getX() >> 4;
            aheadZ = pos.getZ() >> 4;
        }

        java.util.Set<Long> desired = new java.util.HashSet<>(4);
        for (int x = Math.min(chunkX, aheadX); x <= Math.max(chunkX, aheadX); x++) {
            for (int z = Math.min(chunkZ, aheadZ); z <= Math.max(chunkZ, aheadZ); z++) {
                desired.add(ChunkPos.asLong(x, z));
            }
        }
        return desired;
    }

    /**
     * 退出侦察、转入返航：把侦察扫描环收缩成返航票据，而不是全部释放。
     *
     * <p>侦察机允许飞到 200 格，可能已经超出玩家的实体 tick 范围；若此时释放全部强加载，
     * 区块卸载后实体不再 tick，返航会永久冻结。3×3 票据保证返航途中始终能推进。
     */
    public static void beginReconReturnLoading(AircraftEntity craft) {
        applyForcedChunks(craft, returnForcedChunkSet(craft), false);
        craft.lastForcedChunkX = craft.getBlockX() >> 4;
        craft.lastForcedChunkZ = craft.getBlockZ() >> 4;
    }

    /** 返航每 tick：票据跟随前进方向移动；读档后集合为空时也会重建。 */
    public static void tickReturnLoading(AircraftEntity craft) {
        // 只有侦察机需要返航票据；普通飞机受距离上限约束，始终在 tick 范围内。
        if (!craft.isReconAircraft()) return;
        java.util.Set<Long> desired = returnForcedChunkSet(craft);
        if (!desired.equals(craft.reconForcedChunks)) {
            applyForcedChunks(craft, desired, false);
        }
        craft.lastForcedChunkX = craft.getBlockX() >> 4;
        craft.lastForcedChunkZ = craft.getBlockZ() >> 4;
    }

    private static void sendPendingChunks(AircraftEntity craft, ServerPlayer player) {
        if (craft.reconPendingSend.isEmpty()) return;
        if (!(craft.level() instanceof ServerLevel serverLevel)) return;

        java.util.Iterator<Long> iterator = craft.reconPendingSend.iterator();
        int sent = 0;
        while (iterator.hasNext() && sent < ModEquipmentConfig.RECON_CHUNK_SEND_RATE.get()) {
            long key = iterator.next();
            int x = ChunkPos.getX(key);
            int z = ChunkPos.getZ(key);
            net.minecraft.world.level.chunk.LevelChunk chunk = serverLevel.getChunkSource()
                    .getChunkNow(x, z);
            if (chunk != null) {
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                        chunk, serverLevel.getLightEngine(), null, null));
                iterator.remove();
                sent++;
            }
        }
    }

    /**
     * 侦察机飞出原版实体追踪范围后，手工维持 owner 客户端上的飞机实体。
     *
     * <p>WHY 需要：{@code ChunkMap.TrackedEntity.updatePlayer} 要求实体所在区块在玩家的
     * 区块追踪范围内、且距离 ≤ 视距 × 16，否则向玩家发移除包。侦察机航程（兜底 200 格）
     * 大于该范围，原版会在半路把实体从 owner 客户端摘掉：摄像机冻结在最后位置、
     * {@code ReconInputHandler} 又找不到实体，表现就是「撞空气墙」。
     *
     * <p>生成包只在第一次补发（与原版配对包等价：生成 + 同步数据）；位置用绝对传送包
     * 每 tick 补一次，因为远处时 {@code ServerEntity.sendChanges} 不会发给 owner。
     */
    private static void syncDetachedReconEntity(AircraftEntity craft, ServerPlayer player) {
        if (!craft.reconEntityManuallySynced) {
            player.connection.send(new ClientboundAddEntityPacket(
                    craft.getId(), craft.getUUID(),
                    craft.getX(), craft.getY(), craft.getZ(),
                    craft.getXRot(), craft.getYRot(), craft.getType(), 0,
                    craft.getDeltaMovement(), craft.getYHeadRot()));
            java.util.List<SynchedEntityData.DataValue<?>> data =
                    craft.getEntityData().getNonDefaultValues();
            if (data != null) {
                player.connection.send(new ClientboundSetEntityDataPacket(craft.getId(), data));
            }
            craft.reconEntityManuallySynced = true;
        }
        player.connection.send(new ClientboundTeleportEntityPacket(craft));
    }

    /**
     * 移除手工同步出来的客户端实体。
     *
     * <p>手工生成包绕过了原版 {@code seenBy} 集合，{@code broadcastRemoved} 不会通知 owner，
     * 因此退出侦察/回收时必须自己补一个移除包；否则客户端会残留一架不再存在的飞机。
     */
    public static void removeManualReconEntity(AircraftEntity craft) {
        if (!craft.reconEntityManuallySynced) return;
        craft.reconEntityManuallySynced = false;
        if (craft.level() instanceof ServerLevel serverLevel && craft.getOwnerUUID() != null) {
            ServerPlayer owner = serverLevel.getServer().getPlayerList()
                    .getPlayer(craft.getOwnerUUID());
            if (owner != null) {
                owner.connection.send(new ClientboundRemoveEntitiesPacket(craft.getId()));
            }
        }
    }

    public static void releaseAllForcedChunks(AircraftEntity craft) {
        if (!(craft.level() instanceof ServerLevel serverLevel)) {
            craft.reconForcedChunks.clear();
            craft.reconPendingSend.clear();
            craft.lastForcedChunkX = Integer.MIN_VALUE;
            craft.lastForcedChunkZ = Integer.MIN_VALUE;
            return;
        }
        for (long key : craft.reconForcedChunks) {
            serverLevel.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        craft.reconForcedChunks.clear();
        craft.reconPendingSend.clear();
        craft.lastForcedChunkX = Integer.MIN_VALUE;
        craft.lastForcedChunkZ = Integer.MIN_VALUE;
    }
}
