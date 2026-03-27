package com.piranport.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

public class GuidebookItem extends Item {

    private static final ResourceLocation BOOK_ID =
            ResourceLocation.fromNamespaceAndPath("piranport", "guidebook");

    public GuidebookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            if (ModList.get().isLoaded("patchouli")) {
                try {
                    Class<?> apiClass = Class.forName("vazkii.patchouli.api.PatchouliAPI");
                    Object api = apiClass.getMethod("get").invoke(null);
                    api.getClass()
                       .getMethod("openBookGUI", ServerPlayer.class, ResourceLocation.class)
                       .invoke(api, sp, BOOK_ID);
                } catch (Exception ignored) {}
            } else {
                sp.displayClientMessage(
                        Component.translatable("message.piranport.patchouli_required"), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
