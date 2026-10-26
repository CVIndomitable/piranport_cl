package com.piranport.aviation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 客户端火控列表自检：已确认死亡的目标必须能就地剔除。 */
class ClientFireControlDataTest {

    @AfterEach
    void clear() {
        ClientFireControlData.clear();
    }

    @Test
    void removeWhereDropsOnlyMatchingTargets() {
        UUID boss = UUID.randomUUID();
        UUID destroyer = UUID.randomUUID();
        ClientFireControlData.setTargets(List.of(boss, destroyer));

        assertTrue(ClientFireControlData.removeWhere(uuid -> uuid.equals(boss)));
        assertEquals(List.of(destroyer), ClientFireControlData.getTargets());
    }

    @Test
    void removeWhereReportsNoChangeWhenNothingMatches() {
        UUID target = UUID.randomUUID();
        ClientFireControlData.setTargets(List.of(target));

        assertFalse(ClientFireControlData.removeWhere(uuid -> false));
        assertEquals(List.of(target), ClientFireControlData.getTargets());
    }

    @Test
    void removeAllTargetsLeavesEmptyList() {
        ClientFireControlData.setTargets(List.of(UUID.randomUUID(), UUID.randomUUID()));

        assertTrue(ClientFireControlData.removeWhere(uuid -> true));
        assertTrue(ClientFireControlData.getTargets().isEmpty());
    }
}
