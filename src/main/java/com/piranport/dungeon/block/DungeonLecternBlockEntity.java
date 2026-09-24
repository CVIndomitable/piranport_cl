package com.piranport.dungeon.block;

import com.piranport.PiranPort;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 副本讲台 BlockEntity。整合版 §2.2：钥匙插在讲台上，玩家不携带进副本。
 *
 * <p>本 BE 持有：
 * <ul>
 *   <li>{@code keyStack}：当前插入的钥匙 ItemStack（包含 stageId/instanceId/progress DataComponent）</li>
 *   <li>{@code dungeonInstanceUuid}：该钥匙对应的副本实例 UUID（冗余存储以便反查讲台→实例）</li>
 * </ul>
 *
 * <p>不实现公开的 IItemHandler capability（防漏斗/AE2 等自动设备误操作），钥匙的插入/取出
 * 只能通过 {@link #tryInsertKey(Player)} / {@link #extractKeyForShiftRightClick(Player)} 显式调用。
 *
 * <p>外观同步：钥匙的有无会被回写到方块状态 {@link DungeonLecternBlock#HAS_KEY}，从而切换
 * "空台面 / 台面插着钥匙"两套模型。之所以放在 BE 侧而不是方块交互里改，是因为钥匙也会被
 * {@link #setKeyStack(ItemStack)}（世界生成/教学触点）和 {@link #loadAdditional} 改动，
 * 这些路径本来就绕不开 BE；单点同步能保证所有路径外观一致。
 */
public class DungeonLecternBlockEntity extends BlockEntity {

    private ItemStack keyStack = ItemStack.EMPTY;
    private UUID dungeonInstanceUuid = null;
    /** 关掉状态重入：{@link #syncHasKeyState} 引起的 setBlock 会再次触发 onLoad。 */
    private boolean syncingState = false;

    public DungeonLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DUNGEON_LECTERN.get(), pos, state);
    }

    // ===== 外观同步 =====

    /**
     * 把"是否插着钥匙"回写到方块状态，驱动 blockstate 换模型。
     *
     * <p>只在真正变化时才 setBlock，避免每次插入/取出都触发一次区块更新。
     * 必须先读当前状态再比对：BE 构造期间 level 为 null，此时直接跳过。
     */
    private void syncHasKeyState() {
        if (level == null || level.isClientSide() || syncingState) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(DungeonLecternBlock.HAS_KEY)) return;
        boolean want = hasKey();
        if (state.getValue(DungeonLecternBlock.HAS_KEY) == want) return;
        syncingState = true;
        try {
            level.setBlock(worldPosition, state.setValue(DungeonLecternBlock.HAS_KEY, want), Block.UPDATE_ALL);
        } finally {
            syncingState = false;
        }
    }

    // ===== Getters =====

    public ItemStack getKeyStack() {
        return keyStack;
    }

    public UUID getDungeonInstanceUuid() {
        return dungeonInstanceUuid;
    }

    public boolean hasKey() {
        return keyStack.getItem() instanceof DungeonKeyItem;
    }

    // ===== Mutators =====

    /** 创建成功后立即同步钥匙与讲台，防止下一次进入重复分配副本。 */
    public void bindInstance(UUID instanceId) {
        if (!hasKey()) return;
        DungeonKeyItem.setInstanceId(keyStack, instanceId);
        dungeonInstanceUuid = instanceId;
        setChanged();
    }

    /**
     * 尝试将玩家背包中任意一把钥匙插入讲台。已持有钥匙的讲台拒绝插入。
     *
     * @return true 表示成功插入；false 表示讲台已有钥匙或玩家背包无钥匙
     */
    public boolean tryInsertKey(Player player, net.minecraft.world.InteractionHand hand) {
        if (hasKey()) return false;
        ItemStack key = player.getItemInHand(hand);
        if (!(key.getItem() instanceof DungeonKeyItem)) return false;
        this.keyStack = key.copy();
        // 同步实例 UUID：钥匙上的 instanceId 即为该讲台对应的副本实例
        UUID keyInstanceId = DungeonKeyItem.getInstanceId(key);
        this.dungeonInstanceUuid = keyInstanceId;
        key.shrink(1);
        setChanged();
        syncHasKeyState();
        PiranPort.LOGGER.info("DungeonLectern @ {}: inserted key (instanceId={})",
                worldPosition, keyInstanceId);
        return true;
    }

    /**
     * Shift+右键取出钥匙，钥匙返回玩家背包。
     * 整合版 §3.4：副本永不删除，dungeonInstanceUuid 仍保留，让其他玩家能继续该副本。
     *
     * @return true 表示成功取出；false 表示讲台无钥匙或玩家背包已满
     */
    public boolean extractKeyForShiftRightClick(Player player) {
        if (!hasKey()) return false;
        ItemStack key = this.keyStack.copy();
        if (!player.getInventory().add(key)) {
            // 背包满 → 掉落到地上
            player.drop(key, false);
        }
        this.keyStack = ItemStack.EMPTY;
        // dungeonInstanceUuid 保留，副本可被其他玩家继续
        setChanged();
        syncHasKeyState();
        PiranPort.LOGGER.info("DungeonLectern @ {}: extracted key (instanceId={})",
                worldPosition, dungeonInstanceUuid);
        return true;
    }

    /**
     * 程序化地设置讲台上的钥匙（用于世界生成 / 结构放置）。
     * 教学触点专用：讲台在生成时已预设教学关卡钥匙，无需玩家背包插入。
     *
     * @param keyStack 钥匙 ItemStack，必须为 DungeonKeyItem
     */
    public void setKeyStack(ItemStack keyStack) {
        if (!(keyStack.getItem() instanceof DungeonKeyItem)) {
            return;
        }
        this.keyStack = keyStack.copy();
        UUID keyInstanceId = DungeonKeyItem.getInstanceId(keyStack);
        this.dungeonInstanceUuid = keyInstanceId;
        setChanged();
        syncHasKeyState();
    }

    // ===== NBT Serialization =====

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (keyStack.getItem() instanceof DungeonKeyItem) {
            tag.put("KeyStack", keyStack.save(registries));
        }
        if (dungeonInstanceUuid != null) {
            tag.putUUID("DungeonInstanceUuid", dungeonInstanceUuid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("KeyStack")) {
            ItemStack loaded = ItemStack.parse(registries, tag.getCompound("KeyStack")).orElse(ItemStack.EMPTY);
            if (loaded.getItem() instanceof DungeonKeyItem) {
                this.keyStack = loaded;
            }
        }
        if (tag.hasUUID("DungeonInstanceUuid")) {
            this.dungeonInstanceUuid = tag.getUUID("DungeonInstanceUuid");
        }
    }

    /**
     * 区块加载后把外观状态追平。
     *
     * <p>存档里的方块状态可能是旧的（讲台世界生成时先放空台面、再由结构逻辑灌入钥匙），
     * 所以不能只依赖插入时的同步——加载时以 BE 的实际钥匙为准重算一次。
     * 放在下一 tick 执行：{@code onLoad} 期间改方块状态会被区块自身的加载流程覆盖掉。
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(DungeonLecternBlock.HAS_KEY)) return;
        if (state.getValue(DungeonLecternBlock.HAS_KEY) == hasKey()) return;
        level.scheduleTick(worldPosition, state.getBlock(), 1);
    }

    /** 承接 {@link #onLoad} 调度的那次 tick，复检外观。（ticker 由方块注册，见 DungeonLecternBlock。） */
    void tickFromScheduledUpdate() {
        syncHasKeyState();
    }

    /** 服务端→客户端的状态同步标签，带上 HAS_KEY 以免刚进视距的客户端看到空台面。 */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (hasKey()) {
            tag.put("KeyStack", keyStack.save(registries));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 客户端只需要知道"有没有钥匙"，钥匙内容本身不参与客户端渲染。
        if (tag.contains("KeyStack")) {
            ItemStack loaded = ItemStack.parse(registries, tag.getCompound("KeyStack")).orElse(ItemStack.EMPTY);
            if (loaded.getItem() instanceof DungeonKeyItem) {
                this.keyStack = loaded;
            }
        }
    }
}
