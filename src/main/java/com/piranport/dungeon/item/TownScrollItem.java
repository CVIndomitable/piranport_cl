package com.piranport.dungeon.item;

import com.piranport.aviation.ReconManager;
import com.piranport.dungeon.DungeonConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Town scroll — allows players to leave a dungeon instance.
 * One is automatically given when entering a dungeon.
 */
public class TownScrollItem extends Item {

    /**
     * Tracks recent server-side right-click intents per player. Only when this is
     * present and not yet expired will TownScrollUsePayload actually consume a scroll.
     * Prevents an unmodified-client-bypass where the payload alone could trigger a teleport.
     *
     * <p>Concurrent map: writes from item use (game thread) and reads from payload
     * handler (also game thread, via enqueueWork) — using CHM purely as a defensive
     * measure since item use & payload handlers both run on the main thread but
     * NeoForge does not formally guarantee mutual exclusion in all builds.</p>
     */
    private static final ConcurrentHashMap<UUID, Intent> PENDING = new ConcurrentHashMap<>();

    /** Records which slot / hand the player right-clicked, plus an expiry time. */
    public record Intent(int slot, long expiresAtMillis) {
        public boolean isValid() {
            return System.currentTimeMillis() <= expiresAtMillis;
        }
    }

    public TownScrollItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Block usage while in recon mode to prevent aircraft loss
        if (level.isClientSide()) {
            if (com.piranport.aviation.ClientReconData.isInReconMode()) {
                player.displayClientMessage(
                        Component.translatable("item.piranport.town_scroll.recon_blocked"), true);
                return InteractionResultHolder.fail(stack);
            }
            // Open confirmation screen client-side
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.piranport.dungeon.client.TownScrollScreen());
            return InteractionResultHolder.success(stack);
        }

        if (ReconManager.isInRecon(player.getUUID())) {
            return InteractionResultHolder.fail(stack);
        }

        // Record intent: which slot the player actually clicked. The payload
        // handler will consume *that* slot and refuse if no recent intent exists.
        int slot = player.getInventory().selected;
        if (hand == InteractionHand.OFF_HAND) {
            slot = net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND;
        }
        PENDING.put(player.getUUID(), new Intent(slot,
                System.currentTimeMillis() + DungeonConstants.TOWN_SCROLL_INTENT_WINDOW_MS));

        player.getCooldowns().addCooldown(this, DungeonConstants.TOWN_SCROLL_COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }

    /** Consume the recorded intent for the player; returns null if absent or expired. */
    public static Intent consumeIntent(UUID playerId) {
        Intent i = PENDING.remove(playerId);
        return (i != null && i.isValid()) ? i : null;
    }

    public static void clearIntent(UUID playerId) {
        PENDING.remove(playerId);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.piranport.town_scroll.tooltip"));
    }
}
