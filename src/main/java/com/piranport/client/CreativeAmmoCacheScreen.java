package com.piranport.client;

import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.ammo.CreativeAmmoCatalog;
import com.piranport.component.CreativeAmmoSelection;
import com.piranport.network.CreativeAmmoCachePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 创造模式弹药库勾选界面。
 *
 * <p>只有打勾的条目会被 {@link CreativeAmmoCache} 视为"数量无限"。
 * 界面按弹药类别分组，图标网格可滚动；点击图标切换勾选状态。</p>
 */
public class CreativeAmmoCacheScreen extends Screen {

    private static final int COLS = 9;
    private static final int CELL = 20;
    private static final int ITEM_ROW_H = 20;
    private static final int HEADER_H = 14;
    private static final int PANEL_W = COLS * CELL + 16;
    private static final int PANEL_H_MAX = 300;

    private record Row(@Nullable Component header, List<Item> items, int height) {}

    private final InteractionHand hand;
    private final Set<String> selected = new HashSet<>();
    private final List<Row> rows = new ArrayList<>();

    private int panelX;
    private int panelY;
    private int panelH;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;
    private int totalRowsHeight;
    private int scroll;
    private int maxScroll;

    public CreativeAmmoCacheScreen(InteractionHand hand) {
        super(Component.translatable("gui.piranport.creative_ammo_cache.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        super.init();

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            CreativeAmmoSelection selection = CreativeAmmoCache.selection(player.getItemInHand(hand));
            selected.clear();
            selected.addAll(selection.itemIds());
        }

        rows.clear();
        for (CreativeAmmoCatalog.Category category : CreativeAmmoCatalog.categories()) {
            rows.add(new Row(Component.translatable(category.translationKey()), List.of(), HEADER_H));
            List<Item> items = category.items();
            for (int i = 0; i < items.size(); i += COLS) {
                List<Item> rowItems = List.copyOf(items.subList(i, Math.min(items.size(), i + COLS)));
                rows.add(new Row(null, rowItems, ITEM_ROW_H));
            }
        }

        panelH = Math.max(120, Math.min(PANEL_H_MAX, height - 24));
        panelX = Math.max(8, (width - PANEL_W) / 2);
        panelY = (height - panelH) / 2;
        contentX = panelX + 8;
        contentY = panelY + 22;
        contentW = COLS * CELL;
        contentH = Math.max(20, panelH - 22 - 30);

        totalRowsHeight = rows.stream().mapToInt(Row::height).sum();
        maxScroll = Math.max(0, totalRowsHeight - contentH);
        scroll = Mth.clamp(scroll, 0, maxScroll);

        int buttonY = panelY + panelH - 23;
        int bw = 56;
        int gap = 4;
        int bx = panelX + (PANEL_W - (bw * 3 + gap * 2)) / 2;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.piranport.creative_ammo_cache.select_all"),
                        b -> selectAll())
                .bounds(bx, buttonY, bw, 18).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.piranport.creative_ammo_cache.clear"),
                        b -> clearAll())
                .bounds(bx + bw + gap, buttonY, bw, 18).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.piranport.creative_ammo_cache.done"),
                        b -> onClose())
                .bounds(bx + (bw + gap) * 2, buttonY, bw, 18).build());
    }

    private void selectAll() {
        selected.clear();
        for (Item item : CreativeAmmoCatalog.allItems()) {
            selected.add(CreativeAmmoCatalog.idOf(item));
        }
        send(CreativeAmmoCachePayload.ACTION_SELECT_ALL, "", true);
    }

    private void clearAll() {
        selected.clear();
        send(CreativeAmmoCachePayload.ACTION_CLEAR, "", false);
    }

    private void toggle(Item item) {
        String id = CreativeAmmoCatalog.idOf(item);
        boolean enabled = !selected.contains(id);
        if (enabled) {
            selected.add(id);
        } else {
            selected.remove(id);
        }
        send(CreativeAmmoCachePayload.ACTION_TOGGLE, id, enabled);
    }

    private void send(int action, String itemId, boolean enabled) {
        PacketDistributor.sendToServer(new CreativeAmmoCachePayload(
                action, itemId, enabled, hand == InteractionHand.OFF_HAND));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);

        gfx.fill(panelX, panelY, panelX + PANEL_W, panelY + panelH, 0xF0101418);
        gfx.renderOutline(panelX, panelY, PANEL_W, panelH, 0xFFFFD700);
        gfx.drawCenteredString(font, title, panelX + PANEL_W / 2, panelY + 7, 0xFFFFD700);

        Item hovered = null;
        gfx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        int y = contentY - scroll;
        for (Row row : rows) {
            if (row.height() == HEADER_H) {
                if (y + HEADER_H > contentY && y < contentY + contentH) {
                    gfx.drawString(font, row.header(), contentX + 1, y + 3, 0xFFB8B8B8, false);
                    gfx.fill(contentX, y + HEADER_H - 2, contentX + contentW, y + HEADER_H - 1,
                            0xFF3A3A3A);
                }
            } else {
                for (int i = 0; i < row.items().size(); i++) {
                    Item item = row.items().get(i);
                    int x = contentX + i * CELL;
                    if (y + CELL > contentY && y < contentY + contentH) {
                        boolean isSelected = selected.contains(CreativeAmmoCatalog.idOf(item));
                        gfx.fill(x, y, x + CELL - 2, y + CELL - 2, 0xFF262626);
                        gfx.renderOutline(x, y, CELL - 2, CELL - 2,
                                isSelected ? 0xFF33FF66 : 0xFF555555);
                        gfx.renderFakeItem(new ItemStack(item), x + 1, y + 1);
                        if (isSelected) {
                            // 右下角绿点：在缩略图上仍然能一眼看出勾选状态
                            gfx.fill(x + CELL - 6, y + CELL - 6, x + CELL - 3, y + CELL - 3,
                                    0xFF33FF66);
                        }
                    }
                    if (mouseX >= x && mouseX < x + CELL - 2
                            && mouseY >= y && mouseY < y + CELL - 2
                            && mouseY >= contentY && mouseY < contentY + contentH) {
                        hovered = item;
                    }
                }
            }
            y += row.height();
        }
        gfx.disableScissor();

        if (maxScroll > 0) {
            int trackX = contentX + contentW + 3;
            gfx.fill(trackX, contentY, trackX + 3, contentY + contentH, 0xFF2A2A2A);
            int thumbH = Math.max(12, contentH * contentH / Math.max(1, totalRowsHeight));
            int thumbY = contentY + (contentH - thumbH) * scroll / Math.max(1, maxScroll);
            gfx.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xFF888888);
        }

        super.render(gfx, mouseX, mouseY, partialTick);

        if (hovered != null) {
            gfx.renderTooltip(font, new ItemStack(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        if (mouseX < contentX || mouseX >= contentX + contentW
                || mouseY < contentY || mouseY >= contentY + contentH) {
            return false;
        }

        int relY = (int) mouseY - contentY + scroll;
        int y = 0;
        for (Row row : rows) {
            if (relY >= y && relY < y + row.height()) {
                if (row.height() == ITEM_ROW_H) {
                    int col = ((int) mouseX - contentX) / CELL;
                    if (col >= 0 && col < row.items().size()) {
                        toggle(row.items().get(col));
                    }
                }
                return true;
            }
            y += row.height();
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll <= 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        scroll = Mth.clamp(scroll - (int) Math.round(scrollY * 18), 0, maxScroll);
        return true;
    }
}
