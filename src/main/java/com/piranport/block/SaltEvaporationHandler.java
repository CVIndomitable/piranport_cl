package com.piranport.block;

import com.piranport.PiranPort;
import com.piranport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Water above a lit furnace gradually evaporates into salt.
 * <ul>
 *   <li>Water source → salt block</li>
 *   <li>Flowing water → salt chip (thin layer)</li>
 * </ul>
 * Detection via {@link BlockEvent.NeighborNotifyEvent}, conversion via tick timer.
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID)
public class SaltEvaporationHandler {

    /** Ticks until water converts to salt (10 seconds). */
    private static final int EVAPORATION_TICKS = 200;

    /** dimension → (waterPos → remaining ticks). */
    private static final Map<ResourceKey<Level>, Map<BlockPos, Integer>> evaporating = new HashMap<>();

    // ── Detection ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onBlockUpdate(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        // The changed block might be water placed above a furnace
        tryTrack(level, pos);
        // Or a furnace that just became lit — check the block above
        tryTrack(level, pos.above());
    }

    private static void tryTrack(ServerLevel level, BlockPos waterPos) {
        BlockState state = level.getBlockState(waterPos);
        if (!state.is(Blocks.WATER)) return;
        if (!isLitFurnace(level.getBlockState(waterPos.below()))) return;
        evaporating.computeIfAbsent(level.dimension(), k -> new HashMap<>())
                .putIfAbsent(waterPos, EVAPORATION_TICKS);
    }

    // ── Tick processing ────────────────────────────────────────

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (evaporating.isEmpty()) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            Map<BlockPos, Integer> map = evaporating.get(level.dimension());
            if (map == null || map.isEmpty()) continue;

            Iterator<Map.Entry<BlockPos, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> entry = it.next();
                BlockPos pos = entry.getKey();

                // Validate conditions still hold
                BlockState waterState = level.getBlockState(pos);
                if (!waterState.is(Blocks.WATER)
                        || !isLitFurnace(level.getBlockState(pos.below()))) {
                    it.remove();
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
                } else {
                    entry.setValue(remaining);
                }
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private static boolean isLitFurnace(BlockState state) {
        return state.getBlock() instanceof AbstractFurnaceBlock
                && state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        evaporating.clear();
    }
}
