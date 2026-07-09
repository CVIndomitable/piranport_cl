package com.piranport.menu;

import com.piranport.block.entity.ShipCoreModifierBlockEntity;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ShipCoreModifierMenu extends AbstractContainerMenu {
    private final ShipCoreModifierBlockEntity blockEntity;
    private final BlockPos blockPos;
    private final ContainerData data;

    private record ClientMenuData(BlockPos pos, @Nullable ShipCoreModifierBlockEntity blockEntity,
                                  ContainerData data) {}

    // 客户端构造器（从网络数据）
    public ShipCoreModifierMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData) {
        this(containerId, playerInv, readClientMenuData(playerInv, extraData));
    }

    // 服务端构造器
    public ShipCoreModifierMenu(int containerId, Inventory playerInv, ShipCoreModifierBlockEntity blockEntity, ContainerData data) {
        this(containerId, playerInv, blockEntity, data,
                blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO);
    }

    private ShipCoreModifierMenu(int containerId, Inventory playerInv, ClientMenuData clientData) {
        this(containerId, playerInv, clientData.blockEntity(), clientData.data(), clientData.pos());
    }

    private static ClientMenuData readClientMenuData(Inventory playerInv, FriendlyByteBuf extraData) {
        BlockPos pos = extraData.readBlockPos();
        if (playerInv.player.level().getBlockEntity(pos) instanceof ShipCoreModifierBlockEntity be) {
            return new ClientMenuData(pos, be, be.getDataAccess());
        }
        SimpleContainerData fallback = new SimpleContainerData(3);
        fallback.set(0, ShipType.SMALL.weaponSlots);
        fallback.set(1, ShipType.SMALL.enhancementSlots);
        fallback.set(2, ShipType.SMALL.ordinal());
        return new ClientMenuData(pos, null, fallback);
    }

    private ShipCoreModifierMenu(int containerId, Inventory playerInv,
                                 @Nullable ShipCoreModifierBlockEntity blockEntity,
                                 ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.SHIP_CORE_MODIFIER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.blockPos = blockPos;
        this.data = data;

        addDataSlots(data);

        if (blockEntity != null) {
            // 核心槽位（中央位置）
            addSlot(new SlotItemHandler(blockEntity.getItemHandler(), ShipCoreModifierBlockEntity.CORE_SLOT, 80, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof ShipCoreItem;
                }
            });
        } else {
            addSlot(new Slot(new SimpleContainer(1), 0, 80, 35) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        // 玩家背包（标准布局）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // 玩家快捷栏
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    public int getWeaponSlots() {
        return data.get(0);
    }

    public int getEnhancementSlots() {
        return data.get(1);
    }

    public ShipType getShipType() {
        int index = data.get(2);
        ShipType[] values = ShipType.values();
        return index >= 0 && index < values.length ? values[index] : ShipType.SMALL;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Nullable
    public ShipCoreModifierBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // 核心槽位索引
            int coreSlotIndex = 0;
            int playerInvStart = 1;
            int playerInvEnd = playerInvStart + 36;

            if (index == coreSlotIndex) {
                // 从核心槽移到玩家背包
                if (!moveItemStackTo(stack, playerInvStart, playerInvEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= playerInvStart && index < playerInvEnd) {
                // 从玩家背包移到核心槽
                if (stack.getItem() instanceof ShipCoreItem) {
                    if (!moveItemStackTo(stack, coreSlotIndex, coreSlotIndex + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity != null) {
            BlockPos pos = blockEntity.getBlockPos();
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
        }
        return true; // 客户端菜单没有方块实体位置约束
    }
}
