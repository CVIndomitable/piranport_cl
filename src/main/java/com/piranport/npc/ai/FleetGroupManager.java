package com.piranport.npc.ai;

import com.piranport.PiranPort;
import com.piranport.npc.deepocean.AbstractDeepOceanEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global manager for all active fleet groups. Persisted as SavedData.
 * <p>
 * Design: event-driven, NOT per-tick polling.
 * - Members notify the manager when they discover a target.
 * - Members are removed on death/unload.
 * - Empty groups are cleaned up lazily.
 */
public class FleetGroupManager extends SavedData {
    private static final String DATA_NAME = "piranport_fleet_groups";

    private final Map<UUID, FleetGroup> groups = new HashMap<>();

    public FleetGroupManager() {}

    // --- Group lifecycle ---

    /**
     * Create a new fleet group and return it.
     */
    public FleetGroup createGroup() {
        UUID id = UUID.randomUUID();
        FleetGroup group = new FleetGroup(id);
        groups.put(id, group);
        setDirty();
        return group;
    }

    /**
     * Get a group by ID.
     */
    @Nullable
    public FleetGroup getGroup(UUID groupId) {
        return groups.get(groupId);
    }

    /**
     * Add an entity to a group.
     */
    public void addMember(UUID groupId, UUID entityUuid) {
        FleetGroup group = groups.get(groupId);
        if (group != null) {
            group.addMember(entityUuid);
            setDirty();
        }
    }

    /**
     * Remove an entity from a group. Destroys the group if empty.
     */
    public void removeMember(UUID groupId, UUID entityUuid) {
        FleetGroup group = groups.get(groupId);
        if (group != null) {
            group.removeMember(entityUuid);
            if (group.isEmpty()) {
                groups.remove(groupId);
            }
            setDirty();
        }
    }

    /**
     * Alert the entire group that a member found a target.
     * IDLE members receive the shared target and enter COMBAT.
     * Members already in COMBAT keep their current target.
     *
     * @param groupId       the fleet group
     * @param target        the discovered target
     * @param discovererUuid the member that found the target
     */
    public void alertGroup(UUID groupId, LivingEntity target, UUID discovererUuid) {
        FleetGroup group = groups.get(groupId);
        if (group == null) return;

        group.setSharedTarget(target.getUUID());
        setDirty();

        // Propagate to IDLE members in the same level
        ServerLevel level = null;
        Entity discoverer = null;
        for (UUID memberUuid : group.getMembers()) {
            if (memberUuid.equals(discovererUuid)) continue;
            // We need the level to look up entities — get it from any member
            if (level == null) {
                // Try to find level from any loaded entity
                for (ServerLevel sl : target.getServer().getAllLevels()) {
                    Entity e = sl.getEntity(memberUuid);
                    if (e != null) {
                        level = sl;
                        break;
                    }
                }
                if (level == null) level = (ServerLevel) target.level();
            }
            Entity member = level.getEntity(memberUuid);
            if (member instanceof AbstractDeepOceanEntity deepOcean) {
                if (deepOcean.getTarget() == null) {
                    deepOcean.setTarget(target);
                }
            }
        }
    }

    /**
     * Clear target for a group (e.g., when target dies).
     */
    public void clearGroupTarget(UUID groupId) {
        FleetGroup group = groups.get(groupId);
        if (group != null) {
            group.clearTarget();
            setDirty();
        }
    }

    /**
     * Get all groups (for debug commands).
     */
    public Map<UUID, FleetGroup> getAllGroups() {
        return groups;
    }

    /**
     * Clean up groups with no living members (call periodically, not every tick).
     */
    public void cleanup(ServerLevel level) {
        groups.entrySet().removeIf(entry -> {
            FleetGroup group = entry.getValue();
            // Remove members that are no longer loaded or alive
            group.getMembers().removeIf(uuid -> {
                Entity e = level.getEntity(uuid);
                return e == null || !e.isAlive();
            });
            return group.isEmpty();
        });
        setDirty();
    }

    // --- SavedData ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag groupList = new ListTag();
        for (FleetGroup group : groups.values()) {
            groupList.add(group.save());
        }
        tag.put("Groups", groupList);
        return tag;
    }

    public static FleetGroupManager load(CompoundTag tag, HolderLookup.Provider registries) {
        FleetGroupManager mgr = new FleetGroupManager();
        ListTag groupList = tag.getList("Groups", Tag.TAG_COMPOUND);
        for (int i = 0; i < groupList.size(); i++) {
            FleetGroup group = FleetGroup.load(groupList.getCompound(i));
            mgr.groups.put(group.getGroupId(), group);
        }
        return mgr;
    }

    public static FleetGroupManager get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FleetGroupManager::new, FleetGroupManager::load, null),
                DATA_NAME);
    }
}
