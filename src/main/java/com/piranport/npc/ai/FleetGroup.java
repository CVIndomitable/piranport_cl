package com.piranport.npc.ai;

import com.piranport.PiranPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A fleet group that manages a set of deep ocean entities sharing alert/combat state.
 */
public class FleetGroup {

    public enum State {
        IDLE,    // Wandering, no enemy detected
        ALERT,   // An enemy was spotted but not yet engaged
        COMBAT   // Actively fighting
    }

    private final UUID groupId;
    private final Set<UUID> members = new HashSet<>();
    private State state = State.IDLE;
    @Nullable
    private UUID sharedTargetUuid;

    public FleetGroup(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public Set<UUID> getMembers() {
        return java.util.Collections.unmodifiableSet(members);
    }

    public void removeDeadMembers(java.util.function.Predicate<UUID> isDead) {
        members.removeIf(isDead);
    }

    public void addMember(UUID entityUuid) {
        members.add(entityUuid);
    }

    public void removeMember(UUID entityUuid) {
        members.remove(entityUuid);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public int size() {
        return members.size();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Nullable
    public UUID getSharedTargetUuid() {
        return sharedTargetUuid;
    }

    public void setSharedTarget(@Nullable UUID target) {
        this.sharedTargetUuid = target;
        if (target != null && state == State.IDLE) {
            state = State.COMBAT;
        }
    }

    /**
     * Clear combat state if the shared target is gone.
     */
    public void clearTarget() {
        this.sharedTargetUuid = null;
        this.state = State.IDLE;
    }

    // --- Serialization ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("GroupId", groupId);
        tag.putInt("State", state.ordinal());
        if (sharedTargetUuid != null) {
            tag.putUUID("SharedTarget", sharedTargetUuid);
        }
        ListTag memberList = new ListTag();
        for (UUID uuid : members) {
            memberList.add(NbtUtils.createUUID(uuid));
        }
        tag.put("Members", memberList);
        return tag;
    }

    public static FleetGroup load(CompoundTag tag) {
        UUID groupId = tag.getUUID("GroupId");
        FleetGroup group = new FleetGroup(groupId);
        int stateOrdinal = tag.getInt("State");
        State[] states = State.values();
        group.state = stateOrdinal >= 0 && stateOrdinal < states.length ? states[stateOrdinal] : State.IDLE;
        if (tag.hasUUID("SharedTarget")) {
            group.sharedTargetUuid = tag.getUUID("SharedTarget");
        }
        ListTag memberList = tag.getList("Members", Tag.TAG_INT_ARRAY);
        if (memberList.isEmpty() && tag.contains("Members")) {
            PiranPort.LOGGER.warn("FleetGroup {}: Members tag exists but is empty or type mismatch",
                    groupId);
        }
        for (int i = 0; i < memberList.size(); i++) {
            group.members.add(NbtUtils.loadUUID(memberList.get(i)));
        }
        return group;
    }
}
