package com.piranport.ammo;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

public record AmmoRecipe(
        String id,
        AmmoCategory category,
        String typeName,
        String caliberName,
        Supplier<Item> resultItem,
        int outputCount,
        List<MaterialRequirement> materials,
        int craftTimeTicks,
        int totalMaterialValue,
        /** 合成所需蓝图；null = 不校验。蓝图不消耗，创造蓝图可替代。 */
        @Nullable Supplier<Item> requiredBlueprint
) {
    public ItemStack getResultStack(int quantity) {
        return new ItemStack(resultItem.get(), outputCount * quantity);
    }

    public int totalMaterialValue() {
        return totalMaterialValue;
    }

    public Component getResultName() {
        return resultItem.get().getDefaultInstance().getHoverName();
    }

    /**
     * Calculates the total material value from all requirements.
     */
    public static int calcTotalValue(List<MaterialRequirement> materials) {
        return materials.stream()
                .mapToInt(MaterialRequirement::materialValue)
                .sum();
    }

    public record MaterialRequirement(Supplier<Item> item, int count, int materialValue) {
        public MaterialRequirement(Supplier<Item> item, int count) {
            this(item, count, 0);
        }

        public Component getDisplayName() {
            return item.get().getDefaultInstance().getHoverName();
        }

        public int getRequired(int quantity) {
            return count * quantity;
        }
    }
}
