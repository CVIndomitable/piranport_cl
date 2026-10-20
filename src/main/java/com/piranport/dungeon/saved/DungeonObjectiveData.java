package com.piranport.dungeon.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 用实体生成/死亡记录判定清场，区块卸载不等于敌人被消灭。 */
public final class DungeonObjectiveData extends SavedData {
    private static final class Progress {
        boolean registered;
        final Set<UUID> enemies = new HashSet<>();
        final Set<UUID> flagships = new HashSet<>();
        /** 夺旗占领进度，0–100 百分比。 */
        double captureProgress;
    }

    private final Map<UUID, Map<String, Progress>> instances = new HashMap<>();

    private Progress progress(UUID instance, String node) {
        return instances.computeIfAbsent(instance, ignored -> new HashMap<>())
                .computeIfAbsent(node, ignored -> new Progress());
    }

    public void register(UUID instance, String node, Iterable<Entity> entities) {
        Progress p = progress(instance, node);
        for (Entity entity : entities) {
            p.enemies.add(entity.getUUID());
            if (entity.getTags().contains("dungeon_flagship")) p.flagships.add(entity.getUUID());
        }
        p.registered = !p.enemies.isEmpty();
        setDirty();
    }

    public void died(UUID instance, String node, UUID entity) {
        Progress p = progress(instance, node);
        if (p.enemies.remove(entity) | p.flagships.remove(entity)) setDirty();
    }

    /** 护航失败重置：丢弃该节点的敌人登记与夺旗进度，下一次 register 重新开始。 */
    public void reset(UUID instance, String node) {
        Map<String, Progress> nodes = instances.get(instance);
        if (nodes != null && nodes.remove(node) != null) setDirty();
    }

    public boolean defeated(UUID instance, String node, boolean allEnemies) {
        Progress p = progress(instance, node);
        return p.registered && (allEnemies ? p.enemies : p.flagships).isEmpty();
    }

    /** 离开圈不清零：《副本/12》只规定加减，不规定离圈重置。 */
    public double addCaptureProgress(UUID instance, String node, double delta) {
        Progress p = progress(instance, node);
        double next = Math.max(0.0, Math.min(100.0, p.captureProgress + delta));
        if (next != p.captureProgress) { p.captureProgress = next; setDirty(); }
        return next;
    }

    public double captureProgress(UUID instance, String node) {
        return progress(instance, node).captureProgress;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag records = new ListTag();
        instances.forEach((instance, nodes) -> nodes.forEach((node, p) -> {
            CompoundTag record = new CompoundTag();
            record.putUUID("Instance", instance);
            record.putString("Node", node);
            record.putBoolean("Registered", p.registered);
            record.putDouble("CaptureProgress", p.captureProgress);
            ListTag enemies = new ListTag();
            for (UUID id : p.enemies) {
                CompoundTag enemy = new CompoundTag();
                enemy.putUUID("Id", id);
                enemy.putBoolean("Flagship", p.flagships.contains(id));
                enemies.add(enemy);
            }
            record.put("Enemies", enemies);
            records.add(record);
        }));
        tag.put("Objectives", records);
        return tag;
    }

    public static DungeonObjectiveData load(CompoundTag tag, HolderLookup.Provider registries) {
        DungeonObjectiveData data = new DungeonObjectiveData();
        ListTag records = tag.getList("Objectives", Tag.TAG_COMPOUND);
        for (int i = 0; i < records.size(); i++) {
            CompoundTag record = records.getCompound(i);
            if (!record.hasUUID("Instance") || record.getString("Node").isBlank()) continue;
            Progress p = data.progress(record.getUUID("Instance"), record.getString("Node"));
            p.registered = record.getBoolean("Registered");
            p.captureProgress = Math.max(0.0, Math.min(100.0, record.getDouble("CaptureProgress")));
            ListTag enemies = record.getList("Enemies", Tag.TAG_COMPOUND);
            for (int j = 0; j < enemies.size(); j++) {
                CompoundTag enemy = enemies.getCompound(j);
                if (!enemy.hasUUID("Id")) continue;
                UUID id = enemy.getUUID("Id");
                p.enemies.add(id);
                if (enemy.getBoolean("Flagship")) p.flagships.add(id);
            }
        }
        return data;
    }

    public static DungeonObjectiveData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                DungeonObjectiveData::new, DungeonObjectiveData::load, null), "piranport_dungeon_objectives");
    }
}
