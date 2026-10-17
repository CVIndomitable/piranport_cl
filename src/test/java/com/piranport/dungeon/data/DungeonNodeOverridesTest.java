package com.piranport.dungeon.data;

import com.piranport.terminal.TerminalParameterSpec;
import com.piranport.terminal.TerminalParameters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 调试终端“按节点换怪”的数量覆盖测试。
 *
 * <p>不启动游戏：用 {@link TerminalParameters#applyClient} 注入一份客户端覆盖快照，
 * 让 {@link DungeonNodeOverrides#effective} 走和游戏内相同的读取路径。</p>
 */
class DungeonNodeOverridesTest {

    private static final String STAGE = "1-1";
    private static final String NODE = "A";
    private static final String DESTROYER = "piranport:deep_ocean_destroyer";
    private static final String CRUISER = "piranport:deep_ocean_light_cruiser";
    private static final String BATTLESHIP = "piranport:deep_ocean_battleship";

    @AfterEach
    void clearOverrides() {
        TerminalParameters.clearClient();
    }

    @Test
    void aggregateSpawnCountsSumsDuplicateEntries() {
        EnemySetData set = new EnemySetData("test", List.of(
                new EnemySetData.SpawnEntry(CRUISER, 1),
                new EnemySetData.SpawnEntry(CRUISER, 2),
                new EnemySetData.SpawnEntry(DESTROYER, 3)), null, null);

        assertEquals(Map.of(CRUISER, 3, DESTROYER, 3),
                DungeonNodeOverrides.aggregateSpawnCounts(set));
    }

    @Test
    void noOverrideReturnsOriginalEnemySet() {
        EnemySetData base = baseSet();

        assertSame(base, DungeonNodeOverrides.effective(STAGE, NODE, base));
    }

    @Test
    void overrideCanRemoveOneTypeAndAddAnother() {
        String destroyerKey = DungeonNodeOverrides.key(STAGE, NODE,
                DungeonNodeOverrides.spawnProperty(DESTROYER));
        String battleshipKey = DungeonNodeOverrides.key(STAGE, NODE,
                DungeonNodeOverrides.spawnProperty(BATTLESHIP));
        installOverrides(Map.of(
                destroyerKey, new Spec(destroyerKey, "spawn_deep_ocean_destroyer", 0),
                battleshipKey, new Spec(battleshipKey, "spawn_deep_ocean_battleship", 1)));

        EnemySetData effective = DungeonNodeOverrides.effective(STAGE, NODE, baseSet());

        assertEquals(List.of(
                new EnemySetData.SpawnEntry(CRUISER, 1),
                new EnemySetData.SpawnEntry(BATTLESHIP, 1)), effective.spawnList());
        // 旗舰身份不受数量覆盖影响。
        assertEquals(baseSet().flagship(), effective.flagship());
        assertEquals("double_line", effective.formation());
    }

    @Test
    void flagshipIsNeverPartOfSpawnOverrides() {
        EnemySetData base = baseSet();
        String key = DungeonNodeOverrides.key(STAGE, NODE,
                DungeonNodeOverrides.spawnProperty(DESTROYER));
        installOverrides(Map.of(key, new Spec(key, "spawn_deep_ocean_destroyer", 0)));

        EnemySetData effective = DungeonNodeOverrides.effective(STAGE, NODE, base);

        assertSame(base.flagship(), effective.flagship());
    }

    @Test
    void countIsCappedAtEight() {
        String key = DungeonNodeOverrides.key(STAGE, NODE,
                DungeonNodeOverrides.spawnProperty(DESTROYER));
        installOverrides(Map.of(key, new Spec(key, "spawn_deep_ocean_destroyer", 99, 0, 999)));

        EnemySetData effective = DungeonNodeOverrides.effective(STAGE, NODE, baseSet());

        assertEquals(8, effective.spawnList().get(0).count());
    }

    private static EnemySetData baseSet() {
        return new EnemySetData("test", List.of(
                new EnemySetData.SpawnEntry(DESTROYER, 2),
                new EnemySetData.SpawnEntry(CRUISER, 1)),
                new EnemySetData.SpawnEntry(CRUISER, 1), "double_line");
    }

    /** value = 终端里填写的覆盖值；min/max 用于模拟目录边界。 */
    private record Spec(String key, String property, int value, int min, int max) {
        Spec(String key, String property, int value) {
            this(key, property, value, 0, DungeonNodeOverrides.SPAWN_MAX);
        }
    }

    private static void installOverrides(Map<String, Spec> specs) {
        Map<String, String> values = new HashMap<>();
        Map<String, String> overrides = new HashMap<>();
        List<TerminalParameterSpec> metadata = new ArrayList<>();
        specs.forEach((key, spec) -> {
            values.put(key, Integer.toString(spec.value()));
            overrides.put(key, Integer.toString(spec.value()));
            metadata.add(new TerminalParameterSpec(key, "dungeon_node", STAGE + "/" + NODE,
                    spec.property(), TerminalParameterSpec.ValueType.INTEGER,
                    Integer.toString(spec.value()), spec.min(), spec.max()));
        });
        TerminalParameters.applyClient(values, overrides, metadata, 1L, "");
    }
}
