package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.registry.ModAttachmentTypes;
import com.piranport.rigging.RiggingLoadout;
import com.piranport.rigging.RiggingLoadoutResolver;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时把已变身玩家的舰装挂点快照写进 {@link ModAttachmentTypes#RIGGING_LOADOUT}。
 *
 * <p><b>为什么由服务端算</b>：远程玩家的背包不在客户端手里，客户端只读同步结果。
 *
 * <p><b>为什么不是每 tick 同步</b>：附件同步是整包广播，每 tick 发一次会给附近所有客户端
 * 持续制造无意义流量。这里每 {@link #SYNC_INTERVAL} tick 重建快照，先比指纹，
 * 内容真的变了才 {@code setData}（setData 才会触发同步）。
 *
 * <p><b>线程模型</b>：服务端主线程；缓存仍用 {@link ConcurrentHashMap}，
 * 与 {@link PlayerTickHandler} 一致，防御关服时的竞态。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class RiggingLoadoutHandler {

    /** 快照重建间隔（tick）。10 tick = 0.5 秒，跟手且不会成为每 tick 载荷。 */
    private static final int SYNC_INTERVAL = 10;

    /** 玩家 UUID → 上次已同步快照的指纹。 */
    private static final Map<UUID, Long> LAST_FINGERPRINT = new ConcurrentHashMap<>();

    private RiggingLoadoutHandler() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % SYNC_INTERVAL != 0) return;
        refresh(player);
    }

    private static void refresh(ServerPlayer player) {
        UUID id = player.getUUID();
        RiggingLoadout loadout = RiggingLoadoutResolver.resolve(player);

        if (loadout.isEmpty()) {
            // 未变身：清掉旧快照。首次登录时本来就没有附件，不必无意义地发一次移除包。
            if (player.hasData(ModAttachmentTypes.RIGGING_LOADOUT)) {
                player.removeData(ModAttachmentTypes.RIGGING_LOADOUT);
            }
            LAST_FINGERPRINT.remove(id);
            return;
        }

        long fingerprint = loadout.fingerprint();
        Long previous = LAST_FINGERPRINT.put(id, fingerprint);
        // 指纹相同也要确认附件还在：重生会换实体实例，缓存键还在但新实体没有附件。
        if (previous != null && previous == fingerprint
                && player.hasData(ModAttachmentTypes.RIGGING_LOADOUT)) {
            return;
        }
        player.setData(ModAttachmentTypes.RIGGING_LOADOUT.get(), loadout);
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_FINGERPRINT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LAST_FINGERPRINT.clear();
    }
}
