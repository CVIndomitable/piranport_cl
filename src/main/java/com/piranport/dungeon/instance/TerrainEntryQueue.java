package com.piranport.dungeon.instance;

import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.NodeData;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.event.DungeonEntryService;
import com.piranport.dungeon.block.DungeonLecternBlockEntity;
import com.piranport.dungeon.key.DungeonKeyItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** 入口等待队列：地形完成前不登记节点、不传送；请求随讲台距离和玩家在线状态失效。 */
public final class TerrainEntryQueue extends SavedData {
    private static final String DATA_NAME = "piranport_terrain_entry_queue";
    private final List<Request> requests = new ArrayList<>();

    private record Request(UUID player, BlockPos lectern, String dimension, UUID instance,
                           String node, boolean checkpoint) {}

    public static TerrainEntryQueue get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                TerrainEntryQueue::new, TerrainEntryQueue::load, null), DATA_NAME);
    }

    public void enqueue(ServerPlayer player, BlockPos lectern, String node, boolean checkpoint) {
        requests.removeIf(r -> r.player().equals(player.getUUID()));
        if (!(player.level().getBlockEntity(lectern) instanceof DungeonLecternBlockEntity block)) return;
        UUID instance = DungeonKeyItem.getInstanceId(block.getKeyStack());
        if (instance == null) return;
        requests.add(new Request(player.getUUID(), lectern.immutable(),
                player.level().dimension().location().toString(), instance, node, checkpoint));
        setDirty();
    }

    public void cancel(UUID player) {
        if (requests.removeIf(r -> r.player().equals(player))) setDirty();
    }

    /** 在 ServerTickEvent.Post 调用；所有请求共享 TerrainWorkBudget。 */
    public void tick(MinecraftServer server) {
        ServerLevel dungeon = server.getLevel(com.piranport.dungeon.event.DungeonEventHandler.DUNGEON_DIMENSION);
        if (dungeon == null) return;
        boolean changed = false;
        Iterator<Request> it = requests.iterator();
        List<Request> ready = new ArrayList<>();
        while (it.hasNext()) {
            Request request = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(request.player());
            if (player == null || !player.isAlive() || player.isSpectator()
                    || !player.level().dimension().location().toString().equals(request.dimension())
                    || player.distanceToSqr(request.lectern().getX() + .5,
                    request.lectern().getY() + .5, request.lectern().getZ() + .5) > 64.0) {
                it.remove(); changed = true; continue;
            }
            DungeonInstanceManager manager = DungeonInstanceManager.get(server.overworld());
            DungeonInstance instance = null;
            if (player.level().getBlockEntity(request.lectern()) instanceof DungeonLecternBlockEntity lectern
                    && lectern.hasKey()) {
                UUID id = DungeonKeyItem.getInstanceId(lectern.getKeyStack());
                if (request.instance().equals(id)) instance = manager.getInstance(id);
            }
            if (instance == null) { it.remove(); changed = true; continue; }
            StageData stage = DungeonRegistry.INSTANCE.getStage(instance.getStageId());
            NodeData node = stage == null ? null : stage.nodes().get(request.node());
            if (node == null) { it.remove(); changed = true; continue; }
            TerrainGenerationPipeline.tick(dungeon, instance, node);
            if (TerrainGenerationPipeline.isReady(dungeon, instance, node)) {
                it.remove(); ready.add(request); changed = true;
            }
        }
        if (changed) setDirty();
        for (Request request : ready) {
            ServerPlayer player = server.getPlayerList().getPlayer(request.player());
            if (player != null) DungeonEntryService.enter(player, request.lectern(), request.checkpoint(), request.node());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Request request : requests) {
            CompoundTag item = new CompoundTag();
            item.putUUID("Player", request.player());
            item.putLong("Lectern", request.lectern().asLong());
            item.putString("Node", request.node());
            item.putString("Dimension", request.dimension());
            item.putUUID("Instance", request.instance());
            item.putBoolean("Checkpoint", request.checkpoint());
            list.add(item);
        }
        tag.put("Requests", list);
        return tag;
    }

    private static TerrainEntryQueue load(CompoundTag tag, HolderLookup.Provider registries) {
        TerrainEntryQueue queue = new TerrainEntryQueue();
        ListTag list = tag.getList("Requests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            if (item.hasUUID("Player") && item.hasUUID("Instance")) {
                queue.requests.add(new Request(item.getUUID("Player"), BlockPos.of(item.getLong("Lectern")),
                        item.getString("Dimension"), item.getUUID("Instance"),
                        item.getString("Node"), item.getBoolean("Checkpoint")));
            }
        }
        return queue;
    }
}
