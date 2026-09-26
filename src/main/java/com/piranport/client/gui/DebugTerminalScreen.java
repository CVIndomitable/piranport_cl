package com.piranport.client.gui;

import com.piranport.client.input.DebugInputHandler;
import com.piranport.menu.DebugTerminalMenu;
import com.piranport.network.DebugCooldownOverridePayload;
import com.piranport.network.DebugTogglePayload;
import com.piranport.network.HitDisplayTogglePayload;
import com.piranport.network.SnapshotRequestPayload;
import com.piranport.network.SaveTerminalParametersPayload;
import com.piranport.network.TerminalParameterActionPayload;
import com.piranport.network.UpdateTerminalParameterPayload;
import com.piranport.terminal.TerminalParameterSpec;
import com.piranport.terminal.TerminalParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;

/** 服务端参数快照的编辑界面。草稿只在明确确认时提交。 */
@OnlyIn(Dist.CLIENT)
public class DebugTerminalScreen extends AbstractContainerScreen<DebugTerminalMenu> {
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 61;
    private final Map<String, String> drafts = new HashMap<>();
    private final Map<String, EditBox> editors = new HashMap<>();
    private List<TerminalParameterSpec> visible = List.of();
    private String query = "";
    private String filename = "terminal-parameters.csv";
    private String selectedKey;
    private String localStatus = "";
    private int page;
    private boolean byProperty;
    private int scrollOffset;
    private int visibleRows;
    private long lastRevision = Long.MIN_VALUE;
    private long lastSequence = Long.MIN_VALUE;
    private boolean resetConfirmation;
    private boolean searchDirty;
    private List<TerminalParameterSpec> lastSpecs = List.of();
    private EditBox searchBox;
    private EditBox filenameBox;
    private Button debugButton;
    private Button cooldownButton;
    private Button hitButton;

    public DebugTerminalScreen(DebugTerminalMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 440;
        imageHeight = 300;
    }

    private static Component label(String suffix) {
        return Component.translatable("gui.piranport.debug_terminal." + suffix);
    }

    private int left() { return (width - imageWidth) / 2; }
    private int top() { return (height - imageHeight) / 2; }

    @Override
    protected void init() {
        imageWidth = Math.min(440, width - 12);
        imageHeight = Math.min(330, height - 8);
        visibleRows = Math.max(1, (imageHeight - 68 - (LIST_TOP + 13) - 2) / ROW_HEIGHT);
        super.init();
        editors.clear();
        rebuildList();
        int x = left();
        int y = top();
        int half = (imageWidth - 24) / 2;
        addRenderableWidget(Button.builder(label("tab.parameters"), b -> switchPage(0))
                .bounds(x + 8, y + 20, half, 18).build());
        addRenderableWidget(Button.builder(label("tab.controls"), b -> switchPage(1))
                .bounds(x + 16 + half, y + 20, half, 18).build());
        if (page == 0) {
            initParameters(x, y);
        } else {
            initControls(x, y);
        }
    }

    private void initParameters(int x, int y) {
        searchBox = addRenderableWidget(new EditBox(font, x + 8, y + 40,
                imageWidth - 116, 17, label("search")));
        searchBox.setMaxLength(80);
        searchBox.setHint(label("search"));
        searchBox.setValue(query);
        searchBox.setResponder(value -> {
            if (!query.equals(value)) {
                query = value;
                scrollOffset = 0;
                rebuildList();
                searchDirty = true;
            }
        });
        addRenderableWidget(Button.builder(label(byProperty ? "mode.property" : "mode.target"),
                b -> {
                    byProperty = !byProperty;
                    scrollOffset = 0;
                    rebuildWidgets();
                }).bounds(x + imageWidth - 104, y + 40, 96, 17).build());

        int draftWidth = Math.max(64, imageWidth / 5);
        int draftX = x + imageWidth - draftWidth - 25;
        addRows(x, y, draftX, draftWidth);
        int listBottom = y + imageHeight - 68;
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> scroll(-1))
                .bounds(x + imageWidth - 19, y + LIST_TOP + 13, 14, 16).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> scroll(1))
                .bounds(x + imageWidth - 19, listBottom - 16, 14, 16).build());
        int actionY = y + imageHeight - 65;
        int gap = 4;
        int actionWidth = (imageWidth - 16 - gap * 3) / 4;
        addRenderableWidget(Button.builder(label("confirm"), b -> confirmSelected())
                .bounds(x + 8, actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("save"), b -> saveAll())
                .bounds(x + 8 + (actionWidth + gap), actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("refresh"), b -> refresh())
                .bounds(x + 8 + 2 * (actionWidth + gap), actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("reset_all"), b -> resetAll())
                .bounds(x + 8 + 3 * (actionWidth + gap), actionY, actionWidth, 18).build());

        int csvY = y + imageHeight - 43;
        filenameBox = addRenderableWidget(new EditBox(font, x + 8, csvY,
                imageWidth - 150, 17, label("filename")));
        filenameBox.setMaxLength(128);
        filenameBox.setHint(label("filename"));
        filenameBox.setValue(filename);
        filenameBox.setResponder(value -> filename = value);
        addRenderableWidget(Button.builder(label("import"), b -> sendCsv("import"))
                .bounds(x + imageWidth - 137, csvY, 64, 17).build());
        addRenderableWidget(Button.builder(label("export"), b -> sendCsv("export"))
                .bounds(x + imageWidth - 69, csvY, 61, 17).build());
    }

    private void addRows(int x, int y, int draftX, int draftWidth) {
        editors.clear();
        for (int i = 0; i < Math.min(visibleRows, visible.size() - scrollOffset); i++) {
            TerminalParameterSpec spec = visible.get(scrollOffset + i);
            int rowY = y + LIST_TOP + 13 + i * ROW_HEIGHT;
            String key = spec.key();
            if (spec.type() == TerminalParameterSpec.ValueType.BOOLEAN) {
                addRenderableWidget(Button.builder(booleanLabel(draftValue(spec)), b -> {
                    selectedKey = key;
                    String next = Boolean.toString(!Boolean.parseBoolean(draftValue(spec)));
                    drafts.put(key, next);
                    b.setMessage(booleanLabel(next));
                    localStatus = label("status.unsaved").getString();
                }).bounds(draftX, rowY, draftWidth, 16).build());
            } else {
                EditBox box = addRenderableWidget(new EditBox(font, draftX, rowY,
                        draftWidth, 16, Component.literal(key)));
                box.setMaxLength(64);
                box.setValue(draftValue(spec));
                box.setResponder(value -> {
                    drafts.put(key, value);
                    selectedKey = key;
                    localStatus = label("status.unsaved").getString();
                });
                editors.put(key, box);
            }
        }
    }

    private void initControls(int x, int y) {
        int controlWidth = Math.min(210, imageWidth - 20);
        int controlX = x + (imageWidth - controlWidth) / 2;
        debugButton = addRenderableWidget(Button.builder(toggleLabel("debug",
                        DebugInputHandler.isDebugEnabledClient()), b ->
                        PacketDistributor.sendToServer(new DebugTogglePayload(
                                !DebugInputHandler.isDebugEnabledClient())))
                .bounds(controlX, y + 49, controlWidth, 20).build());
        cooldownButton = addRenderableWidget(Button.builder(toggleLabel("cooldown",
                        DebugInputHandler.isTestModeClient()), b ->
                        PacketDistributor.sendToServer(new DebugCooldownOverridePayload(
                                !DebugInputHandler.isTestModeClient())))
                .bounds(controlX, y + 73, controlWidth, 20).build());
        hitButton = addRenderableWidget(Button.builder(toggleLabel("hit",
                        DebugInputHandler.isHitDisplayEnabled()), b ->
                        PacketDistributor.sendToServer(new HitDisplayTogglePayload(
                                !DebugInputHandler.isHitDisplayEnabled())))
                .bounds(controlX, y + 97, controlWidth, 20).build());
        addRenderableWidget(Button.builder(label("control.snapshot"), b ->
                        PacketDistributor.sendToServer(new SnapshotRequestPayload(true)))
                .bounds(controlX, y + 121, controlWidth, 20).build());
    }

    private Component toggleLabel(String name, boolean value) {
        return label("control." + name).copy().append(": ")
                .append(label("state." + (value ? "on" : "off")));
    }

    private Component booleanLabel(String value) {
        return label("state." + (Boolean.parseBoolean(value) ? "on" : "off"));
    }

    private void rebuildList() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<TerminalParameterSpec> rows = new ArrayList<>();
        for (TerminalParameterSpec spec : TerminalParameters.clientSpecs()) {
            String terms = spec.key() + " " + spec.group() + " " + spec.target() + " " + spec.property()
                    + " " + displayName(spec);
            if (terms.toLowerCase(Locale.ROOT).contains(needle)) rows.add(spec);
        }
        Comparator<TerminalParameterSpec> comparator = byProperty
                ? Comparator.comparing(TerminalParameterSpec::property)
                    .thenComparing(TerminalParameterSpec::group)
                    .thenComparing(TerminalParameterSpec::target)
                : Comparator.comparing(TerminalParameterSpec::group)
                    .thenComparing(TerminalParameterSpec::target)
                    .thenComparing(TerminalParameterSpec::property);
        rows.sort(comparator);
        visible = rows;
        scrollOffset = Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows));
    }

    private String displayName(TerminalParameterSpec spec) {
        String key = "gui.piranport.debug_terminal.parameter." + spec.key();
        String translated = Component.translatable(key).getString();
        if (!translated.equals(key)) return translated;
        String targetKey = "item.piranport." + spec.target();
        String target = Component.translatable(targetKey).getString();
        if (target.equals(targetKey)) target = readable(spec.target());
        String group = translated("group." + spec.group(), readable(spec.group()));
        String property = translated("property." + spec.property(), readable(spec.property()));
        return byProperty ? property + " · " + group + "/" + target
                : group + "/" + target + " · " + property;
    }

    private String translated(String suffix, String fallback) {
        String key = "gui.piranport.debug_terminal." + suffix;
        String value = Component.translatable(key).getString();
        return value.equals(key) ? fallback : value;
    }

    private String readable(String value) {
        return value.replace('_', ' ');
    }

    private String currentValue(TerminalParameterSpec spec) {
        return TerminalParameters.clientValues().getOrDefault(spec.key(), spec.baseValue());
    }

    private String draftValue(TerminalParameterSpec spec) {
        return drafts.getOrDefault(spec.key(), currentValue(spec));
    }

    private void switchPage(int next) {
        if (page == next) return;
        resetConfirmation = false;
        page = next;
        rebuildWidgets();
    }

    private void scroll(int direction) {
        int next = Math.max(0, Math.min(scrollOffset + direction, Math.max(0, visible.size() - visibleRows)));
        if (next != scrollOffset) {
            scrollOffset = next;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (page == 0 && scrollY != 0) {
            scroll(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        for (Map.Entry<String, EditBox> entry : editors.entrySet()) {
            if (entry.getValue().isFocused()) {
                selectedKey = entry.getKey();
                break;
            }
        }
        return handled;
    }

    private boolean valid(TerminalParameterSpec spec, String value) {
        try {
            spec.canonical(value);
            return true;
        } catch (RuntimeException exception) {
            localStatus = label("status.invalid").getString() + ": " + spec.key();
            return false;
        }
    }

    private void confirmSelected() {
        if (selectedKey == null) {
            localStatus = label("status.select").getString();
            return;
        }
        TerminalParameterSpec spec = TerminalParameters.clientSpecs().stream()
                .filter(row -> row.key().equals(selectedKey)).findFirst().orElse(null);
        if (spec == null) return;
        String draft = draftValue(spec);
        if (!valid(spec, draft)) return;
        PacketDistributor.sendToServer(new UpdateTerminalParameterPayload(spec.key(), draft));
        localStatus = label("status.pending").getString();
    }

    private void saveAll() {
        List<TerminalParameterSpec> changed = TerminalParameters.clientSpecs().stream()
                .filter(spec -> drafts.containsKey(spec.key())
                        && !drafts.get(spec.key()).equals(currentValue(spec))).toList();
        for (TerminalParameterSpec spec : changed) {
            if (!valid(spec, drafts.get(spec.key()))) return;
        }
        if (!changed.isEmpty()) {
            Map<String, String> batch = new LinkedHashMap<>();
            for (TerminalParameterSpec spec : changed) batch.put(spec.key(), drafts.get(spec.key()));
            PacketDistributor.sendToServer(new SaveTerminalParametersPayload(batch));
        }
        localStatus = changed.isEmpty() ? label("status.no_changes").getString()
                : label("status.pending").getString();
    }

    private void refresh() {
        resetConfirmation = false;
        drafts.clear();
        selectedKey = null;
        PacketDistributor.sendToServer(new TerminalParameterActionPayload("refresh", ""));
        localStatus = label("status.pending").getString();
        rebuildWidgets();
    }

    private void resetAll() {
        if (!resetConfirmation) {
            resetConfirmation = true;
            localStatus = label("status.confirm_reset").getString();
            return;
        }
        resetConfirmation = false;
        drafts.clear();
        selectedKey = null;
        PacketDistributor.sendToServer(new TerminalParameterActionPayload("reset_all", ""));
        localStatus = label("status.pending").getString();
        rebuildWidgets();
    }

    private void sendCsv(String action) {
        PacketDistributor.sendToServer(new TerminalParameterActionPayload(action, filename.trim()));
        localStatus = label("status.pending").getString() + " · " + label("csv.folder").getString();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (searchDirty) {
            searchDirty = false;
            int cursor = searchBox.getCursorPosition();
            rebuildWidgets();
            searchBox.setCursorPosition(cursor);
            setFocused(searchBox);
            searchBox.setFocused(true);
        }
        if (lastSequence != TerminalParameters.syncSequence()
                || lastSpecs != TerminalParameters.clientSpecs()) {
            lastSequence = TerminalParameters.syncSequence();
            lastRevision = TerminalParameters.revision();
            lastSpecs = TerminalParameters.clientSpecs();
            // 服务端确认后清理已一致的草稿；其余草稿继续保留供玩家决定。
            drafts.entrySet().removeIf(entry -> TerminalParameters.clientSpecs().stream()
                    .filter(spec -> spec.key().equals(entry.getKey()))
                    .anyMatch(spec -> canonicalMatches(spec, entry.getValue())));
            localStatus = TerminalParameters.clientMessage().isBlank()
                    ? (drafts.isEmpty() ? "" : label("status.unsaved").getString())
                    : TerminalParameters.clientMessage();
            if (page == 0) rebuildWidgets();
        }
        if (page == 1) {
            debugButton.setMessage(toggleLabel("debug", DebugInputHandler.isDebugEnabledClient()));
            cooldownButton.setMessage(toggleLabel("cooldown", DebugInputHandler.isTestModeClient()));
            hitButton.setMessage(toggleLabel("hit", DebugInputHandler.isHitDisplayEnabled()));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.drawString(font, title, x + 8, y + 6, 0xFFFFFF, false);
        if (page == 0) {
            int draftWidth = Math.max(64, imageWidth / 5);
            int draftX = x + imageWidth - draftWidth - 25;
            int currentX = draftX - 80;
            graphics.drawString(font, label("header.parameter"), x + 10, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.current"), currentX, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.draft"), draftX, y + LIST_TOP, 0xFFD6D6D6, false);
            for (int i = 0; i < Math.min(visibleRows, visible.size() - scrollOffset); i++) {
                TerminalParameterSpec spec = visible.get(scrollOffset + i);
                int rowY = y + LIST_TOP + 16 + i * ROW_HEIGHT;
                int color = drafts.containsKey(spec.key())
                        && !drafts.get(spec.key()).equals(currentValue(spec))
                        ? 0xFFFFD779 : 0xFFFFFFFF;
                String name = displayName(spec);
                graphics.drawString(font, font.plainSubstrByWidth(name, currentX - x - 18),
                        x + 10, rowY, color, false);
                graphics.drawString(font, font.plainSubstrByWidth(currentValue(spec), 74),
                        currentX, rowY, 0xFFC7ECCF, false);
                if (mouseX >= x + 10 && mouseX < x + imageWidth - 20
                        && mouseY >= rowY - 3 && mouseY < rowY + 15) {
                    graphics.renderTooltip(font, Component.literal(name + " (" + spec.key() + ") · "
                            + label("header.current").getString() + ": " + currentValue(spec) + " · "
                            + label("header.draft").getString() + ": " + draftValue(spec)), mouseX, mouseY);
                }
            }
            String count = visible.isEmpty() ? "0/0" :
                    (scrollOffset + 1) + "-" + Math.min(scrollOffset + visibleRows, visible.size())
                            + "/" + visible.size();
            graphics.drawString(font, count, x + imageWidth - 83, y + imageHeight - 19,
                    0xFFD0D0D0, false);
        }
        String message = localStatus;
        if (!message.isBlank()) {
            if (page == 0 && mouseY >= y + imageHeight - 21 && mouseX < x + imageWidth - 90) {
                graphics.renderTooltip(font, Component.literal(message), mouseX, mouseY);
            }
            graphics.drawString(font, font.plainSubstrByWidth(message, imageWidth - 105),
                    x + 8, y + imageHeight - 19, 0xFFEAD890, false);
        }
    }

    private boolean canonicalMatches(TerminalParameterSpec spec, String value) {
        try {
            return spec.canonical(value).equals(currentValue(spec));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = left();
        int y = top();
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF34393C);
        graphics.fill(x, y, x + imageWidth, y + 18, 0xFF20282B);
        if (page == 0) {
            graphics.fill(x + 6, y + LIST_TOP + 11, x + imageWidth - 4,
                    y + imageHeight - 68, 0xFF292F31);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            for (EditBox box : editors.values()) {
                if (box.isFocused()) {
                    box.setFocused(false);
                    return true;
                }
            }
            if (searchBox != null && searchBox.isFocused()) {
                searchBox.setFocused(false);
                return true;
            }
            if (filenameBox != null && filenameBox.isFocused()) {
                filenameBox.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
