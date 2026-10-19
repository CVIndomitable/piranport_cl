package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.event.UlanActivityRules;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.NodeBattleField;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 《副本/18》ulan 彩蛋关脚本。
 *
 * <p>只生成一形态、无护卫：当天第 1–3 次进本为补给苏赫巴托尔，第 4 次及以后为
 * 航空战列舰苏赫巴托尔。形态在实例首次进本时由 {@link UlanActivityRules} 锁定，
 * 跨日不会改写已建成的实例。击杀后建传送门；奖励由 settleNode 按活动规则发放。</p>
 */
public class UlanScript implements DungeonScript {

    public static final String TYPE_ID = "ulan_activity";

    private final UUID instanceId;
    private final String nodeId;
    private final List<UUID> playerUuids;
    private final BlockPos spawnPos;
    private final String variant;

    private boolean spawned = false;
    private boolean portalPending = false;
    private boolean portalSpawned = false;
    private boolean finished = false;

    public UlanScript(DungeonInstance instance, String nodeId, List<UUID> playerUuids) {
        this.instanceId = instance.getInstanceId();
        this.nodeId = nodeId;
        this.playerUuids = List.copyOf(playerUuids);
        this.spawnPos = instance.getNodeSpawnPos(nodeId);
        this.variant = instance.getActivityVariant();
    }

    private UlanScript(CompoundTag tag) {
        this.instanceId = NbtUtils.loadUUID(tag.get("InstanceId"));
        this.nodeId = tag.getString("NodeId");
        List<UUID> players = new ArrayList<>();
        ListTag list = tag.getList("PlayerUuids", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < list.size(); i++) players.add(NbtUtils.loadUUID(list.get(i)));
        this.playerUuids = List.copyOf(players);
        this.spawnPos = NbtUtils.readBlockPos(tag, "SpawnPos").orElse(BlockPos.ZERO);
        this.variant = tag.getString("Variant");
        this.spawned = tag.getBoolean("Spawned");
        this.portalPending = tag.getBoolean("PortalPending");
        this.portalSpawned = tag.getBoolean("PortalSpawned");
        this.finished = tag.getBoolean("Finished");
    }

    public static UlanScript loadFromNbt(CompoundTag tag) {
        return new UlanScript(tag);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("InstanceId", NbtUtils.createUUID(instanceId));
        tag.putString("NodeId", nodeId);
        ListTag list = new ListTag();
        for (UUID u : playerUuids) list.add(NbtUtils.createUUID(u));
        tag.put("PlayerUuids", list);
        tag.put("SpawnPos", NbtUtils.writeBlockPos(spawnPos));
        tag.putString("Variant", variant == null ? "" : variant);
        tag.putBoolean("Spawned", spawned);
        tag.putBoolean("PortalPending", portalPending);
        tag.putBoolean("PortalSpawned", portalSpawned);
        tag.putBoolean("Finished", finished);
    }

    /** 形态与敌舰一一对应；航空战列舰没有独立舰种，按《副本/18》用旗舰/战列代替。 */
    public void spawnEntities(ServerLevel level) {
        if (spawned) return;
        spawned = true;
        String entityId = UlanActivityRules.VARIANT_AVIATION.equals(variant)
                ? "piranport:deep_ocean_battleship"
                : "piranport:deep_ocean_supply";
        Entity entity = NodeBattleField.createEntity(level, entityId);
        if (entity == null) {
            PiranPort.LOGGER.warn("[Ulan] 无法生成 {}，直接完成关卡", entityId);
            portalPending = true;
            return;
        }
        entity.setPos(spawnPos.getX() + 0.5, DungeonConstants.SPAWN_Y, spawnPos.getZ() + 0.5);
        entity.addTag("dungeon_instance_" + instanceId);
        entity.addTag("dungeon_node_" + nodeId);
        entity.addTag("dungeon_script");
        level.addFreshEntity(entity);
    }

    @Override
    public boolean tick(ServerLevel dungeonLevel) {
        if (portalPending && !portalSpawned) {
            portalSpawned = true;
            com.piranport.dungeon.block.PortalStructureHelper.buildPortalStructure(
                    dungeonLevel, spawnPos.below(), instanceId, nodeId);
            finished = true;
        }
        return false;
    }

    @Override
    public boolean onEntityDeath(Entity entity) {
        if (portalSpawned || finished) return false;
        if (!(entity instanceof LivingEntity)) return false;
        if (!entity.getTags().contains("dungeon_instance_" + instanceId)) return false;
        if (!entity.getTags().contains("dungeon_node_" + nodeId)) return false;
        // 只认本脚本生成的敌舰，避免误把其他来源的实体算成击杀。
        if (!entity.getTags().contains("dungeon_script")) return false;

        portalPending = true;
        return true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
