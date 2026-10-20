package com.piranport.dungeon.client;

import com.piranport.dungeon.event.BranchDoors;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 本地玩家的分歧门快照缓存，按（实例，来源节点）分组。
 *
 * <p>快照是每个玩家私有的：别人的门在本地显示为“无状态”，不会套用本人的亮灭。
 * 只在主线程读写（网络包经 enqueueWork 入队、渲染在主线程），因此用普通 HashMap。</p>
 */
public final class BranchDoorClientState {
    private BranchDoorClientState() {}

    private static final Map<String, Map<String, BranchDoors.Door>> DOORS = new HashMap<>();

    public static void put(UUID instanceId, String fromNode, List<BranchDoors.Door> doors) {
        Map<String, BranchDoors.Door> byTarget = new HashMap<>();
        for (BranchDoors.Door door : doors) byTarget.put(door.target(), door);
        DOORS.put(key(instanceId.toString(), fromNode), byTarget);
    }

    @Nullable
    public static BranchDoors.Door find(String instanceKey, String fromNode, String target) {
        Map<String, BranchDoors.Door> byTarget = DOORS.get(key(instanceKey, fromNode));
        return byTarget == null ? null : byTarget.get(target);
    }

    private static String key(String instanceKey, String fromNode) {
        return instanceKey + "|" + fromNode;
    }
}
