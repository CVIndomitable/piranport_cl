package com.piranport.dungeon.instance;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DungeonFleetApproachTest {
    @Test
    void advancesTowardCenterAndStopsAtArrival() {
        BlockPos center = new BlockPos(1280, 64, 1280);
        Vec3 velocity = DungeonFleetApproach.approachVelocity(new Vec3(1408, 64, 1408), center);
        assertTrue(velocity.x < 0);
        assertTrue(velocity.z < 0);
        assertEquals(0.14, velocity.horizontalDistance(), 1e-9);
        assertEquals(Vec3.ZERO, DungeonFleetApproach.approachVelocity(new Vec3(1281, 64, 1281), center));
    }

    @Test
    void runsOnlyForActiveUnclearedNodeWithPresentPlayer() {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test", 0);
        instance.setState(DungeonInstance.State.ACTIVE);
        assertTrue(instance.beginNode("A"));
        assertTrue(DungeonFleetApproach.shouldRun(instance, "A", true));
        assertFalse(DungeonFleetApproach.shouldRun(instance, "A", false));
        assertFalse(DungeonFleetApproach.shouldRun(instance, "B", true));
        instance.setState(DungeonInstance.State.SUSPENDED);
        assertFalse(DungeonFleetApproach.shouldRun(instance, "A", true));
        instance.setState(DungeonInstance.State.ACTIVE);
        assertTrue(instance.clearNode("A"));
        assertFalse(DungeonFleetApproach.shouldRun(instance, "A", true));
    }

    @Test
    void preservesJourneyAcrossRestart() {
        UUID entity = UUID.randomUUID();
        UUID instance = UUID.randomUUID();
        ChunkPos chunk = new ChunkPos(-12, 44);
        CompoundTag item = new CompoundTag();
        item.putUUID("Entity", entity);
        item.putUUID("Instance", instance);
        item.putString("Node", "A");
        item.putLong("Chunk", chunk.toLong());
        ListTag list = new ListTag();
        list.add(item);
        CompoundTag input = new CompoundTag();
        input.put("Journeys", list);

        CompoundTag saved = DungeonFleetApproach.load(input, null).save(new CompoundTag(), null);
        CompoundTag restored = saved.getList("Journeys", 10).getCompound(0);
        assertEquals(entity, restored.getUUID("Entity"));
        assertEquals(instance, restored.getUUID("Instance"));
        assertEquals("A", restored.getString("Node"));
        assertEquals(chunk.toLong(), restored.getLong("Chunk"));
    }
}
