package com.piranport.ammo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public record AmmoRecipe(
        String id,
        AmmoCategory category,
        String typeName,
        String caliberName,
        Supplier<Item> resultItem,
        int outputCount,
        List<MaterialRequirement> materials,
        int craftTimeTicks
) {
    public ItemStack getResultStack(int quantity) {
        return new ItemStack(resultItem.get(), outputCount * quantity);
    }

    public Component getResultName() {
        return resultItem.get().getDefaultInstance().getHoverName();
    }

    public record MaterialRequirement(Supplier<Item> item, int count) {
        public Component getDisplayName() {
            return item.get().getDefaultInstance().getHoverName();
        }

        public int getRequired(int quantity) {
            return count * quantity;
        }
    }
}
