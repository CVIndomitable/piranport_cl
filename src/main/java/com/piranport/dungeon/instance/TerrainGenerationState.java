package com.piranport.dungeon.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 共享海面或单个节点的生成游标；节点特征不会复用其他节点的完成标记。
 */
public final class TerrainGenerationState extends SavedData {
    public enum Phase { BASE, FEATURES, POI, BOUNDARY, READY }

    private Phase phase = Phase.BASE;
    private long cursor;
    private int startX;
    private int startZ;
    private long seed;
    private int terrainOrdinal;
    private boolean initialized;
    private boolean nodeSpecific;

    public TerrainGenerationState() {}

    public Phase phase() { return phase; }
    public long cursor() { return cursor; }
    public int startX() { return startX; }
    public int startZ() { return startZ; }
    public long seed() { return seed; }
    public int terrainOrdinal() { return terrainOrdinal; }
    public boolean initialized() { return initialized; }
    public boolean nodeSpecific() { return nodeSpecific; }

    public void initialize(int startX, int startZ, long seed, int terrainOrdinal) {
        if (initialized) return;
        this.startX = startX;
        this.startZ = startZ;
        this.seed = seed;
        this.terrainOrdinal = terrainOrdinal;
        this.initialized = true;
        setDirty();
    }

    public void initializeNode(int centerX, int centerZ, long seed, int terrainOrdinal) {
        if (initialized) return;
        initialize(centerX, centerZ, seed, terrainOrdinal);
        nodeSpecific = true;
        phase = Phase.FEATURES;
    }

    public void advance(long amount) {
        cursor += amount;
        setDirty();
    }

    public void nextPhase() {
        cursor = 0;
        phase = switch (phase) {
            case BASE -> Phase.FEATURES;
            case FEATURES -> Phase.POI;
            case POI -> Phase.BOUNDARY;
            case BOUNDARY -> Phase.READY;
            case READY -> Phase.READY;
        };
        setDirty();
    }

    public void markReady() {
        phase = Phase.READY;
        cursor = 0;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putString("Phase", phase.name());
        tag.putLong("Cursor", cursor);
        tag.putInt("StartX", startX);
        tag.putInt("StartZ", startZ);
        tag.putLong("Seed", seed);
        tag.putInt("Terrain", terrainOrdinal);
        tag.putBoolean("Initialized", initialized);
        tag.putInt("Version", 3);
        tag.putBoolean("NodeSpecific", nodeSpecific);
        return tag;
    }

    public static TerrainGenerationState load(CompoundTag tag, HolderLookup.Provider registries) {
        TerrainGenerationState state = new TerrainGenerationState();
        try {
            state.phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException ignored) {
            state.phase = Phase.BASE;
        }
        state.cursor = Math.max(0, tag.getLong("Cursor"));
        state.startX = tag.getInt("StartX");
        state.startZ = tag.getInt("StartZ");
        state.seed = tag.getLong("Seed");
        state.terrainOrdinal = Math.max(0, Math.min(5, tag.getInt("Terrain")));
        state.initialized = tag.getBoolean("Initialized");
        state.nodeSpecific = tag.getBoolean("NodeSpecific");
        // 旧版本把首节点特征写入共享游标。保留海面进度，独立节点游标负责补齐各节点。
        if (tag.getInt("Version") < 2 && (state.phase == Phase.FEATURES || state.phase == Phase.POI)) {
            state.phase = Phase.BOUNDARY;
            state.cursor = 0;
            state.setDirty();
        }
        // 屏障环改为写到建筑高度上限并加盖（260929）。V2 存档写下的是矮环且没有顶盖，
        // 若沿用 READY/BOUNDARY 游标就永远不会补写，玩家照样能从上方越出——退回重扫一遍。
        // 屏障写入幂等，重扫只是多花队列额度。
        if (tag.getInt("Version") < 3 && (state.phase == Phase.BOUNDARY || state.phase == Phase.READY)) {
            state.phase = Phase.BOUNDARY;
            state.cursor = 0;
            state.setDirty();
        }
        return state;
    }
}
