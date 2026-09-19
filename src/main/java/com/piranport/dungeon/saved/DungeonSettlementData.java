package com.piranport.dungeon.saved;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 节点统计和发奖边界随世界持久化，服务端重启不会清空击杀数或再次结算。 */
public final class DungeonSettlementData extends SavedData {
    private final Map<UUID, Map<String, NodeRecord>> instances = new HashMap<>();
    private static final class NodeRecord {
        private long activeTicks;
        private final Set<UUID> defeatedEntities = new HashSet<>();
        private boolean settled;
    }
    private NodeRecord node(UUID instanceId, String nodeId) {
        return instances.computeIfAbsent(instanceId, ignored -> new HashMap<>())
                .computeIfAbsent(nodeId, ignored -> new NodeRecord());
    }
    public void tick(UUID instanceId, String nodeId) { NodeRecord r=node(instanceId,nodeId); if(!r.settled){r.activeTicks++;setDirty();} }
    public boolean recordKill(UUID instanceId, String nodeId, UUID entityId) { NodeRecord r=node(instanceId,nodeId); if(r.settled||!r.defeatedEntities.add(entityId))return false;setDirty();return true; }
    public int kills(UUID instanceId,String nodeId){return node(instanceId,nodeId).defeatedEntities.size();}
    public long elapsedMillis(UUID instanceId,String nodeId){return node(instanceId,nodeId).activeTicks*50L;}
    public boolean claimSettlement(UUID instanceId,String nodeId){NodeRecord r=node(instanceId,nodeId);if(r.settled)return false;r.settled=true;setDirty();return true;}
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries){ListTag list=new ListTag();instances.forEach((id,nodes)->nodes.forEach((node,r)->{CompoundTag e=new CompoundTag();e.putUUID("Instance",id);e.putString("Node",node);e.putLong("ActiveTicks",r.activeTicks);e.putBoolean("Settled",r.settled);ListTag ks=new ListTag();for(UUID k:r.defeatedEntities){CompoundTag ke=new CompoundTag();ke.putUUID("Entity",k);ks.add(ke);}e.put("Kills",ks);list.add(e);}));tag.put("Nodes",list);return tag;}
    public static DungeonSettlementData load(CompoundTag tag, HolderLookup.Provider registries){DungeonSettlementData d=new DungeonSettlementData();ListTag list=tag.getList("Nodes",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){CompoundTag e=list.getCompound(i);if(!e.hasUUID("Instance")||e.getString("Node").isBlank())continue;NodeRecord r=d.node(e.getUUID("Instance"),e.getString("Node"));r.activeTicks=Math.max(0,e.getLong("ActiveTicks"));r.settled=e.getBoolean("Settled");ListTag ks=e.getList("Kills",Tag.TAG_COMPOUND);for(int j=0;j<ks.size();j++){CompoundTag k=ks.getCompound(j);if(k.hasUUID("Entity"))r.defeatedEntities.add(k.getUUID("Entity"));}}return d;}
    public static DungeonSettlementData get(ServerLevel level){return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(DungeonSettlementData::new,DungeonSettlementData::load,null),"piranport_dungeon_settlements");}
}
