package com.piranport.dungeon.script;

import com.piranport.dungeon.instance.DungeonInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 覆盖暂停、恢复、持久化、终结及区块加载边界，防止无人时剧情继续推进。 */
class DungeonScriptLifecycleTest {
    @Test
    void suspendedScriptRetainsProgressAndResumesFromSameTimer() {
        DungeonInstance instance = instance(DungeonInstance.State.ACTIVE);
        DungeonScriptManager manager = new DungeonScriptManager();
        CountingScript script = new CountingScript();
        manager.start(instance.getInstanceId(), script);

        manager.tickAll(null, id -> instance, ignored -> true);
        assertEquals(1, script.ticks);

        instance.setState(DungeonInstance.State.SUSPENDED);
        manager.setDirty(false);
        for (int i = 0; i < 100; i++) {
            manager.tickAll(null, id -> instance, ignored -> true);
        }
        assertEquals(1, script.ticks);
        assertSame(script, manager.getScript(instance.getInstanceId()));
        assertFalse(manager.isDirty());

        instance.setState(DungeonInstance.State.ACTIVE);
        manager.tickAll(null, id -> instance, ignored -> true);
        assertEquals(2, script.ticks);
        assertTrue(manager.isDirty(), "脚本只更新计时器也必须保存");
    }

    @ParameterizedTest
    @EnumSource(value = DungeonInstance.State.class, names = {"COMPLETED", "CLEANUP"})
    void terminalInstanceDoesNotRunScript(DungeonInstance.State state) {
        DungeonInstance instance = instance(state);
        DungeonScriptManager manager = new DungeonScriptManager();
        CountingScript script = new CountingScript();
        manager.start(instance.getInstanceId(), script);
        manager.setDirty(false);

        manager.tickAll(null, id -> instance, ignored -> true);

        assertEquals(0, script.ticks);
        assertNull(manager.getScript(instance.getInstanceId()));
        assertTrue(manager.isDirty());
    }

    @Test
    void orphanedScriptIsRemovedWithoutAdvancing() {
        UUID id = UUID.randomUUID();
        DungeonScriptManager manager = new DungeonScriptManager();
        CountingScript script = new CountingScript();
        manager.start(id, script);
        manager.tickAll(null, ignored -> null, ignored -> true);
        assertEquals(0, script.ticks);
        assertNull(manager.getScript(id));
    }

    @Test
    void creatingInstanceAndUnloadedNodeBothWaitWithoutChangingProgress() {
        DungeonInstance instance = instance(DungeonInstance.State.CREATING);
        DungeonScriptManager manager = new DungeonScriptManager();
        CountingScript script = new CountingScript();
        manager.start(instance.getInstanceId(), script);

        manager.tickAll(null, id -> instance, ignored -> true);
        instance.setState(DungeonInstance.State.ACTIVE);
        manager.tickAll(null, id -> instance, ignored -> false);
        assertEquals(0, script.ticks);
        assertSame(script, manager.getScript(instance.getInstanceId()));

        manager.tickAll(null, id -> instance, ignored -> true);
        assertEquals(1, script.ticks);
    }

    @Test
    void suspendedScriptSurvivesSaveAndReload() {
        DungeonScriptRegistry.register(CountingScript.TYPE, tag -> {
            CountingScript script = new CountingScript();
            script.ticks = tag.getInt("Ticks");
            return script;
        });
        DungeonInstance instance = instance(DungeonInstance.State.ACTIVE);
        DungeonScriptManager original = new DungeonScriptManager();
        original.start(instance.getInstanceId(), new CountingScript());
        original.tickAll(null, id -> instance, ignored -> true);
        instance.setState(DungeonInstance.State.SUSPENDED);

        DungeonScriptManager restored = DungeonScriptManager.load(
                original.save(new CompoundTag(), null), null);
        CountingScript script = (CountingScript) restored.getScript(instance.getInstanceId());
        assertNotNull(script);
        restored.tickAll(null, id -> instance, ignored -> true);
        assertEquals(1, script.ticks);

        instance.setState(DungeonInstance.State.ACTIVE);
        restored.tickAll(null, id -> instance, ignored -> true);
        assertEquals(2, script.ticks);
    }

    @Test
    void throwingScriptIsQuarantinedButItsStateIsStillSaved() {
        DungeonInstance instance = instance(DungeonInstance.State.ACTIVE);
        DungeonScriptManager manager = new DungeonScriptManager();
        CountingScript script = new CountingScript();
        script.fail = true;
        manager.start(instance.getInstanceId(), script);

        manager.tickAll(null, id -> instance, ignored -> true);
        manager.tickAll(null, id -> instance, ignored -> true);
        assertEquals(1, script.ticks, "出错后不能逐 tick 重试导致重复副作用");
        assertSame(script, manager.getScript(instance.getInstanceId()));
        CompoundTag saved = manager.save(new CompoundTag(), null);
        assertEquals(1, saved.getList("Scripts", Tag.TAG_COMPOUND).size());
    }

    private static DungeonInstance instance(DungeonInstance.State state) {
        DungeonInstance instance = new DungeonInstance(UUID.randomUUID(), "test_stage", 0);
        instance.setState(state);
        return instance;
    }

    private static final class CountingScript implements DungeonScript {
        private static final String TYPE = "test_lifecycle_counting";
        private int ticks;
        private boolean fail;

        @Override
        public boolean tick(ServerLevel level) {
            ticks++;
            if (fail) throw new IllegalStateException("测试脚本异常");
            return false;
        }

        @Override
        public boolean onEntityDeath(Entity entity) { return false; }

        @Override
        public boolean isFinished() { return false; }

        @Override
        public String typeId() { return TYPE; }

        @Override
        public void writeNbt(CompoundTag tag) { tag.putInt("Ticks", ticks); }
    }
}
