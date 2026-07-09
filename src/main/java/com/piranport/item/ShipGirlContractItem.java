package com.piranport.item;

import com.piranport.advancement.ModAdvancements;
import com.piranport.npc.shipgirl.ShipGirlEntity;
import com.piranport.registry.ModEntityTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Contract reward that recruits a persistent ship girl NPC. */
public class ShipGirlContractItem extends TooltipItem {

    public ShipGirlContractItem(Properties properties, String tooltipKey) {
        super(properties, tooltipKey);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        ShipGirlEntity shipGirl = ModEntityTypes.SHIP_GIRL.get().create(level);
        if (shipGirl == null) {
            return InteractionResult.FAIL;
        }
        shipGirl.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                player.getYRot() + 180.0f, 0.0f);

        if (!level.noCollision(shipGirl)) {
            player.displayClientMessage(Component.translatable("message.piranport.ship_girl_contract_no_space"), true);
            return InteractionResult.FAIL;
        }

        level.addFreshEntity(shipGirl);
        ItemStack stack = context.getItemInHand();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ModAdvancements.award(player, "story/recruit_ship_girl");
        player.displayClientMessage(Component.translatable("message.piranport.ship_girl_recruited"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("tooltip.piranport.ship_girl_contract.use")
                .withStyle(ChatFormatting.AQUA));
    }
}
