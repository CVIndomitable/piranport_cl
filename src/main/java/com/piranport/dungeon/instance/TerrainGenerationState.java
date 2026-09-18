package com.piranport.dungeon.instance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 一个实例节点的地形生成游标。所有字段都可恢复，服务器重启或区块卸载不会重新刷已经写完的方块。
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

    public TerrainGenerationState() {}

    public Phase phase() { return phase; }
    public long cursor() { return cursor; }
    public int startX() { return startX; }
    public int startZ() { return startZ; }
    public long seed() { return seed; }
    public int terrainOrdinal() { return terrainOrdinal; }
    public boolean initialized() { return initialized; }

    public void initialize(int startX, int startZ, long seed, int terrainOrdinal) {
        if (initialized) return;
        this.startX = startX;
        this.startZ = startZ;
        this.seed = seed;
        this.terrainOrdinal = terrainOrdinal;
        this.initialized = true;
        setDirty();
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
            case BOUNDARY, READY -> Phase.READY;
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
        return state;
    }
}
