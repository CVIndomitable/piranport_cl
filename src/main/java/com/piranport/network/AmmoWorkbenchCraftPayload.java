package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.ammo.AmmoRecipe;
import com.piranport.ammo.AmmoRecipeRegistry;
import com.piranport.block.entity.AmmoWorkbenchBlockEntity;
import com.piranport.menu.AmmoWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public record AmmoWorkbenchCraftPayload(BlockPos pos, String recipeId, int quantity)
        implements CustomPacketPayload {

    public static final Type<AmmoWorkbenchCraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ammo_workbench_craft"));

    private static final int MAX_RECIPE_ID_LEN = 64;
    private static final int MAX_QUANTITY = 64;
    // Rate limit: one request per player per 10 ticks (~0.5s).
    private static final long MIN_TICKS_BETWEEN_REQUESTS = 10L;
    private static final Map<UUID, Long> LAST_REQUEST_TICK = new WeakHashMap<>();

    public static final StreamCodec<FriendlyByteBuf, AmmoWorkbenchCraftPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeUtf(p.recipeId, MAX_RECIPE_ID_LEN); buf.writeVarInt(p.quantity); },
                    buf -> new AmmoWorkbenchCraftPayload(buf.readBlockPos(), buf.readUtf(MAX_RECIPE_ID_LEN), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AmmoWorkbenchCraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Rate limiting
            long now = player.level().getGameTime();
            UUID uuid = player.getUUID();
            synchronized (LAST_REQUEST_TICK) {
                Long last = LAST_REQUEST_TICK.get(uuid);
                if (last != null && now - last < MIN_TICKS_BETWEEN_REQUESTS) return;
                LAST_REQUEST_TICK.put(uuid, now);
            }

            BlockPos pos = payload.pos;
            if (!player.level().isLoaded(pos)) return;
            int minY = player.level().getMinBuildHeight();
            int maxY = player.level().getMaxBuildHeight();
            if (pos.getY() < minY || pos.getY() > maxY) return;
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) > 64.0) return;

            // Must have the corresponding menu open and bound to this position.
            if (!(player.containerMenu instanceof AmmoWorkbenchMenu menu)) return;
            if (!menu.getBlockPos().equals(pos)) return;
            if (!menu.stillValid(player)) return;

            if (!(player.level().getBlockEntity(pos) instanceof AmmoWorkbenchBlockEntity be)) return;
            if (be.isCrafting()) return;

            // Validate recipe
            AmmoRecipe recipe = AmmoRecipeRegistry.findById(payload.recipeId);
            if (recipe == null) return;

            int qty = Math.min(MAX_QUANTITY, Math.max(1, payload.quantity));

            // Check output slot can hold result (same-item stackable or empty)
            ItemStack pendingResult = recipe.getResultStack(qty);
            ItemStack existing = be.getItemHandler().getStackInSlot(AmmoWorkbenchBlockEntity.OUTPUT_SLOT);
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItem(existing, pendingResult)) return;
                if (existing.getCount() + pendingResult.getCount() > existing.getMaxStackSize()) return;
            }

            // Count materials in player inventory
            Map<Item, Integer> available = new HashMap<>();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) {
                    available.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
            for (AmmoRecipe.MaterialRequirement mat : recipe.materials()) {
                int required = mat.getRequired(qty);
                int has = available.getOrDefault(mat.item().get(), 0);
                if (has < required) return;
            }

            // Transfer materials from player inventory into BE's pending buffer.
            NonNullList<ItemStack> taken = NonNullList.create();
            for (AmmoRecipe.MaterialRequirement mat : recipe.materials()) {
                int toConsume = mat.getRequired(qty);
                for (int i = 0; i < player.getInventory().getContainerSize() && toConsume > 0; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(mat.item().get())) {
                        int take = Math.min(toConsume, stack.getCount());
                        ItemStack movedPart = stack.copyWithCount(take);
                        stack.shrink(take);
                        taken.add(movedPart);
                        toConsume -= take;
                    }
                }
            }

            be.startCrafting(player, payload.recipeId, qty, taken);
        });
    }
}
