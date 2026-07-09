package com.piranport.item;

import com.piranport.entitycore.EntityCoreDefinition;
import com.piranport.entitycore.EntityCoreDefinitions;
import com.piranport.entitycore.EntityCoreState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EntityCoreItem extends Item {
    private final int coreId;

    public EntityCoreItem(Properties properties, int coreId) {
        super(properties);
        this.coreId = coreId;
    }

    public int getCoreId() {
        return coreId;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            int currentCore = EntityCoreState.getActiveEntityCore(player);

            if (currentCore == coreId) {
                EntityCoreState.clearActiveEntityCore(sp);
                sp.displayClientMessage(Component.translatable("message.piranport.entity_core_removed"), true);
            } else {
                if (currentCore > 0) {
                    EntityCoreState.returnEntityCore(sp, currentCore);
                }
                EntityCoreState.setActiveEntityCore(sp, coreId);
                stack.shrink(1);
                sp.displayClientMessage(
                        Component.translatable("message.piranport.entity_core_applied", entityName()), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.piranport.entity_core", entityName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private Component entityName() {
        return EntityCoreDefinitions.get(coreId)
                .map(EntityCoreDefinition::displayName)
                .orElse(Component.translatable("entity.piranport.unknown"));
    }
}
