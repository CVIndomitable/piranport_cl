package com.piranport.handler;

import java.util.UUID;

import com.piranport.PiranPort;
import com.piranport.aviation.AircraftIndex;
import com.piranport.aviation.FireControlManager;
import com.piranport.aviation.ReconManager;
import com.piranport.combat.HitNotifier;
import com.piranport.combat.TorpedoGuidanceManager;
import com.piranport.config.ModCommonConfig;
import com.piranport.dungeon.network.DungeonRegistrySyncPayload;
import com.piranport.entitycore.EntityCoreState;
import com.piranport.network.RecallAllAircraftPayload;
import com.piranport.registry.ModItems;
import com.piranport.skin.SkinManager;
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

    /**
     * 精英损管：原版不死图腾之后判定——原版图腾仅手持（主手/副手）时在 die() 之前由
     * 原版先行结算；未触发时致死事件照常发出，本方法按背包槽位序号（0→40，含副手）
     * 扫描并消耗一个精英损管，抵消本次致命伤害。损管判定不识别原版不死图腾
     * （背包内未手持的图腾不参与本结算，仍只在手持时按原版规则生效）。
     * <p>
     * 副本维度同样生效：此处取消死亡后，DungeonEventHandler 的副本死亡流程（回讲台等）
     * 因事件已取消而不再接管，玩家原地以 1 血存活。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEliteDamageControl(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 按背包槽位序号扫描，消耗最先命中的精英损管
        Inventory inv = player.getInventory();
        int foundSlot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.ELITE_DAMAGE_CONTROL.get())) {
                foundSlot = i;
                break;
            }
        }

        com.piranport.debug.PiranPortDebug.event(
            "EliteDamageControl | entity={} isPlayer={} foundSlot={} eventCanceledBefore={}",
            event.getEntity().getUUID(),
            event.getEntity() instanceof ServerPlayer,
            foundSlot,
            event.isCanceled()
        );

        if (foundSlot < 0) {
            com.piranport.debug.PiranPortDebug.event("EliteDamageControl | 未找到精英损管，死亡继续");
            return;
        }

        // 消耗损管
        ItemStack stack = inv.getItem(foundSlot);
        stack.shrink(1);
        com.piranport.debug.PiranPortDebug.event(
            "EliteDamageControl | 消耗损管 slot={} 剩余={}",
            foundSlot,
            inv.getItem(foundSlot).getCount()
        );

        // 取消死亡事件
        event.setCanceled(true);
        com.piranport.debug.PiranPortDebug.event(
            "EliteDamageControl | 事件已取消，health={} maxHealth={}",
            player.getHealth(),
            player.getMaxHealth()
        );

        // 恢复至1血
        player.setHealth(1.0f);
        com.piranport.debug.PiranPortDebug.event("EliteDamageControl | 已设置health=1");

        // 添加效果包
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
        com.piranport.debug.PiranPortDebug.event("EliteDamageControl | 已添加效果包");

        // 播放图腾动画
        player.level().broadcastEntityEvent(player, (byte) 35);
        com.piranport.debug.PiranPortDebug.event(
            "EliteDamageControl | 图腾动画已广播，最终health={} isDead={}",
            player.getHealth(),
            player.isDeadOrDying()
        );
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
        EntityCoreState.syncAllEntityCoresToPlayer(joiner);
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
        com.piranport.combat.AASilenceManager.clear(player);

        // 清理玩家 Tick 缓存
        PlayerTickHandler.onPlayerLogout(uuid);

        // 清理飞机索引
        AircraftIndex.removePlayerAircraft(uuid);

        // 副本暂停/恢复由 DungeonEventHandler 统一处理，避免单个成员退出暂停整个实例。
        if (player instanceof ServerPlayer sp) {
            com.piranport.server.ScopingManager.handleDisconnect(sp);
        }

        // 登出通知
        HitNotifier.onPlayerLogout(uuid);
        RecallAllAircraftPayload.onPlayerDisconnect(uuid);

        // 整合版 §3.1：联机大厅与队长机制已作废（副本/10），不再清理 lobby。
    }
}
