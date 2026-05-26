package com.piranport.menu;

import com.piranport.component.CustomCoreConfig;
import com.piranport.component.WeaponCategory;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipType;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * 舰装核心装备界面的 Menu。
 * 包含：1个核心槽位 + 动态武器槽位 + 动态强化槽位 + 玩家背包。
 */
public class ShipCoreEquipmentMenu extends AbstractContainerMenu {

    // ===== 槽位索引常量 =====
    private static final int CORE_SLOT_INDEX = 0;

    // ===== 状态数据 =====
    private final Player player;
    private final int playerCoreSlot;  // 玩家背包中核心所在槽位
    private ItemStack coreStack;       // 核心物品引用
    private ShipType shipType;         // 当前核心的舰型
    private int weaponSlotCount;       // 武器槽位数量
    private int enhancementSlotCount;  // 强化槽位数量

    // ===== 槽位容器 =====
    private final SimpleContainer equipmentContainer; // 存储核心+装备的虚拟容器

    // ===== 槽位起始索引 =====
    private int weaponSlotStart;
    private int enhancementSlotStart;
    private int playerInventoryStart;

    /**
     * 网络构造函数（客户端）
     */
    public static ShipCoreEquipmentMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        int coreSlot = buf.readVarInt();
        return new ShipCoreEquipmentMenu(containerId, playerInventory, coreSlot);
    }

    /**
     * 服务端构造函数
     */
    public ShipCoreEquipmentMenu(int containerId, Inventory playerInventory, int coreSlot) {
        super(ModMenuTypes.SHIP_CORE_EQUIPMENT_MENU.get(), containerId);
        this.player = playerInventory.player;
        this.playerCoreSlot = coreSlot;
        this.coreStack = playerInventory.getItem(coreSlot);

        // 获取舰型和槽位配置
        if (coreStack.getItem() instanceof ShipCoreItem sci) {
            this.shipType = sci.getShipType();

            // 检查是否有自定义配置
            CustomCoreConfig config = coreStack.get(ModDataComponents.CUSTOM_CORE_CONFIG.get());
            if (config != null && config.isCustomized()) {
                this.weaponSlotCount = config.getWeaponSlots();
                this.enhancementSlotCount = config.getEnhancementSlots();
            } else {
                this.weaponSlotCount = shipType.weaponSlots;
                this.enhancementSlotCount = shipType.enhancementSlots;
            }
        } else {
            // 默认值（不应该发生）
            this.shipType = ShipType.SMALL;
            this.weaponSlotCount = 4;
            this.enhancementSlotCount = 2;
        }

        // 创建虚拟容器：1个核心槽 + 武器槽 + 强化槽
        int totalEquipmentSlots = 1 + weaponSlotCount + enhancementSlotCount;
        this.equipmentContainer = new SimpleContainer(totalEquipmentSlots);

        // 从核心加载装备数据
        loadEquipmentFromCore();

        // 创建槽位
        createSlots(playerInventory);
    }

    /**
     * 从核心物品加载装备数据到虚拟容器
     */
    private void loadEquipmentFromCore() {
        // 核心槽位
        equipmentContainer.setItem(CORE_SLOT_INDEX, coreStack.copy());

        // 从 SHIP_CORE_CONTENTS 读取装备
        ItemContainerContents contents = coreStack.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);

        if (!contents.stream().allMatch(ItemStack::isEmpty)) {
            int totalSlots = getCoreTotalSlots();
            NonNullList<ItemStack> allItems = NonNullList.withSize(totalSlots, ItemStack.EMPTY);
            contents.copyInto(allItems);

            // 复制武器到容器
            for (int i = 0; i < weaponSlotCount; i++) {
                equipmentContainer.setItem(1 + i, allItems.get(i).copy());
            }

            // 复制强化部件到容器（增强起始偏移 = 自定义武器槽 + 弹药槽）
            int enhancementStart = weaponSlotCount + shipType.ammoSlots;
            for (int i = 0; i < enhancementSlotCount; i++) {
                equipmentContainer.setItem(1 + weaponSlotCount + i,
                        allItems.get(enhancementStart + i).copy());
            }
        }
    }

    /**
     * 创建所有槽位
     */
    private void createSlots(Inventory playerInventory) {
        // 1. 核心槽位（右上角）
        addSlot(new CoreSlot(equipmentContainer, CORE_SLOT_INDEX, 200, 20));

        // 2. 动态武器槽位（左侧，每行3个）
        weaponSlotStart = 1;
        for (int i = 0; i < weaponSlotCount; i++) {
            int x = 8 + (i % 3) * 18;
            int y = 50 + (i / 3) * 18;
            addSlot(new WeaponSlot(equipmentContainer, weaponSlotStart + i, x, y));
        }

        // 3. 动态强化槽位（右侧，每行2个）
        enhancementSlotStart = weaponSlotStart + weaponSlotCount;
        for (int i = 0; i < enhancementSlotCount; i++) {
            int x = 120 + (i % 2) * 18;
            int y = 50 + (i / 2) * 18;
            addSlot(new EnhancementSlot(equipmentContainer, enhancementSlotStart + i, x, y));
        }

        // 4. 玩家背包槽位（标准36槽）
        playerInventoryStart = slots.size();

        // 主背包（3x9）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + col,
                        8 + col * 18, 140 + row * 18));
            }
        }

        // 快捷栏（1x9）
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }

    /**
     * 保存装备数据到核心物品
     */
    private void saveEquipmentToCore() {
        // 获取核心槽位的物品
        ItemStack currentCore = equipmentContainer.getItem(CORE_SLOT_INDEX);
        if (currentCore.isEmpty() || !(currentCore.getItem() instanceof ShipCoreItem)) {
            return;
        }

        // 创建完整的槽位列表（使用自定义槽位数计算总容量）
        int totalSlots = getCoreTotalSlots();
        NonNullList<ItemStack> allItems = NonNullList.withSize(totalSlots, ItemStack.EMPTY);

        // 复制武器
        for (int i = 0; i < weaponSlotCount; i++) {
            allItems.set(i, equipmentContainer.getItem(weaponSlotStart + i).copy());
        }

        // 复制强化部件（增强起始偏移 = 自定义武器槽 + 弹药槽）
        int enhancementStart = weaponSlotCount + shipType.ammoSlots;
        for (int i = 0; i < enhancementSlotCount; i++) {
            allItems.set(enhancementStart + i,
                    equipmentContainer.getItem(enhancementSlotStart + i).copy());
        }

        // 保存到核心
        ItemContainerContents contents = ItemContainerContents.fromItems(allItems);
        currentCore.set(ModDataComponents.SHIP_CORE_CONTENTS.get(), contents);

        // 更新玩家背包中的核心
        player.getInventory().setItem(playerCoreSlot, currentCore);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            // 保存装备数据
            saveEquipmentToCore();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack originalStack = stack.copy();

        // 如果是核心槽位
        if (index == CORE_SLOT_INDEX) {
            // 保存装备数据
            saveEquipmentToCore();

            // 将所有装备退回玩家背包
            for (int i = weaponSlotStart; i < enhancementSlotStart + enhancementSlotCount; i++) {
                ItemStack equipment = equipmentContainer.removeItemNoUpdate(i);
                if (!equipment.isEmpty() && !player.addItem(equipment)) {
                    player.drop(equipment, false);
                }
            }

            // 移动核心到背包
            if (moveItemStackTo(stack, playerInventoryStart, playerInventoryStart + 36, true)) {
                slot.set(ItemStack.EMPTY);
                return originalStack;
            }
        }
        // 如果是装备槽位
        else if (index >= weaponSlotStart && index < playerInventoryStart) {
            // 装备 -> 背包
            if (moveItemStackTo(stack, playerInventoryStart, playerInventoryStart + 36, true)) {
                slot.setChanged();
                return originalStack;
            }
        }
        // 如果是背包槽位
        else if (index >= playerInventoryStart) {
            // 背包 -> 装备槽位
            boolean moved = false;

            // 尝试放入武器槽
            WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
            if (cat != null) {
                if (isWeaponCategory(cat)) {
                    moved = moveItemStackTo(stack, weaponSlotStart,
                            weaponSlotStart + weaponSlotCount, false);
                } else if (isEnhancementCategory(cat)) {
                    moved = moveItemStackTo(stack, enhancementSlotStart,
                            enhancementSlotStart + enhancementSlotCount, false);
                }
            }

            // 尝试放入核心槽
            if (!moved && stack.getItem() instanceof ShipCoreItem) {
                moved = moveItemStackTo(stack, CORE_SLOT_INDEX, CORE_SLOT_INDEX + 1, false);
            }

            if (moved) {
                slot.setChanged();
                return originalStack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true; // 核心在玩家背包中，始终有效
    }

    // ===== Getter 方法 =====

    public ShipType getShipType() {
        return shipType;
    }

    public int getWeaponSlotCount() {
        return weaponSlotCount;
    }

    public int getEnhancementSlotCount() {
        return enhancementSlotCount;
    }

    public ItemStack getCoreStack() {
        return equipmentContainer.getItem(CORE_SLOT_INDEX);
    }

    // ===== 辅助方法 =====

    private boolean isWeaponCategory(WeaponCategory cat) {
        return cat == WeaponCategory.CANNON
            || cat == WeaponCategory.TORPEDO
            || cat == WeaponCategory.AIRCRAFT
            || cat == WeaponCategory.MISSILE
            || cat == WeaponCategory.DEPTH_CHARGE;
    }

    private boolean isEnhancementCategory(WeaponCategory cat) {
        return cat == WeaponCategory.ARMOR || cat == WeaponCategory.ENGINE;
    }

    /** 获取核心容器的总槽位数（考虑自定义配置） */
    private int getCoreTotalSlots() {
        CustomCoreConfig config = coreStack.get(ModDataComponents.CUSTOM_CORE_CONFIG.get());
        if (config != null && config.isCustomized()) {
            return config.totalSlots();
        }
        return shipType.totalSlots();
    }

    // ===== 自定义槽位类 =====

    /**
     * 核心槽位：只接受 ShipCoreItem
     */
    private static class CoreSlot extends Slot {
        public CoreSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof ShipCoreItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /**
     * 武器槽位：只接受武器类物品
     */
    private static class WeaponSlot extends Slot {
        public WeaponSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
            if (cat == null) return false;
            return cat == WeaponCategory.CANNON
                || cat == WeaponCategory.TORPEDO
                || cat == WeaponCategory.AIRCRAFT
                || cat == WeaponCategory.MISSILE
                || cat == WeaponCategory.DEPTH_CHARGE;
        }
    }

    /**
     * 强化槽位：只接受强化类物品
     */
    private static class EnhancementSlot extends Slot {
        public EnhancementSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            WeaponCategory cat = stack.get(ModDataComponents.WEAPON_CATEGORY.get());
            if (cat == null) return false;
            return cat == WeaponCategory.ARMOR || cat == WeaponCategory.ENGINE;
        }
    }
}
