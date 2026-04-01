package com.piranport.dungeon.key;

import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.UUID;

public class DungeonKeyItem extends Item {

    public DungeonKeyItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String stageId = stack.getOrDefault(ModDataComponents.DUNGEON_STAGE_ID.get(), "");
        if (!stageId.isEmpty()) {
            StageData stage = DungeonRegistry.INSTANCE.getStage(stageId);
            if (stage != null) {
                tooltip.add(Component.literal(stage.displayName()).withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal(stageId).withStyle(ChatFormatting.GRAY));
            }
        }

        UUID instanceId = stack.get(ModDataComponents.DUNGEON_INSTANCE_ID.get());
        if (instanceId != null) {
            DungeonProgress progress = stack.getOrDefault(
                    ModDataComponents.DUNGEON_PROGRESS.get(), DungeonProgress.EMPTY);
            if (!progress.clearedNodes().isEmpty()) {
                tooltip.add(Component.translatable("item.piranport.dungeon_key.progress",
                        progress.clearedNodes().size()).withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /**
     * Gets the stage ID from a dungeon key stack.
     */
    public static String getStageId(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DUNGEON_STAGE_ID.get(), "");
    }

    /**
     * Gets the instance UUID from a dungeon key stack.
     */
    public static UUID getInstanceId(ItemStack stack) {
        return stack.get(ModDataComponents.DUNGEON_INSTANCE_ID.get());
    }

    /**
     * Gets the progress from a dungeon key stack.
     */
    public static DungeonProgress getProgress(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.DUNGEON_PROGRESS.get(), DungeonProgress.EMPTY);
    }

    /**
     * Updates the progress on a dungeon key stack.
     */
    public static void setProgress(ItemStack stack, DungeonProgress progress) {
        stack.set(ModDataComponents.DUNGEON_PROGRESS.get(), progress);
    }

    /**
     * Sets the instance UUID on a dungeon key stack.
     */
    public static void setInstanceId(ItemStack stack, UUID instanceId) {
        stack.set(ModDataComponents.DUNGEON_INSTANCE_ID.get(), instanceId);
    }
}
