package com.piranport.npc.deepocean;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 验证 JSON 缺省语义：follow_range 缺失时必须是 null，
 * 调用方才不会用一个默认 32 把子类硬编码兜底值覆盖掉。
 */
class DeepOceanEntityDataTest {

    @Test
    void missingFollowRangeStaysNull() {
        DeepOceanEntityData data = DeepOceanEntityData.fromJson(new JsonObject());
        assertNull(data.followRange(), "缺失 follow_range 时应保留 null，交给实体硬编码兜底");
        assertNull(data.torpedoRange(), "缺失 torpedo_range 时应保留 null，交给常规索敌兜底");
    }

    @Test
    void followRangeIsReadFromJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("entity_type", "piranport:deep_ocean_destroyer");
        obj.addProperty("follow_range", 48.0);
        obj.addProperty("torpedo_range", 18.0);

        DeepOceanEntityData data = DeepOceanEntityData.fromJson(obj);

        assertEquals("piranport:deep_ocean_destroyer", data.entityType());
        assertEquals(48.0, data.followRange().doubleValue());
        assertEquals(18.0, data.torpedoRange().doubleValue());
    }
}
