package com.piranport.combat.cannon;

import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.registry.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative one-shot press and held-fire sequence. */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public final class CannonHoldFireManager {
    private static final int HOLD_TIMEOUT_TICKS = 20;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final SalvoContext context;
        final CannonHoldCadence cadence;
        long lastUpdate;
        Vec3 target;
        int lastFiredSlot = -1;

        Session(ServerPlayer player, ArtilleryItem cannon, Vec3 target) {
            context = new SalvoContext(player, player.serverLevel(), player.getMainHandItem());
            lastUpdate = player.serverLevel().getGameTime();
            cadence = new CannonHoldCadence(lastUpdate, cannon.getEffectiveData(player.serverLevel()).salvoInterval());
            this.target = target;
        }
    }

    private CannonHoldFireManager() {}

    public static void start(ServerPlayer player, Vec3 target) {
        stop(player);
        if (!player.isAlive() || player.isSpectator()
                || !(player.getMainHandItem().getItem() instanceof ArtilleryItem cannon)
                || TransformationManager.findTransformedCore(player).isEmpty()) return;
        Session session = new Session(player, cannon, target);
        SESSIONS.put(player.getUUID(), session);
        if (!fireOne(player, session)) session.cadence.stop();
    }

    public static void update(ServerPlayer player, Vec3 target) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !valid(player, session)) return;
        session.lastUpdate = player.serverLevel().getGameTime();
        session.target = target;
        session.cadence.confirmHold();
    }

    public static void stop(ServerPlayer player) {
        SESSIONS.remove(player.getUUID());
    }

    private static boolean valid(ServerPlayer player, Session session) {
        return session.context.isValid(player, player.serverLevel(), player.getMainHandItem(),
                player.isAlive(), player.isSpectator())
                && !TransformationManager.findTransformedCore(player).isEmpty();
    }

    private static boolean fireOne(ServerPlayer player, Session session) {
        ItemStack core = TransformationManager.findTransformedCore(player);
        int[] slot = session.lastFiredSlot < 0
                ? CannonInventory.findBestArtillerySlot(player, player.getMainHandItem().getItem(), core)
                : CannonInventory.findNextArtillerySlot(player, player.getMainHandItem().getItem(), session.lastFiredSlot);
        if (slot == null || slot[1] == -1) return false;
        ItemStack weapon = CannonInventory.weaponAt(player.getInventory(), slot[0]);
        LoadedAmmo before = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        CannonAim aim = session.target == null ? new CannonAim.MaxRange() : new CannonAim.Aimed(session.target);
        CannonFiring.fireLoadedCannon(player.serverLevel(), player, core, player.getInventory(),
                slot[0], slot[1], weapon, aim);
        LoadedAmmo after = weapon.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (after.count() >= before.count()) return false;
        session.lastFiredSlot = slot[0];
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || !valid(player, session)
                    || player.serverLevel().getGameTime() - session.lastUpdate > HOLD_TIMEOUT_TICKS) {
                it.remove();
                continue;
            }
            long now = player.serverLevel().getGameTime();
            if (!session.cadence.canFire(now)) continue;
            // Zero interval can fire all currently ready guns in this tick, bounded by inventory size.
            int limit = session.cadence.simultaneous() ? player.getInventory().items.size() + 1 : 1;
            for (int i = 0; i < limit; i++) {
                if (!fireOne(player, session)) {
                    session.cadence.stop();
                    break;
                }
                session.cadence.fired(now);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SESSIONS.remove(event.getEntity().getUUID());
    }
}
