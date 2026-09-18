package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Goldencatcat 活动关卡脚本 — 打靶练习关。
 *
 * <p>行为：进入关卡后生成 2~3 艘金猫猫（数量基于玩家人数随机），全部击杀后生成传送门。
 * 通关奖励为经验炮弹与吐司面包。</p>
 */
public class GoldencatcatScript implements DungeonScript {

    public static final String TYPE_ID = "goldencatcat_activity";

    private final UUID instanceId;
    private final String nodeId;
    private final List<UUID> playerUuids;
    private final BlockPos spawnPos;

    private boolean portalSpawned = false;
    private int catsSpawned = 0;
    private int catsKilled = 0;
    private boolean finished = false;
    private boolean portalPending = false;

    public GoldencatcatScript(DungeonInstance instance, String nodeId,
                               List<UUID> playerUuids) {
        this.instanceId = instance.getInstanceId();
        this.nodeId = nodeId;
        this.playerUuids = List.copyOf(playerUuids);
        this.spawnPos = instance.getNodeSpawnPos(nodeId);
    }

    private GoldencatcatScript(CompoundTag tag) {
        this.instanceId = NbtUtils.loadUUID(tag.get("InstanceId"));
        this.nodeId = tag.getString("NodeId");
        List<UUID> players = new ArrayList<>();
        ListTag plist = tag.getList("PlayerUuids", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < plist.size(); i++) {
            players.add(NbtUtils.loadUUID(plist.get(i)));
        }
        this.playerUuids = List.copyOf(players);
        this.spawnPos = NbtUtils.readBlockPos(tag, "SpawnPos").orElse(BlockPos.ZERO);
        this.portalSpawned = tag.getBoolean("PortalSpawned");
        this.portalPending = tag.getBoolean("PortalPending");
        this.catsSpawned = tag.getInt("CatsSpawned");
        this.catsKilled = tag.getInt("CatsKilled");
        this.finished = tag.getBoolean("Finished");
    }

    public static GoldencatcatScript loadFromNbt(CompoundTag tag) {
        return new GoldencatcatScript(tag);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("InstanceId", NbtUtils.createUUID(instanceId));
        tag.putString("NodeId", nodeId);
        ListTag plist = new ListTag();
        for (UUID u : playerUuids) {
            plist.add(NbtUtils.createUUID(u));
        }
        tag.put("PlayerUuids", plist);
        tag.put("SpawnPos", NbtUtils.writeBlockPos(spawnPos));
        tag.putBoolean("PortalSpawned", portalSpawned);
        tag.putBoolean("PortalPending", portalPending);
        tag.putInt("CatsSpawned", catsSpawned);
        tag.putInt("CatsKilled", catsKilled);
        tag.putBoolean("Finished", finished);
    }

    @Override
    public boolean tick(ServerLevel dungeonLevel) {
        if (portalPending && !portalSpawned) {
            spawnCompletionPortal(dungeonLevel);
        }
        if (portalSpawned || finished) return false;
        return false;
    }

    @Override
    public boolean onEntityDeath(Entity entity) {
        if (portalSpawned || finished) return false;
        if (!entity.getTags().contains("dungeon_instance_" + instanceId)) return false;
        if (!entity.getTags().contains("dungeon_node_" + nodeId)) return false;
        if (!(entity instanceof com.piranport.entity.GoldencatcatEntity)) return false;

        catsKilled++;
        PiranPort.LOGGER.info("[Goldencatcat] Killed cat {} / {}", catsKilled, catsSpawned);

        if (catsKilled >= catsSpawned) {
            portalPending = true;
        }
        return true;
    }

    /** Called once per node entry — spawns cats and starts tracking. */
    public void onStart() {
        if (portalSpawned) return;
        catsSpawned = computeCatCount();
        PiranPort.LOGGER.info("[Goldencatcat] Spawning {} cats for instance {}", catsSpawned, instanceId);
    }

    /**
     * Spawns GoldencatcatEntity instances for the given server level.
     * Called internally after reload to rehydrate state if needed.
     */
    public void spawnEntities(ServerLevel dungeonLevel) {
        if (portalSpawned || catsSpawned > 0) return;
        catsSpawned = computeCatCount();
        var rng = dungeonLevel.getRandom();
        for (int i = 0; i < catsSpawned; i++) {
            com.piranport.entity.GoldencatcatEntity cat =
                    new com.piranport.entity.GoldencatcatEntity(
                            ModEntityTypes.GOLDENCATCAT.get(), dungeonLevel);
            double angle = rng.nextDouble() * Math.PI * 2;
            double dist = 20 + rng.nextDouble() * 20;
            double ex = spawnPos.getX() + 0.5 + Math.cos(angle) * dist;
            double ez = spawnPos.getZ() + 0.5 + Math.sin(angle) * dist;
            cat.setPos(ex, DungeonConstants.SPAWN_Y, ez);
            cat.addTag("dungeon_instance_" + instanceId);
            cat.addTag("dungeon_node_" + nodeId);
            dungeonLevel.addFreshEntity(cat);
        }
        PiranPort.LOGGER.info("[Goldencatcat] Spawned {} cats at instance {}", catsSpawned, instanceId);
    }

    private void spawnCompletionPortal(ServerLevel dungeonLevel) {
        if (portalSpawned) return;
        portalSpawned = true;

        BlockPos center = spawnPos;

        var portal = DungeonPortalEntity.create(dungeonLevel, instanceId, nodeId,
                center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
        if (portal != null) {
            dungeonLevel.addFreshEntity(portal);
        }

        // Spawn boss loot ship at portal position
        spawnLootShip(dungeonLevel, center);

        finished = true;
        PiranPort.LOGGER.info("[Goldencatcat] All cats cleared, portal spawned at {}", center);
    }

    private void spawnLootShip(ServerLevel level, BlockPos center) {
        LootShipEntity loot = LootShipEntity.create(level,
                center.getX() + 0.5, DungeonConstants.SPAWN_Y,
                center.getZ() + 0.5, 0);
        List<ItemStack> lootItems = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            lootItems.add(new ItemStack(ModItems.EXP_SHELL.get(), 16));
            lootItems.add(new ItemStack(ModItems.TOAST_BREAD.get(), 16));
        }
        loot.fillInventory(lootItems);
        level.addFreshEntity(loot);
    }

    private int computeCatCount() {
        int base = Math.max(2, playerUuids.size());
        // 策划建议 1~3 只；这里按人数缩放，最多 5 只
        return Math.min(5, base);
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
