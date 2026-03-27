package com.piranport;

import com.piranport.aviation.ClientFireControlData;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import com.piranport.network.CycleWeaponPayload;
import com.piranport.network.FireControlPayload;
import com.piranport.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ClientTickHandler {

    private static final double FIRE_CONTROL_RANGE = 40.0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Weapon cycling
        while (ClientEvents.CYCLE_WEAPON_KEY.consumeClick()) {
            ItemStack hand = mc.player.getMainHandItem();
            if (hand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(hand)) {
                PacketDistributor.sendToServer(new CycleWeaponPayload());
            }
        }

        // Fire control — only while transformed
        boolean transformed = mc.player.getMainHandItem().getItem() instanceof ShipCoreItem sci
                && TransformationManager.isTransformed(mc.player.getMainHandItem());

        while (ModKeyMappings.FIRE_CONTROL_LOCK.consumeClick()) {
            if (!transformed) continue;
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);
            if (target != null) {
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.LOCK, target.getUUID()));
            }
        }

        while (ModKeyMappings.FIRE_CONTROL_ADD.consumeClick()) {
            if (!transformed) continue;
            Entity target = getTargetInCrosshair(mc, FIRE_CONTROL_RANGE);
            if (target != null) {
                PacketDistributor.sendToServer(new FireControlPayload(
                        FireControlPayload.FireAction.ADD, target.getUUID()));
            }
        }

        while (ModKeyMappings.FIRE_CONTROL_CANCEL.consumeClick()) {
            PacketDistributor.sendToServer(FireControlPayload.cancel());
            ClientFireControlData.clear(); // local optimistic clear
        }
    }

    /**
     * Ray-cast from the player's eyes along the look vector up to {@code range} blocks.
     * Returns the first LivingEntity hit (excluding the player itself), or null.
     */
    @Nullable
    private static Entity getTargetInCrosshair(Minecraft mc, double range) {
        if (mc.player == null || mc.level == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookDir = mc.player.getLookAngle();
        Vec3 end = eyePos.add(lookDir.scale(range));

        AABB searchBox = mc.player.getBoundingBox()
                .expandTowards(lookDir.scale(range))
                .inflate(1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.level, mc.player, eyePos, end, searchBox,
                e -> e instanceof LivingEntity && e.isAlive() && e != mc.player,
                0.0f);

        return hit != null ? hit.getEntity() : null;
    }
}
