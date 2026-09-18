package com.piranport.dungeon.instance;

import com.piranport.dungeon.event.DungeonEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 验证在线参与者位置才是暂停/恢复的依据，通关不会丢失实例存档。 */
class DungeonInstanceLifecycleTest {
    @Test
    void oneParticipantLeavingDoesNotSuspendOtherParticipants() {
        UUID departing = UUID.randomUUID();
        UUID remaining = UUID.randomUUID();
        DungeonInstance source = instance(0, departing, remaining);
        DungeonInstanceManager manager = manager(source);
        DungeonInstance instance = manager.getInstance(source.getInstanceId());

        manager.refreshPlayerPresence(departing, (ignored, uuid) -> true);
        assertEquals(DungeonInstance.State.ACTIVE, instance.getState());

        // 模拟登出回调仍可在 PlayerList 中查到正在离开的最后一人。
        manager.refreshPlayerPresence(remaining, (ignored, uuid) -> uuid.equals(remaining));
        assertEquals(DungeonInstance.State.SUSPENDED, instance.getState());
        assertEquals(2, instance.getPlayerUuids().size(), "退出不删除历史参与者和重连身份");
    }

    @Test
    void lastParticipantLeavingSuspendsAndReturningResumes() {
        UUID player = UUID.randomUUID();
        DungeonInstance source = instance(0, player);
        DungeonInstanceManager manager = manager(source);
        DungeonInstance instance = manager.getInstance(source.getInstanceId());

        manager.refreshPlayerPresence(null, (ignored, uuid) -> false);
        assertEquals(DungeonInstance.State.SUSPENDED, instance.getState());
        manager.refreshPlayerPresence(null, (ignored, uuid) -> true);
        assertEquals(DungeonInstance.State.ACTIVE, instance.getState());
        assertEquals("checkpoint_a", instance.getLatestCheckpointFor(player));
    }

    @Test
    void previousParticipationInAnotherInstanceCannotKeepItActive() {
        UUID player = UUID.randomUUID();
        DungeonInstance first = instance(0, player);
        DungeonInstance second = instance(1, player);
        DungeonInstanceManager manager = manager(first, second);
        BlockPos currentPosition = new BlockPos(second.getUsableMinX(), 64, second.getUsableMinZ());

        manager.refreshPlayerPresence(null, (instance, uuid) ->
                DungeonInstanceManager.isInsideInstance(instance,
                        DungeonEventHandler.DUNGEON_DIMENSION, currentPosition));

        assertEquals(DungeonInstance.State.SUSPENDED,
                manager.getInstance(first.getInstanceId()).getState());
        assertEquals(DungeonInstance.State.ACTIVE,
                manager.getInstance(second.getInstanceId()).getState());
        assertFalse(DungeonInstanceManager.isInsideInstance(second, Level.OVERWORLD, currentPosition));
        assertFalse(DungeonInstanceManager.isInsideInstance(first,
                DungeonEventHandler.DUNGEON_DIMENSION, new BlockPos(-1, 64, 512)));
    }

    @Test
    void completedInstanceAndCheckpointRemainSavedWithoutRepeatedSettlement() {
        UUID player = UUID.randomUUID();
        DungeonInstance source = instance(0, player);
        DungeonInstanceManager manager = manager(source);
        UUID instanceId = source.getInstanceId();

        assertTrue(manager.completeInstance(instanceId));
        long endTime = manager.getInstance(instanceId).getEndTimeMillis();
        assertFalse(manager.completeInstance(instanceId));
        manager.refreshPlayerPresence(null, (ignored, uuid) -> false);
        manager.refreshPlayerPresence(null, (ignored, uuid) -> true);
        manager.sweepLeaks();

        DungeonInstanceManager restored = DungeonInstanceManager.load(
                manager.save(new CompoundTag(), null), null);
        DungeonInstance completed = restored.getInstance(instanceId);
        assertNotNull(completed);
        assertEquals(DungeonInstance.State.COMPLETED, completed.getState());
        assertEquals(endTime, completed.getEndTimeMillis());
        assertTrue(completed.getClearedNodes().contains("node_a"));
        assertEquals("checkpoint_a", completed.getLatestCheckpointFor(player));
        assertEquals(source.getInstanceIndex(), completed.getInstanceIndex());
    }

    private static DungeonInstance instance(int index, UUID... players) {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test_stage", index);
        instance.setState(DungeonInstance.State.ACTIVE);
        instance.setCurrentNode("node_a");
        instance.addClearedNode("node_a");
        for (UUID player : players) {
            instance.addPlayer(player);
            instance.markCheckpointReached(player, "checkpoint_a");
        }
        return instance;
    }

    private static DungeonInstanceManager manager(DungeonInstance... instances) {
        ListTag entries = new ListTag();
        for (DungeonInstance instance : instances) entries.add(instance.save());
        CompoundTag tag = new CompoundTag();
        tag.put("Instances", entries);
        tag.putInt("NextIndex", instances.length);
        return DungeonInstanceManager.load(tag, null);
    }
}
