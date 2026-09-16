package com.piranport.npc.ai;

import com.piranport.PiranPort;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A fleet group that manages an ordered list of deep ocean entities sharing alert/combat state.
 * <p>
 * 编队规则（策划决策/深海/01）：
 * - 4~6 船一组，大船在前、同舰种高级在前。
 * - 首船即领舰，继承编队走位职责。
 * - 领舰阵亡后由队列下一顺位递补。
 * - 后续舰船按队形站位跟随领舰。
 * - 潜艇单独成队（1~2 船），不参与水面舰队形排列。
 */
public class FleetGroup {

    /** 编队队形四式（策划决策/深海/01 §3-9） */
    public enum FormationType {
        SINGLE_LINE,        // 单纵阵（一字长蛇，默认/主用）
        DOUBLE_LINE,        // 复纵阵（两列纵队）
        WHEEL,              // 轮型阵（环形防御）
        SINGLE_HORIZONTAL   // 单横阵（一横排前进）
    }

    public enum State {
        IDLE,    // Wandering, no enemy detected
        ALERT,   // An enemy was spotted but not yet engaged
        COMBAT   // Actively fighting
    }

    private final UUID groupId;
    /** 成员按编队顺序排列（索引 0 = 领舰） */
    private final List<UUID> members = new ArrayList<>();
    private State state = State.IDLE;
    @Nullable
    private UUID sharedTargetUuid;
    private FormationType formation = FormationType.SINGLE_LINE;
    @Nullable
    private UUID leaderUuid;

    public FleetGroup(UUID groupId) {
        this.groupId = groupId;
    }

    public UUID getGroupId() {
        return groupId;
    }

    /**
     * 返回不可变的有序成员列表。索引 0 = 领舰。
     */
    public List<UUID> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void removeDeadMembers(java.util.function.Predicate<UUID> isDead) {
        members.removeIf(isDead);
    }

    public void addMember(UUID entityUuid) {
        members.add(entityUuid);
        // 首个成员自动成为领舰
        if (leaderUuid == null && members.size() == 1) {
            leaderUuid = entityUuid;
        }
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

    // --- Formation ---

    public FormationType getFormation() {
        return formation;
    }

    public void setFormation(FormationType formation) {
        this.formation = formation != null ? formation : FormationType.SINGLE_LINE;
    }

    @Nullable
    public UUID getLeaderUuid() {
        return leaderUuid;
    }

    public void setLeaderUuid(@Nullable UUID uuid) {
        this.leaderUuid = uuid;
    }

    /**
     * 计算给定成员在编队中的队形偏移量。
     *
     * @param memberIndex   成员在队列中的索引（0 = 领舰）
     * @param totalMembers  编队总人数
     * @param leaderPos     领舰当前世界位置
     * @param targetPos     当前攻击目标位置（用于确定领舰朝向）
     * @return 相对于 leaderPos 的偏移向量（Y 分量通常为 0）
     */
    public Vec3 getFormationOffset(int memberIndex, int totalMembers, Vec3 leaderPos, Vec3 targetPos) {
        if (memberIndex == 0) {
            return Vec3.ZERO; // 领舰无偏移
        }

        Vec3 toTarget = targetPos.subtract(leaderPos);
        toTarget = new Vec3(toTarget.x, 0, toTarget.z);
        double hDist = toTarget.horizontalDistance();
        Vec3 forward;
        if (hDist < 0.01) {
            // 领舰与目标重合，使用默认朝北
            forward = new Vec3(0, 0, 1);
        } else {
            forward = toTarget.normalize();
        }
        Vec3 backward = forward.scale(-1);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);

        return switch (formation) {
            case DOUBLE_LINE -> {
                // 复纵阵：两列并行，左右偏移 ±4，前后间距 6
                // 领舰在最前，第 1、2 个成员在后方 6 格左右各 4，第 3、4 个在后方 12 格...
                int row = (memberIndex - 1) / 2;
                int col = (memberIndex - 1) % 2;
                double lateral = col == 0 ? 4.0 : -4.0;
                yield right.scale(lateral).add(backward.scale(6.0 * (row + 1)));
            }
            case WHEEL -> {
                // 轮型阵：围成一圈，半径 12，均匀分布
                double angle = (2.0 * Math.PI * memberIndex) / totalMembers;
                yield new Vec3(Math.cos(angle) * 12.0, 0, Math.sin(angle) * 12.0);
            }
            case SINGLE_HORIZONTAL -> {
                // 单横阵：一横排，与领舰同前后位置，左右间距 6
                // 对称分布：memberIndex 1→右6, 2→左6, 3→右12, 4→左12...
                int side = memberIndex % 2 == 1 ? 1 : -1;
                int pos = (memberIndex + 1) / 2;
                yield right.scale(6.0 * pos * side);
            }
            case SINGLE_LINE -> {
                // 单纵阵（默认）：所有船在领舰后方直线排列，间距 8
                yield backward.scale(8.0 * memberIndex);
            }
        };
    }

    // --- Serialization ---

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("GroupId", groupId);
        tag.putInt("State", state.ordinal());
        tag.putInt("Formation", formation.ordinal());
        if (sharedTargetUuid != null) {
            tag.putUUID("SharedTarget", sharedTargetUuid);
        }
        if (leaderUuid != null) {
            tag.putUUID("Leader", leaderUuid);
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

        int formationOrdinal = tag.getInt("Formation");
        FormationType[] formations = FormationType.values();
        group.formation = formationOrdinal >= 0 && formationOrdinal < formations.length
                ? formations[formationOrdinal] : FormationType.SINGLE_LINE;

        if (tag.hasUUID("SharedTarget")) {
            group.sharedTargetUuid = tag.getUUID("SharedTarget");
        }
        if (tag.hasUUID("Leader")) {
            group.leaderUuid = tag.getUUID("Leader");
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
