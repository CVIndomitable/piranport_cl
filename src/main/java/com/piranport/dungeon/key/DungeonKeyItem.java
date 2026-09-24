package com.piranport.dungeon.key;

import com.piranport.dungeon.block.DungeonLecternBlock;
import com.piranport.dungeon.data.ChapterData;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.data.StageData;
import com.piranport.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.LevelReader;

import java.util.List;
import java.util.UUID;

public class DungeonKeyItem extends Item {
    /**
     * 章节钥匙的章节号。0 表示旧版通用钥匙；旧钥匙仍通过
     * {@link com.piranport.registry.ModDataComponents#DUNGEON_STAGE_ID} 兼容读取。
     */
    private final int chapterNumber;

    public DungeonKeyItem(Properties props) {
        this(0, props);
    }

    /** 创建一个绑定固定章节的钥匙。章节号属于物品类型，不再写在钥匙组件里做差分。 */
    public DungeonKeyItem(int chapterNumber, Properties props) {
        super(props);
        this.chapterNumber = chapterNumber;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public boolean isChapterKey() {
        return chapterNumber >= 1;
    }

    /**
     * 让潜行右键在<b>指向讲台时</b>绕过"潜行不触发方块交互"的引擎闸门。
     *
     * <h2>为什么必须是这个 pos-aware 的重载</h2>
     * <p>1.21.1 + NeoForge 里，"潜行 + 手上拿着钥匙"取不出讲台上的钥匙，卡在两道独立的闸门上：</p>
     * <ol>
     *   <li><b>NeoForge flag1</b>（{@code ServerPlayerGameMode.useItemOn} 补丁）：玩家潜行且主/副手
     *       任一手非空时算出 {@code flag1 = true}，把 {@code blockstate.useItemOn} 整块跳过。
     *       {@code flag1} 里带的豁免条件是
     *       {@code !(主手.doesSneakBypassUse(...) && 副手.doesSneakBypassUse(...))}——
     *       注意是 <b>&&</b>：两只手都得返回 true 才豁免。空手那侧走 {@link ItemStack#EMPTY} 的
     *       默认实现（返回 false），全项目没有任何 Uncraftable 之外的空手豁免，所以
     *       "一手钥匙 + 一手舰装核心"这种最常见的组合，只要副手拿的不是同样返回 true 的物品，
     *       flag1 就恒为 true，讲台的 {@code useItemOn} 永远进不去。</li>
     *   <li><b>原版 PASS_TO_DEFAULT_BLOCK_INTERACTION</b>：非潜行路径靠它把交互续接到
     *       {@code useWithoutItem}，而它只在触发手是 {@code MAIN_HAND} 时才续接（原版硬编码）。
     *       这条路对"主手空手"有效，对"主手拿钥匙"无效。</li>
     * </ol>
     *
     * <p>本方法只解决第 1 道：{@code flag1} 的两侧判定各自问"这个物品允不允许潜行穿透这个方块"，
     * 所以让钥匙在指向讲台时返回 true，就能在"主手钥匙 + 副手任意非豁免物品"时
     * 把 {@code flag1} 压成 false，让讲台的 {@code useItemOn} 拿到控制权。
     * 钥匙指向别的方块时返回 false，保持原版"潜行右键不误触方块"的手感。</p>
     *
     * <p><b>为什么不能用无 pos 的 {@code Item#doesSneakBypassUse}：</b>旧版签名没有坐标参数，
     * 一旦返回 true 就是"拿着钥匙潜行点任何方块都穿透"，会全局破坏潜行交互语义。
     * 带 pos 的重载是 NeoForge 1.21.1 唯一能把豁免范围收窄到单个方块的入口。</p>
     *
     * <p><b>已知残留：</b>副手若也拿着钥匙，两手都返回 true，豁免成立；若副手拿的是其它物品且该物品
     * 未实现本方法，则豁免不成立，flag1 仍拦截。副手这一侧无法从钥匙本类影响。</p>
     */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).getBlock() instanceof DungeonLecternBlock;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String stageId = getStageId(stack);
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
        String stored = stack.getOrDefault(ModDataComponents.DUNGEON_STAGE_ID.get(), "");
        if (!stored.isEmpty()) return stored;
        if (stack.getItem() instanceof DungeonKeyItem key && key.isChapterKey()) {
            return "chapter_" + key.getChapterNumber();
        }
        return "";
    }

    /** 返回钥匙所属章节；旧版通用钥匙从旧组件回退解析。 */
    public static int getChapterNumber(ItemStack stack) {
        if (stack.getItem() instanceof DungeonKeyItem key && key.isChapterKey()) {
            return key.getChapterNumber();
        }
        String stageId = stack.getOrDefault(ModDataComponents.DUNGEON_STAGE_ID.get(), "");
        if (stageId.startsWith("chapter_")) {
            try {
                return Integer.parseInt(stageId.substring("chapter_".length()));
            } catch (NumberFormatException ignored) {
                // fall through to the legacy unknown value
            }
        }
        return 0;
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
