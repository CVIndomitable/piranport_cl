package com.piranport.dungeon.block;

import com.piranport.PiranPort;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 */
public class DungeonLecternBlockEntity extends BlockEntity {

    private ItemStack keyStack = ItemStack.EMPTY;
    private UUID dungeonInstanceUuid = null;

    public DungeonLecternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.DUNGEON_LECTERN.get(), pos, state);
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

    /**
     * 尝试将玩家背包中任意一把钥匙插入讲台。已持有钥匙的讲台拒绝插入。
     *
     * @return true 表示成功插入；false 表示讲台已有钥匙或玩家背包无钥匙
     */
    public boolean tryInsertKey(Player player) {
        if (hasKey()) return false;
        int slot = DungeonKeyItem.findAnyKeySlot((net.minecraft.server.level.ServerPlayer) player);
        if (slot < 0) return false;
        ItemStack key = player.getInventory().getItem(slot);
        if (key.isEmpty()) return false;
        this.keyStack = key.copy();
        // 同步实例 UUID：钥匙上的 instanceId 即为该讲台对应的副本实例
        UUID keyInstanceId = DungeonKeyItem.getInstanceId(key);
        this.dungeonInstanceUuid = keyInstanceId;
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        setChanged();
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
        PiranPort.LOGGER.info("DungeonLectern @ {}: extracted key (instanceId={})",
                worldPosition, dungeonInstanceUuid);
        return true;
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
}