package com.piranport.dungeon.script;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.EnemySetData;
import com.piranport.dungeon.entity.LootShipEntity;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.FormationGenerator;
import com.piranport.dungeon.instance.NodeBattleField;
import com.piranport.registry.ModEntityTypes;
import com.piranport.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import com.piranport.dungeon.network.DungeonBossOverlayPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Boss 登场 + 战斗 + 结算演出脚本。
 *
 * <p>四阶段流程：
 * <ul>
 *   <li>Phase 1 (INTRO)：Boss 登场动画 — 标题/副标题、Boss 实体生成、 narration</li>
 *   <li>Phase 2 (BATTLE)：战斗阶段 — 护卫敌群刷新、Boss 进入 aggressive 状态</li>
 *   <li>Phase 3 (DEFEATED)：Boss 被击杀 — 触发结算延时</li>
 *   <li>Phase 4 (OUTRO)：结算演出 — 胜利标题、Boss 战利品箱船、传送门、奖励分发</li>
 * </ul>
 *
 * <p>支持多种 Boss 类型：通过 node 的 enemy_set 读取旗舰配置，
 * 自动生成对应实体。NBT 持久化，UUID 引用实体，服务端重启不丢状态。</p>
 */
public class BossIntroScript implements DungeonScript {

    public static final String TYPE_ID = "boss_intro";

    private enum Phase { INTRO, BATTLE, DEFEATED, OUTRO, COMPLETED }

    private final UUID instanceId;
    private final String nodeId;
    private final String stageDisplayName;
    private final String stageId;
    private final List<UUID> playerUuids;
    private final BlockPos spawnPos;
    private final String enemySetId;

    private Phase phase = Phase.INTRO;
    private int tickCounter = 0;
    private boolean finished = false;

    // Phase 1 (INTRO) state
    private UUID bossUuid;
    private boolean titleSent = false;
    private boolean victorySent = false;
    private boolean bossSpawned = false;

    // Phase 2 (BATTLE) state
    private int totalEscorts = 0;
    private int escortsKilled = 0;
    private boolean escortsSpawned = false;

    // Phase 3 (DEFEATED) state
    private BlockPos bossDeathPos;
    private int defeatDelayTicks = 0;
    private static final int DEFEAT_DELAY_TICKS = 60; // 3 seconds

    // Phase 4 (OUTRO) state
    private boolean portalSpawned = false;
    private boolean lootShipSpawned = false;

    public BossIntroScript(DungeonInstance instance, String nodeId,
                           String stageDisplayName, List<UUID> playerUuids,
                           String enemySetId) {
        this.instanceId = instance.getInstanceId();
        this.nodeId = nodeId;
        this.stageDisplayName = stageDisplayName;
        this.stageId = instance.getStageId();
        this.playerUuids = List.copyOf(playerUuids);
        this.spawnPos = instance.getNodeSpawnPos(nodeId);
        this.enemySetId = enemySetId;
    }

    /** NBT-load constructor used by {@link DungeonScriptRegistry}. */
    private BossIntroScript(CompoundTag tag) {
        this.instanceId = NbtUtils.loadUUID(tag.get("InstanceId"));
        this.nodeId = tag.getString("NodeId");
        this.stageDisplayName = tag.getString("StageDisplayName");
        this.stageId = tag.getString("StageId");
        this.enemySetId = tag.getString("EnemySetId");

        List<UUID> players = new ArrayList<>();
        ListTag plist = tag.getList("PlayerUuids", Tag.TAG_INT_ARRAY);
        for (int i = 0; i < plist.size(); i++) {
            players.add(NbtUtils.loadUUID(plist.get(i)));
        }
        this.playerUuids = List.copyOf(players);

        this.spawnPos = NbtUtils.readBlockPos(tag, "SpawnPos").orElse(BlockPos.ZERO);

        try {
            this.phase = Phase.valueOf(tag.getString("Phase"));
        } catch (IllegalArgumentException e) {
            this.phase = Phase.INTRO;
        }
        this.tickCounter = tag.getInt("TickCounter");
        if (tag.hasUUID("BossUuid")) {
            this.bossUuid = tag.getUUID("BossUuid");
        }
        this.titleSent = tag.getBoolean("TitleSent");
        this.victorySent = tag.getBoolean("VictorySent");
        this.bossSpawned = tag.getBoolean("BossSpawned");
        this.totalEscorts = tag.getInt("TotalEscorts");
        this.escortsKilled = tag.getInt("EscortsKilled");
        this.escortsSpawned = tag.getBoolean("EscortsSpawned");
        if (tag.contains("BossDeathPos")) {
            NbtUtils.readBlockPos(tag, "BossDeathPos").ifPresent(p -> this.bossDeathPos = p);
        }
        this.defeatDelayTicks = tag.getInt("DefeatDelayTicks");
        this.portalSpawned = tag.getBoolean("PortalSpawned");
        this.lootShipSpawned = tag.getBoolean("LootShipSpawned");
        this.finished = tag.getBoolean("Finished");
    }

    public static BossIntroScript loadFromNbt(CompoundTag tag) {
        return new BossIntroScript(tag);
    }

    @Override
    public String typeId() {
        return TYPE_ID;
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.put("InstanceId", NbtUtils.createUUID(instanceId));
        tag.putString("NodeId", nodeId);
        tag.putString("StageDisplayName", stageDisplayName);
        tag.putString("StageId", stageId);
        tag.putString("EnemySetId", enemySetId);

        ListTag plist = new ListTag();
        for (UUID u : playerUuids) {
            plist.add(NbtUtils.createUUID(u));
        }
        tag.put("PlayerUuids", plist);

        tag.put("SpawnPos", NbtUtils.writeBlockPos(spawnPos));
        tag.putString("Phase", phase.name());
        tag.putInt("TickCounter", tickCounter);
        if (bossUuid != null) tag.putUUID("BossUuid", bossUuid);
        tag.putBoolean("TitleSent", titleSent);
        tag.putBoolean("VictorySent", victorySent);
        tag.putBoolean("BossSpawned", bossSpawned);
        tag.putInt("TotalEscorts", totalEscorts);
        tag.putInt("EscortsKilled", escortsKilled);
        tag.putBoolean("EscortsSpawned", escortsSpawned);
        if (bossDeathPos != null) {
            tag.put("BossDeathPos", NbtUtils.writeBlockPos(bossDeathPos));
        }
        tag.putInt("DefeatDelayTicks", defeatDelayTicks);
        tag.putBoolean("PortalSpawned", portalSpawned);
        tag.putBoolean("LootShipSpawned", lootShipSpawned);
        tag.putBoolean("Finished", finished);
    }

    @Override
    public boolean tick(ServerLevel dungeonLevel) {
        tickCounter++;
        Phase before = phase;
        boolean stateMutated = switch (phase) {
            case INTRO -> tickIntro(dungeonLevel);
            case BATTLE -> tickBattle(dungeonLevel);
            case DEFEATED -> tickDefeated(dungeonLevel);
            case OUTRO -> tickOutro(dungeonLevel);
            case COMPLETED -> false;
        };
        return stateMutated || phase != before;
    }

    // ========== Phase 1: INTRO ==========

    private boolean tickIntro(ServerLevel level) {
        boolean changed = false;

        // 首次进入：发送标题 + 生成 Boss
        if (!titleSent) {
            sendBossTitle(level);
            changed = true;
        }
        if (!bossSpawned) {
            spawnBoss(level);
            changed = true;
        }
        if (bossSpawned && tickCounter % 5 == 0) sendBossOverlay(level, true, false);

        // 验证 Boss 实体存活（服务端重启后重解析 UUID）
        Entity boss = resolveBoss(level);
        if (boss != null && boss.isAlive()) {
            // Boss 已就位，短暂延时后进入战斗
            if (tickCounter > 60) { // 3 秒延时让标题播放完
                phase = Phase.BATTLE;
                PiranPort.LOGGER.info("[BossIntro] Phase INTRO → BATTLE, instance={}", instanceId);
                changed = true;
            }
        } else if (bossSpawned && (boss == null || !boss.isAlive())) {
            // Boss 实体丢失或死亡（异常情况），直接进入战斗阶段让escort推进
            PiranPort.LOGGER.warn("[BossIntro] Boss entity missing/dead in INTRO, forcing BATTLE");
            phase = Phase.BATTLE;
            changed = true;
        }

        return changed;
    }

    private void sendBossTitle(ServerLevel level) {
        if (titleSent) return;
        titleSent = true;

        // 从 enemy_set 读取旗舰名称用于标题
        String bossName = resolveBossDisplayName();

        for (ServerPlayer player : getOnlinePlayers(level)) {
            // 动画：淡入 10 ticks，停留 40 ticks，淡出 10 ticks
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(bossName).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.translatable("dungeon.piranport.boss_intro.subtitle")
                            .withStyle(ChatFormatting.GOLD)));
        }
    }

    private void spawnBoss(ServerLevel level) {
        if (bossSpawned) return;

        EnemySetData enemySet = DungeonRegistry.INSTANCE.getEnemySet(enemySetId);
        if (enemySet == null || enemySet.flagship() == null) {
            PiranPort.LOGGER.error("[BossIntro] Enemy set '{}' not found or has no flagship", enemySetId);
            return;
        }

        // 使用 NodeBattleField.createEntity 创建旗舰实体
        Entity boss = NodeBattleField.createEntity(level, enemySet.flagship().entity());
        if (boss == null) {
            PiranPort.LOGGER.error("[BossIntro] Failed to create boss entity: {}", enemySet.flagship().entity());
            return;
        }

        // Boss 出生在玩家 spawn 前方 30 格
        boss.setPos(spawnPos.getX() + 0.5, DungeonConstants.SPAWN_Y, spawnPos.getZ() + 30);
        boss.addTag("dungeon_instance_" + instanceId);
        boss.addTag("dungeon_node_" + nodeId);
        boss.addTag("dungeon_flagship");
        boss.addTag("dungeon_boss_intro_target");

        level.addFreshEntity(boss);
        bossUuid = boss.getUUID();
        bossSpawned = true;

        PiranPort.LOGGER.info("[BossIntro] Boss spawned: {} at {} for instance {}",
                enemySet.flagship().entity(), boss.blockPosition(), instanceId);
    }

    // ========== Phase 2: BATTLE ==========

    private boolean tickBattle(ServerLevel level) {
        boolean changed = false;

        // 首次进入战斗：生成护卫敌群
        if (!escortsSpawned) {
            spawnEscorts(level);
            changed = true;
        }
        if (tickCounter % 5 == 0) sendBossOverlay(level, true, false);

        // 安全超时：10 分钟强制进入结算
        if (tickCounter > 12000 + 60 && !finished) {
            PiranPort.LOGGER.warn("[BossIntro] Battle timeout, forcing outro");
            bossDeathPos = spawnPos.offset(0, 0, 30);
            phase = Phase.DEFEATED;
            defeatDelayTicks = 0;
            changed = true;
        }

        return changed;
    }

    private void spawnEscorts(ServerLevel level) {
        if (escortsSpawned) return;

        EnemySetData enemySet = DungeonRegistry.INSTANCE.getEnemySet(enemySetId);
        if (enemySet == null) {
            PiranPort.LOGGER.warn("[BossIntro] Enemy set '{}' not found for escorts", enemySetId);
            escortsSpawned = true;
            return;
        }

        // 使用 FormationGenerator 如果有 formation 配置
        com.piranport.dungeon.instance.FormationGenerator.FormationResult formationResult =
                com.piranport.dungeon.instance.FormationGenerator.generateWithFleetGroup(
                        enemySet, spawnPos, level);

        if (formationResult != null) {
            // 有 formation 配置：按队形生成
            com.piranport.npc.ai.FleetGroup group = formationResult.group();
            for (com.piranport.dungeon.instance.FormationGenerator.SpawnPlan plan : formationResult.plans()) {
                if (plan.isLeader()) continue; // 跳过旗舰（Boss 已在 INTRO 生成）
                Entity entity = plan.entityType().create(level);
                if (entity != null) {
                    entity.setPos(plan.position().x, DungeonConstants.SPAWN_Y, plan.position().z);
                    entity.addTag("dungeon_instance_" + instanceId);
                    entity.addTag("dungeon_node_" + nodeId);
                    level.addFreshEntity(entity);
                    totalEscorts++;
                    if (entity instanceof com.piranport.npc.deepocean.AbstractDeepOceanEntity deep) {
                        com.piranport.npc.ai.FleetGroupManager.get(level)
                                .addMember(group.getGroupId(), deep.getUUID());
                    }
                }
            }
        } else {
            // 无 formation：沿用原有随机散布逻辑
            var rng = level.getRandom();
            for (EnemySetData.SpawnEntry entry : enemySet.spawnList()) {
                for (int i = 0; i < entry.count(); i++) {
                    Entity entity = NodeBattleField.createEntity(level, entry.entity());
                    if (entity != null) {
                        double angle = rng.nextDouble() * Math.PI * 2;
                        double dist = 20 + rng.nextDouble() * 25;
                        double ex = spawnPos.getX() + Math.cos(angle) * dist;
                        double ez = spawnPos.getZ() + Math.sin(angle) * dist;
                        entity.setPos(ex, DungeonConstants.SPAWN_Y, ez);
                        entity.addTag("dungeon_instance_" + instanceId);
                        entity.addTag("dungeon_node_" + nodeId);
                        level.addFreshEntity(entity);
                        totalEscorts++;
                    }
                }
            }
        }

        escortsSpawned = true;
        PiranPort.LOGGER.info("[BossIntro] Spawned {} escorts for instance {}", totalEscorts, instanceId);
    }

    // ========== Phase 3: DEFEATED ==========

    private boolean tickDefeated(ServerLevel level) {
        defeatDelayTicks++;
        if (defeatDelayTicks >= DEFEAT_DELAY_TICKS) {
            phase = Phase.OUTRO;
            PiranPort.LOGGER.info("[BossIntro] Phase DEFEATED → OUTRO, instance={}", instanceId);
            return true;
        }
        return false;
    }

    // ========== Phase 4: OUTRO ==========

    private boolean tickOutro(ServerLevel level) {
        boolean changed = false;

        if (!titleSent) {
            sendVictoryTitle(level);
            changed = true;
        }
        if (!lootShipSpawned && bossDeathPos != null) {
            spawnBossLootShip(level, bossDeathPos);
            lootShipSpawned = true;
            changed = true;
        }
        if (!portalSpawned && bossDeathPos != null) {
            spawnCompletionPortal(level, bossDeathPos);
            portalSpawned = true;
            changed = true;
        }

        if (portalSpawned && !finished) {
            finished = true;
            phase = Phase.COMPLETED;
            sendBossOverlay(level, false, false);
            sendActionBar(level, Component.translatable("dungeon.piranport.boss_intro.cleared"));
            PiranPort.LOGGER.info("[BossIntro] Outro complete, instance={}", instanceId);
            changed = true;
        }

        return changed;
    }

    private void sendVictoryTitle(ServerLevel level) {
        if (victorySent) return;
        victorySent = true;

        for (ServerPlayer player : getOnlinePlayers(level)) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.translatable("dungeon.piranport.boss_intro.victory_title")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal(stageDisplayName)
                            .withStyle(ChatFormatting.YELLOW)));
        }
    }

    private void spawnBossLootShip(ServerLevel level, BlockPos center) {
        double x = center.getX() + 0.5;
        double z = center.getZ() + 0.5;
        double y = DungeonConstants.SPAWN_Y;

        LootShipEntity lootShip = LootShipEntity.create(level, x, y, z, 3); // tier 3 = boss loot
        lootShip.setBossLootMarker(true);

        // 为每个玩家填充 Boss 奖励
        List<ItemStack> loot = new ArrayList<>();
        for (int i = 0; i < playerUuids.size(); i++) {
            loot.add(new ItemStack(ModItems.LARGE_GUN.get()));
            loot.add(new ItemStack(ModItems.LARGE_HE_SHELL.get(), 64));
            loot.add(new ItemStack(ModItems.LARGE_AP_SHELL.get(), 64));
            loot.add(new ItemStack(ModItems.EXP_SHELL.get(), 8));
            loot.add(new ItemStack(ModItems.QUICK_REPAIR.get(), 3));
            loot.add(new ItemStack(ModItems.FUEL.get(), 16));
        }
        lootShip.fillInventory(loot);
        level.addFreshEntity(lootShip);

        PiranPort.LOGGER.info("[BossIntro] Boss loot ship spawned at {}", center);
    }

    private void spawnCompletionPortal(ServerLevel level, BlockPos center) {
        if (portalSpawned) return;

        // 多方块传送门系统：构建 4x5 框架结构而非实体传送门
        com.piranport.dungeon.block.PortalStructureHelper.buildPortalStructure(
                level, center.below(), instanceId, nodeId);

        PiranPort.LOGGER.info("[BossIntro] Multi-block portal structure built at {}", center);
    }

    // ========== Entity Death Handling ==========

    @Override
    public boolean onEntityDeath(Entity entity) {
        if (finished || phase == Phase.COMPLETED) return false;

        // 检查是否为 Boss 实体
        if (entity.getTags().contains("dungeon_boss_intro_target")
                && entity.getTags().contains("dungeon_instance_" + instanceId)
                && entity.getTags().contains("dungeon_node_" + nodeId)) {
            bossDeathPos = entity.blockPosition();
            phase = Phase.DEFEATED;
            defeatDelayTicks = 0;
            PiranPort.LOGGER.info("[BossIntro] Boss defeated at {}, instance={}",
                    bossDeathPos, instanceId);
            if (entity.level() instanceof ServerLevel sl) {
                sendActionBar(sl, Component.translatable("dungeon.piranport.boss_intro.boss_defeated"));
                sl.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + 1.0, entity.getZ(),
                        18, 1.2, 0.5, 1.2, 0.04);
                sl.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + 1.0, entity.getZ(),
                        12, 0.9, 0.4, 0.9, 0.02);
                sl.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                        SoundSource.HOSTILE, 0.65f, 0.85f);
                sendBossOverlay(sl, true, true);
            }
            return true;
        }

        // 检查是否为护卫实体
        if (phase == Phase.BATTLE
                && entity.getTags().contains("dungeon_instance_" + instanceId)
                && entity.getTags().contains("dungeon_node_" + nodeId)
                && !entity.getTags().contains("dungeon_flagship")) {
            escortsKilled++;
            if (entity.level() instanceof ServerLevel sl) {
                int remaining = totalEscorts - escortsKilled;
                sendActionBar(sl, Component.translatable(
                        "dungeon.piranport.boss_intro.escorts",
                        escortsKilled, totalEscorts, remaining));
            }
            return true;
        }

        return false;
    }

    /** Boss 铭牌和血条由服务端权威状态同步，所有在节点内的玩家看到同一份信息。 */
    private void sendBossOverlay(ServerLevel level, boolean visible, boolean quietBattlefield) {
        Entity boss = resolveBoss(level);
        float health = boss instanceof net.minecraft.world.entity.LivingEntity living
                ? Math.max(0.0f, living.getHealth()) : 0.0f;
        float maxHealth = boss instanceof net.minecraft.world.entity.LivingEntity living
                ? Math.max(1.0f, living.getMaxHealth()) : 1.0f;
        int segment = visible ? Math.max(0, Math.min(4, (int) Math.ceil(health / maxHealth * 4.0f))) : 0;
        String shipType = resolveShipType();
        String chapter = "";
        var stage = DungeonRegistry.INSTANCE.getStage(stageId);
        if (stage != null) chapter = stage.chapter();
        DungeonBossOverlayPayload payload = new DungeonBossOverlayPayload(
                resolveBossDisplayName(), shipType, chapter, segment, health, maxHealth,
                visible, quietBattlefield);
        for (ServerPlayer player : getOnlinePlayers(level)) PacketDistributor.sendToPlayer(player, payload);
    }

    private String resolveShipType() {
        var enemySet = DungeonRegistry.INSTANCE.getEnemySet(enemySetId);
        if (enemySet == null || enemySet.flagship() == null) return "";
        String id = enemySet.flagship().entity();
        int colon = id.lastIndexOf(':');
        return (colon >= 0 ? id.substring(colon + 1) : id).replace('_', ' ');
    }

    // ========== Utilities ==========

    @Override
    public boolean isFinished() {
        return finished;
    }

    private Entity resolveBoss(ServerLevel level) {
        if (bossUuid == null) return null;
        Entity entity = level.getEntity(bossUuid);
        if (entity != null && !entity.isAlive()) return null;
        return entity;
    }

    private String resolveBossDisplayName() {
        EnemySetData enemySet = DungeonRegistry.INSTANCE.getEnemySet(enemySetId);
        if (enemySet != null && enemySet.flagship() != null) {
            // 尝试从实体类型获取显示名称
            String entityId = enemySet.flagship().entity();
            // 简化：直接用 entity ID 的最后部分作为名称
            int lastDot = entityId.lastIndexOf(':');
            String namePart = lastDot >= 0 ? entityId.substring(lastDot + 1) : entityId;
            return namePart.replace('_', ' ');
        }
        return "Unknown Boss";
    }

    private List<ServerPlayer> getOnlinePlayers(ServerLevel level) {
        List<ServerPlayer> result = new ArrayList<>();
        for (UUID uuid : playerUuids) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(uuid);
            if (player != null && DungeonEventHandler.isInDungeon(player)) {
                result.add(player);
            }
        }
        return result;
    }

    private void sendActionBar(ServerLevel level, Component message) {
        for (ServerPlayer player : getOnlinePlayers(level)) {
            player.displayClientMessage(message, true);
        }
    }
}
