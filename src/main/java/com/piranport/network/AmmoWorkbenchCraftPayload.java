package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.ammo.AmmoRecipe;
import com.piranport.ammo.AmmoRecipeRegistry;
import com.piranport.block.entity.AmmoWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
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

public record AmmoWorkbenchCraftPayload(BlockPos pos, String recipeId, int quantity)
        implements CustomPacketPayload {

    public static final Type<AmmoWorkbenchCraftPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "ammo_workbench_craft"));

    public static final StreamCodec<FriendlyByteBuf, AmmoWorkbenchCraftPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, p) -> { buf.writeBlockPos(p.pos); buf.writeUtf(p.recipeId); buf.writeVarInt(p.quantity); },
                    buf -> new AmmoWorkbenchCraftPayload(buf.readBlockPos(), buf.readUtf(256), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AmmoWorkbenchCraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Validate block entity
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.pos)) > 64.0) return;
            if (!(player.level().getBlockEntity(payload.pos) instanceof AmmoWorkbenchBlockEntity be)) return;
            if (be.isCrafting()) return;

            // Validate recipe
            AmmoRecipe recipe = AmmoRecipeRegistry.findById(payload.recipeId);
            if (recipe == null) return;

            int qty = Math.min(64, Math.max(1, payload.quantity));

            // Check output slot can hold result
            ItemStack pendingResult = recipe.getResultStack(qty);
            ItemStack existing = be.getItemHandler().getStackInSlot(AmmoWorkbenchBlockEntity.OUTPUT_SLOT);
            if (!existing.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(existing, pendingResult)) return;
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

            // Check all materials
            for (AmmoRecipe.MaterialRequirement mat : recipe.materials()) {
                int required = mat.getRequired(qty);
                int has = available.getOrDefault(mat.item().get(), 0);
                if (has < required) return;
            }

            // Consume materials from player inventory
            for (AmmoRecipe.MaterialRequirement mat : recipe.materials()) {
                int toConsume = mat.getRequired(qty);
                for (int i = 0; i < player.getInventory().getContainerSize() && toConsume > 0; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack.is(mat.item().get())) {
                        int take = Math.min(toConsume, stack.getCount());
                        stack.shrink(take);
                        toConsume -= take;
                    }
                }
            }

            // Start crafting
            be.startCrafting(payload.recipeId, qty);
        });
    }
}
