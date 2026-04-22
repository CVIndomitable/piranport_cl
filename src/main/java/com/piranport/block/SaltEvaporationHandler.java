package com.piranport.block;

import com.piranport.PiranPort;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * Water above a lit furnace gradually evaporates into salt.
 * <ul>
 *   <li>Water source → salt block</li>
 *   <li>Flowing water → salt chip (thin layer)</li>
 * </ul>
 * Detection via {@link BlockEvent.NeighborNotifyEvent}, conversion via tick timer.
 * Timer state persists per-dimension in {@link SaltEvaporationData}.
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class SaltEvaporationHandler {

    /** Ticks until water converts to salt (10 seconds). */
    private static final int EVAPORATION_TICKS = 200;

    // ── Detection ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState sourceState = event.getState();
        BlockPos pos = event.getPos();
        // Only relevant if the changed block is water (self) or a furnace (below the tracked water)
        if (sourceState.is(Blocks.WATER)) {
            tryTrack(level, pos);
        } else if (sourceState.getBlock() instanceof AbstractFurnaceBlock) {
            tryTrack(level, pos.above());
        }
    }

    private static void tryTrack(ServerLevel level, BlockPos waterPos) {
        BlockState state = level.getBlockState(waterPos);
        if (!state.is(Blocks.WATER)) return;
        if (!isLitFurnace(level.getBlockState(waterPos.below()))) return;
        SaltEvaporationData data = SaltEvaporationData.get(level);
        if (!data.containsKey(waterPos)) {
            data.put(waterPos, EVAPORATION_TICKS);
        }
    }

    // ── Tick processing ────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            SaltEvaporationData data = SaltEvaporationData.get(level);
            Map<BlockPos, Integer> map = data.entries();
            if (map.isEmpty()) continue;

            boolean dirty = false;
            Iterator<Map.Entry<BlockPos, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = it.next();
                BlockPos pos = entry.getKey();

                // Skip unloaded chunks to avoid forcing chunk loads
                if (!level.isLoaded(pos)) continue;

                // Validate conditions still hold
                BlockState waterState = level.getBlockState(pos);
                if (!waterState.is(Blocks.WATER)
                        || !isLitFurnace(level.getBlockState(pos.below()))) {
                    it.remove();
                    dirty = true;
                    continue;
                }

                int remaining = entry.getValue() - 1;

                // Steam particles every second
                if (remaining % 20 == 0) {
                    level.sendParticles(ParticleTypes.CLOUD,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            2, 0.25, 0.15, 0.25, 0.01);
                }

                if (remaining <= 0) {
                    boolean isSource = waterState.getFluidState().isSource();
                    BlockState salt = isSource
                            ? ModBlocks.SALT_BLOCK.get().defaultBlockState()
                            : ModBlocks.SALT_CHIP.get().defaultBlockState();
                    level.setBlockAndUpdate(pos, salt);
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                            SoundSource.BLOCKS, 0.5f, 1.2f);
                    it.remove();
                    dirty = true;
                } else {
                    entry.setValue(remaining);
                }
            }
            if (dirty) data.setDirty();
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private static boolean isLitFurnace(BlockState state) {
        return state.getBlock() instanceof AbstractFurnaceBlock
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }
}
