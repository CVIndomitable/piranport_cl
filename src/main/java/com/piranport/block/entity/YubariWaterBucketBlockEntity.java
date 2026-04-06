package com.piranport.block.entity;

import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class YubariWaterBucketBlockEntity extends BlockEntity {

    private static final int PUSH_INTERVAL = 20; // every second
    private static final int PUSH_AMOUNT = 1000; // 1 bucket per push per face
    private int tickCounter;

    public YubariWaterBucketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.YUBARI_WATER_BUCKET.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, YubariWaterBucketBlockEntity be) {
        if (++be.tickCounter < PUSH_INTERVAL) return;
        be.tickCounter = 0;

        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.relative(dir);
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, adjacent, dir.getOpposite());
            if (handler != null) {
                handler.fill(new FluidStack(Fluids.WATER, PUSH_AMOUNT), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    /**
     * Exposed as a capability so other mods' pipes can extract water from this block.
     */
    public IFluidHandler getFluidHandler(Direction side) {
        return new InfiniteWaterHandler();
    }

    private static class InfiniteWaterHandler implements IFluidHandler {
        private static final FluidStack WATER = new FluidStack(Fluids.WATER, Integer.MAX_VALUE);

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tank) { return WATER.copy(); }

        @Override
        public int getTankCapacity(int tank) { return Integer.MAX_VALUE; }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) { return false; }

        @Override
        public int fill(FluidStack resource, FluidAction action) { return 0; }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.getFluid().isSame(Fluids.WATER)) {
                return new FluidStack(Fluids.WATER, resource.getAmount());
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return new FluidStack(Fluids.WATER, maxDrain);
        }
    }
}
