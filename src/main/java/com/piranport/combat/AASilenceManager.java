package com.piranport.combat;

import com.piranport.item.ShipCoreItem;
import com.piranport.network.AASilenceSyncPayload;
import com.piranport.registry.ModAttachmentTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** 大口径开火或航空起降后静默 5 秒；只压制自动防空，不阻塞玩家主动作。 */
public final class AASilenceManager {
    public static final int SILENCE_DURATION_TICKS = AASilenceState.DURATION_TICKS;

    private AASilenceManager() {}

    /** 重复开火或起降从最新一次动作重新计时。 */
    public static void startSilence(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !TransformationManager.isPlayerTransformed(player)) return;
        AASilenceState state = player.getData(ModAttachmentTypes.AA_SILENCE.get());
        boolean wasActive = state.isActive(player.level().getGameTime());
        state.start(player.level().getGameTime());
        PacketDistributor.sendToPlayer(serverPlayer, new AASilenceSyncPayload(state.endsAt()));
        if (!wasActive) {
            serverPlayer.playNotifySound(SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.PLAYERS, 0.6f, 0.7f);
        }
    }

    public static boolean isSilenced(Player player) {
        return player != null && player.getData(ModAttachmentTypes.AA_SILENCE.get())
                .isActive(player.level().getGameTime());
    }

    /** 到期后清除状态；计时使用世界 tick，避免调用顺序导致少静默一 tick。 */
    public static void tickDown(Player player) {
        if (player == null || player.level().isClientSide()) return;
        AASilenceState state = player.getData(ModAttachmentTypes.AA_SILENCE.get());
        if (state.endsAt() != 0L && !state.isActive(player.level().getGameTime())) clear(player);
    }

    public static void clear(Player player) {
        if (player == null) return;
        AASilenceState state = player.getData(ModAttachmentTypes.AA_SILENCE.get());
        if (state.endsAt() == 0L) return;
        state.clear();
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new AASilenceSyncPayload(0L));
        }
    }

    public static void onCannonFire(Player player, ItemStack ammoFired) {
        if (ammoFired != null && ammoFired.is(ShipCoreItem.LARGE_SHELLS)) startSilence(player);
    }
}
