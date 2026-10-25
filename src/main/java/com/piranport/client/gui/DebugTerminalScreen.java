package com.piranport.client.gui;

import com.piranport.aviation.AircraftDefinition;
import com.piranport.aviation.AircraftDefinitionService;
import com.piranport.client.input.DebugInputHandler;
import com.piranport.dungeon.data.StageData;
import com.piranport.dungeon.network.ClientDungeonData;
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
import net.minecraft.client.Minecraft;
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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
            "aircraft", "cannon", "enhancement", "deep_ocean", "dungeon", "vanilla_mob",
            "ship_girl", "ammo", "torpedo", "missile_launcher", "core", "projectile", "system");
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
    /** 副本分类的关卡下钻：null = 关卡列表，非 null = 该关卡的节点列表。 */
    private String dungeonStageFilter;
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

    private enum TargetKind { NORMAL, STAGE, NODE, BACK }

    private record TargetChoice(String category, String group, String target, int parameterCount,
                                TargetKind kind) {}

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

        int draftWidth = Math.max(76, imageWidth / 4);
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
                CheckboxButton checkbox = new CheckboxButton(draftX, rowY, draftWidth, 16,
                        Boolean.parseBoolean(draftValue(spec)), b -> {
                            selectedKey = key;
                            CheckboxButton button = (CheckboxButton) b;
                            boolean next = !button.selected();
                            drafts.put(key, Boolean.toString(next));
                            button.setSelected(next);
                            localStatus = label("status.unsaved").getString();
                        });
                addRenderableWidget(checkbox);
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
        boolean searching = !needle.isEmpty();
        Map<String, TargetChoice> unique = new LinkedHashMap<>();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, java.util.Set<String>> stageNodes = new LinkedHashMap<>();

        for (TerminalParameterSpec spec : TerminalParameters.clientSpecs()) {
            boolean dungeonNode = "dungeon_node".equals(spec.group());
            String stageId = dungeonNode ? stageOf(spec.target()) : null;
            if (dungeonNode && stageId != null) {
                stageNodes.computeIfAbsent(stageId, ignored -> new LinkedHashSet<>()).add(spec.target());
            }
            // 进入某个关卡后只列该关节点；全局副本参数留在关卡列表层，避免节点列表被稀释。
            if (!searching && dungeonStageFilter != null && "dungeon".equals(spec.group())) continue;
            if (!searching && dungeonNode) {
                // 副本节点两级导航：先在关卡列表选关，进关后再列节点。
                if (dungeonStageFilter == null) continue;
                if (!dungeonStageFilter.equals(stageId)) continue;
            }
            String category = categoryFor(spec.group());
            String key = category + "|" + spec.group() + "|"
                    + targetIdentity(spec.group(), spec.target());
            TargetKind kind = dungeonNode ? TargetKind.NODE : TargetKind.NORMAL;
            unique.computeIfAbsent(key, ignored ->
                    new TargetChoice(category, spec.group(), spec.target(), 0, kind));
            counts.merge(key, 1, Integer::sum);
        }

        List<TargetChoice> rows = new ArrayList<>();
        for (TargetChoice choice : unique.values()) {
            TargetChoice counted = new TargetChoice(choice.category(), choice.group(), choice.target(),
                    counts.getOrDefault(choiceKey(choice), 0), choice.kind());
            if (matchesTarget(counted, needle)) rows.add(counted);
        }
        if (!searching && dungeonStageFilter == null) {
            for (var entry : stageNodes.entrySet()) {
                rows.add(new TargetChoice("dungeon", "dungeon_node", entry.getKey(),
                        entry.getValue().size(), TargetKind.STAGE));
            }
        }
        if (!searching && dungeonStageFilter != null) {
            rows.add(new TargetChoice("dungeon", "dungeon_node", "", 0, TargetKind.BACK));
        }

        rows.sort(Comparator.comparingInt((TargetChoice choice) -> CATEGORY_ORDER.indexOf(choice.category()))
                .thenComparingInt(choice -> switch (choice.kind()) {
                    case STAGE -> 0;
                    case BACK -> 1;
                    case NODE -> 2;
                    case NORMAL -> 3;
                })
                .thenComparing(choice -> targetDisplayName(choice.group(), choice.target()))
                .thenComparing(TargetChoice::target));
        targets = rows;
        targetScrollOffset = Math.min(targetScrollOffset,
                Math.max(0, targetsForSelectedCategory().size() - targetVisibleRows));
    }

    private boolean matchesTarget(TargetChoice choice, String needle) {
        String terms = categoryLabel(choice.category()).getString() + " " + targetLabel(choice)
                + " " + choice.group() + " " + choice.target();
        return terms.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String stageOf(String target) {
        if (target == null) return "";
        int slash = target.indexOf('/');
        return slash > 0 ? target.substring(0, slash) : target;
    }

    private String stageDisplayName(String stageId) {
        StageData stage = ClientDungeonData.getStage(stageId);
        return stage == null ? stageId : stage.displayName();
    }

    private String choiceKey(TargetChoice choice) {
        return choice.category() + "|" + choice.group() + "|"
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
            // 副本缩放/时序与节点编成放在同一分类，避免节点参数散进“系统参数”。
            case "dungeon", "dungeon_node" -> "dungeon";
            case "vanilla_mob" -> "vanilla_mob";
            case "ship_girl" -> "ship_girl";
            case "ammo" -> "ammo";
            case "torpedo" -> "torpedo";
            // 导弹发射器独立分类（型号数值 + 共享默认值），不再混进 enhancement（equipment）。
            case "missile_launcher" -> "missile_launcher";
            case "core" -> "core";
            case "projectile", "projectiles" -> "projectile";
            default -> "system";
        };
    }

    private List<TargetChoice> targetsForSelectedCategory() {
        return targets.stream().filter(choice -> choice.category().equals(selectedCategory)).toList();
    }

    private List<TargetChoice> targetsForCategory(String category) {
        return targets.stream().filter(choice -> choice.category().equals(category)).toList();
    }

    private int targetCount(String category) {
        return (int) targets.stream().filter(choice -> choice.category().equals(category))
                .map(choice -> choice.group() + "\u0000" + choice.target()).distinct().count();
    }

    private Component categoryLabel(String category) {
        return label("category." + category);
    }

    private String targetLabel(TargetChoice choice) {
        if (choice.kind() == TargetKind.STAGE) {
            return stageDisplayName(choice.target()) + "  ·  "
                    + choice.parameterCount() + " " + label("node_count").getString();
        }
        if (choice.kind() == TargetKind.BACK) {
            return label("back_to_stages").getString();
        }
        String count = choice.parameterCount() + " " + label("parameter_count").getString();
        if (choice.kind() == TargetKind.NODE && query.isBlank()) {
            // 已经在关卡内：节点列表只显示节点字母，不再重复关卡名。
            String nodeId = choice.target();
            int slash = nodeId.indexOf('/');
            if (slash > 0) nodeId = nodeId.substring(slash + 1);
            return nodeId + "  ·  " + count;
        }
        return targetDisplayName(choice.group(), choice.target()) + "  ·  " + count;
    }

    private String parameterName(TerminalParameterSpec spec) {
        String key = "gui.piranport.debug_terminal.parameter." + spec.key();
        String translated = Component.translatable(key).getString();
        if (!translated.equals(key)) return translated;
        // 节点生成数量：实体名优先走 entity.piranport.<path>，新舰种无需逐条补翻译。
        if (spec.property().startsWith("spawn_")) {
            String entityName = spawnEntityName(spec.property());
            if (entityName != null) return entityName + "数量";
        }
        return translated("property." + spec.property(), readable(spec.property()));
    }

    /** 从 spawn_<entity_path> 解析实体中文名；没有实体翻译时回退到属性翻译。 */
    private String spawnEntityName(String property) {
        String path = property.substring("spawn_".length());
        String entityKey = "entity.piranport." + path;
        String entityName = Component.translatable(entityKey).getString();
        if (!entityName.equals(entityKey)) return entityName;
        String propKey = "gui.piranport.debug_terminal.property." + property;
        String propName = Component.translatable(propKey).getString();
        if (!propName.equals(propKey)) {
            return propName.endsWith("数量")
                    ? propName.substring(0, propName.length() - 2) : propName;
        }
        return path;
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
        // 节点生成数量统一用同一句说明；具体舰种名由 spawnEntityName 解析。
        if (spec.property().startsWith("spawn_")) {
            return Component.literal("控制该节点生成多少" + spawnEntityName(spec.property())
                    + "；设为 0 表示不生成。");
        }
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
        // 副本节点目标形如 "7-4/E"，用客户端已同步的关卡名替换裸 ID。
        if ("dungeon_node".equals(group)) {
            int slash = rawTarget.indexOf('/');
            if (slash > 0) {
                StageData stage = ClientDungeonData.getStage(rawTarget.substring(0, slash));
                if (stage != null) {
                    return stage.displayName() + " · " + rawTarget.substring(slash + 1);
                }
            }
            return rawTarget;
        }
        String path = rawTarget;
        String namespace = "piranport";
        int colon = rawTarget.indexOf(':');
        if (colon > 0 && colon < rawTarget.length() - 1) {
            namespace = rawTarget.substring(0, colon);
            path = rawTarget.substring(colon + 1);
        }
        for (String key : targetTranslationKeys(group, namespace, path)) {
            String value = Component.translatable(key).getString();
            if (!value.equals(key)) return withAircraftClass(group, path, value);
        }
        // 查不到翻译时回退到 target 本身（可读化后的完整路径），而不是分类名：
        // 分类名会让同一分类下所有缺翻译的目标重名成一整屏“装备/弹体”。
        return withAircraftClass(group, path, readableTargetName(namespace, path, rawTarget));
    }

    /**
     * 终端 target 对照的物品注册路径。
     * <p>近防炮的终端 target 是 {@code ciws_<注册名>}（见 {@code ModEquipmentConfig#ciws}），
     * 物品注册名则是去掉 {@code ciws_} 的 {@code <注册名>}。17 座近防炮逐一核对过，
     * 不存在别名或缩写，故这里可安全去前缀；新增近防炮若有别名，应在此建显式映射。</p>
     */
    static String itemTargetPath(String group, String path) {
        String prefix = "ciws_";
        if ("equipment".equals(group) && path.startsWith(prefix) && path.length() > prefix.length()) {
            return path.substring(prefix.length());
        }
        return path;
    }

    /**
     * target 的翻译键候选，按优先级排列。
     * <p>资源 ID 常带数据层前缀（aircraft/、entity/ 等），实际翻译注册在末段路径上，
     * 因此 path 与 leaf 各查一次。抽成纯字符串函数，便于测试复用同一套解析规则。</p>
     */
    static List<String> targetTranslationKeys(String group, String namespace, String path) {
        String leaf = lastPathSegment(path);
        String itemPath = itemTargetPath(group, path);
        String itemLeaf = lastPathSegment(itemPath);
        List<String> keys = new ArrayList<>();
        if ("core".equals(group)) {
            keys.add("gui.piranport.debug_terminal.ship_type." + path);
        }
        keys.add("gui.piranport.debug_terminal.target." + group + "." + path);
        if (!leaf.equals(path)) {
            keys.add("gui.piranport.debug_terminal.target." + group + "." + leaf);
        }
        keys.add("item." + namespace + "." + itemPath);
        if (!itemLeaf.equals(itemPath)) {
            keys.add("item." + namespace + "." + itemLeaf);
        }
        keys.add("entity." + namespace + "." + path);
        if (!leaf.equals(path)) {
            keys.add("entity." + namespace + "." + leaf);
        }
        keys.add("block." + namespace + "." + path);
        return keys;
    }

    private static String lastPathSegment(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 && slash + 1 < path.length() ? path.substring(slash + 1) : path;
    }

    /**
     * 缺翻译时的可读回退名：保留完整路径（namespace 非 piranport 时连带 namespace），
     * 只把下划线与路径分隔符换成空格，确保不同 target 仍有区分度。
     */
    static String readableTargetName(String namespace, String path, String rawTarget) {
        String readable = path.replace('/', ' ').replace('_', ' ').trim();
        if (readable.isEmpty()) readable = rawTarget.trim();
        return "piranport".equals(namespace) ? readable : namespace + ": " + readable;
    }

    /**
     * 飞机分类下，具体机型目标后面补上机种，例如“剑鱼 · 鱼雷机”。
     * 全局战斗参数的目标（fighter_combat 等）不走这里，它们自己的 target 翻译已写清机种。
     */
    private String withAircraftClass(String group, String path, String name) {
        if (!"aircraft".equals(group)) return name;
        AircraftDefinition definition = AircraftDefinitionService.find(path);
        if (definition == null) return name;
        String type = switch (definition.aircraftClass()) {
            case FIGHTER -> "战斗机";
            case ROCKET_FIGHTER -> "火箭战斗机";
            case DIVE_BOMBER -> "俯冲轰炸机";
            case LEVEL_BOMBER -> "水平轰炸机";
            case TORPEDO_BOMBER -> "鱼雷机";
            case ASW -> "反潜机";
            case RECON -> "侦察机";
        };
        return name + " · " + type;
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
        if (spec.type() != TerminalParameterSpec.ValueType.DOUBLE) return value;
        try {
            return new BigDecimal(value).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ignored) {
            return value;
        }
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
            double result = panelSpeed * 20.0 * multiplier;
            return Double.isFinite(result)
                    ? Component.translatable("gui.piranport.debug_terminal.speed_value",
                            String.format(Locale.ROOT, "%.2f", result)).getString()
                    : "";
        } catch (NumberFormatException ignored) {
            return "";
        }
    }

    private void selectCategory(String category) {
        // 切换分类时回到副本分类的关卡列表，避免停留在上一次下钻的节点列表。
        dungeonStageFilter = null;
        rebuildTargets();
        List<TargetChoice> choices = targetsForCategory(category);
        if (choices.isEmpty()) {
            localStatus = label("status.no_parameters").getString();
            return;
        }
        selectedCategory = category;
        if (choices.size() == 1) {
            openTarget(choices.get(0));
            return;
        }
        targetScrollOffset = 0;
        localStatus = "";
        rebuildWidgets();
    }

    private void openTarget(TargetChoice choice) {
        if (choice.kind() == TargetKind.STAGE) {
            dungeonStageFilter = choice.target();
            selectedCategory = "dungeon";
            selectedGroup = null;
            selectedTarget = null;
            detailView = false;
            query = "";
            targetScrollOffset = 0;
            searchDirty = false;
            localStatus = "";
            rebuildWidgets();
            return;
        }
        if (choice.kind() == TargetKind.BACK) {
            dungeonStageFilter = null;
            selectedCategory = "dungeon";
            selectedGroup = null;
            selectedTarget = null;
            detailView = false;
            query = "";
            targetScrollOffset = 0;
            searchDirty = false;
            localStatus = "";
            rebuildWidgets();
            return;
        }
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
            int draftWidth = Math.max(76, imageWidth / 4);
            int draftX = x + imageWidth - draftWidth - 12;
            int defaultX = draftX - 68;
            int currentX = defaultX - 68;
            String targetName = targetDisplayName(selectedGroup, selectedTarget);
            graphics.drawString(font, label("detail_prefix"), x + 110, y + 46, 0xFFD6D6D6, false);
            graphics.drawString(font, font.plainSubstrByWidth(targetName, imageWidth - 178),
                    x + 170, y + 46, 0xFFFFFFFF, false);
            graphics.drawString(font, label("header.parameter"), x + 10, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.current"), currentX, y + LIST_TOP, 0xFFD6D6D6, false);
            graphics.drawString(font, label("header.default"), defaultX, y + LIST_TOP, 0xFFD6D6D6, false);
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
                if (mouseX >= x + 10 && mouseX < x + imageWidth - 20
                        && mouseY >= rowY - 3 && mouseY < rowY + 15) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(name + (suffix.isEmpty() ? "" : " (" + suffix + ")") + " · "
                            + label("header.current").getString() + ": " + currentValue(spec) + " · "
                            + label("header.default").getString() + ": " + defaultValue(spec) + " · "
                            + label("header.draft").getString() + ": " + draftValue(spec)));
                    String currentSpeed = computedSpeed(spec, false);
                    String defaultSpeed = computedSpeed(spec, true);
                    if (!currentSpeed.isEmpty() || !defaultSpeed.isEmpty()) {
                        tooltip.add(Component.literal("速度: " + currentSpeed + " / 默认 " + defaultSpeed));
                    }
                    if (!spec.target().isBlank() && !"system".equals(spec.group())) {
                        // 目标 ID 也按界面语言显示（舰种、武器、节点名），不要直接露注册名。
                        tooltip.add(Component.literal(label("object_id").getString() + ": "
                                + targetDisplayName(spec.group(), spec.target())));
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

    /** 紧凑打勾开关：16px 高，左侧方框 + 勾，右侧“是/否”。 */
    private static final class CheckboxButton extends Button {
        private boolean selected;

        private CheckboxButton(int x, int y, int width, int height, boolean selected,
                               Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, supplier -> supplier.get());
            this.selected = selected;
        }

        private boolean selected() {
            return selected;
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            int boxX = getX() + 4;
            int boxY = getY() + (getHeight() - 9) / 2;
            graphics.fill(boxX, boxY, boxX + 9, boxY + 9, 0xFF000000);
            graphics.fill(boxX + 1, boxY + 1, boxX + 8, boxY + 8,
                    selected ? 0xFF1E6B1E : 0xFF3A3A3A);
            if (selected) {
                // 用像素段画对勾，避免依赖字体是否包含 ✓ 字符
                graphics.fill(boxX + 2, boxY + 5, boxX + 4, boxY + 7, 0xFFFFFFFF);
                graphics.fill(boxX + 3, boxY + 6, boxX + 5, boxY + 8, 0xFFFFFFFF);
                graphics.fill(boxX + 4, boxY + 5, boxX + 6, boxY + 7, 0xFFFFFFFF);
                graphics.fill(boxX + 5, boxY + 4, boxX + 7, boxY + 6, 0xFFFFFFFF);
                graphics.fill(boxX + 6, boxY + 3, boxX + 8, boxY + 5, 0xFFFFFFFF);
            }
            Component stateLabel = label(selected ? "state.yes" : "state.no");
            graphics.drawString(Minecraft.getInstance().font, stateLabel,
                    boxX + 13, getY() + 4, 0xFFFFFF, false);
        }
    }
}
