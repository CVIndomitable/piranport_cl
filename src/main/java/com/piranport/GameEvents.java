package com.piranport;

import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.AircraftEntity;
import com.piranport.item.ShipCoreItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class GameEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ShipCoreItem)) return;
        if (!TransformationManager.isTransformed(mainHand)) return;

        // 水面行走：脚踩水但眼睛未入水时，取消下沉速度
        if (player.isInWater() && !player.isEyeInFluid(FluidTags.WATER)) {
            Vec3 vel = player.getDeltaMovement();
            if (vel.y < 0) {
                player.setDeltaMovement(vel.x, 0.0, vel.z);
            }
            player.resetFallDistance();
        }
    }

    /** When a player dies, recall all their airborne aircraft. */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        recallAircraftForPlayer(player);
    }

    /** When a player logs out, recall all their airborne aircraft. */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        recallAircraftForPlayer(player);
    }

    private static void recallAircraftForPlayer(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        UUID ownerUUID = player.getUUID();
        AABB searchBox = AABB.ofSize(player.position(), 200, 200, 200);
        List<AircraftEntity> aircraft = serverLevel.getEntitiesOfClass(
                AircraftEntity.class, searchBox,
                a -> ownerUUID.equals(a.getOwnerUUID()));
        for (AircraftEntity a : aircraft) {
            a.recallAndRemove();
        }
        FireControlManager.clearTargets(player.getUUID());
    }
}
