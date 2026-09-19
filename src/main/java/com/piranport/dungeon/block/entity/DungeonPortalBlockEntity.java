package com.piranport.dungeon.block.entity;

import com.piranport.PiranPort;
import com.piranport.dungeon.DungeonConstants;
import com.piranport.dungeon.event.DungeonEventHandler;
import com.piranport.dungeon.instance.DungeonInstance;
import com.piranport.dungeon.instance.DungeonInstanceManager;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Block entity for the dungeon portal block.
 * Tracks portal structure state and handles player teleportation when structure is complete.
 */
public class DungeonPortalBlockEntity extends BlockEntity {
    private static final VoxelShape PORTAL_SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    // Portal frame structure dimensions
    private static final int FRAME_WIDTH = 4;   // X dimension
    private static final int FRAME_HEIGHT = 5;  // Y dimension
    private static final int FRAME_DEPTH = 4;   // Z dimension
    private static final int PORTAL_WIDTH = 2;  // Interior opening width
    private static final int PORTAL_HEIGHT = 3; // Interior opening height

    // Portal state
    private boolean isComplete = false;
    private boolean isActive = false;
    private UUID instanceId = null;
    private String nodeId = null;
    private final Set<UUID> enteredPlayers = new HashSet<>();

    public DungeonPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DUNGEON_PORTAL.get(), pos, state);
    }

    /**
     * Checks if the portal frame structure is complete.
     * Should be called when neighboring blocks change.
     */
    public void checkStructure() {
        if (level == null || level.isClientSide()) {
            return;
        }

        boolean previouslyComplete = isComplete;
        isComplete = checkFrameStructure();
        isActive = isComplete && instanceId != null && nodeId != null;

        // If structure just became complete, notify nearby players
        if (isComplete && !previouslyComplete) {
            PiranPort.LOGGER.debug("Dungeon portal structure completed at {}", getBlockPos());
        }
    }

    /**
     * Checks if the frame structure around this block forms a valid portal.
     * Looks for a 4x5 frame of dungeon portal blocks surrounding a 2x3 opening.
     */
    private boolean checkFrameStructure() {
        // We need to check if we're part of a valid portal frame
        // For simplicity, we'll check from this block as a corner

        // Try different orientations - we'll check if this block could be a bottom corner
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            if (checkFrameAtOrientation(facing)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a portal frame exists with this block at the specified corner orientation.
     */
    private boolean checkFrameAtOrientation(Direction facing) {
        BlockPos origin = getBlockPos();

        // Check if we have the right dimensions for the frame
        // Frame: 4 wide, 5 tall, 4 deep
        // Opening: 2 wide, 3 tall in the center

        // Calculate frame corners
        Direction right = facing.getClockWise();
        Direction left = facing.getCounterClockWise();

        // Bottom front left corner of frame
        BlockPos frameOrigin = origin;

        // Check if we can place the full frame
        for (int x = 0; x < FRAME_WIDTH; x++) {
            for (int y = 0; y < FRAME_HEIGHT; y++) {
                for (int z = 0; z < FRAME_DEPTH; z++) {
                    // Skip the interior opening (2x3 in the middle)
                    boolean isInOpening =
                        x >= (FRAME_WIDTH - PORTAL_WIDTH) / 2 &&
                        x < (FRAME_WIDTH - PORTAL_WIDTH) / 2 + PORTAL_WIDTH &&
                        y >= (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 &&
                        y < (FRAME_HEIGHT - PORTAL_HEIGHT) / 2 + PORTAL_HEIGHT;

                    if (isInOpening) {
                        // Interior should be air or portal block
                        BlockPos checkPos = frameOrigin
                                .relative(facing, x)
                                .relative(Direction.UP, y)
                                .relative(right, z);

                        if (!isAirOrPortal(checkPos)) {
                            return false;
                        }
                    } else {
                        // Frame should be dungeon portal block
                        BlockPos checkPos = frameOrigin
                                .relative(facing, x)
                                .relative(Direction.UP, y)
                                .relative(right, z);

                        if (!isPortalFrameBlock(checkPos)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Checks if a position contains air or a portal block (for interior).
     */
    private boolean isAirOrPortal(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(ModBlocks.ABYSSAL_PORTAL.get());
    }

    /**
     * Checks if a position contains our dungeon portal block (for frame).
     */
    private boolean isPortalFrameBlock(BlockPos pos) {
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.DUNGEON_PORTAL.get());
    }

    /**
     * Checks if the portal is ready for activation (has instance and node ID).
     */
    public boolean isReadyForActivation() {
        return instanceId != null && nodeId != null && !nodeId.isEmpty();
    }

    /**
     * Activates the portal for a player.
     */
    public void activatePortal(Player player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!isReadyForActivation()) {
            player.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.missing_data"), true);
            return;
        }

        isActive = true;
        player.displayClientMessage(
                Component.translatable("block.piranport.dungeon_portal.activated"), true);
    }

    /**
     * Handles entity collision with the portal.
     * When a player enters an active portal, teleport them and track progression.
     */
    public void entityInside(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }

        if (!isActive || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        DungeonInstance instance = DungeonInstanceManager.get(serverLevel).getInstance(instanceId);
        if (instance != null && player instanceof ServerPlayer serverPlayer) {
            DungeonEventHandler.onPortalEntered(serverPlayer, instance, nodeId);
        }
    }

    /**
     * Teleports a player to the dungeon dimension.
     */
    private void teleportPlayerToDungeon(ServerLevel serverLevel, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (instanceId == null) {
            serverPlayer.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.no_instance"), true);
            return;
        }

        DungeonInstanceManager mgr = DungeonInstanceManager.get(serverLevel);
        DungeonInstance instance = mgr.getInstance(instanceId);

        if (instance == null) {
            player.displayClientMessage(
                    Component.translatable("block.piranport.dungeon_portal.instance_not_found"), true);
            return;
        }

        // Get the spawn position for this node
        BlockPos spawnPos = instance.getNodeSpawnPos(nodeId);

        // Teleport to dungeon dimension
        serverPlayer.teleportTo(serverLevel,
                spawnPos.getX() + 0.5,
                spawnPos.getY(),
                spawnPos.getZ() + 0.5,
                serverPlayer.getYRot(),
                serverPlayer.getXRot());

        player.displayClientMessage(
                Component.translatable("block.piranport.dungeon_portal.teleporting"), true);
    }

    /**
     * Checks if all players in the instance have entered the portal.
     * If so, calls DungeonEventHandler.onPortalComplete().
     */
    private void checkForCompletion(ServerLevel serverLevel) {
        if (instanceId == null) {
            return;
        }

        DungeonInstanceManager mgr = DungeonInstanceManager.get(serverLevel);
        DungeonInstance instance = mgr.getInstance(instanceId);

        if (instance == null) {
            return;
        }

        // Get all players that should be in this instance
        Set<UUID> instancePlayers = instance.getPlayerUuids();

        // Check if all online players in the instance have entered
        boolean allEntered = true;
        for (UUID playerId : instancePlayers) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(playerId);
            if (player != null &&
                player.level() == serverLevel &&  // Same dimension
                !enteredPlayers.contains(playerId)) {
                allEntered = false;
                break;
            }
        }

        if (allEntered && !enteredPlayers.isEmpty()) {
            PiranPort.LOGGER.info("All players entered dungeon portal at {}, advancing instance {}",
                    getBlockPos(), instanceId);

            // Call the event handler to advance the dungeon
            DungeonEventHandler.onPortalComplete(serverLevel, instance, nodeId);

            // Reset for next use
            enteredPlayers.clear();
            isActive = false;
        }
    }

    /**
     * Sets the dungeon instance and node ID for this portal.
     */
    public void setDungeonData(UUID instanceId, String nodeId) {
        this.instanceId = instanceId;
        this.nodeId = nodeId;
        this.isActive = isComplete && instanceId != null && nodeId != null;
    }

    /**
     * Gets the dungeon instance ID.
     */
    public UUID getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the node ID.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Checks if the portal structure is complete.
     */
    public boolean isStructureComplete() {
        return isComplete;
    }

    /**
     * Checks if the portal is active (complete and has data).
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Server tick - updates portal state and checks for player teleportation.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, DungeonPortalBlockEntity be) {
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) return;
        // Periodic structure check
        if (be.tickCounter % 100 == 0) {
            be.checkStructure();
        }
        be.tickCounter++;
    }

    private int tickCounter = 0;

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (instanceId != null) {
            tag.putUUID("InstanceId", instanceId);
        }
        if (nodeId != null) {
            tag.putString("NodeId", nodeId);
        }
        tag.putBoolean("IsComplete", isComplete);
        tag.putBoolean("IsActive", isActive);

        // Save entered players (limit to prevent excessive save size)
        if (!enteredPlayers.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            int count = 0;
            for (UUID uuid : enteredPlayers) {
                if (count >= 100) break; // Limit saved players
                CompoundTag entry = new CompoundTag();
                entry.putUUID("UUID", uuid);
                list.add(entry);
                count++;
            }
            tag.put("EnteredPlayers", list);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("InstanceId")) {
            instanceId = tag.getUUID("InstanceId");
        }
        if (tag.contains("NodeId", net.minecraft.nbt.Tag.TAG_STRING)) {
            nodeId = tag.getString("NodeId");
        }
        isComplete = tag.getBoolean("IsComplete");
        isActive = tag.getBoolean("IsActive");

        if (tag.contains("EnteredPlayers")) {
            net.minecraft.nbt.ListTag list = tag.getList("EnteredPlayers", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (entry.hasUUID("UUID")) {
                    enteredPlayers.add(entry.getUUID("UUID"));
                }
            }
        }
    }
}
