package com.piranport.dungeon.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime state of a single dungeon instance.
 *
 * <p>整合版 §3.1 联机大厅与队长机制已作废（副本/10）：删除 flagshipUuid 字段，
 * 不再有"队长/权限"概念——副本内所有玩家平等。</p>
 */
public class DungeonInstance {
    public enum State {
        CREATING, ACTIVE, SUSPENDED, COMPLETED, CLEANUP
    }

    private final UUID instanceId;
    private final String stageId;
    private final int instanceIndex; // determines region offset
    private State state;
    private String currentNode;
    private final Set<String> clearedNodes = new HashSet<>();
    private final Set<UUID> playerUuids = new HashSet<>(); // all players who participated
    /** 整合版 §3.2：按玩家个人计算的"最新记录点 id"。每个玩家仅保留最新 checkpoint id。 */
    private final Map<UUID, String> playerCheckpoints = new HashMap<>();
    private BlockPos lecternPos; // the lectern block that opened this instance
    private String lecternDimension; // dimension key of the lectern
    private long startTimeMillis;
    private long endTimeMillis;

    public DungeonInstance(UUID instanceId, String stageId, int instanceIndex) {
        this.instanceId = instanceId;
        this.stageId = stageId;
        this.instanceIndex = instanceIndex;
        this.state = State.CREATING;
    }

    // ===== Getters =====

    public UUID getInstanceId() { return instanceId; }
    public String getStageId() { return stageId; }
    public int getInstanceIndex() { return instanceIndex; }
    public State getState() { return state; }
    public String getCurrentNode() { return currentNode; }
    public Set<String> getClearedNodes() { return java.util.Collections.unmodifiableSet(clearedNodes); }
    public Set<UUID> getPlayerUuids() { return java.util.Collections.unmodifiableSet(playerUuids); }
    /** 整合版 §3.2：返回玩家个人最新记录点 id（null 表示尚未踩过任何 checkpoint）。 */
    public String getLatestCheckpointFor(UUID playerUuid) { return playerCheckpoints.get(playerUuid); }
    /** 整合版 §3.2：标记玩家踩到了某 checkpoint。 */
    public void markCheckpointReached(UUID playerUuid, String checkpointId) {
        if (playerUuid == null || checkpointId == null) return;
        playerCheckpoints.put(playerUuid, checkpointId);
    }
    public BlockPos getLecternPos() { return lecternPos; }
    public String getLecternDimension() { return lecternDimension; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }

    /**
     * Returns the X offset of this instance's region in the dungeon dimension.
     * Instances are laid out on a 2D grid (副本/01 §2.1) instead of a single
     * X-axis line so the shared dimension's footprint stays compact.
     */
    public int getRegionOriginX() {
        int gridX = Math.floorMod(instanceIndex, com.piranport.dungeon.DungeonConstants.GRID_SIZE);
        return gridX * com.piranport.dungeon.DungeonConstants.REGION_SIZE;
    }

    /**
     * Returns the Z offset of this instance's region in the dungeon dimension.
     * Previously hardcoded to 0; now varies along Z based on the 2D instance grid
     * (副本/01 §2.1).
     */
    public int getRegionOriginZ() {
        int gridZ = Math.floorDiv(instanceIndex, com.piranport.dungeon.DungeonConstants.GRID_SIZE);
        return gridZ * com.piranport.dungeon.DungeonConstants.REGION_SIZE;
    }

    /** Inclusive min corner of this instance's playable 512x512 area (副本/01 §2.1: 居中). */
    public int getUsableMinX() {
        return getRegionOriginX() + com.piranport.dungeon.DungeonConstants.MAP_BORDER_PADDING;
    }

    /** Inclusive max X corner of the playable area. */
    public int getUsableMaxX() {
        return getUsableMinX() + com.piranport.dungeon.DungeonConstants.MAP_USABLE_SIZE - 1;
    }

    /** Inclusive min Z corner of the playable area. */
    public int getUsableMinZ() {
        return getRegionOriginZ() + com.piranport.dungeon.DungeonConstants.MAP_BORDER_PADDING;
    }

    /** Inclusive max Z corner of the playable area. */
    public int getUsableMaxZ() {
        return getUsableMinZ() + com.piranport.dungeon.DungeonConstants.MAP_USABLE_SIZE - 1;
    }

    /**
     * Returns true if the given block position is inside this instance's playable area.
     * Used by the boundary-protection tick to push players back when they cross into the
     * padding buffer surrounding the 512x512 usable region (副本/01 §2.1).
     */
    public boolean isInsideUsableArea(BlockPos pos) {
        return pos.getX() >= getUsableMinX() && pos.getX() <= getUsableMaxX()
                && pos.getZ() >= getUsableMinZ() && pos.getZ() <= getUsableMaxZ();
    }

    /**
     * Clamp the given position to the nearest point inside the playable area. If the
     * position is already inside it is returned unchanged. Used by boundary protection.
     */
    public BlockPos clampToUsableArea(BlockPos pos) {
        int x = Math.max(getUsableMinX(), Math.min(getUsableMaxX(), pos.getX()));
        int z = Math.max(getUsableMinZ(), Math.min(getUsableMaxZ(), pos.getZ()));
        return new BlockPos(x, pos.getY(), z);
    }

    /**
     * Returns the spawn position for players entering this instance's current node.
     * Uses the stage's deterministic lexicographic node index when available so
     * nodeIds like "boss1"/"boss2" don't collide. Falls back to first-letter
     * mapping for legacy single-letter ids when the stage hasn't loaded yet.
     *
     * <p>Nodes are laid out in a single row along the X axis within the centered
     * 512x512 usable area; each node occupies a 128x128 battlefield tile.</p>
     */
    public BlockPos getNodeSpawnPos(String nodeId) {
        int nodeIndex = 0;
        com.piranport.dungeon.data.StageData stage =
                com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(stageId);
        if (stage != null && nodeId != null) {
            nodeIndex = stage.nodeIndexOf(nodeId);
        } else if (nodeId != null && !nodeId.isEmpty()) {
            char ch = nodeId.charAt(0);
            if (ch >= 'A' && ch <= 'Z') {
                nodeIndex = ch - 'A';
            } else if (ch >= 'a' && ch <= 'z') {
                nodeIndex = ch - 'a';
            } else {
                nodeIndex = Math.floorMod(nodeId.hashCode(),
                        com.piranport.dungeon.DungeonConstants.MAX_NODES_PER_STAGE);
            }
        }
        // 将节点放置在 512×512 居中区域的首行（副本/01 §2.1：实际地图 512×512）
        int nodeX = getUsableMinX() + nodeIndex * com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE
                + com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE / 2;
        int nodeZ = getUsableMinZ() + com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE / 2;
        return new BlockPos(nodeX, com.piranport.dungeon.DungeonConstants.SPAWN_Y, nodeZ);
    }

    // ===== Setters =====

    public void setState(State state) { this.state = state; }
    public void setCurrentNode(String node) { this.currentNode = node; }
    public void addClearedNode(String node) { clearedNodes.add(node); }
    public void addPlayer(UUID uuid) { playerUuids.add(uuid); }
    public void setLecternPos(BlockPos pos) { this.lecternPos = pos; }
    public void setLecternDimension(String dim) { this.lecternDimension = dim; }
    public void setStartTimeMillis(long t) { this.startTimeMillis = t; }
    public void setEndTimeMillis(long t) { this.endTimeMillis = t; }

    // ===== NBT Serialization =====

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("InstanceId", instanceId);
        tag.putString("StageId", stageId);
        tag.putInt("InstanceIndex", instanceIndex);
        tag.putString("State", state.name());
        if (currentNode != null) tag.putString("CurrentNode", currentNode);

        ListTag clearedList = new ListTag();
        for (String s : clearedNodes) {
            CompoundTag ct = new CompoundTag();
            ct.putString("Node", s);
            clearedList.add(ct);
        }
        tag.put("ClearedNodes", clearedList);

        ListTag playerList = new ListTag();
        for (UUID u : playerUuids) {
            playerList.add(NbtUtils.createUUID(u));
        }
        tag.put("Players", playerList);

        // 整合版 §3.2：持久化每个玩家的最新 checkpoint id
        if (!playerCheckpoints.isEmpty()) {
            ListTag checkpointsList = new ListTag();
            for (Map.Entry<UUID, String> e : playerCheckpoints.entrySet()) {
                CompoundTag cpTag = new CompoundTag();
                cpTag.putUUID("Player", e.getKey());
                cpTag.putString("Checkpoint", e.getValue());
                checkpointsList.add(cpTag);
            }
            tag.put("PlayerCheckpoints", checkpointsList);
        }

        // 整合版 §3.1：FlagshipUuid 字段不再写出（已删除玩家旗舰权限概念）
        if (lecternPos != null) {
            tag.put("LecternPos", NbtUtils.writeBlockPos(lecternPos));
        }
        if (lecternDimension != null) {
            tag.putString("LecternDimension", lecternDimension);
        }
        tag.putLong("StartTime", startTimeMillis);
        tag.putLong("EndTime", endTimeMillis);
        return tag;
    }

    public static DungeonInstance load(CompoundTag tag) {
        UUID id = tag.getUUID("InstanceId");
        String stageId = tag.getString("StageId");
        int index = tag.getInt("InstanceIndex");
        DungeonInstance inst = new DungeonInstance(id, stageId, index);

        try {
            inst.state = State.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            inst.state = State.SUSPENDED;
        }

        if (tag.contains("CurrentNode")) {
            inst.currentNode = tag.getString("CurrentNode");
        }

        ListTag clearedList = tag.getList("ClearedNodes", Tag.TAG_COMPOUND);
        for (int i = 0; i < clearedList.size(); i++) {
            inst.clearedNodes.add(clearedList.getCompound(i).getString("Node"));
        }

        ListTag playerList = tag.getList("Players", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < playerList.size(); i++) {
            inst.playerUuids.add(NbtUtils.loadUUID(playerList.get(i)));
        }

        // 整合版 §3.2：读玩家 checkpoint 进度
        if (tag.contains("PlayerCheckpoints", Tag.TAG_LIST)) {
            ListTag cpList = tag.getList("PlayerCheckpoints", Tag.TAG_COMPOUND);
            for (int i = 0; i < cpList.size(); i++) {
                CompoundTag cpTag = cpList.getCompound(i);
                UUID playerUuid = cpTag.getUUID("Player");
                String cpId = cpTag.getString("Checkpoint");
                inst.playerCheckpoints.put(playerUuid, cpId);
            }
        }

        // 整合版 §3.1：旧存档的 FlagshipUuid 字段读时忽略（不再需要），保证向前兼容
        if (tag.contains("LecternPos")) {
            NbtUtils.readBlockPos(tag, "LecternPos").ifPresent(inst::setLecternPos);
        }
        if (tag.contains("LecternDimension")) {
            inst.lecternDimension = tag.getString("LecternDimension");
        }
        inst.startTimeMillis = tag.getLong("StartTime");
        inst.endTimeMillis = tag.getLong("EndTime");
        return inst;
    }
}
