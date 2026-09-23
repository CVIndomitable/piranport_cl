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
        if (foundSlot < 0) return;

        // 消耗损管
        inv.getItem(foundSlot).shrink(1);

        // 取消死亡事件
        event.setCanceled(true);

        // 恢复生命值 — 强制置 1，并清除所有持续伤害状态
        player.setHealth(1.0f);
        player.clearFire();
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);

        // 效果包（抗性 II 30s + 防火 30s + 再生 II 6s）
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));

        // 播放原版图腾动画
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
        EntityCoreState.syncAllEntityCoresToPlayer(joiner);
        PacketDistributor.sendToPlayer(joiner, DungeonRegistrySyncPayload.fromRegistry());

        // 终端覆盖的客户端镜像是进程级静态态，只在「打开终端」与「重置」时刷新。
        // 不在这里推一次的话，联机下非 OP 玩家永远不会收到 sync：他背包里鱼雷的 tooltip
        // 显示的是基准航速，而实际飞行速度按覆盖值走，两边对不上。
        PacketDistributor.sendToPlayer(joiner, com.piranport.network.SyncTerminalOverridesPayload.from(
                com.piranport.terminal.TerminalOverridesSavedData.get(joiner.serverLevel())));

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

        // 舰娘形态的护甲/速度是 transient 属性修饰符，进程重启或重新登录后会丢，
        // 而变身的 DataComponent 标志位仍留在核心物品上。这里补一次重放，
        // 否则玩家会保持「显示为舰娘但没有任何舰装加成」的裸状态，直到下次手动变形。
        // 同时使载重缓存失效，让下一 tick 的 tick 循环重新计算一次（登录瞬间
        // 背包可能还没同步完，靠 tick 循环再兜一次）。
        ItemStack loginCore = com.piranport.combat.TransformationManager.findTransformedCore(joiner);
        if (!loginCore.isEmpty()) {
            PlayerTickHandler.invalidateLoadCache(joiner);
            com.piranport.combat.TransformationManager.applyTransformationAttributes(joiner, loginCore);
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
