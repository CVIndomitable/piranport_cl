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
import net.minecraft.core.registries.BuiltInRegistries;
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

    @Test
    void nonBlueprintAndPaperRouting() {
        BlueprintChestBlockEntity chest = newChest();
        assertFalse(chest.getItemHandler().isItemValid(0, new ItemStack(Items.PAPER)));
        assertTrue(chest.getItemHandler().isItemValid(BlueprintChestBlockEntity.PAPER_SLOT,
                new ItemStack(Items.PAPER)));
    }
}
