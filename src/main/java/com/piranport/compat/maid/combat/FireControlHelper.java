package com.piranport.compat.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.piranport.aviation.FireControlManager;
import com.piranport.combat.TransformationManager;
import com.piranport.item.ShipCoreItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class FireControlHelper {
    private FireControlHelper() {}

    public static Optional<LivingEntity> getOwnerFireControlTarget(EntityMaid maid) {
        if (!(maid.getOwner() instanceof Player owner)) return Optional.empty();
        if (!(maid.level() instanceof ServerLevel serverLevel)) return Optional.empty();
        if (!hasTransformedCore(owner)) return Optional.empty();

        List<UUID> targetIds = FireControlManager.getTargets(owner.getUUID());
        if (targetIds == null || targetIds.isEmpty()) return Optional.empty();

        for (UUID id : targetIds) {
            if (id == null) continue;
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof LivingEntity living && living.isAlive() && !living.isRemoved()) {
                return Optional.of(living);
            }
        }
        return Optional.empty();
    }

    private static boolean hasTransformedCore(Player player) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(offhand)) return true;
        ItemStack mainhand = player.getMainHandItem();
        return mainhand.getItem() instanceof ShipCoreItem && TransformationManager.isTransformed(mainhand);
    }
}
