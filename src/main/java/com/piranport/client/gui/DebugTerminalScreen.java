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
    private static final int PAGE_PARAMETERS = 0;
    private static final int PAGE_CONTROLS = 1;
    private static final int ROW_HEIGHT = 18;
    private static final int LIST_TOP = 68;
    private static final int TARGET_ROW_HEIGHT = 21;
    private static final List<String> CATEGORY_ORDER = List.of(
            "aircraft", "cannon", "enhancement", "deep_ocean", "vanilla_mob", "ship_girl",
            "ammo", "torpedo", "core", "projectile", "system");
    private static final List<String> CONTROL_TOOLTIP_KEYS = List.of(
            "debug.tooltip", "cooldown.tooltip", "hit.tooltip", "snapshot.tooltip");
    private final Map<String, String> drafts = new HashMap<>();
    private final Map<String, EditBox> editors = new HashMap<>();
    private List<TerminalParameterSpec> visible = List.of();
    private List<TargetChoice> targets = List.of();
    private String query = "";
    private String filename = "terminal-parameters.csv";
    private String selectedKey;
    private String localStatus = "";
    private int page = PAGE_PARAMETERS;
    private boolean detailView;
    private String selectedCategory = "aircraft";
    private String selectedGroup;
    private String selectedTarget;
    private int scrollOffset;
    private int targetScrollOffset;
    private int categoryScrollOffset;
    private int visibleRows;
    private int targetVisibleRows;
    private int categoryVisibleRows;
    private long lastRevision = Long.MIN_VALUE;
    private long lastSequence = Long.MIN_VALUE;
    private boolean resetConfirmation;
    private boolean resetTargetConfirmation;
    private boolean searchDirty;
    private List<TerminalParameterSpec> lastSpecs = List.of();
    private EditBox searchBox;
    private EditBox filenameBox;
    private Button debugButton;
    private Button cooldownButton;
    private Button hitButton;

    private record TargetChoice(String category, String group, String target, int parameterCount) {}

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
        targetVisibleRows = Math.max(1, (imageHeight - 68 - 64) / TARGET_ROW_HEIGHT);
        categoryVisibleRows = targetVisibleRows;
        super.init();
        editors.clear();
        searchBox = null;
        filenameBox = null;
        debugButton = null;
        cooldownButton = null;
        hitButton = null;
        rebuildList();
        int x = left();
        int y = top();
        int half = (imageWidth - 24) / 2;
        addRenderableWidget(Button.builder(label("tab.parameters"), b -> switchPage(0))
                .bounds(x + 8, y + 20, half, 18).build());
        addRenderableWidget(Button.builder(label("tab.controls"), b -> switchPage(1))
                .bounds(x + 16 + half, y + 20, half, 18).build());
        if (page == PAGE_PARAMETERS) {
            initParameters(x, y);
        } else {
            initControls(x, y);
        }
    }

    private void initParameters(int x, int y) {
        if (!detailView) {
            searchBox = addRenderableWidget(new EditBox(font, x + 8, y + 40,
                    imageWidth - 16, 17, label("search_target")));
            searchBox.setMaxLength(80);
            searchBox.setHint(label("search_target"));
            searchBox.setValue(query);
            searchBox.setResponder(value -> {
                if (!query.equals(value)) {
                    query = value;
                    scrollOffset = 0;
                    targetScrollOffset = 0;
                    rebuildList();
                    searchDirty = true;
                }
            });
        } else {
            searchBox = null;
        }

        if (detailView) {
            initParameterDetail(x, y);
        } else {
            initTargetSelection(x, y);
        }
    }

    private void initTargetSelection(int x, int y) {
        int contentTop = y + 63;
        int categoryWidth = Math.min(142, Math.max(112, imageWidth / 3));
        int targetX = x + categoryWidth + 12;
        int targetWidth = imageWidth - categoryWidth - 20;
        int maxCategoryOffset = Math.max(0, CATEGORY_ORDER.size() - categoryVisibleRows);
        categoryScrollOffset = Math.min(categoryScrollOffset, maxCategoryOffset);
        for (int index = 0; index < Math.min(categoryVisibleRows,
                CATEGORY_ORDER.size() - categoryScrollOffset); index++) {
            String category = CATEGORY_ORDER.get(categoryScrollOffset + index);
            int rowY = contentTop + index * TARGET_ROW_HEIGHT;
            int count = targetCount(category);
            addRenderableWidget(Button.builder(
                    categoryLabel(category).copy().append(" (" + count + ")"),
                    b -> selectCategory(category))
                    .bounds(x + 8, rowY, categoryWidth - 24, 18).build());
        }
        if (maxCategoryOffset > 0) {
            if (categoryScrollOffset > 0) {
                addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollCategories(-1))
                        .bounds(x + categoryWidth - 13, contentTop, 14, 16).build());
            }
            if (categoryScrollOffset < maxCategoryOffset) {
                addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollCategories(1))
                        .bounds(x + categoryWidth - 13, y + imageHeight - 68, 14, 16).build());
            }
        }

        List<TargetChoice> choices = targetsForSelectedCategory();
        int maxOffset = Math.max(0, choices.size() - targetVisibleRows);
        targetScrollOffset = Math.min(targetScrollOffset, maxOffset);
        for (int index = 0; index < Math.min(targetVisibleRows, choices.size() - targetScrollOffset); index++) {
            TargetChoice choice = choices.get(targetScrollOffset + index);
            int rowY = contentTop + index * TARGET_ROW_HEIGHT;
            addRenderableWidget(Button.builder(Component.literal(targetLabel(choice)), b -> openTarget(choice))
                    .bounds(targetX, rowY, targetWidth, 18).build());
        }
        if (choices.size() > targetVisibleRows) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollTargets(-1))
                    .bounds(x + imageWidth - 19, contentTop, 14, 16).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollTargets(1))
                    .bounds(x + imageWidth - 19, y + imageHeight - 68, 14, 16).build());
        }

        int actionY = y + imageHeight - 65;
        addRenderableWidget(Button.builder(label("refresh"), b -> refresh())
                .bounds(x + 8, actionY, 90, 18).build());
        addRenderableWidget(Button.builder(label("reset_all"), b -> resetAll())
                .bounds(x + 102, actionY, 90, 18).build());
        addCsvActions(x, y);
    }

    private void initParameterDetail(int x, int y) {
        addRenderableWidget(Button.builder(label("back_to_targets"), b -> closeDetail())
                .bounds(x + 8, y + 40, 94, 18).build());

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
        int actionWidth = (imageWidth - 16 - gap * 4) / 5;
        addRenderableWidget(Button.builder(label("confirm"), b -> confirmSelected())
                .bounds(x + 8, actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("save"), b -> saveAll())
                .bounds(x + 8 + (actionWidth + gap), actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("refresh"), b -> refresh())
                .bounds(x + 8 + 2 * (actionWidth + gap), actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("reset_target"), b -> resetTarget())
                .bounds(x + 8 + 3 * (actionWidth + gap), actionY, actionWidth, 18).build());
        addRenderableWidget(Button.builder(label("reset_all"), b -> resetAll())
                .bounds(x + 8 + 4 * (actionWidth + gap), actionY, actionWidth, 18).build());
        addCsvActions(x, y);
    }

    private void addCsvActions(int x, int y) {
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
                        draftWidth, 16, Component.literal(parameterName(spec))));
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
        if (!detailView) {
            rebuildTargets();
            visible = List.of();
            return;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<TerminalParameterSpec> rows = new ArrayList<>();
        for (TerminalParameterSpec spec : TerminalParameters.clientSpecs()) {
            if (!sameTarget(spec, selectedGroup, selectedTarget)) continue;
            String terms = spec.key() + " " + spec.property() + " " + parameterName(spec);
            if (terms.toLowerCase(Locale.ROOT).contains(needle)) rows.add(spec);
        }
        rows.sort(Comparator.comparing(TerminalParameterSpec::property)
                .thenComparing(TerminalParameterSpec::key));
        visible = rows;
        scrollOffset = Math.min(scrollOffset, Math.max(0, rows.size() - visibleRows));
    }

    private void rebuildTargets() {
        String needle = query.trim().toLowerCase(Locale.ROOT);
        Map<String, TargetChoice> unique = new LinkedHashMap<>();
        for (TerminalParameterSpec spec : TerminalParameters.clientSpecs()) {
            String category = categoryFor(spec.group());
            String key = category + "\u0000" + spec.group() + "\u0000"
                    + targetIdentity(spec.group(), spec.target());
            unique.computeIfAbsent(key, ignored -> new TargetChoice(category, spec.group(), spec.target(), 0));
        }
        Map<String, Integer> counts = new HashMap<>();
        for (TerminalParameterSpec spec : TerminalParameters.clientSpecs()) {
            String key = categoryFor(spec.group()) + "\u0000" + spec.group() + "\u0000"
                    + targetIdentity(spec.group(), spec.target());
            counts.merge(key, 1, Integer::sum);
        }
        List<TargetChoice> rows = new ArrayList<>();
        for (TargetChoice choice : unique.values()) {
            TargetChoice counted = new TargetChoice(choice.category(), choice.group(), choice.target(),
                    counts.getOrDefault(choiceKey(choice), 0));
            String terms = categoryLabel(choice.category()).getString() + " " + targetLabel(counted)
                    + " " + choice.group() + " " + choice.target();
            if (terms.toLowerCase(Locale.ROOT).contains(needle)) rows.add(counted);
        }
        rows.sort(Comparator.comparingInt((TargetChoice choice) -> CATEGORY_ORDER.indexOf(choice.category()))
                .thenComparing(choice -> targetDisplayName(choice.group(), choice.target()))
                .thenComparing(TargetChoice::target));
        targets = rows;
        targetScrollOffset = Math.min(targetScrollOffset,
                Math.max(0, targetsForSelectedCategory().size() - targetVisibleRows));
    }

    private String choiceKey(TargetChoice choice) {
        return choice.category() + "\u0000" + choice.group() + "\u0000"
                + targetIdentity(choice.group(), choice.target());
    }

    private String targetIdentity(String group, String target) {
        if ("ammo".equals(group) && target != null && !target.contains(":")) {
            return "piranport:" + target;
        }
        return target;
    }

    private boolean sameTarget(TerminalParameterSpec spec, String group, String target) {
        return group != null && target != null && spec.group().equals(group) && spec.target().equals(target);
    }

    private String categoryFor(String group) {
        return switch (group) {
            case "aircraft" -> "aircraft";
            case "cannon", "artillery" -> "cannon";
            case "equipment" -> "enhancement";
            case "ship", "deep_ocean" -> "deep_ocean";
            case "vanilla_mob" -> "vanilla_mob";
            case "ship_girl" -> "ship_girl";
            case "ammo" -> "ammo";
            case "torpedo" -> "torpedo";
            case "core" -> "core";
            case "projectile", "projectiles" -> "projectile";
            default -> "system";
        };
    }

    private List<TargetChoice> targetsForSelectedCategory() {
        return targets.stream().filter(choice -> choice.category().equals(selectedCategory)).toList();
    }

    private int targetCount(String category) {
        return (int) targets.stream().filter(choice -> choice.category().equals(category))
                .map(choice -> choice.group() + "\u0000" + choice.target()).distinct().count();
    }

    private Component categoryLabel(String category) {
        return label("category." + category);
    }

    private String targetLabel(TargetChoice choice) {
        return targetDisplayName(choice.group(), choice.target()) + "  ·  "
                + choice.parameterCount() + " " + label("parameter_count").getString();
    }

    private String parameterName(TerminalParameterSpec spec) {
        String key = "gui.piranport.debug_terminal.parameter." + spec.key();
        String translated = Component.translatable(key).getString();
        if (!translated.equals(key)) return translated;
        return translated("property." + spec.property(), readable(spec.property()));
    }

    private String unitSuffix(TerminalParameterSpec spec) {
        if (spec.isLinearSpeed()) return "格/秒";
        if ("drag_coeff".equals(spec.property())) return "";
        return "";
    }

    private Component parameterDescription(TerminalParameterSpec spec) {
        String descriptionKey = "gui.piranport.debug_terminal.description." + spec.key();
        Component translated = Component.translatable(descriptionKey);
        if (!translated.getString().equals(descriptionKey)) return translated;
        String propertyDescriptionKey = "gui.piranport.debug_terminal.description.property."
                + spec.property();
        Component propertyDescription = Component.translatable(propertyDescriptionKey);
        if (!propertyDescription.getString().equals(propertyDescriptionKey)) return propertyDescription;
        String range = spec.type() == TerminalParameterSpec.ValueType.BOOLEAN
                ? "布尔开关"
                : String.format(Locale.ROOT, "范围 %.4g–%.4g", spec.displayMin(), spec.displayMax());
        String unit = unitSuffix(spec);
        String value = unit.isEmpty() ? range : range + " " + unit;
        return Component.translatable("gui.piranport.debug_terminal.description.fallback",
                parameterName(spec), value);
    }

    private String displayName(TerminalParameterSpec spec) {
        return parameterName(spec);
    }

    private String targetDisplayName(String group, String rawTarget) {
        if (rawTarget == null || rawTarget.isBlank()) return categoryLabel(categoryFor(group)).getString();
        String path = rawTarget;
        String namespace = "piranport";
        int colon = rawTarget.indexOf(':');
        if (colon > 0 && colon < rawTarget.length() - 1) {
            namespace = rawTarget.substring(0, colon);
            path = rawTarget.substring(colon + 1);
        }
        List<String> candidates = new ArrayList<>();
        // Resource IDs often carry a data-only prefix (aircraft/, entity/). The
        // actual item/entity translation is registered under the final path.
        String leaf = path;
        int slash = leaf.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < leaf.length()) leaf = leaf.substring(slash + 1);
        if ("core".equals(group)) {
            candidates.add("ship_type." + path);
        }
        candidates.add("target." + group + "." + path);
        candidates.add("target." + group + "." + leaf);
        candidates.add("item." + namespace + "." + path);
        candidates.add("item." + namespace + "." + leaf);
        if ("equipment".equals(group) && path.startsWith("ciws_")) {
            candidates.add("item." + namespace + ".auto_" + path);
        }
        candidates.add("entity." + namespace + "." + path);
        candidates.add("entity." + namespace + "." + leaf);
        candidates.add("block." + namespace + "." + path);
        candidates.add("group." + group);
        for (String suffix : candidates) {
            String key = suffix.startsWith("item.") || suffix.startsWith("entity.")
                    || suffix.startsWith("block.") ? suffix : "gui.piranport.debug_terminal." + suffix;
            String value = Component.translatable(key).getString();
            if (!value.equals(key)) return value;
        }
        // Never expose a raw registry ID in the terminal. A missing translation
        // is still identifiable by its translated category.
        return translated("group." + group, categoryLabel(categoryFor(group)).getString());
    }

    private String translated(String suffix, String fallback) {
        String key = "gui.piranport.debug_terminal." + suffix;
        String value = Component.translatable(key).getString();
        return value.equals(key) ? fallback : value;
    }

    private String readable(String value) {
        String result = value.replace('_', ' ');
        return result.isEmpty() ? value : Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    private String rawCurrentValue(TerminalParameterSpec spec) {
        return TerminalParameters.clientValues().getOrDefault(spec.key(), spec.baseValue());
    }

    private String currentValue(TerminalParameterSpec spec) {
        return formatValue(spec, rawCurrentValue(spec));
    }

    private String defaultValue(TerminalParameterSpec spec) {
        return formatValue(spec, spec.baseValue());
    }

    private String draftValue(TerminalParameterSpec spec) {
        return drafts.getOrDefault(spec.key(), currentValue(spec));
    }

    private String formatValue(TerminalParameterSpec spec, String raw) {
        String value = spec.displayValue(raw);
        if ("drag_coeff".equals(spec.property())) {
            try {
                return String.format(Locale.ROOT, "%.6e", Double.parseDouble(value));
            } catch (NumberFormatException ignored) {
                // Keep a malformed server value visible; validation will reject edits.
            }
        }
        return value;
    }

    private String computedSpeed(TerminalParameterSpec multiplierSpec, boolean defaults) {
        if (!multiplierSpec.property().endsWith("_coefficient")) return "";
        boolean aircraft = "aircraft".equals(multiplierSpec.group())
                && multiplierSpec.property().endsWith("_speed_coefficient");
        boolean entityMovement = multiplierSpec.property().startsWith("movement_speed_")
                && multiplierSpec.property().endsWith("_coefficient");
        if (!aircraft && !entityMovement) return "";
        String speedProperty = aircraft ? "panel_speed" : "movement_speed";
        TerminalParameterSpec speedSpec = TerminalParameters.clientSpecs().stream()
                .filter(candidate -> candidate.group().equals(multiplierSpec.group())
                        && multiplierSpec.target().equals(candidate.target())
                        && speedProperty.equals(candidate.property()))
                .findFirst().orElse(null);
        if (speedSpec == null) return "";
        try {
            double panelSpeed = Double.parseDouble(defaults
                    ? speedSpec.baseValue() : rawCurrentValue(speedSpec));
            double multiplier = Double.parseDouble(defaults
                    ? multiplierSpec.baseValue() : rawCurrentValue(multiplierSpec));
            double phaseFactor = aircraft ? switch (multiplierSpec.property()) {
                    case "launch_speed_coefficient", "cruise_speed_coefficient" -> 0.3;
                    case "follow_speed_coefficient" -> 0.35;
                    case "attack_speed_coefficient" -> 0.5;
                    case "return_speed_coefficient" -> 0.52;
                    case "recon_speed_coefficient" -> 0.4;
                    default -> 1.0;
                } : 1.0;
            double result = panelSpeed * 20.0 * phaseFactor * multiplier;
            return Double.isFinite(result)
                    ? Component.translatable("gui.piranport.debug_terminal.speed_value",
                            String.format(Locale.ROOT, "%.2f", result)).getString()
                    : "";
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private void selectCategory(String category) {
        if (targetCount(category) == 0) {
            localStatus = label("status.no_parameters").getString();
            return;
        }
        selectedCategory = category;
        targetScrollOffset = 0;
        localStatus = "";
        rebuildWidgets();
    }

    private void openTarget(TargetChoice choice) {
        selectedCategory = choice.category();
        selectedGroup = choice.group();
        selectedTarget = choice.target();
        detailView = true;
        query = "";
        scrollOffset = 0;
        selectedKey = null;
        resetTargetConfirmation = false;
        searchDirty = false;
        localStatus = "";
        rebuildWidgets();
    }

    private void closeDetail() {
        detailView = false;
        selectedGroup = null;
        selectedTarget = null;
        selectedKey = null;
        resetTargetConfirmation = false;
        query = "";
        scrollOffset = 0;
        searchDirty = false;
        rebuildWidgets();
    }

    private void scrollTargets(int direction) {
        int max = Math.max(0, targetsForSelectedCategory().size() - targetVisibleRows);
        int next = Math.max(0, Math.min(targetScrollOffset + direction, max));
        if (next != targetScrollOffset) {
            targetScrollOffset = next;
            rebuildWidgets();
        }
    }

    private void scrollCategories(int direction) {
        int max = Math.max(0, CATEGORY_ORDER.size() - categoryVisibleRows);
        int next = Math.max(0, Math.min(categoryScrollOffset + direction, max));
        if (next != categoryScrollOffset) {
            categoryScrollOffset = next;
            rebuildWidgets();
        }
    }

    private void switchPage(int next) {
        if (page == next) return;
        resetConfirmation = false;
        searchDirty = false;
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
        if (page == PAGE_PARAMETERS && scrollY != 0) {
            if (detailView) {
                scroll(scrollY > 0 ? -1 : 1);
            } else {
                int categoryWidth = Math.min(142, Math.max(112, imageWidth / 3));
                if (mouseX < left() + categoryWidth) {
                    scrollCategories(scrollY > 0 ? -1 : 1);
                } else {
                    scrollTargets(scrollY > 0 ? -1 : 1);
                }
            }
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
            spec.canonicalDisplay(value);
            return true;
        } catch (RuntimeException exception) {
            localStatus = label("status.invalid").getString() + ": " + parameterName(spec);
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
        PacketDistributor.sendToServer(new UpdateTerminalParameterPayload(spec.key(),
                spec.canonicalDisplay(draft)));
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
            for (TerminalParameterSpec spec : changed) {
                batch.put(spec.key(), spec.canonicalDisplay(drafts.get(spec.key())));
            }
            PacketDistributor.sendToServer(new SaveTerminalParametersPayload(batch));
        }
        localStatus = changed.isEmpty() ? label("status.no_changes").getString()
                : label("status.pending").getString();
    }

    private void refresh() {
        resetConfirmation = false;
        resetTargetConfirmation = false;
        drafts.clear();
        selectedKey = null;
        PacketDistributor.sendToServer(new TerminalParameterActionPayload("refresh", ""));
        localStatus = label("status.pending").getString();
        rebuildWidgets();
    }

    private void resetAll() {
        resetTargetConfirmation = false;
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

    private void resetTarget() {
        if (selectedGroup == null || selectedTarget == null) return;
        if (!resetTargetConfirmation) {
            resetTargetConfirmation = true;
            localStatus = label("status.confirm_reset_target").getString();
            return;
        }
        resetTargetConfirmation = false;
        drafts.entrySet().removeIf(entry -> TerminalParameters.clientSpecs().stream()
                .filter(spec -> spec.key().equals(entry.getKey()))
                .anyMatch(spec -> sameTarget(spec, selectedGroup, selectedTarget)));
        selectedKey = null;
        PacketDistributor.sendToServer(new TerminalParameterActionPayload(
                "reset_target", selectedGroup + "\u0000" + selectedTarget));
        localStatus = label("status.pending").getString();
        rebuildWidgets();
    }

    private void sendCsv(String action) {
        PacketDistributor.sendToServer(new TerminalParameterActionPayload(action, filename.trim()));
        localStatus = label("status.pending").getString() + " · " + label("csv.folder").getString();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (searchDirty && searchBox != null) {
            searchDirty = false;
            int cursor = searchBox.getCursorPosition();
            rebuildWidgets();
            searchBox.setCursorPosition(cursor);
            setFocused(searchBox);
            searchBox.setFocused(true);
        } else if (searchDirty) {
            searchDirty = false;
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
            if (page == PAGE_PARAMETERS) {
                if (detailView && TerminalParameters.clientSpecs().stream()
                        .noneMatch(spec -> sameTarget(spec, selectedGroup, selectedTarget))) {
                    closeDetail();
                } else {
                    rebuildWidgets();
                }
            }
        }
        if (page == PAGE_CONTROLS) {
            debugButton.setMessage(toggleLabel("debug", DebugInputHandler.isDebugEnabledClient()));
            cooldownButton.setMessage(toggleLabel("cooldown", DebugInputHandler.isTestModeClient()));
            hitButton.setMessage(toggleLabel("hit", DebugInputHandler.isHitDisplayEnabled()));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        graphics.drawString(font, title, x + 8, y + 6, 0xFFFFFF, false);
        if (page == PAGE_PARAMETERS && !detailView) {
            int categoryWidth = Math.min(142, Math.max(112, imageWidth / 3));
            int targetX = x + categoryWidth + 12;
            int contentTop = y + 63;
            graphics.drawString(font, label("category_header"), x + 12, contentTop - 12,
                    0xFFD6D6D6, false);
            graphics.drawString(font, label("target_header"), targetX, contentTop - 12,
                    0xFFD6D6D6, false);
            List<TargetChoice> choices = targetsForSelectedCategory();
            if (choices.isEmpty()) {
                graphics.drawString(font, label("status.no_parameters"), targetX, contentTop + 8,
                        0xFFEAD890, false);
            }
            String count = choices.isEmpty() ? "0/0" : (targetScrollOffset + 1) + "-"
                    + Math.min(targetScrollOffset + targetVisibleRows, choices.size()) + "/" + choices.size();
            graphics.drawString(font, count, x + imageWidth - 43, y + imageHeight - 19,
                    0xFFD0D0D0, false);
        } else if (page == PAGE_PARAMETERS) {
            int draftWidth = Math.max(56, imageWidth / 7);
            int draftX = x + imageWidth - draftWidth - 12;
            int defaultSpeedX = draftX - 58;
            int currentSpeedX = defaultSpeedX - 58;
            int defaultX = currentSpeedX - 62;
            int currentX = defaultX - 62;
            String targetName = targetDisplayName(selectedGroup, selectedTarget);
            graphics.drawString(font, label("detail_prefix"), x + 110, y + 46, 0xFFD6D6D6, false);
            graphics.drawString(font, font.plainSubstrByWidth(targetName, imageWidth - 178),
                    x + 170, y + 46, 0xFFFFFFFF, false);
            graphics.drawString(font, label("header.parameter"), x + 10, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.current"), currentX, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.default"), defaultX, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, font.plainSubstrByWidth(label("header.current_speed").getString(), 54),
                    currentSpeedX, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, font.plainSubstrByWidth(label("header.default_speed").getString(), 54),
                    defaultSpeedX, y + LIST_TOP, 0xFFD6D6D6, false);
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
                String current = currentValue(spec);
                String defaultValue = defaultValue(spec);
                String suffix = unitSuffix(spec);
                graphics.drawString(font, font.plainSubstrByWidth(current + suffix, 74),
                        currentX, rowY, 0xFFC7ECCF, false);
                graphics.drawString(font, font.plainSubstrByWidth(defaultValue + suffix, 74),
                        defaultX, rowY, 0xFFD5D5B8, false);
                graphics.drawString(font, computedSpeed(spec, false), currentSpeedX, rowY, 0xFFC7ECCF, false);
                graphics.drawString(font, computedSpeed(spec, true), defaultSpeedX, rowY, 0xFFD5D5B8, false);
                if (mouseX >= x + 10 && mouseX < x + imageWidth - 20
                        && mouseY >= rowY - 3 && mouseY < rowY + 15) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(name + (suffix.isEmpty() ? "" : " (" + suffix + ")") + " · "
                            + label("header.current").getString() + ": " + currentValue(spec) + " · "
                            + label("header.default").getString() + ": " + defaultValue(spec) + " · "
                            + label("header.draft").getString() + ": " + draftValue(spec)));
                    if (!spec.target().isBlank() && !"system".equals(spec.group())) {
                        tooltip.add(Component.literal(label("object_id").getString() + ": " + spec.target()));
                    }
                    tooltip.add(parameterDescription(spec));
                    graphics.renderTooltip(font, tooltip.stream().map(Component::getVisualOrderText).toList(),
                            mouseX, mouseY);
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
            if (page == PAGE_PARAMETERS && mouseY >= y + imageHeight - 21 && mouseX < x + imageWidth - 90) {
                graphics.renderTooltip(font, Component.literal(message), mouseX, mouseY);
            }
            graphics.drawString(font, font.plainSubstrByWidth(message, imageWidth - 105),
                    x + 8, y + imageHeight - 19, 0xFFEAD890, false);
        }
        if (page == PAGE_CONTROLS) {
            renderControlTooltip(graphics, mouseX, mouseY, x, y);
        }
    }

    private void renderControlTooltip(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        int controlWidth = Math.min(210, imageWidth - 20);
        int controlX = x + (imageWidth - controlWidth) / 2;
        for (int index = 0; index < CONTROL_TOOLTIP_KEYS.size(); index++) {
            int buttonY = y + 49 + index * 24;
            if (mouseX >= controlX && mouseX < controlX + controlWidth
                    && mouseY >= buttonY && mouseY < buttonY + 20) {
                graphics.renderTooltip(font, label("control." + CONTROL_TOOLTIP_KEYS.get(index)),
                        mouseX, mouseY);
                return;
            }
        }
    }

    private boolean canonicalMatches(TerminalParameterSpec spec, String value) {
        try {
            return spec.canonicalDisplay(value).equals(rawCurrentValue(spec));
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
        if (page == PAGE_PARAMETERS) {
            graphics.fill(x + 6, y + 59, x + imageWidth - 4,
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
