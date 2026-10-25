package com.piranport.dungeon.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.key.DungeonProgress;
import net.minecraft.SharedConstants;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 钥匙复制配方的<b>服务端结算</b>路径（{@code matches} / {@code assemble}）。
 *
 * <p>这里刻意不走 {@code getResultItem}：{@code CraftingMenu.slotChangedCraftingGrid} 在服务端
 * 只认 {@code assemble}，JEI / 配方书展示才用 {@code getResultItem}。本测试证明
 * 「按七格铝+铜摆好后产物章节错乱」不是 {@code assemble} 的锅，而是旧的单一
 * 「任意钥匙」Ingredient 让自动摆放从背包里抓到了第一章钥匙。</p>
 *
 * <p>注册表 bootstrap：项目其余测试都不碰注册表，所以这里手工把 DungeonKeyItem 和三个
 * DataComponentType 按正式 ID 塞进已解冻的内建注册表；随后加载 {@code ModItems} 时，
 * 它的 DeferredHolder 会按 ID 绑定到这些实例上（DeferredHolder.bind 是 Registry 查表）。</p>
 *
 * <p><b>必须用 {@code mc-bootstrap} 标签单独跑：</b>普通单测依赖「JVM 从未 bootstrap」
 * （见 {@code DungeonDataValidator.registriesAvailable()}），而 bootstrap 后的
 * {@code BuiltInRegistries} 已冻结、且会让副本数据包校验去检查测试环境里没注册的模组条目。
 * Gradle 里 {@code test} 排除本标签，{@code mcBootstrapTest} 在独立 JVM 只跑本标签。</p>
 */
@Tag("mc-bootstrap")
class KeyResetRecipeTest {

    private static final String NS = "piranport";

    private static DataComponentType<String> stageIdType;
    private static DataComponentType<UUID> instanceIdType;
    private static DataComponentType<DungeonProgress> progressType;

    private static DungeonKeyItem ch1;
    private static DungeonKeyItem ch3;
    private static DungeonKeyItem ch5;
    private static DungeonKeyItem genericKey;
    private static Item aluminum;

    private static boolean bootstrapped;

    @BeforeAll
    static void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        SharedConstants.tryDetectVersion();
        // 纯 JUnit 环境没有 FML 启动流程；FeatureFlags.<clinit> 会读 LoadingModList。
        // 塞一个空列表进去，加载器就会认为"没有模组声明特性开关"，bootstrap 得以继续。
        net.neoforged.fml.loading.LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), java.util.Map.of());
        Bootstrap.bootStrap();

        // 解冻后手工注册，模拟 RegisterEvent 的注册结果。
        unfreeze(BuiltInRegistries.DATA_COMPONENT_TYPE);
        unfreeze(BuiltInRegistries.ITEM);

        stageIdType = DataComponentType.<String>builder()
                .persistent(Codec.STRING)
                .build();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(NS, "dungeon_stage_id"), stageIdType);

        instanceIdType = DataComponentType.<UUID>builder()
                .persistent(UUIDUtil.CODEC)
                .build();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(NS, "dungeon_instance_id"), instanceIdType);

        progressType = DataComponentType.<DungeonProgress>builder()
                .persistent(DungeonProgress.CODEC)
                .build();
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(NS, "dungeon_progress"), progressType);

        ch1 = registerKey("dungeon_key_ch1", 1);
        ch3 = registerKey("dungeon_key_ch3", 3);
        ch5 = registerKey("dungeon_key_ch5", 5);
        genericKey = registerKey("dungeon_key", 0);
        // codec/展示测试要覆盖全部七章，其余章节只需注册进表。
        for (int chapter : new int[]{2, 4, 6, 7}) {
            registerKey("dungeon_key_ch" + chapter, chapter);
        }
        aluminum = new Item(new Item.Properties());
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(NS, "aluminum_ingot"), aluminum);

        // 触发 ModItems 类初始化，让它的 DeferredHolder 绑定到上面的同名条目。
        assertSame(ch3, com.piranport.registry.ModItems.DUNGEON_KEY_CH3.get(),
                "ModItems 的 ch3 钥匙应绑定到测试注册的同 ID 实例");
        assertSame(aluminum, com.piranport.registry.ModItems.ALUMINUM_INGOT.get());

        // 恢复全局状态：注册完立即冻结，避免影响同一 JVM 里其它测试类。
        freeze(BuiltInRegistries.DATA_COMPONENT_TYPE);
        freeze(BuiltInRegistries.ITEM);
    }

    /** 主线钥匙：左上 ch3 + 正中铜锭 + 其余七格铝锭。 */
    @Test
    void mainlineLayoutMatchesAndCopiesSourceChapter() {
        KeyResetRecipe recipe = new KeyResetRecipe(3, false, CraftingBookCategory.MISC);
        ItemStack usedKey = new ItemStack(ch3);
        DungeonKeyItem.setStageId(usedKey, "3-2");
        DungeonKeyItem.setProgress(usedKey, new DungeonProgress("B", Set.of("A"), Set.of("A", "B"), 1234L, true));
        DungeonKeyItem.setInstanceId(usedKey, UUID.randomUUID());

        CraftingInput input = mainlineInput(usedKey);
        assertTrue(recipe.matches(input, null), "合法摆法应匹配");

        ItemStack result = recipe.assemble(input, null);
        assertSame(ch3, result.getItem(), "复制的钥匙必须仍是 ch3 物品，不能变 ch1");
        assertEquals(3, DungeonKeyItem.getChapterNumber(result));
        assertEquals("3-2", DungeonKeyItem.getStageId(result));
        assertEquals(new DungeonProgress("B", Set.of("A"), Set.of("A", "B"), 1234L, true),
                DungeonKeyItem.getProgress(result), "进度应保留");
        assertNull(DungeonKeyItem.getInstanceId(result), "instanceId 应清除");
    }

    /**
     * 根因回归：本章配方的左上 Ingredient 只能匹配本章钥匙。
     *
     * <p>旧实现把七章钥匙塞进同一个 Ingredient，JEI「+」/ 原版配方书自动摆放时
     * 会从背包里挑第一个匹配项（通常是第一章钥匙），于是复制 5 章钥匙却产出 1 章钥匙。
     * 拆成按章配方后，ch3 配方不可能接受 ch5 钥匙，自动摆放也无从抓错。</p>
     */
    @Test
    void chapterRecipeIngredientAcceptsOnlyItsOwnKey() {
        KeyResetRecipe recipe = new KeyResetRecipe(3, false, CraftingBookCategory.MISC);
        var keyIngredient = recipe.getIngredients().get(0);
        assertTrue(keyIngredient.test(new ItemStack(ch3)));
        assertFalse(keyIngredient.test(new ItemStack(ch5)),
                "ch3 复制配方不能接受 ch5 钥匙，否则自动摆放会复制错章节");
        assertFalse(keyIngredient.test(new ItemStack(genericKey)));

        // 反向：ch3 钥匙也不能被 ch5 配方接受。
        KeyResetRecipe ch5Recipe = new KeyResetRecipe(5, false, CraftingBookCategory.MISC);
        assertFalse(ch5Recipe.getIngredients().get(0).test(new ItemStack(ch3)));
    }

    /** 空白 ch5 钥匙（无 stageId 组件）复制后仍应是 ch5。 */
    @Test
    void blankChapterKeyCopiesItsOwnItem() {
        KeyResetRecipe recipe = new KeyResetRecipe(5, false, CraftingBookCategory.MISC);
        ItemStack blankCh5 = new ItemStack(ch5);

        CraftingInput input = mainlineInput(blankCh5);
        assertTrue(recipe.matches(input, null));

        ItemStack result = recipe.assemble(input, null);
        assertSame(ch5, result.getItem());
        assertEquals(5, DungeonKeyItem.getChapterNumber(result));
    }

    /** 活动钥匙：正中末影之眼 + 七格紫水晶，产物仍应是那把活动钥匙。 */
    @Test
    void activityLayoutCopiesSourceKey() {
        KeyResetRecipe recipe = new KeyResetRecipe(0, true, CraftingBookCategory.MISC);
        ItemStack activity = new ItemStack(genericKey);
        DungeonKeyItem.setStageId(activity, "delusion_1");

        CraftingInput input = activityInput(activity);
        assertTrue(recipe.matches(input, null));

        ItemStack result = recipe.assemble(input, null);
        assertSame(genericKey, result.getItem());
        assertEquals("delusion_1", DungeonKeyItem.getStageId(result));
    }

    /**
     * 《副本/18》：金猫猫与 ulan 也是活动钥匙，必须用末影之眼 + 紫水晶摆法；
     * 早先 {@code isActivityKey} 只认 delusion_/hell_，这两把会被错当成主线钥匙。
     */
    @Test
    void goldencatcatAndUlanUseActivityLayout() {
        KeyResetRecipe activityRecipe = new KeyResetRecipe(0, true, CraftingBookCategory.MISC);
        KeyResetRecipe legacyRecipe = new KeyResetRecipe(0, false, CraftingBookCategory.MISC);

        for (String stage : new String[]{"goldencatcat", "ulan"}) {
            ItemStack key = new ItemStack(genericKey);
            DungeonKeyItem.setStageId(key, stage);
            assertTrue(activityRecipe.matches(activityInput(key), null),
                    stage + " 应该走活动钥匙摆法（末影之眼 + 紫水晶）");
            assertFalse(legacyRecipe.matches(mainlineInput(key), null),
                    stage + " 不应再被当成旧主线通用钥匙");
        }
    }

    /** 旧存档里的通用主线钥匙（stageId 如 1-1）仍保留铜锭 + 铝锭的复制摆法。 */
    @Test
    void legacyGenericMainlineKeyStillUsesCopperLayout() {
        KeyResetRecipe legacyRecipe = new KeyResetRecipe(0, false, CraftingBookCategory.MISC);
        ItemStack legacyKey = new ItemStack(genericKey);
        DungeonKeyItem.setStageId(legacyKey, "1-1");

        assertTrue(legacyRecipe.matches(mainlineInput(legacyKey), null));
        assertFalse(legacyRecipe.matches(activityInput(legacyKey), null));
    }

    /**
     * 数据包里的复制配方 JSON 必须能被 {@link KeyResetRecipe.Serializer#CODEC} 解出来，
     * 且 chapter/activity 与文件名对得上。7 章各一条 + 活动通用 + 旧通用，缺一条就复制不了对应钥匙。
     */
    @Test
    void bundledCopyRecipeJsonFilesDecode() throws Exception {
        Path dir = Path.of("src/main/resources/data/piranport/recipe");
        record Case(String file, int chapter, boolean activity) {}
        List<Case> cases = new ArrayList<>(List.of(
                new Case("dungeon_key_copy.json", 0, true),
                new Case("dungeon_key_copy_legacy.json", 0, false)));
        for (int chapter = 1; chapter <= 7; chapter++) {
            cases.add(new Case("dungeon_key_copy_ch" + chapter + ".json", chapter, false));
        }

        for (Case c : cases) {
            var json = JsonParser.parseString(Files.readString(dir.resolve(c.file()))).getAsJsonObject();
            KeyResetRecipe recipe = KeyResetRecipe.Serializer.CODEC.codec()
                    .parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
            assertEquals(c.chapter(), recipe.chapter(), c.file());
            assertEquals(c.activity(), recipe.activity(), c.file());
            assertSame(com.piranport.registry.ModItems.chapterDungeonKey(c.chapter()),
                    recipe.getResultItem(null).getItem(), c.file() + " 的展示产物章节不对");
        }

        try (var paths = Files.list(dir)) {
            assertEquals(7, paths.filter(p -> p.getFileName().toString()
                    .matches("dungeon_key_copy_ch[1-7]\\.json")).count(),
                    "七章各需要一条复制配方，否则该章钥匙无法复制");
        }
    }

    /**
     * 展示侧回归：本章配方展示的结果必须是本章钥匙，不能是第一章（木）钥匙。
     * 旧实现只有一条通用配方，{@code getResultItem} 硬编码 ch1，JEI 会把任何章节的复制
     * 都显示成木钥匙。
     */
    @Test
    void displayResultIsTheRecipeOwnChapterKey() {
        KeyResetRecipe ch3Recipe = new KeyResetRecipe(3, false, CraftingBookCategory.MISC);
        ItemStack shown = ch3Recipe.getResultItem(null);
        assertFalse(shown.isEmpty(), "结果为空会被 JEI 直接过滤掉（CategoryRecipeValidator）");
        assertSame(ch3, shown.getItem(), "ch3 复制配方应展示 ch3 钥匙");
        assertNotSame(ch1, shown.getItem(), "不能展示成第一章（木）钥匙");

        KeyResetRecipe ch5Recipe = new KeyResetRecipe(5, false, CraftingBookCategory.MISC);
        assertSame(ch5, ch5Recipe.getResultItem(null).getItem());
    }

    /**
     * 记录一条原版限制（不是本模组能靠配方侧修掉的）：配方书放料时用
     * {@code StackedContents.fromStackingIndex()} 新建<b>默认组件</b>的栈，再用
     * {@code Inventory.findSlotMatchingUnusedItem()}（{@code isSameItemSameComponents}）
     * 去背包里找源物品。带 progress / stageId / instanceId 的钥匙因此永远匹配不上，
     * 点配方书里的复制配方只会放上铜锭和铝锭、放不进钥匙。
     *
     * <p>这也是为什么不能靠「让玩家走配方书」来复制：必须手工把钥匙放到左上格。</p>
     */
    @Test
    void vanillaRecipeBookCannotPlaceKeyWithComponents() {
        ItemStack usedKey = new ItemStack(ch3);
        DungeonKeyItem.setStageId(usedKey, "3-2");
        DungeonKeyItem.setProgress(usedKey,
                new DungeonProgress("B", Set.of("A"), Set.of("A"), 100L, true));
        DungeonKeyItem.setInstanceId(usedKey, UUID.randomUUID());

        // 记账只看物品 id：配方书认为"背包里有可用的钥匙"。
        assertEquals(
                net.minecraft.world.entity.player.StackedContents.getStackingIndex(new ItemStack(ch3)),
                net.minecraft.world.entity.player.StackedContents.getStackingIndex(usedKey));

        // 放料时却拿默认组件栈去找，组件不同 → 找不到。
        ItemStack representative = net.minecraft.world.entity.player.StackedContents
                .fromStackingIndex(net.minecraft.world.entity.player.StackedContents.getStackingIndex(usedKey));
        assertTrue(representative.is(ch3));
        assertFalse(ItemStack.isSameItemSameComponents(representative, usedKey),
                "带组件的已使用钥匙与原版配方书的默认组件栈不相等，配方书放不进合成格");
    }

    private static CraftingInput mainlineInput(ItemStack key) {
        List<ItemStack> items = new ArrayList<>();
        items.add(key);
        for (int i = 1; i < 9; i++) {
            items.add(i == 4 ? new ItemStack(Items.COPPER_INGOT) : new ItemStack(aluminum));
        }
        return CraftingInput.of(3, 3, items);
    }

    private static CraftingInput activityInput(ItemStack key) {
        List<ItemStack> items = new ArrayList<>();
        items.add(key);
        for (int i = 1; i < 9; i++) {
            items.add(i == 4 ? new ItemStack(Items.ENDER_EYE)
                    : new ItemStack(Items.AMETHYST_SHARD));
        }
        return CraftingInput.of(3, 3, items);
    }

    private static DungeonKeyItem registerKey(String id, int chapter) {
        DungeonKeyItem item = new DungeonKeyItem(chapter, new Item.Properties().stacksTo(1)
                .component(progressType, DungeonProgress.EMPTY));
        Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(NS, id), item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static <T> void unfreeze(Registry<T> registry) {
        ((MappedRegistry<T>) registry).unfreeze();
    }

    private static <T> void freeze(Registry<T> registry) {
        registry.freeze();
    }
}
