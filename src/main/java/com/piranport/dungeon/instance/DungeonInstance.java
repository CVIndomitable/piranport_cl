package com.piranport.dungeon.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Runtime state of a single dungeon instance.
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
    public Set<String> getClearedNodes() { return Set.copyOf(clearedNodes); }
    public Set<UUID> getPlayerUuids() { return Set.copyOf(playerUuids); }
    public BlockPos getLecternPos() { return lecternPos; }
    public String getLecternDimension() { return lecternDimension; }
    public long getStartTimeMillis() { return startTimeMillis; }
    public long getEndTimeMillis() { return endTimeMillis; }

    /**
     * Returns the X offset of this instance's region in the dungeon dimension.
     */
    public int getRegionOriginX() {
        return instanceIndex * com.piranport.dungeon.DungeonConstants.REGION_SIZE;
    }

    /**
     * Returns the Z offset (always 0 for simplicity — instances line up along X axis).
     */
    public int getRegionOriginZ() {
        return 0;
    }

    /**
     * Returns the spawn position for players entering this instance's current node.
     */
    public BlockPos getNodeSpawnPos(String nodeId) {
        // Nodes are laid out in a grid within the instance region.
        // Simple: A=0, B=1, C=2, ... along X axis with NODE_AREA_SIZE spacing.
        int nodeIndex = 0;
        if (nodeId != null && !nodeId.isEmpty()) {
            char ch = nodeId.charAt(0);
            if (ch >= 'A' && ch <= 'Z') {
                nodeIndex = ch - 'A';
            } else if (ch >= 'a' && ch <= 'z') {
                nodeIndex = ch - 'a';
            } else {
                // Fallback: use hashCode for non-letter nodeIds (floorMod avoids Integer.MIN_VALUE issue)
                nodeIndex = Math.floorMod(nodeId.hashCode(), com.piranport.dungeon.DungeonConstants.MAX_NODES_PER_STAGE);
            }
        }
        int nodeX = getRegionOriginX() + nodeIndex * com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE
                + com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE / 2;
        int nodeZ = getRegionOriginZ() + com.piranport.dungeon.DungeonConstants.NODE_AREA_SIZE / 2;
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
