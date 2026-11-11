package com.piranport.dungeon.instance;

import com.piranport.PiranPort;
import com.piranport.dungeon.event.DungeonEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** 让远端出生的普通战斗敌舰在节点活跃时驶向中心。 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class DungeonFleetApproach extends SavedData {
    private static final String TARGET = "piranport_dungeon_approach";
    private static final String DATA_NAME = "piranport_dungeon_approach";
    private static final TicketType<UUID> TICKET = TicketType.create("piranport_dungeon_approach", Comparator.<UUID>naturalOrder());
    private static final int TICKET_RADIUS = 2;
    private static final double SPEED = 0.14;
    private static final double ARRIVAL_RADIUS = 8.0;
    private final Map<UUID, Journey> journeys = new HashMap<>();
    private final Map<UUID, ChunkPos> tickets = new HashMap<>();
    private final Map<UUID, ChunkPos> pendingTickets = new HashMap<>();

    private record Journey(UUID instance, String node, ChunkPos chunk) {}

    private static DungeonFleetApproach get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                DungeonFleetApproach::new, DungeonFleetApproach::load, null), DATA_NAME);
    }

    /** 必须在实体加入世界前写入，使首次生成与读档走同一个挂载路径。 */
    public static void prepare(Mob mob, BlockPos center) {
        mob.getPersistentData().putLong(TARGET, center.asLong());
    }

    /** 目标节点没有玩家时冻结，不以其他节点的玩家代替。 */
    static boolean shouldRun(DungeonInstance instance, String node, boolean playerInNode) {
        return instance != null && instance.getState() == DungeonInstance.State.ACTIVE
                && instance.hasEnteredNode(node) && !instance.getClearedNodes().contains(node) && playerInNode;
    }

    static Vec3 approachVelocity(Vec3 position, BlockPos center) {
        Vec3 delta = new Vec3(center.getX() + 0.5 - position.x, 0, center.getZ() + 0.5 - position.z);
        if (delta.horizontalDistanceSqr() <= ARRIVAL_RADIUS * ARRIVAL_RADIUS) return Vec3.ZERO;
        return delta.normalize().scale(SPEED);
    }

    public static void prepareScripted(ServerLevel level, UUID instanceId, String nodeId, Entity entity) {
        if (!(entity instanceof Mob mob)) return;
        DungeonInstance instance = DungeonInstanceManager.get(level).getInstance(instanceId);
        if (instance == null) return;
        prepare(mob, instance.getNodeSpawnPos(nodeId));
    }

    public static boolean spawnScripted(ServerLevel level, UUID instanceId, String nodeId, Entity entity) {
        prepareScripted(level, instanceId, nodeId, entity);
        if (!level.addFreshEntity(entity)) return false;
        DungeonInstance instance = DungeonInstanceManager.get(level).getInstance(instanceId);
        if (instance != null && entity instanceof Mob) track(level, instance, nodeId, entity);
        return true;
    }

    public static void track(ServerLevel level, DungeonInstance instance, String node, Entity entity) {
        DungeonFleetApproach data = get(level.getServer());
        ChunkPos pos = entity.chunkPosition();
        data.journeys.put(entity.getUUID(), new Journey(instance.getInstanceId(), node, pos));
        data.setDirty();
        data.moveTicket(level, entity.getUUID(), pos);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(DungeonEventHandler.DUNGEON_DIMENSION)
                || !(event.getEntity() instanceof Mob mob) || !mob.getPersistentData().contains(TARGET)) return;
        mob.goalSelector.addGoal(0, new ApproachGoal(mob));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity().level() instanceof ServerLevel level)
                || !level.dimension().equals(DungeonEventHandler.DUNGEON_DIMENSION)) return;
        get(level.getServer()).remove(level, event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        ServerLevel level = DungeonEventHandler.getDungeonLevel(event.getServer());
        if (level != null) get(event.getServer()).tick(level);
    }

    private void tick(ServerLevel level) {
        DungeonInstanceManager manager = DungeonInstanceManager.get(level);
        Iterator<Map.Entry<UUID, Journey>> it = journeys.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Journey> entry = it.next();
            UUID id = entry.getKey();
            Journey journey = entry.getValue();
            DungeonInstance instance = manager.getInstance(journey.instance());
            if (instance == null || instance.getClearedNodes().contains(journey.node())
                    || !instance.hasEnteredNode(journey.node())) {
                releaseTicket(level, id);
                it.remove();
                setDirty();
                continue;
            }
            BlockPos center = instance.getNodeSpawnPos(journey.node());
            boolean playerInNode = instance.getState() == DungeonInstance.State.ACTIVE
                    && manager.getPresentPlayers(instance, level.getServer()).stream()
                    .anyMatch(player -> DungeonScaling.isInsideNodeTile(player.getX(), player.getZ(),
                            center.getX(), center.getZ(), com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE));
            if (!shouldRun(instance, journey.node(), playerInNode)) {
                releaseTicket(level, id);
                continue;
            }
            if (!tickets.containsKey(id) && !pendingTickets.containsKey(id)) requestTicket(level, id, journey.chunk());
            promoteTicket(level, id);
            Entity entity = level.getEntity(id);
            if (entity == null) continue;
            if (!entity.isAlive() || !entity.getPersistentData().contains(TARGET)) {
                releaseTicket(level, id);
                it.remove();
                setDirty();
                continue;
            }
            ChunkPos current = entity.chunkPosition();
            if (!current.equals(journey.chunk())) {
                entry.setValue(new Journey(journey.instance(), journey.node(), current));
                setDirty();
            }
            if (!current.equals(tickets.get(id)) && !current.equals(pendingTickets.get(id))) {
                if (!pendingTickets.containsKey(id)) requestTicket(level, id, current);
            }
        }
    }

    private void moveTicket(ServerLevel level, UUID id, ChunkPos next) {
        if (!next.equals(tickets.get(id))) requestTicket(level, id, next);
    }

    private void requestTicket(ServerLevel level, UUID id, ChunkPos next) {
        ChunkPos previousPending = pendingTickets.put(id, next);
        if (previousPending != null) removeTicket(level, id, previousPending);
        level.getChunkSource().addRegionTicket(TICKET, next, TICKET_RADIUS, id, true);
    }

    private void promoteTicket(ServerLevel level, UUID id) {
        ChunkPos pending = pendingTickets.get(id);
        if (pending == null || !level.areEntitiesLoaded(pending.toLong())
                || !level.isPositionEntityTicking(pending.getWorldPosition())) return;
        pendingTickets.remove(id);
        ChunkPos old = tickets.put(id, pending);
        if (old != null) removeTicket(level, id, old);
    }

    private void releaseTicket(ServerLevel level, UUID id) {
        ChunkPos old = tickets.remove(id);
        if (old != null) removeTicket(level, id, old);
        ChunkPos pending = pendingTickets.remove(id);
        if (pending != null) removeTicket(level, id, pending);
    }

    private void removeTicket(ServerLevel level, UUID id, ChunkPos chunk) {
        level.getChunkSource().removeRegionTicket(TICKET, chunk, TICKET_RADIUS, id, true);
    }

    private void remove(ServerLevel level, UUID id) {
        if (journeys.remove(id) != null) setDirty();
        releaseTicket(level, id);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        journeys.forEach((id, journey) -> {
            CompoundTag item = new CompoundTag();
            item.putUUID("Entity", id);
            item.putUUID("Instance", journey.instance());
            item.putString("Node", journey.node());
            item.putLong("Chunk", journey.chunk().toLong());
            list.add(item);
        });
        tag.put("Journeys", list);
        return tag;
    }

    static DungeonFleetApproach load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonFleetApproach data = new DungeonFleetApproach();
        ListTag list = tag.getList("Journeys", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            data.journeys.put(item.getUUID("Entity"), new Journey(item.getUUID("Instance"),
                    item.getString("Node"), new ChunkPos(item.getLong("Chunk"))));
        }
        return data;
    }

    private static final class ApproachGoal extends Goal {
        private final Mob mob;

        private ApproachGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return mob.isAlive() && mob.getTarget() == null && mob.getPersistentData().contains(TARGET);
        }

        @Override
        public boolean canContinueToUse() { return canUse(); }

        @Override
        public boolean requiresUpdateEveryTick() { return true; }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (mob.getTarget() != null) mob.getPersistentData().remove(TARGET);
        }

        @Override
        public void tick() {
            BlockPos center = BlockPos.of(mob.getPersistentData().getLong(TARGET));
            Vec3 direction = approachVelocity(mob.position(), center);
            if (direction.equals(Vec3.ZERO)) {
                mob.getPersistentData().remove(TARGET);
                Vec3 velocity = mob.getDeltaMovement();
                mob.setDeltaMovement(0, velocity.y, 0);
                return;
            }
            Vec3 velocity = mob.getDeltaMovement();
            mob.setDeltaMovement(direction.x, velocity.y, direction.z);
        }
    }
}
