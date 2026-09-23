package com.piranport.client.gui;

import com.piranport.item.ShipType;
import com.piranport.item.TorpedoItem;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.network.ResetTerminalOverridesPayload;
import com.piranport.network.UpdateTerminalOverridePayload;
import com.piranport.terminal.TerminalOverrides;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 调试终端界面（客户端渲染）。
 *
 * <p>结构对齐 {@link ArtilleryConfigToolScreen}（同为「无槽位 + 自定义网络包」的配置界面），
 * 但补掉那边三个已知缺陷：
 * <ol>
 *   <li>那边只在 {@link #removed()} 里统一提交，玩家输完必须关界面才生效。
 *       这里改成编辑框失焦即时提交（{@link #commitField}），{@code removed()} 只做兜底。</li>
 *   <li>那边的 {@code keyPressed} 直接吃掉 ESC，输入框里按 ESC 会连带关界面；
 *       这里先让聚焦中的编辑框消费按键，再走快捷键。</li>
 *   <li>那边切标签页只改本地 {@code currentTab}，菜单侧不知道；
 *       这里的标签页本身就是客户端渲染态，切换后统一 {@link #rebuildWidgets()} 重建，
 *       并把当前页写回本地字段供提交时判定域。</li>
 * </ol>
 *
 * <p>数值单位：鱼雷航速一律显示/录入<b>原始 blocks/tick</b>，不做任何换算 ——
 * 界面上看到的数就是 {@code TorpedoItem.getSpeed()} 返回的数。
 */
@OnlyIn(Dist.CLIENT)
public class DebugTerminalScreen extends AbstractContainerScreen<DebugTerminalMenu> {

    // ===== 标签页 =====
    private static final int TAB_TORPEDO = 0;
    private static final int TAB_CORE = 1;
    private int currentTab = TAB_TORPEDO;

    // ===== 布局 =====
    private static final int VISIBLE_ROWS = 9;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 50;       // 列表区相对 y 的偏移
    private static final int EDIT_X = 200;        // 编辑框相对 x 的偏移
    private static final int EDIT_WIDTH = 50;
    private static final int EDIT_HEIGHT = 16;

    /** 每行编辑框，key = 「域:键」（如 {@code torpedo:torpedo_533mm_mk14} / {@code core:LARGE}）。 */
    private final Map<String, EditBox> editBoxes = new LinkedHashMap<>();

    /**
     * 上次提交过的文本，key 同 {@link #editBoxes}。
     *
     * <p>WHY 要记这一份：失焦提交是「值变了就发包」，而 rebuildWidgets 会重建编辑框，
     * 没有基准线的话每切一次标签页都会把当前所有值重发一遍网络包。存下来后只发真正改动过的行。
     */
    private final Map<String, String> lastSubmitted = new LinkedHashMap<>();

    // ===== 数据缓存 =====
    /** 鱼雷型号：裸注册 ID → 基准航速。按注册 ID 排序，保证多次打开顺序一致。 */
    private final Map<String, Float> torpedoModels = new LinkedHashMap<>();
    /** 舰型：枚举常量名 → 空载倍率（界面用作「基准值」展示）。 */
    private final Map<String, Double> shipTypes = new LinkedHashMap<>();

    private int scrollOffset = 0;

    /**
     * 上次刷新编辑框内容时 {@link TerminalOverrides} 的版本号。
     *
     * <p>WHY 需要它：编辑框文本是 {@link #createRowWidgets()} 里按当时镜像算出来的一次性字符串，
     * 之后服务端把覆盖清了（重置按钮 / 别人改了同一存档）屏上还是旧数字。
     * 在 {@link #render} 里比对版本号，变了就把所有未聚焦的框刷成权威值。
     */
    private long lastSeenRevision = -1L;

    public DebugTerminalScreen(DebugTerminalMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 240;
        collectTorpedoModels();
        collectShipTypes();
    }

    /**
     * 扫描注册表收集所有带 modelKey 的鱼雷。
     *
     * <p>WHY 扫注册表而不是写死型号清单：型号清单散在 {@code AmmoItems} 的几十个注册点里，
     * 写死意味着以后加一个鱼雷就要回来同步一次，必然漏。{@code modelKey} 非空本身就是
     * 「该型号支持覆盖」的唯一标志（见 {@code TorpedoItem} 构造器注释）。
     */
    private void collectTorpedoModels() {
        List<Map.Entry<String, Float>> found = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof TorpedoItem torpedo)) {
                continue;
            }
            String modelKey = torpedo.getModelKey();
            if (modelKey == null || modelKey.isEmpty()) {
                continue;
            }
            found.add(Map.entry(modelKey, torpedo.getBaseSpeed()));
        }
        found.sort(Comparator.comparing(Map.Entry::getKey));
        for (Map.Entry<String, Float> entry : found) {
            torpedoModels.put(entry.getKey(), entry.getValue());
        }
    }

    private void collectShipTypes() {
        for (ShipType type : ShipType.values()) {
            shipTypes.put(type.name(), type.emptySpeed);
        }
    }

    @Override
    protected void init() {
        super.init();
        editBoxes.clear();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.debug_terminal.tab.torpedo"),
                button -> switchTab(TAB_TORPEDO)
        ).bounds(x + 10, y + 25, 70, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.debug_terminal.tab.core"),
                button -> switchTab(TAB_CORE)
        ).bounds(x + 85, y + 25, 70, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("▲"),
                button -> scroll(-1)
        ).bounds(x + 235, y + 48, 15, 15).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("▼"),
                button -> scroll(1)
        ).bounds(x + 235, y + 195, 15, 15).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.debug_terminal.reset_tab"),
                button -> resetCurrentTab()
        ).bounds(x + 10, y + 215, 70, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.debug_terminal.reset_all"),
                button -> resetAll()
        ).bounds(x + 85, y + 215, 70, 20).build());

        createRowWidgets();
    }

    /** 当前标签页的行键（有序）。 */
    private List<String> currentKeys() {
        return currentTab == TAB_TORPEDO
                ? new ArrayList<>(torpedoModels.keySet())
                : new ArrayList<>(shipTypes.keySet());
    }

    /** 当前域名，与 {@link UpdateTerminalOverridePayload} 的 category 常量一致。 */
    private String currentCategory() {
        return currentTab == TAB_TORPEDO
                ? UpdateTerminalOverridePayload.CATEGORY_TORPEDO
                : UpdateTerminalOverridePayload.CATEGORY_CORE;
    }

    private void createRowWidgets() {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        List<String> keys = currentKeys();
        for (int i = 0; i < Math.min(VISIBLE_ROWS, keys.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= keys.size()) {
                break;
            }
            String key = keys.get(index);
            String mapKey = currentCategory() + ":" + key;
            int rowY = y + LIST_TOP + i * ROW_HEIGHT;

            EditBox box = new EditBox(this.font, x + EDIT_X, rowY, EDIT_WIDTH, EDIT_HEIGHT, Component.empty());
            box.setMaxLength(12);
            box.setValue(formatCurrentValue(key));
            editBoxes.put(mapKey, box);
            // 基准线必须在装 responder 之前落好：setValue 会立刻触发 responder，
            // 而此时 lastSubmitted 里还没有这一行，会被 commitField 当成"值变了"误发包。
            lastSubmitted.put(mapKey, box.getValue());
            // 失焦即提交：这是「输入的数字马上生效」的关键，removed() 只是关界面时的兜底。
            box.setResponder(value -> {
                if (!box.isFocused()) {
                    commitField(mapKey, value);
                }
            });
            this.addRenderableWidget(box);
        }
    }

    /** 界面展示的当前值：基准值 + 覆盖偏移（= 服务端实际使用的值）。 */
    private String formatCurrentValue(String key) {
        if (currentTab == TAB_TORPEDO) {
            float base = torpedoModels.getOrDefault(key, 0f);
            float actual = base + TerminalOverrides.torpedoSpeedDelta(key);
            return String.format(java.util.Locale.ROOT, "%.2f", actual);
        }
        double base = shipTypes.getOrDefault(key, 0d);
        double actual = base + TerminalOverrides.coreSpeedDelta(key);
        return String.format(java.util.Locale.ROOT, "%.3f", actual);
    }

    /**
     * 提交一行：值真的变了才发包。
     *
     * @param mapKey 「域:键」
     * @param raw    编辑框文本
     */
    private void commitField(String mapKey, String raw) {
        String previous = lastSubmitted.get(mapKey);
        if (previous != null && previous.equals(raw)) {
            return;  // 没变，别发包也别记录
        }

        int sep = mapKey.indexOf(':');
        if (sep < 0) {
            return;
        }
        String category = mapKey.substring(0, sep);
        String key = mapKey.substring(sep + 1);

        double target;
        try {
            target = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            // 输入不是数字（半截负号、空串、中文输入法未上屏）：退回显示值，不发包。
            // WHY 不弹错误提示：这是每敲一个键都会走的路径，弹提示会刷屏。
            revertValue(mapKey, key, category);
            return;
        }
        if (!Double.isFinite(target)) {
            revertValue(mapKey, key, category);
            return;
        }

        double base = baseValue(category, key);
        double delta = target - base;

        // 零化阈值：显示精度（%.2f/%.3f）低于基准值的真实精度，把显示串原样提交回来会算出
        // 1e-8 量级的残差（例：(double)0.7f = 0.699999988，0.70 - 它 ≈ 1.19e-8）。
        // 不零化就会落一条玩家从未有意设置的覆盖，星标常亮。delta=0 在数据层等价于删除覆盖。
        if (Math.abs(delta) < 1e-4) {
            delta = 0d;
        }

        double applied = clampClientSide(category, delta);

        // 数值等价的写法（0.58 vs 0.580）不该算改动：比较的是解析后的目标值，
        // 而不是文本。文本比较会放过 0.58→0.580 这种纯写法的差异。
        Double previousTarget = parseOrNull(previous);
        if (previousTarget != null && Math.abs(previousTarget - target) < 1e-4) {
            return;
        }

        PacketDistributor.sendToServer(new UpdateTerminalOverridePayload(category, key, applied));

        // 把编辑框刷成「实际生效的绝对值」：越界输入（填 999 被钳到 2.0）后框里必须显示钳后值，
        // 否则玩家看到的永远是那个从没生效过的数字，且再点一次还会被文本比较判为「没变」。
        EditBox box = editBoxes.get(mapKey);
        if (box != null && !box.isFocused()) {
            String shown = formatAbsoluteValue(category, base + applied);
            if (!shown.equals(box.getValue())) {
                box.setValue(shown);  // 未聚焦，不会重入 responder
            }
            lastSubmitted.put(mapKey, shown);
        } else {
            lastSubmitted.put(mapKey, raw);
        }
    }

    /** 与 {@link #formatCurrentValue} 同一套格式，但输入是绝对值而非型号键。 */
    private String formatAbsoluteValue(String category, double value) {
        return UpdateTerminalOverridePayload.CATEGORY_TORPEDO.equals(category)
                ? String.format(java.util.Locale.ROOT, "%.2f", value)
                : String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static Double parseOrNull(String text) {
        if (text == null) {
            return null;
        }
        try {
            double v = Double.parseDouble(text.trim());
            return Double.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 基准值（不含覆盖）：鱼雷 = 注册时写死的航速，舰型 = 空载倍率。 */
    private double baseValue(String category, String key) {
        if (UpdateTerminalOverridePayload.CATEGORY_TORPEDO.equals(category)) {
            return torpedoModels.getOrDefault(key, 0f);
        }
        return shipTypes.getOrDefault(key, 0d);
    }

    /**
     * 客户端侧先钳一次区间。
     *
     * <p>WHY 服务端已经钳了（{@code TerminalOverridesSavedData.clamp*} 与包 handler 各一道），
     * 这里再钳是为了让输入框立刻显示钳后值 —— 玩家填 999 时看到的应当是「实际生效的那个数」，
     * 而不是等下一次同步才被改回去。两边用同一组常量，不会出现第三种口径。
     */
    private double clampClientSide(String category, double delta) {
        if (UpdateTerminalOverridePayload.CATEGORY_TORPEDO.equals(category)) {
            return Math.max(TerminalOverrides.TORPEDO_DELTA_MIN,
                    Math.min(TerminalOverrides.TORPEDO_DELTA_MAX, delta));
        }
        return Math.max(TerminalOverrides.CORE_SPEED_MIN,
                Math.min(TerminalOverrides.CORE_SPEED_MAX, delta));
    }

    private void revertValue(String mapKey, String key, String category) {
        EditBox box = editBoxes.get(mapKey);
        if (box == null) {
            return;
        }
        String restored = formatCurrentValue(key);
        box.setValue(restored);
        lastSubmitted.put(mapKey, restored);
    }

    /** 把界面上所有已改动但还没提交的行提交掉（切页/滚动/关界面之前调用）。 */
    private void commitVisibleEdits() {
        for (Map.Entry<String, EditBox> entry : editBoxes.entrySet()) {
            EditBox box = entry.getValue();
            commitField(entry.getKey(), box.getValue());
        }
    }

    private void switchTab(int tab) {
        if (currentTab == tab) {
            return;
        }
        commitVisibleEdits();
        currentTab = tab;
        scrollOffset = 0;
        this.rebuildWidgets();
    }

    private void scroll(int direction) {
        int maxItems = currentKeys().size();
        int next = scrollOffset + direction;
        if (next < 0 || next > Math.max(0, maxItems - VISIBLE_ROWS)) {
            return;
        }
        commitVisibleEdits();
        scrollOffset = next;
        this.rebuildWidgets();
    }

    private void resetCurrentTab() {
        commitVisibleEdits();
        PacketDistributor.sendToServer(new ResetTerminalOverridesPayload(currentCategory(), ""));
        // 本地镜像的清空交给服务端回推的 sync 包，不在本地瞎猜 —— 服务端才是权威值。
    }

    private void resetAll() {
        commitVisibleEdits();
        PacketDistributor.sendToServer(new ResetTerminalOverridesPayload("all", ""));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshFromMirrorIfChanged();
        super.render(graphics, mouseX, mouseY, partialTick);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.drawString(this.font, this.title, x + 8, y + 6, 0xFFFFFF, false);

        // 表头
        graphics.drawString(this.font,
                Component.translatable("gui.piranport.debug_terminal.header.model").getString(),
                x + 10, y + 38, 0xFFD0D0D0, false);
        graphics.drawString(this.font,
                Component.translatable("gui.piranport.debug_terminal.header.base").getString(),
                x + 148, y + 38, 0xFFD0D0D0, false);
        graphics.drawString(this.font,
                Component.translatable("gui.piranport.debug_terminal.header.value").getString(),
                x + EDIT_X, y + 38, 0xFFD0D0D0, false);

        List<String> keys = currentKeys();
        for (int i = 0; i < Math.min(VISIBLE_ROWS, keys.size() - scrollOffset); i++) {
            int index = scrollOffset + i;
            if (index >= keys.size()) {
                break;
            }
            String key = keys.get(index);
            int rowY = y + LIST_TOP + i * ROW_HEIGHT + 4;

            graphics.drawString(this.font, displayName(key), x + 10, rowY, 0xFFFFFF, false);

            // 基准值只读展示，玩家改的是右侧编辑框里的绝对值。
            graphics.drawString(this.font, baseDisplay(key), x + 148, rowY, 0xFFB0B0B0, false);

            // 有覆盖的行加一个角标，一眼看出哪些型号被改过。
            if (hasOverride(key)) {
                graphics.drawString(this.font, "*", x + EDIT_X - 8, rowY, 0xFFFF55, false);
            }
        }

        // 滚动位置提示（行数超过一屏时才显示）
        if (keys.size() > VISIBLE_ROWS) {
            String pos = (scrollOffset + 1) + "-"
                    + Math.min(scrollOffset + VISIBLE_ROWS, keys.size()) + "/" + keys.size();
            graphics.drawString(this.font, pos, x + 228, y + 220, 0xFFD0D0D0, false);
        }
    }

    /** 行名：型号走物品翻译键，舰型走终端自己的翻译键。 */
    private String displayName(String key) {
        if (currentTab == TAB_TORPEDO) {
            return Component.translatable("item.piranport." + key).getString();
        }
        return Component.translatable("gui.piranport.debug_terminal.ship_type." + key).getString();
    }

    private String baseDisplay(String key) {
        if (currentTab == TAB_TORPEDO) {
            return String.format(java.util.Locale.ROOT, "%.2f", torpedoModels.getOrDefault(key, 0f));
        }
        return String.format(java.util.Locale.ROOT, "%.3f", shipTypes.getOrDefault(key, 0d));
    }

    private boolean hasOverride(String key) {
        if (currentTab == TAB_TORPEDO) {
            return TerminalOverrides.torpedoSpeedDelta(key) != 0f;
        }
        return TerminalOverrides.coreSpeedDelta(key) != 0d;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        graphics.fill(x, y, x + this.imageWidth, y + 210, 0xFF8B8B8B);
        graphics.fill(x, y, x + this.imageWidth, y + 20, 0xFF5A5A5A);
        graphics.fill(x + 5, y + 48, x + 232, y + 210, 0xFF6B6B6B);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // 空实现：标题与表头已在 render() 手工绘制，避免再渲染物品栏标签。
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 编辑框聚焦时按 ESC：取消这次输入，不关界面。
        // WHY 单独处理而不是交给 EditBox.keyPressed：MC 的 EditBox.keyPressed 里
        // 根本没有 ESC(256) 分支，default 一律 return false，按键会穿透到
        // super.keyPressed → shouldCloseOnEsc → 关界面 —— 与「取消输入」的意图相悖。
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            for (Map.Entry<String, EditBox> entry : editBoxes.entrySet()) {
                EditBox box = entry.getValue();
                if (box.isFocused()) {
                    int sep = entry.getKey().indexOf(':');
                    String key = sep < 0 ? "" : entry.getKey().substring(sep + 1);
                    box.setFocused(false);
                    // 还原成当前镜像值，丢掉输了一半的文本。
                    String restored = formatCurrentValue(key);
                    box.setValue(restored);
                    lastSubmitted.put(entry.getKey(), restored);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        // 编辑框聚焦时先让它吃按键（数字、退格、方向键）。
        for (EditBox box : editBoxes.values()) {
            if (box.isFocused() && box.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 镜像版本号变了就把编辑框刷成权威值。
     *
     * <p>跳过聚焦中的框：玩家正输一半时被服务端同步打断会很难用。那个框自己失焦时
     * 会走 {@code commitField}，届时以玩家输入为准（服务端可能再钳一次并回推）。
     *
     * <p>刷新后必须同步 {@link #lastSubmitted}，否则下一帧的 {@code commitField}
     * 会把这批「界面刷新」误判成玩家输入，把刚同步来的值再原样发回服务端。
     */
    private void refreshFromMirrorIfChanged() {
        long rev = TerminalOverrides.revision();
        if (rev == lastSeenRevision) {
            return;
        }
        lastSeenRevision = rev;

        for (Map.Entry<String, EditBox> entry : editBoxes.entrySet()) {
            EditBox box = entry.getValue();
            if (box.isFocused()) {
                continue;
            }
            int sep = entry.getKey().indexOf(':');
            if (sep < 0) {
                continue;
            }
            String refreshed = formatCurrentValue(entry.getKey().substring(sep + 1));
            if (!refreshed.equals(box.getValue())) {
                box.setValue(refreshed);
            }
            lastSubmitted.put(entry.getKey(), refreshed);
        }
    }

    @Override
    public void removed() {
        super.removed();
        // 兜底：正常路径下失焦已经提交过，这里只处理「输入着就直接关界面」的情况。
        commitVisibleEdits();
    }
}
