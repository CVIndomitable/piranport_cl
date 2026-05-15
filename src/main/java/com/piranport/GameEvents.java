package com.piranport;

import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.debug.PiranPortCommands;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.entity.DungeonPortalEntity;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.script.DungeonScriptManager;
import com.piranport.handler.PlayerTickHandler;
import com.piranport.npc.ai.FleetGroupManager;
import com.piranport.registry.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class GameEvents {

    /**
     * 将武器类物品拾取定向到主背包（9-35格）而非快捷栏。
     * 主背包满时回退原版行为。
     */
    @SubscribeEvent
    public static void onWeaponPickup(ItemEntityPickupEvent.Pre event) {
        if (!ModCommonConfig.WEAPON_PICKUP_TO_INVENTORY.get()) return;
        Player player = event.getPlayer();
        ItemEntity itemEntity = event.getItemEntity();
        ItemStack stack = itemEntity.getItem();
        if (TransformationManager.getItemLoad(stack) <= 0) return;

        Inventory inv = player.getInventory();
        int total = stack.getCount();
        int taken = 0;

        // 第一轮：合并到主背包已有叠堆
        for (int i = 9; i < 36 && taken < total; i++) {
            ItemStack existing = inv.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int add = Math.min(space, total - taken);
                existing.grow(add);
                taken += add;
            }
        }

        // 第二轮：放入主背包空格
        for (int i = 9; i < 36 && taken < total; i++) {
            if (inv.getItem(i).isEmpty()) {
                int add = Math.min(stack.getMaxStackSize(), total - taken);
                inv.setItem(i, stack.copyWithCount(add));
                taken += add;
            }
        }

        if (taken == 0) return;

        if (player instanceof ServerPlayer sp) {
            sp.awardStat(Stats.ITEM_PICKED_UP.get(stack.getItem()), taken);
        }

        stack.shrink(taken);
        if (stack.isEmpty()) {
            itemEntity.discard();
        }

        event.setCanPickup(TriState.FALSE);
    }

    /** 每 tick 驱动地牢脚本 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel dungeonLevel = event.getServer().getLevel(
                DungeonEventHandler.DUNGEON_DIMENSION);
        if (dungeonLevel != null) {
            DungeonScriptManager.get(event.getServer()).tickAll(dungeonLevel);
        }

        // 舰队编队清理（每10分钟）
        if (event.getServer().getTickCount() % 12000 == 0) {
            ServerLevel overworld = event.getServer().overworld();
            FleetGroupManager.get(overworld).cleanup(event.getServer());
        }

        // 地牢实例泄漏清理（每10分钟）
        if (event.getServer().getTickCount() % 12000 == 0 && dungeonLevel != null) {
            DungeonInstanceManager.get(event.getServer().overworld())
                    .sweepLeaks(dungeonLevel);
        }
    }

    /** 地牢脚本标记的生物死亡时通知脚本管理器 */
    @SubscribeEvent
    public static void onDungeonMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity entity = event.getEntity();
        if (!entity.getTags().contains("dungeon_script")) return;

        for (String tag : entity.getTags()) {
            if (tag.startsWith("dungeon_instance_")) {
                try {
                    UUID instanceId = UUID.fromString(
                            tag.substring("dungeon_instance_".length()));
                    DungeonScriptManager.get(sl.getServer())
                            .onEntityDeath(instanceId, entity);
                } catch (IllegalArgumentException ignored) {}
                break;
            }
        }
    }

    /** 地牢旗舰死亡时检查同节点所有旗舰，全灭则生成传送门 */
    @SubscribeEvent
    public static void onDungeonFlagshipDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        LivingEntity entity = event.getEntity();
        if (!entity.getTags().contains("dungeon_flagship")) return;

        UUID instanceId = null;
        String nodeId = null;
        for (String tag : entity.getTags()) {
            if (tag.startsWith("dungeon_instance_")) {
                try {
                    instanceId = UUID.fromString(tag.substring("dungeon_instance_".length()));
                } catch (IllegalArgumentException ignored) {}
            } else if (tag.startsWith("dungeon_node_")) {
                nodeId = tag.substring("dungeon_node_".length());
            }
        }
        if (instanceId == null || nodeId == null) return;

        if (DungeonScriptManager.get(sl.getServer()).getScript(instanceId) != null) return;

        String matchTag = "dungeon_instance_" + instanceId;
        String nodeTag = "dungeon_node_" + nodeId;
        AABB scanBox = entity.getBoundingBox().inflate(200);
        boolean anyFlagshipAlive = sl.getEntitiesOfClass(LivingEntity.class, scanBox,
                e -> e != entity && e.isAlive()
                        && e.getTags().contains("dungeon_flagship")
                        && e.getTags().contains(matchTag)
                        && e.getTags().contains(nodeTag)).stream().findAny().isPresent();

        if (!anyFlagshipAlive) {
            DungeonInstanceManager mgr = DungeonInstanceManager.get(sl);
            DungeonInstance instance = mgr.getInstance(instanceId);
            if (instance == null) return;

            BlockPos portalPos = entity.blockPosition();
            DungeonPortalEntity portal = DungeonPortalEntity.create(
                    sl, instanceId, nodeId,
                    portalPos.getX() + 0.5,
                    DungeonConstants.SPAWN_Y,
                    portalPos.getZ() + 0.5);
            if (portal != null) {
                sl.addFreshEntity(portal);
            }

            for (UUID playerUuid : instance.getPlayerUuids()) {
                ServerPlayer player = sl.getServer().getPlayerList().getPlayer(playerUuid);
                if (player != null) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "dungeon.piranport.node_cleared"), true);
                }
            }
        }
    }

    /** 经验提升Buff：怪物掉落经验 +50% */
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        Player attacker = event.getAttackingPlayer();
        if (attacker != null && attacker.hasEffect(ModMobEffects.EXPERIENCE_BOOST)) {
            int original = event.getDroppedExperience();
            event.setDroppedExperience((int) (original * 1.5));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        PiranPortCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FireControlManager.clearAll();
        ReconManager.clearAll();
        AircraftIndex.clearAll();
        TorpedoGuidanceManager.clearAll();
        PlayerTickHandler.clearCaches();
    }
}
