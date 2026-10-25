package com.piranport.menu;

import com.piranport.block.entity.BlueprintChestBlockEntity;
import com.piranport.registry.ModBlockEntityTypes;
import com.piranport.registry.ModBlocks;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModItems;
import com.piranport.registry.ModMenuTypes;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 蓝图收纳箱菜单的可执行回归测试。
 *
 * <p>项目其余测试不启动注册表；这里用 {@link Bootstrap#bootStrap()} + 反射读取各
 * {@link DeferredRegister} 的私有 entries，把它们按真实 supplier 注册进内建注册表，
 * 等价于跑一遍 RegisterEvent。这样 {@code DeferredHolder.get()} 能正常绑定，
 * 才能构造真实的 {@link BlueprintChestBlockEntity} / {@link BlueprintChestMenu}。</p>
 */
@Tag("mc-bootstrap")
class BlueprintChestMenuTest {

    private static boolean bootstrapped;

    @BeforeAll
    static void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        SharedConstants.tryDetectVersion();
        // 纯 JUnit 环境没有 FML 启动流程；FeatureFlags.<clinit> 会读 LoadingModList。
        // 塞一个空列表进去，加载器就会认为「没有模组声明特性开关」，bootstrap 得以继续。
        net.neoforged.fml.loading.LoadingModList.of(
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.Map.of());
        Bootstrap.bootStrap();

        bindAll(ModDataComponents.DATA_COMPONENTS);
        bindAll(ModBlocks.BLOCKS);
        bindAll(ModBlockEntityTypes.BLOCK_ENTITY_TYPES);
        bindAll(ModItems.ITEMS);
        bindAll(ModMenuTypes.MENU_TYPES);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bindAll(DeferredRegister<?> register) {
        Registry registry = BuiltInRegistries.REGISTRY.get(register.getRegistryKey().location());
        assertNotNull(registry, "registry for " + register.getRegistryKey());
        if (registry instanceof MappedRegistry<?> mapped) {
            mapped.unfreeze();
        }
        Map<DeferredHolder<?, ?>, Supplier<?>> entries = entriesOf(register);
        for (Map.Entry<DeferredHolder<?, ?>, Supplier<?>> e : entries.entrySet()) {
            ResourceLocation id = e.getKey().getId();
            if (registry.containsKey(id)) continue;
            Object value = e.getValue().get();
            Registry.register(registry, id, value);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<DeferredHolder<?, ?>, Supplier<?>> entriesOf(DeferredRegister<?> register) {
        try {
            Field field = DeferredRegister.class.getDeclaredField("entries");
            field.setAccessible(true);
            return new LinkedHashMap<>((Map<DeferredHolder<?, ?>, Supplier<?>>) field.get(register));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot read DeferredRegister.entries", e);
        }
    }

    private static BlueprintChestBlockEntity newChest() {
        return new BlueprintChestBlockEntity(BlockPos.ZERO,
                ModBlocks.BLUEPRINT_CHEST.get().defaultBlockState());
    }

    private static ItemStack anyBlueprint() {
        for (var holder : com.piranport.registry.WeaponItems.EQUIPMENT_BLUEPRINTS.values()) {
            return new ItemStack(holder.get());
        }
        fail("EQUIPMENT_BLUEPRINTS 为空");
        return ItemStack.EMPTY;
    }

    private static ItemStack anotherBlueprint(ItemStack exclude) {
        for (var holder : com.piranport.registry.WeaponItems.EQUIPMENT_BLUEPRINTS.values()) {
            if (!holder.get().equals(exclude.getItem())) return new ItemStack(holder.get());
        }
        fail("EQUIPMENT_BLUEPRINTS 里只有一种蓝图，无法测试异种共存");
        return ItemStack.EMPTY;
    }

    @Test
    void normalClickInsertDoesNotThrow() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);
        ItemStack blueprint = anyBlueprint();
        assertTrue(BlueprintChestBlockEntity.isBlueprint(blueprint));

        Slot blueprintSlot = menu.slots.get(0);
        assertTrue(blueprintSlot.mayPlace(blueprint));
        // 等价于 doClick PICKUP：safeInsert -> setByPlayer -> setStackInSlot
        ItemStack left = blueprintSlot.safeInsert(blueprint.copy(), 1);
        assertTrue(left.isEmpty(), "蓝图应被放入槽 0");
        assertEquals(blueprint.getItem(), blueprintSlot.getItem().getItem());

        // 服务端每 tick 的同步不应抛异常
        assertDoesNotThrow(menu::broadcastChanges);
    }

    @Test
    void shiftClickMoveIntoAndOutOfChestDoesNotThrow() {
        BlueprintChestBlockEntity chest = newChest();
        Inventory inventory = new Inventory(null);
        BlueprintChestMenu menu = new BlueprintChestMenu(1, inventory, chest);
        ItemStack blueprint = anyBlueprint();

        // 玩家主背包第一格 -> 箱子（menu 槽 19 对应 inventory index 9）
        inventory.setItem(9, blueprint.copy());
        ItemStack result = assertDoesNotThrow(() -> menu.quickMoveStack(null, 19));
        assertFalse(result.isEmpty(), "shift-click 应返回被移动的原栈");
        assertFalse(menu.slots.get(0).getItem().isEmpty(), "蓝图应进入箱子槽 0");

        // 箱子 -> 玩家背包（menu 槽 0）
        ItemStack back = assertDoesNotThrow(() -> menu.quickMoveStack(null, 0));
        assertFalse(back.isEmpty());
        assertTrue(menu.slots.get(0).getItem().isEmpty());
    }

    @Test
    void copySlotPreviewAndTakeConsumesPaper() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);
        ItemStack blueprint = anyBlueprint();
        chest.getItemHandler().setStackInSlot(0, blueprint.copy());
        chest.getItemHandler().setStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER, 2));

        Slot copy = menu.slots.get(BlueprintChestMenu.COPY_SLOT_START);
        assertFalse(copy.getItem().isEmpty(), "有蓝图+纸时复印槽应显示预览");
        assertDoesNotThrow(menu::broadcastChanges);

        ItemStack taken = copy.remove(1);
        assertFalse(taken.isEmpty());
        assertEquals(blueprint.getItem(), taken.getItem());
        assertEquals(1, chest.getItemHandler().getStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT).getCount());
    }

    @Test
    void shiftClickCopySlotDoesNotLoopForever() {
        BlueprintChestBlockEntity chest = newChest();
        Inventory inventory = new Inventory(null);
        BlueprintChestMenu menu = new BlueprintChestMenu(1, inventory, chest);
        chest.getItemHandler().setStackInSlot(0, anyBlueprint());
        chest.getItemHandler().setStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER, 64));

        // 模拟 doClick QUICK_MOVE 的 while 循环（原版会反复调用 quickMoveStack）
        int guard = 0;
        ItemStack moved = menu.quickMoveStack(null, BlueprintChestMenu.COPY_SLOT_START);
        while (!moved.isEmpty() && guard++ < 1000) {
            moved = menu.quickMoveStack(null, BlueprintChestMenu.COPY_SLOT_START);
        }
        assertTrue(guard < 1000, "复印槽 shift-click 不应死循环");
    }

    /** 复刻原版 AbstractContainerMenu#doClick 的 QUICK_MOVE 分支：先 mayPickup，再循环 quickMoveStack。 */
    private static void simulateShiftClick(BlueprintChestMenu menu, int slotId) {
        Slot slot = menu.slots.get(slotId);
        if (!slot.mayPickup(null)) return;
        ItemStack moved = menu.quickMoveStack(null, slotId);
        int extraCalls = 0;
        while (!moved.isEmpty() && ItemStack.isSameItem(slot.getItem(), moved)) {
            if (++extraCalls > 1000) fail("quickMoveStack 死循环");
            moved = menu.quickMoveStack(null, slotId);
        }
        assertEquals(0, extraCalls, "一次 shift 点击不应再调用 quickMoveStack");
    }

    private static int paperCount(BlueprintChestBlockEntity chest) {
        return chest.getItemHandler().getStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT).getCount();
    }

    private static int countInInventory(Inventory inventory, ItemStack sample) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, sample)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /** 一次 shift 点击只复印一份、扣一张纸；再点一次才再复印一份。 */
    @Test
    void shiftClickCopiesExactlyOnePerClick() {
        BlueprintChestBlockEntity chest = newChest();
        Inventory inventory = new Inventory(null);
        BlueprintChestMenu menu = new BlueprintChestMenu(1, inventory, chest);
        ItemStack blueprint = anyBlueprint();
        chest.getItemHandler().setStackInSlot(0, blueprint.copy());
        chest.getItemHandler().setStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER, 5));

        simulateShiftClick(menu, BlueprintChestMenu.COPY_SLOT_START);
        assertEquals(4, paperCount(chest), "一次 shift 点击只应扣 1 张纸");
        assertEquals(1, countInInventory(inventory, blueprint), "一次 shift 点击只应复印 1 份");

        simulateShiftClick(menu, BlueprintChestMenu.COPY_SLOT_START);
        assertEquals(3, paperCount(chest));
        assertEquals(2, countInInventory(inventory, blueprint), "再点一次才应变成 2 份");

        // 普通左键取件不受影响：一次仍是一份
        ItemStack taken = menu.slots.get(BlueprintChestMenu.COPY_SLOT_START).remove(1);
        assertEquals(1, taken.getCount());
        assertEquals(2, paperCount(chest));
    }

    /**
     * 索引/坐标不变量：Screen 的 slotClicked 靠「蓝图槽下标 + COPY_SLOT_START」重映射到复印槽，
     * 一旦槽位注册顺序或常量漂移，点击就会落到玩家背包甚至越界。这里把当前布局钉死。
     */
    @Test
    void slotIndicesAndOverlappingCoordinatesAreStable() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);

        assertEquals(BlueprintChestBlockEntity.TOTAL_SLOTS
                        + BlueprintChestBlockEntity.BLUEPRINT_SLOTS
                        + 27 + 9, menu.slots.size(), "槽位总数");
        assertEquals(55, menu.slots.size());
        assertEquals(BlueprintChestBlockEntity.TOTAL_SLOTS, BlueprintChestMenu.COPY_SLOT_START);
        assertEquals(BlueprintChestMenu.COPY_SLOT_START + BlueprintChestBlockEntity.BLUEPRINT_SLOTS,
                BlueprintChestMenu.PLAYER_INV_START);
        assertEquals(menu.slots.size(), BlueprintChestMenu.PLAYER_INV_END);

        for (int i = 0; i < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; i++) {
            Slot blueprint = menu.slots.get(i);
            Slot copy = menu.slots.get(BlueprintChestMenu.COPY_SLOT_START + i);
            assertEquals(8 + i * 18, blueprint.x, "蓝图槽 x");
            assertEquals(34, blueprint.y, "蓝图槽 y");
            // 重叠坐标是有意设计：findSlot 先命中蓝图层，Screen 再按 tab 重映射到 CopySlot
            assertEquals(blueprint.x, copy.x, "复印槽应与蓝图槽同 x");
            assertEquals(blueprint.y, copy.y, "复印槽应与蓝图槽同 y");
            assertTrue(copy.index < BlueprintChestMenu.PLAYER_INV_START, "复印槽必须落在玩家背包之前");
        }
        assertEquals(80, menu.slots.get(BlueprintChestBlockEntity.PAPER_SLOT).x, "纸槽 x");
        assertEquals(67, menu.slots.get(BlueprintChestBlockEntity.PAPER_SLOT).y, "纸槽 y");
    }

    /** 复印 N 号槽必须取 N 号蓝图，不能串位（重映射 +COPY_SLOT_START 的前提）。 */
    @Test
    void eachCopySlotPreviewsItsOwnBlueprint() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);

        var blueprints = new java.util.ArrayList<ItemStack>();
        for (var holder : com.piranport.registry.WeaponItems.EQUIPMENT_BLUEPRINTS.values()) {
            blueprints.add(new ItemStack(holder.get()));
            if (blueprints.size() == BlueprintChestBlockEntity.BLUEPRINT_SLOTS) break;
        }
        assertEquals(BlueprintChestBlockEntity.BLUEPRINT_SLOTS, blueprints.size(), "至少需要 9 种专属蓝图");
        for (int i = 0; i < blueprints.size(); i++) {
            chest.getItemHandler().setStackInSlot(i, blueprints.get(i).copy());
        }
        chest.getItemHandler().setStackInSlot(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER, 64));

        for (int i = 0; i < blueprints.size(); i++) {
            ItemStack preview = menu.slots.get(BlueprintChestMenu.COPY_SLOT_START + i).getItem();
            assertEquals(blueprints.get(i).getItem(), preview.getItem(), "复印槽 " + i + " 串位");
        }
    }

    /**
     * 崩溃根因的直指回归：registerAll 的飞机循环要读 AIRCRAFT_TIERS。
     * 若 AIRCRAFT_TIERS 声明在 static 块之后，类初始化时它还是 null →
     * ExceptionInInitializerError → 首次校验蓝图（isWorkbenchBlueprint）即崩溃。
     */
    @Test
    void aircraftRecipesAreBuiltDuringStaticInit() {
        var tab4 = com.piranport.crafting.WeaponWorkbenchRecipeRegistry.getRecipesForTab(4);
        assertFalse(tab4.isEmpty(), "静态初始化应读到 AIRCRAFT_TIERS 并生成飞机配方");
        assertTrue(tab4.stream().anyMatch(r ->
                        BuiltInRegistries.ITEM.getKey(r.result()).getPath().equals("f4u_corsair")),
                "改良机 f4u_corsair 应出现在飞机页配方里");
    }

    /** 同一种蓝图不允许放进第二个格子（策划决策 资源/01）。 */
    @Test
    void duplicateBlueprintIsRejectedByHandler() {
        BlueprintChestBlockEntity chest = newChest();
        ItemStack blueprint = anyBlueprint();
        assertTrue(chest.getItemHandler().insertItem(0, blueprint.copy(), false).isEmpty());
        ItemStack rejected = chest.getItemHandler().insertItem(1, blueprint.copy(), false);
        assertFalse(rejected.isEmpty(), "同一种蓝图不应进入第二个格子");
    }

    /**
     * GUI 放置不走 insertItem，而是 SafeInsert / setByPlayer → setStackInSlot。
     * 去重必须挂在这些路径共同的前置闸门 mayPlace 上，且拒绝时物品要留在手里。
     */
    @Test
    void guiPlacementRejectsDuplicateBlueprintWithoutLosingItem() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);
        ItemStack blueprint = anyBlueprint();
        chest.getItemHandler().setStackInSlot(0, blueprint.copy());

        Slot second = menu.slots.get(1);
        assertFalse(second.mayPlace(blueprint), "同种蓝图必须被第二个格子拒绝");
        ItemStack left = second.safeInsert(blueprint.copy(), 1);
        assertEquals(1, left.getCount(), "被拒绝的蓝图必须留在手里，不能被 safeInsert 吞掉");
        assertTrue(second.getItem().isEmpty());

        // 目标槽自身不能把自己挡住：否则已放入的那份无法再参与同格点击
        assertTrue(menu.slots.get(0).mayPlace(blueprint), "已占用槽不应因自己而拒绝");

        // 不同蓝图各占一格
        ItemStack other = anotherBlueprint(blueprint);
        assertTrue(second.mayPlace(other), "不同蓝图应可共存");
        assertTrue(second.safeInsert(other.copy(), 1).isEmpty());
        assertFalse(second.getItem().isEmpty());
    }

    /** 旧存档可能已经存了重复蓝图：读档必须原样恢复，去重只拦新放入。 */
    @Test
    void duplicateBlueprintsFromOldSaveSurviveNbtLoad() {
        ItemStack blueprint = anyBlueprint();
        BlueprintChestBlockEntity source = newChest();
        source.getItemHandler().setStackInSlot(0, blueprint.copy());
        source.getItemHandler().setStackInSlot(1, blueprint.copy());
        CompoundTag tag = source.getItemHandler().serializeNBT(RegistryAccess.EMPTY);

        BlueprintChestBlockEntity loaded = newChest();
        loaded.getItemHandler().deserializeNBT(RegistryAccess.EMPTY, tag);

        assertEquals(blueprint.getItem(), loaded.getItemHandler().getStackInSlot(0).getItem());
        assertEquals(blueprint.getItem(), loaded.getItemHandler().getStackInSlot(1).getItem(),
                "读档不能静默丢弃旧存档里的重复蓝图");
    }

    /** 两个重叠的槽组按标签页互斥显隐；原版 findSlot 只认 isActive 的槽。 */
    @Test
    void slotVisibilityFollowsCurrentTab() {
        BlueprintChestBlockEntity chest = newChest();
        BlueprintChestMenu menu = new BlueprintChestMenu(1, new Inventory(null), chest);

        for (int i = 0; i < BlueprintChestBlockEntity.TOTAL_SLOTS; i++) {
            assertTrue(BlueprintChestMenu.isSlotVisibleOnTab(i, 0), "查看页应显示菜单槽 " + i);
            assertFalse(BlueprintChestMenu.isSlotVisibleOnTab(i, 1), "复印页应隐藏菜单槽 " + i);
        }
        for (int i = BlueprintChestMenu.COPY_SLOT_START; i < BlueprintChestMenu.PLAYER_INV_START; i++) {
            assertFalse(BlueprintChestMenu.isSlotVisibleOnTab(i, 0), "查看页应隐藏复印槽 " + i);
            assertTrue(BlueprintChestMenu.isSlotVisibleOnTab(i, 1), "复印页应显示复印槽 " + i);
        }
        for (int i = BlueprintChestMenu.PLAYER_INV_START; i < BlueprintChestMenu.PLAYER_INV_END; i++) {
            assertTrue(BlueprintChestMenu.isSlotVisibleOnTab(i, 0), "玩家背包应始终可见");
            assertTrue(BlueprintChestMenu.isSlotVisibleOnTab(i, 1), "玩家背包应始终可见");
        }

        menu.setCurrentTab(0);
        for (int i = 0; i < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; i++) {
            assertTrue(menu.slots.get(i).isActive(), "查看页蓝图槽应可见");
            assertFalse(menu.slots.get(BlueprintChestMenu.COPY_SLOT_START + i).isActive(),
                    "查看页复印槽应隐藏（坐标与蓝图槽重叠）");
        }
        assertTrue(menu.slots.get(BlueprintChestBlockEntity.PAPER_SLOT).isActive());
        assertTrue(menu.slots.get(BlueprintChestMenu.PLAYER_INV_START).isActive());

        menu.setCurrentTab(1);
        for (int i = 0; i < BlueprintChestBlockEntity.BLUEPRINT_SLOTS; i++) {
            assertFalse(menu.slots.get(i).isActive(), "复印页蓝图槽应隐藏");
            assertTrue(menu.slots.get(BlueprintChestMenu.COPY_SLOT_START + i).isActive(),
                    "复印页复印槽应可见");
        }
        assertFalse(menu.slots.get(BlueprintChestBlockEntity.PAPER_SLOT).isActive(),
                "复印页不应显示纸槽");
        assertTrue(menu.slots.get(BlueprintChestMenu.PLAYER_INV_START).isActive());
    }

    @Test
    void nonBlueprintAndPaperRouting() {
        BlueprintChestBlockEntity chest = newChest();
        assertFalse(chest.getItemHandler().isItemValid(0, new ItemStack(Items.PAPER)));
        assertTrue(chest.getItemHandler().isItemValid(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER)));
    }
}
