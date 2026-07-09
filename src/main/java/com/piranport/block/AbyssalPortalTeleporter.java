package com.piranport.block;

import com.piranport.advancement.ModAdvancements;
import com.piranport.registry.ModBlocks;
import com.piranport.worldgen.AbyssalDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public final class AbyssalPortalTeleporter {
    private static final int PORTAL_COOLDOWN_TICKS = 100;
    private static final int ABYSSAL_ENTRY_Y = 72;

    private AbyssalPortalTeleporter() {
    }

    public static boolean teleport(ServerPlayer player) {
        if (player.isOnPortalCooldown()) {
            return false;
        }

        ServerLevel currentLevel = player.serverLevel();
        boolean returning = currentLevel.dimension().equals(AbyssalDimensions.ABYSSAL_WORLD);
        ServerLevel targetLevel = returning
                ? player.server.overworld()
                : player.server.getLevel(AbyssalDimensions.ABYSSAL_WORLD);
        if (targetLevel == null) {
            return false;
        }

        BlockPos destination = returning
                ? overworldDestination(targetLevel, player)
                : abyssalDestination(player);
        prepareLanding(targetLevel, destination);

        player.setPortalCooldown(PORTAL_COOLDOWN_TICKS);
        player.teleportTo(targetLevel,
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5,
                player.getYRot(),
                player.getXRot());
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.fallDistance = 0.0f;
        if (!returning) {
            ModAdvancements.award(player, "story/enter_abyssal_world");
        }
        return true;
    }

    private static BlockPos abyssalDestination(Entity entity) {
        return new BlockPos(entity.getBlockX(), ABYSSAL_ENTRY_Y, entity.getBlockZ());
    }

    private static BlockPos overworldDestination(ServerLevel overworld, Entity entity) {
        int x = entity.getBlockX();
        int z = entity.getBlockZ();
        int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
        return new BlockPos(x, y, z);
    }

    private static void prepareLanding(ServerLevel level, BlockPos center) {
        BlockPos floor = center.below();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floorPos = floor.offset(dx, 0, dz);
                level.setBlock(floorPos, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                for (int dy = 0; dy <= 3; dy++) {
                    BlockPos airPos = center.offset(dx, dy, dz);
                    if (!airPos.equals(center.north(2)) && !airPos.equals(center.north(2).above())) {
                        level.setBlock(airPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        BlockPos portalBase = center.north(2);
        level.setBlock(portalBase, ModBlocks.ABYSSAL_PORTAL.get().defaultBlockState(), 3);
        level.setBlock(portalBase.above(), ModBlocks.ABYSSAL_PORTAL.get().defaultBlockState(), 3);
    }
}
