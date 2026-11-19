package com.piranport.recipe;

import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModItems;
import com.piranport.testsupport.ModRegistryFixture;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("mc-bootstrap")
class TeaBrewingTest {
    @Test
    void freshLeavesCanBeReplantedOrFermentedWithoutStealingOtherRecipes() {
        ModRegistryFixture.bootstrap();
        var builder = new PotionBrewing.Builder(FeatureFlags.VANILLA_SET);
        ModBrewingRecipes.register(builder);
        var stand = builder.build();
        var water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        var yeast = stand.mix(Items.SUGAR.getDefaultInstance(), water);
        var leaves = ModItems.TEA_LEAVES.get().getDefaultInstance();
        assertTrue(yeast.is(ModItems.YEAST.get()));
        assertSame(ModBlocks.TEA_BUSH.get(), ModItems.TEA_LEAVES.get().getBlock());
        assertTrue(stand.isInput(yeast) && stand.isIngredient(leaves) && stand.hasMix(yeast, leaves));
        assertTrue(stand.mix(leaves, yeast).is(ModItems.BLACK_TEA.get()));
        assertTrue(stand.mix(ModItems.RICE.get().getDefaultInstance(), yeast).is(ModItems.VINEGAR.get()));
        assertFalse(stand.hasMix(water, leaves));
        assertFalse(stand.hasMix(yeast, Items.COBBLESTONE.getDefaultInstance()));
    }
}
