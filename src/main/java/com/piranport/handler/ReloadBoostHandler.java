package com.piranport.handler;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Applies Reload Boost to vanilla bow/crossbow draw progress. */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class ReloadBoostHandler {
    private static final Map<UseKey, UseState> ACTIVE_DRAWS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isDrawWeapon(event.getItem())) return;

        ACTIVE_DRAWS.put(keyFor(player, event.getHand()),
                new UseState(player.level().getGameTime(), event.getDuration()));
    }

    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UseKey key = keyFor(player, event.getHand());
        UseState state = ACTIVE_DRAWS.get(key);
        if (state == null) return;

        if (!isDrawWeapon(event.getItem())) {
            ACTIVE_DRAWS.remove(key);
            return;
        }

        int actualElapsed = state.actualElapsed() + 1;
        int effectiveElapsed = Math.max(state.effectiveElapsed(),
                TransformationManager.boostedUseProgress(player, actualElapsed));
        int remainingAfterTick = Math.max(0, state.baseDuration() - effectiveElapsed);

        ACTIVE_DRAWS.put(key, new UseState(state.startTick(), state.baseDuration(), actualElapsed, effectiveElapsed));
        event.setDuration(Math.max(1, remainingAfterTick + 1));
    }

    @SubscribeEvent
    public static void onUseStop(LivingEntityUseItemEvent.Stop event) {
        if (event.getEntity() instanceof Player player) {
            ACTIVE_DRAWS.remove(keyFor(player, event.getHand()));
        }
    }

    @SubscribeEvent
    public static void onUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player player) {
            ACTIVE_DRAWS.remove(keyFor(player, event.getHand()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.getEntity().getUUID();
        ACTIVE_DRAWS.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    private static boolean isDrawWeapon(ItemStack stack) {
        UseAnim anim = stack.getUseAnimation();
        return anim == UseAnim.BOW || anim == UseAnim.CROSSBOW;
    }

    private static UseKey keyFor(Player player, InteractionHand hand) {
        return new UseKey(player.getUUID(), hand, player.level().isClientSide());
    }

    private record UseKey(UUID playerId, InteractionHand hand, boolean clientSide) {}

    private record UseState(long startTick, int baseDuration, int actualElapsed, int effectiveElapsed) {
        private UseState(long startTick, int baseDuration) {
            this(startTick, baseDuration, 0, 0);
        }
    }
}
