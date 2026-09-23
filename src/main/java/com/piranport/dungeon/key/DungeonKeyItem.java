package com.piranport.dungeon.key;

import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
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
    /**
     * 把钥匙上的"入口 ID"解析成真实的关卡 ID。
     *
     * <p><b>为什么需要这一步：</b>钥匙的 {@code DUNGEON_STAGE_ID} 并不总是关卡 ID。
     * {@link com.piranport.dungeon.recipe.ChapterKeyRecipe} 合成出来的章节钥匙写的是
     * {@code chapter_*}（如 {@code chapter_1}），而 {@code DungeonRegistry} 里
     * {@code stages} 表只认 {@code 1-1} 这类关卡 ID、{@code chapter_1} 在 {@code chapters} 表里。
     * 直接拿 chapter ID 去 {@code getStage} 必然返回 null → 入口报
     * "钥匙上的副本不存在"——这正是"点继续/从头开始都进不去"的根因。</p>
     *
     * <p>解析规则与讲台打开 ContinueScreen 时的显示名回退（{@code DungeonLecternBlock.useWithoutItem}）
     * 完全一致，两处必须同进退，否则会出现"对话框显示了章节名、点进去却说副本不存在"。
     * 因此这里做成唯一的解析入口，两边共用。</p>
     *
     * @return 真实关卡 ID；无法解析时返回原值（调用方据此报错，不要静默替换成默认关卡）
     */
    public static String resolveStageId(ItemStack stack) {
        String id = getStageId(stack);
        if (id.startsWith("chapter_")) {
            ChapterData chapter = DungeonRegistry.INSTANCE.getChapter(id);
            if (chapter != null && !chapter.stages().isEmpty()) {
                return chapter.stages().get(0);
            }
        }
        return id;
    }

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

    // ========== 钥匙槽位工具（迁移自 FlagshipManager，整合版 §2.2：钥匙插在讲台上，FlagshipManager 玩家权限概念已取消）==========
    // 注：当前阶段仍以"玩家背包"为查找来源（讲台 BE 将在阶段 2 新建后切换为 lecternBE.getKeyStack()）。

    /**
     * 查找玩家背包中匹配指定 instanceId 的钥匙槽位；返回 -1 表示未找到。
     */
    public static int findKeySlot(ServerPlayer player, UUID instanceId) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                UUID keyInstanceId = getInstanceId(stack);
                if (instanceId.equals(keyInstanceId)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 查找玩家背包中任意一把钥匙的槽位；返回 -1 表示未找到。
     */
    public static int findAnyKeySlot(ServerPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof DungeonKeyItem) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从玩家背包取出指定 instanceId 对应的钥匙 ItemStack；未找到返回 ItemStack.EMPTY。
     */
    public static ItemStack getKeyStack(ServerPlayer player, UUID instanceId) {
        int slot = findKeySlot(player, instanceId);
        return slot >= 0 ? player.getInventory().getItem(slot) : ItemStack.EMPTY;
    }
}
