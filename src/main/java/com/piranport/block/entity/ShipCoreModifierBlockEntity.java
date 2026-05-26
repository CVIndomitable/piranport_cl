package com.piranport.block.entity;

import com.piranport.component.CustomCoreConfig;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.menu.ShipCoreModifierMenu;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * 舰装核心改装器方块实体
 * 槽位0: 舰装核心（输入/输出槽）
 */
public class ShipCoreModifierBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CORE_SLOT = 0;
    public static final int TOTAL_SLOTS = 1;

    // 配置参数（通过 ContainerData 同步到客户端）
    private int weaponSlots = 2;      // 武器槽数量 (2-8)
    private int enhancementSlots = 1; // 强化槽数量 (1-6)
    private ShipType shipType = ShipType.SMALL;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // 当核心放入时，读取其配置
            if (slot == CORE_SLOT) {
                ItemStack stack = getStackInSlot(CORE_SLOT);
                if (stack.getItem() instanceof ShipCoreItem sci) {
                    shipType = sci.getShipType();
                    CustomCoreConfig config = stack.get(ModDataComponents.CUSTOM_CORE_CONFIG.get());
                    if (config != null && config.isCustomized()) {
                        weaponSlots = config.customWeaponSlots();
                        enhancementSlots = config.customEnhancementSlots();
                    } else {
                        // 使用舰型默认值
                        weaponSlots = shipType.weaponSlots;
                        enhancementSlots = shipType.enhancementSlots;
                    }
                }
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == CORE_SLOT && stack.getItem() instanceof ShipCoreItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    // 用于客户端同步的数据容器
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> weaponSlots;
                case 1 -> enhancementSlots;
                case 2 -> shipType.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> weaponSlots = value;
                case 1 -> enhancementSlots = value;
                case 2 -> shipType = ShipType.values()[value];
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ShipCoreModifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SHIP_CORE_MODIFIER.get(), pos, state);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    public int getWeaponSlots() {
        return weaponSlots;
    }

    public int getEnhancementSlots() {
        return enhancementSlots;
    }

    public ShipType getShipType() {
        return shipType;
    }

    public void setWeaponSlots(int value) {
        this.weaponSlots = Math.clamp(value, 2, 8);
        setChanged();
    }

    public void setEnhancementSlots(int value) {
        this.enhancementSlots = Math.clamp(value, 1, 6);
        setChanged();
    }

    /**
     * 应用改装配置到核心物品
     * @param player 玩家（用于扣除经验）
     * @return 是否成功应用
     */
    public boolean applyModification(Player player) {
        ItemStack coreStack = itemHandler.getStackInSlot(CORE_SLOT);
        if (coreStack.isEmpty() || !(coreStack.getItem() instanceof ShipCoreItem sci)) {
            return false;
        }

        // 计算经验消耗（每个槽位 5 级经验）
        int totalSlots = weaponSlots + enhancementSlots;
        int defaultSlots = shipType.weaponSlots + shipType.enhancementSlots;
        int extraSlots = Math.max(0, totalSlots - defaultSlots);
        int expCost = extraSlots * 5;

        // 检查玩家经验是否足够
        if (player.experienceLevel < expCost) {
            return false;
        }

        // 扣除经验
        player.giveExperienceLevels(-expCost);

        // 应用配置
        CustomCoreConfig config = new CustomCoreConfig(
                shipType,
                weaponSlots,
                enhancementSlots,
                true
        );
        coreStack.set(ModDataComponents.CUSTOM_CORE_CONFIG.get(), config);

        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", itemHandler.serializeNBT(registries));
        tag.putInt("WeaponSlots", weaponSlots);
        tag.putInt("EnhancementSlots", enhancementSlots);
        tag.putInt("ShipType", shipType.ordinal());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        weaponSlots = tag.contains("WeaponSlots") ? tag.getInt("WeaponSlots") : 2;
        enhancementSlots = tag.contains("EnhancementSlots") ? tag.getInt("EnhancementSlots") : 1;
        shipType = tag.contains("ShipType") ? ShipType.values()[tag.getInt("ShipType")] : ShipType.SMALL;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.piranport.ship_core_modifier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ShipCoreModifierMenu(containerId, playerInv, this, this.dataAccess);
    }
}
