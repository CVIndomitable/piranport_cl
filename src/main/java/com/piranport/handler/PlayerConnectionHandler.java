package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.combat.HitNotifier;
import com.piranport.config.ModCommonConfig;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.dungeon.network.DungeonRegistrySyncPayload;
import com.piranport.network.RecallAllAircraftPayload;
import com.piranport.registry.ModItems;
import com.piranport.skin.SkinManager;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
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
            String tag = "piranport:received_guidebook";
            if (!persisted.getBoolean(tag)) {
                persisted.putBoolean(tag, true);
                ItemStack guidebook = new ItemStack(ModItems.GUIDEBOOK.get());
                if (!joiner.getInventory().add(guidebook)) {
                    joiner.drop(guidebook, false);
                }
            }
        }
    }

    /** 登出时召回战机、清理缓存和讲台大厅 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        HitNotifier.onPlayerLogout(player.getUUID());
        RecallAllAircraftPayload.onPlayerDisconnect(player.getUUID());
        PlayerAircraftHelper.recallAircraftForPlayer(player);
        PlayerTickHandler.onPlayerLogout(player.getUUID());
        var lobbyMgr = DungeonLobbyManager.INSTANCE;
        GlobalPos lecternPos = lobbyMgr.findLobbyOf(player.getUUID());
        if (lecternPos != null && player.getServer() != null) {
            lobbyMgr.leaveLobby(lecternPos, player.getUUID());
            lobbyMgr.broadcastLobbyUpdate(player.getServer(), lecternPos);
        }
    }
}
