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
 * 只能通过 {@link #tryInsertKey} / {@link #extractKeyForShiftRightClick(Player)} 显式调用。
 *
 * <p>副本/17 §3.2：插钥匙即创建（或沿用钥匙上已有的）实例并写 instanceId，换书台不新建实例；
 * 建造进度挂在实例上（{@link LecternBuildScheduler}），书台每秒重算 {@link LecternPattern.Status}
 * 驱动纹路与悬浮字幕。破坏书台时由方块 onRemove 经 {@link #takeKeyForDrop()} 掉出钥匙。
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
    /** 服务端每秒重算的实例状态；纹路与悬浮字幕都从这里取（副本/17 §3.1、§3.3）。 */
    private LecternPattern.Status status = LecternPattern.Status.EMPTY;
    /** 悬浮字幕数据，服务端算好后随 update tag 下发；客户端只读。 */
    private String labelStage = "";
    private int labelCleared = 0;
    private boolean labelCompleted = false;
    private int labelPresent = 0;

    public DungeonLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DUNGEON_LECTERN.get(), pos, state);
    }

    // ===== 外观同步 =====

    /**
     * 把"是否插着钥匙"与纹路三态回写到方块状态，驱动 blockstate 换模型与纹路染色。
     *
     * <p>只在真正变化时才 setBlock，避免每次插入/取出都触发一次区块更新。
     * 必须先读当前状态再比对：BE 构造期间 level 为 null，此时直接跳过。
     */
    private void syncHasKeyState() {
        if (level == null || level.isClientSide() || syncingState) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(DungeonLecternBlock.HAS_KEY)) return;
        boolean want = hasKey();
        LecternPattern pattern = LecternPattern.of(hasKey() ? status : LecternPattern.Status.EMPTY);
        BlockState next = state.setValue(DungeonLecternBlock.HAS_KEY, want);
        if (next.hasProperty(DungeonLecternBlock.PATTERN)) {
            next = next.setValue(DungeonLecternBlock.PATTERN, pattern);
        }
        if (next == state) return;
        syncingState = true;
        try {
            level.setBlock(worldPosition, next, Block.UPDATE_ALL);
        } finally {
            syncingState = false;
        }
    }

    public LecternPattern.Status getStatus() {
        return hasKey() ? status : LecternPattern.Status.EMPTY;
    }

    /**
     * 服务端按实例真实状态重算纹路与字幕（建造中 / 满员 / 可进）。由方块 ticker 每 20 tick 调用，
     * 也在插钥匙、撤钥匙后立即调用。建造本身挂在实例上（{@code LecternBuildScheduler}），不在这里推进。
     */
    public void refreshStatus() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;
        LecternPattern.Status prev = status;
        String prevStage = labelStage;
        int prevCleared = labelCleared, prevPresent = labelPresent;
        boolean prevCompleted = labelCompleted;

        labelStage = "";
        labelCleared = 0;
        labelCompleted = false;
        labelPresent = 0;
        if (!hasKey()) {
            status = LecternPattern.Status.EMPTY;
        } else {
            var manager = com.piranport.dungeon.instance.DungeonInstanceManager.get(serverLevel);
            UUID id = DungeonKeyItem.getInstanceId(keyStack);
            var instance = id == null ? null : manager.getInstance(id);
            var stage = com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(
                    instance != null ? instance.getStageId() : DungeonKeyItem.resolveStageId(keyStack));
            if (stage != null) labelStage = stage.displayName();
            boolean built = false;
            if (instance != null && stage != null) {
                labelCleared = instance.getClearedNodes().size();
                labelCompleted = instance.getState() == com.piranport.dungeon.instance.DungeonInstance.State.COMPLETED;
                labelPresent = manager.countPresentPlayers(instance, serverLevel.getServer());
                built = LecternBuildScheduler.isBuilt(serverLevel.getServer(), instance, stage);
            }
            status = LecternPattern.status(true, instance != null && stage != null, built, labelPresent);
        }
        syncHasKeyState();
        if (status != prev || !labelStage.equals(prevStage) || labelCleared != prevCleared
                || labelPresent != prevPresent || labelCompleted != prevCompleted) {
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ===== 字幕（客户端读取） =====
    public String getLabelStage() { return labelStage; }
    public int getLabelCleared() { return labelCleared; }
    public boolean isLabelCompleted() { return labelCompleted; }
    public int getLabelPresent() { return labelPresent; }

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
        this.keyStack.setCount(1);
        key.shrink(1);
        // 副本/17 §3.1：插入瞬间即创建实例并写入唯一 instanceId（已绑定的钥匙不新建）。
        bindOrCreateInstance();
        setChanged();
        refreshStatus();
        syncHasKeyState();
        PiranPort.LOGGER.info("DungeonLectern @ {}: inserted key (instanceId={})",
                worldPosition, dungeonInstanceUuid);
        return true;
    }

    /**
     * 钥匙上没有 instanceId → 分配新实例并写回钥匙；已有 → 只同步讲台记录，绝不新建。
     * 实例被删除/关卡不存在时保持未绑定，纹路为红（INVALID），允许撤下钥匙。
     */
    private void bindOrCreateInstance() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel) || !hasKey()) return;
        var manager = com.piranport.dungeon.instance.DungeonInstanceManager.get(serverLevel);
        UUID existing = DungeonKeyItem.getInstanceId(keyStack);
        if (existing != null) {
            dungeonInstanceUuid = existing;
            var instance = manager.getInstance(existing);
            if (instance != null) {
                instance.setLecternPos(worldPosition);
                instance.setLecternDimension(level.dimension().location().toString());
                manager.setDirty();
            }
            return;
        }
        String stageId = DungeonKeyItem.resolveStageId(keyStack);
        if (stageId == null || stageId.isEmpty()
                || com.piranport.dungeon.data.DungeonRegistry.INSTANCE.getStage(stageId) == null) return;
        var instance = manager.createInstanceForLectern(stageId, worldPosition,
                level.dimension().location().toString());
        if (instance == null) return;
        DungeonKeyItem.setInstanceId(keyStack, instance.getInstanceId());
        dungeonInstanceUuid = instance.getInstanceId();
        manager.syncKey(instance, keyStack);
    }

    /**
     * 空手潜行右键撤下钥匙（副本/17 §3.2）。建造中锁定，调用方需先判 {@link LecternPattern#canWithdraw}。
     * 整合版 §3.4：副本永不删除，dungeonInstanceUuid 仍保留，让其他玩家能继续该副本。
     *
     * @return true 表示成功取出
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
        refreshStatus();
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
        refreshStatus();
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

    /**
     * 服务端 ticker：每 20 tick 按实例真实状态刷新纹路与字幕（建造完成转绿、满员转红）。
     * 首 tick 也立即刷新一次，承接 {@link #onLoad} 的外观追平。
     */
    void tickFromScheduledUpdate() {
        if (level == null) return;
        if (!initialRefreshDone || level.getGameTime() % 20L == Math.floorMod(worldPosition.asLong(), 20L)) {
            initialRefreshDone = true;
            refreshStatus();
        }
    }

    private boolean initialRefreshDone = false;

    /** 破坏书台时取出钥匙交给方块掉落（副本/17 §3.2：任何破坏方式都掉钥匙）。 */
    ItemStack takeKeyForDrop() {
        ItemStack key = keyStack;
        keyStack = ItemStack.EMPTY;
        setChanged();
        return key;
    }

    /** 服务端→客户端的状态同步标签：钥匙 + 悬浮字幕数据。 */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (hasKey()) {
            tag.put("KeyStack", keyStack.save(registries));
        }
        tag.putString("LabelStage", labelStage);
        tag.putInt("LabelCleared", labelCleared);
        tag.putBoolean("LabelCompleted", labelCompleted);
        tag.putInt("LabelPresent", labelPresent);
        tag.putString("LabelStatus", getStatus().name());
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        // 撤钥匙后的更新标签不带 KeyStack，客户端必须同步清空，否则字幕不会消失。
        ItemStack loaded = tag.contains("KeyStack")
                ? ItemStack.parse(registries, tag.getCompound("KeyStack")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        this.keyStack = loaded.getItem() instanceof DungeonKeyItem ? loaded : ItemStack.EMPTY;
        labelStage = tag.getString("LabelStage");
        labelCleared = tag.getInt("LabelCleared");
        labelCompleted = tag.getBoolean("LabelCompleted");
        labelPresent = tag.getInt("LabelPresent");
        try {
            status = LecternPattern.Status.valueOf(tag.getString("LabelStatus"));
        } catch (IllegalArgumentException e) {
            status = LecternPattern.Status.EMPTY;
        }
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt,
                             HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }
}
