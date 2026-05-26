package com.piranport.handler;

import java.util.UUID;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.HitNotifier;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.dungeon.network.DungeonRegistrySyncPayload;
import com.piranport.network.RecallAllAircraftPayload;
import com.piranport.registry.ModItems;
import com.piranport.skin.SkinManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class PlayerConnectionHandler {

    /** 精英损管：背包内有损管时抵消致命伤害 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEliteDamageControl(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        Inventory inv = player.getInventory();
        int foundSlot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.ELITE_DAMAGE_CONTROL.get())) {
                foundSlot = i;
                break;
            }
        }
        if (foundSlot < 0) return;

        inv.getItem(foundSlot).shrink(1);
        event.setCanceled(true);
        player.setHealth(1.0f);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        player.level().broadcastEntityEvent(player, (byte) 35);
    }

    /** 玩家死亡时召回所有战机 */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.isCanceled()) return;
        PlayerAircraftHelper.recallAircraftForPlayer(player);
    }

    /** 维度切换时召回所有战机 */
    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        PlayerAircraftHelper.recallAircraftForPlayer(player);
    }

    /** 登录时同步皮肤、发放指南书、同步地牢注册表、清除残留减速 */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joiner)) return;
        SkinManager.syncAllSkinsToPlayer(joiner);
        PacketDistributor.sendToPlayer(joiner, DungeonRegistrySyncPayload.fromRegistry());

        var slowness = joiner.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (slowness != null && slowness.getAmplifier() >= 9) {
            joiner.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        if (ModCommonConfig.GIVE_GUIDEBOOK_ON_FIRST_JOIN.get()) {
            CompoundTag persisted = joiner.getPersistentData();
            String tag = PlayerDataHelper.NBT_KEY_RECEIVED_GUIDEBOOK;
            if (!persisted.getBoolean(tag)) {
                persisted.putBoolean(tag, true);
                ItemStack guidebook = new ItemStack(ModItems.GUIDEBOOK.get());
                if (!joiner.getInventory().add(guidebook)) {
                    joiner.drop(guidebook, false);
                }
            }
        }
    }

    /** 登出时召回战机、清理战斗状态、缓存和讲台大厅 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        UUID uuid = player.getUUID();

        // 先召回飞机，防止飞机自动战斗逻辑重新设置火控目标
        PlayerAircraftHelper.recallAircraftForPlayer(player);

        // 清理战斗系统状态
        FireControlManager.clearTargets(uuid);
        ReconManager.endRecon(uuid);
        TorpedoGuidanceManager.endGuidance(uuid);

        // 清理玩家 Tick 缓存
        PlayerTickHandler.onPlayerLogout(uuid);

        // 清理飞机索引
        AircraftIndex.removePlayerAircraft(uuid);

        // 清理瞄准状态和副本状态（合并重复的 instanceof 检查）
        if (player instanceof ServerPlayer sp) {
            com.piranport.server.ScopingManager.handleDisconnect(sp);
            DungeonInstanceManager mgr = DungeonInstanceManager.get(sp.serverLevel());
            mgr.handlePlayerDisconnect(uuid);
        }

        // 登出通知
        HitNotifier.onPlayerLogout(uuid);
        RecallAllAircraftPayload.onPlayerDisconnect(uuid);

        // 清理讲台大厅
        var lobbyMgr = DungeonLobbyManager.INSTANCE;
        GlobalPos lecternPos = lobbyMgr.findLobbyOf(player.getUUID());
        if (lecternPos != null && player.getServer() != null) {
            lobbyMgr.leaveLobby(lecternPos, player.getUUID());
            lobbyMgr.broadcastLobbyUpdate(player.getServer(), lecternPos);
        }
    }
}
