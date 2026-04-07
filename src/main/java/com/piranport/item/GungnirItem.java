package com.piranport.item;

import com.piranport.entity.GungnirEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

/**
 * Gungnir (冈格尼尔) — thrown spear that auto-returns.
 * +6 attack damage, 512 durability, destroys leaves on contact.
 * Returns when hitting a non-leaf block or exceeding 10 blocks range.
 */
public class GungnirItem extends Item {

    public GungnirItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(ResourceLocation.fromNamespaceAndPath("piranport", "gungnir_damage"),
                                6.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            GungnirEntity entity = new GungnirEntity(level, player, stack.copy());
            entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 2.0f, 0.0f);
            level.addFreshEntity(entity);

            level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(),
                    SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        // Remove item from hand (entity carries it)
        player.setItemInHand(hand, ItemStack.EMPTY);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
