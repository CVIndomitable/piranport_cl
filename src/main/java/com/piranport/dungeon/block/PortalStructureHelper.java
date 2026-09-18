package com.piranport.dungeon.block;

import com.piranport.PiranPort;
import com.piranport.dungeon.block.entity.DungeonPortalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Utility class for checking multi-block portal structure completeness.
 * Validates that a 4×5×4 frame surrounds a 2×3 interior opening.
 */
public class PortalStructureHelper {

    // Portal frame structure dimensions
    public static final int FRAME_WIDTH = 4;
    public static final int FRAME_HEIGHT = 5;
    public static final int FRAME_DEPTH = 4;
    public static final int PORTAL_WIDTH = 2;
    public static final int PORTAL_HEIGHT = 3;

    /**
     * Checks if a complete portal structure exists at the given position.
     * Tries multiple orientations to find a valid frame.
     *
     * @param level The level to check in
     * @param pos The position of one corner block of the frame
     * @return true if a complete portal structure is found
     */
    public static boolean isStructureComplete(Level level, BlockPos pos) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (isStructureCompleteAtOrientation(level, pos, facing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a portal structure exists with the given corner and facing direction.
     */
    private static boolean isStructureCompleteAtOrientation(Level level, BlockPos origin, Direction facing) {
        if (level == null || origin == null) {
            return false;
        }

        Direction right = facing.getClockWise();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    // Skip the interior opening (2x3 in the center)
                    boolean isInOpening =
                            x >= (FRAME_WIDTH - PORTAL_WIDTH) / 2 &&
                                    x < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH &&
                                    y >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 &&
                                    y < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;

                    mutable.set(origin.getX() + facing.getStepX() * x + right.getStepX() * z,
                            origin.getY() + y,
                            origin.getZ() + facing.getStepZ() * x + right.getStepZ() * z);

                    BlockState state = level.getBlockState(mutable);

                    if (isInOpening) {
                        if (!state.isAir()) {
                            return false;
                        }
                    } else {
                        // Frame must be made of dungeon portal blocks
                        if (!state.is(com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get())) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Finds the center position of the portal interior (where players teleport).
     *
     * @param level The level to check in
     * @param cornerPos The position of the bottom corner block
     * @return The center position of the portal opening, or null if not found
     */
    public static BlockPos getPortalCenter(Level level, BlockPos cornerPos) {
        if (level == null || cornerPos == null) {
            return null;
        }

        // Try all orientations
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = getPortalCenterAtOrientation(level, cornerPos, facing);
            if (center != null) {
                return center;
            }
        }
        return null;
    }

    /**
     * Gets the center position for a specific orientation.
     */
    private static BlockPos getPortalCenterAtOrientation(Level level, BlockPos origin, Direction facing) {
        if (level == null || origin == null) {
            return null;
        }

        // Verify structure is complete first
        if (!isStructureCompleteAtOrientation(level, origin, facing)) {
            return null;
        }

        Direction right = facing.getClockWise();

        // Calculate center of the opening
        int openingCenterX = (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH / 2;
        int openingCenterY = (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT / 2;

        return origin.relative(facing, openingCenterX)
                .relative(Direction.UP, openingCenterY)
                .relative(right, (FRAME_DEPTH - 1) / 2);
    }

    /**
     * Gets the AABB of the portal interior (for collision/entity detection).
     */
    public static AABB getPortalInteriorAABB(BlockPos cornerPos, Direction facing) {
        if (cornerPos == null) {
            return null;
        }

        BlockPos min = cornerPos
                .relative(facing, (FRAME_WIDTH - PORTAL_WIDTH) / 2)
                .relative(Direction.UP, (FRAME_HEIGHT - PORTAL_HEIGHT) / 2);

        BlockPos max = min.relative(facing, PORTAL_WIDTH)
                .relative(Direction.UP, PORTAL_HEIGHT)
                .relative(facing.getClockWise(), FRAME_DEPTH);

        return new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX(), max.getY(), max.getZ());
    }

    /**
     * Builds a complete dungeon portal structure at the given position.
     * Places a 4x5x4 frame of dungeon portal blocks with a 2x3 interior opening.
     * Also configures the center block's BlockEntity with instance and node data.
     *
     * @param level The level to place the structure in
     * @param cornerPos The position of the bottom corner block of the frame
     * @param instanceId The dungeon instance ID
     * @param nodeId The dungeon node ID
     * @return The center position of the portal interior, or null if placement failed
     */
    public static BlockPos buildPortalStructure(ServerLevel level, BlockPos cornerPos,
                                                UUID instanceId, String nodeId) {
        if (level == null || cornerPos == null || instanceId == null || nodeId == null) {
            return null;
        }

        // We need to determine the facing direction. Since we don't have a facing parameter,
        // we'll try all horizontal directions and pick the one that works.
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos center = tryBuildPortalStructure(level, cornerPos, facing, instanceId, nodeId);
            if (center != null) {
                return center;
            }
        }

        // If none of the horizontal orientations work, try placing with North facing
        // This means we should adjust the cornerPos to be at the right place
        return tryBuildPortalStructure(level, cornerPos, Direction.NORTH, instanceId, nodeId);
    }

    /**
     * Attempts to build a portal structure with a specific facing direction.
     * Returns the center of the portal interior on success, null on failure.
     */
    private static BlockPos tryBuildPortalStructure(ServerLevel level, BlockPos cornerPos,
                                                     Direction facing, UUID instanceId, String nodeId) {
        if (level == null || cornerPos == null) {
            return null;
        }

        // Check if we have enough space in all directions
        Direction right = facing.getClockWise();
        BlockPos maxPos = cornerPos.relative(facing, FRAME_WIDTH - 1)
                .relative(Direction.UP, FRAME_HEIGHT - 1)
                .relative(right, FRAME_DEPTH - 1);

        // Check all positions are loadable
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    BlockPos checkPos = cornerPos.relative(facing, x)
                            .relative(Direction.UP, y)
                            .relative(right, z);

                    // Skip interior for space check
                    boolean isInOpening =
                            x >= (FRAME_WIDTH - PORTAL_WIDTH) / 2 &&
                                    x < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH &&
                                    y >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 &&
                                    y < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;

                    if (!isInOpening) {
                        // Frame block should be replaceable
                        BlockState existing = level.getBlockState(checkPos);
                        if (!existing.isAir() && !existing.canBeReplaced()) {
                            return null;
                        }
                    }
                }
            }
        }

        // Place the frame blocks
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    boolean isInOpening =
                            x >= (FRAME_WIDTH - PORTAL_WIDTH) / 2 &&
                                    x < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH &&
                                    y >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 &&
                                    y < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;

                    BlockPos placePos = cornerPos.relative(facing, x)
                            .relative(Direction.UP, y)
                            .relative(right, z);

                    if (isInOpening) {
                        // Interior: clear any existing blocks
                        level.setBlock(placePos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        // Place dungeon portal block
                        level.setBlock(placePos,
                                com.piranport.registry.ModBlocks.DUNGEON_PORTAL.get().defaultBlockState(), 3);
                    }
                }
            }
        }

        // Configure the BlockEntity at the center of the portal
        // The center block is at the middle of the interior opening
        int openingCenterX = (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH / 2;
        int openingCenterY = (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT / 2;
        int openingCenterZ = (FRAME_DEPTH - 1) / 2;

        BlockPos centerPos = cornerPos.relative(facing, openingCenterX)
                .relative(Direction.UP, openingCenterY)
                .relative(right, openingCenterZ);

        // Find the nearest dungeon portal block BlockEntity and configure it
        // The interior blocks don't have BlockEntities (they're air), so we need to
        // configure one of the surrounding frame blocks.
        // For simplicity, we'll configure the BlockEntity at the bottom-center of the frame.

        BlockPos bePos = cornerPos.relative(facing, (FRAME_WIDTH - PORTAL_WIDTH) / 2)
                .relative(right, (FRAME_DEPTH - 1) / 2);

        if (level.getBlockEntity(bePos) instanceof DungeonPortalBlockEntity portalBE) {
            portalBE.setDungeonData(instanceId, nodeId);
            portalBE.checkStructure(); // Re-check to mark as complete
            return centerPos;
        }

        // If the BE isn't at the expected position, search for it
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    boolean isInOpening =
                            x >= (FRAME_WIDTH - PORTAL_WIDTH) / 2 &&
                                    x < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH &&
                                    y >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 &&
                                    y < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;

                    if (!isInOpening) {
                        BlockPos checkPos = cornerPos.relative(facing, x)
                                .relative(Direction.UP, y)
                                .relative(right, z);

                        if (level.getBlockEntity(checkPos) instanceof DungeonPortalBlockEntity portalBE) {
                            portalBE.setDungeonData(instanceId, nodeId);
                            portalBE.checkStructure();
                            return centerPos;
                        }
                    }
                }
            }
        }

        PiranPort.LOGGER.warn("Failed to find DungeonPortalBlockEntity at portal structure {}", cornerPos);
        return centerPos;
    }
}