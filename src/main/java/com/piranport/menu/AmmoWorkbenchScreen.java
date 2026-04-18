package com.piranport.menu;

import com.piranport.ammo.AmmoCategory;
import com.piranport.ammo.AmmoRecipe;
import com.piranport.ammo.AmmoRecipeRegistry;
import com.piranport.network.AmmoWorkbenchCraftPayload;
import com.piranport.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class AmmoWorkbenchScreen extends AbstractContainerScreen<AmmoWorkbenchMenu> {

    private static final int TAB_W = 26, TAB_H = 24;
    private static final int INV_X = 34;
    private static final int DROPDOWN_VISIBLE = 8;
    private static final String ARROW_UP = "\u25B2";
    private static final String ARROW_DOWN = "\u25BC";

    private static final List<Supplier<Item>> TAB_ICONS = List.of(
            () -> ModItems.SMALL_HE_SHELL.get(),
            () -> ModItems.TORPEDO_533MM.get(),
            () -> ModItems.AERIAL_BOMB.get(),
            () -> ModItems.DEPTH_CHARGE.get(),
            () -> ModItems.SY1_MISSILE.get()
    );

    private AmmoCategory selectedCategory = AmmoCategory.SHELL;
    private int selectedTypeIndex = 0;
    private int selectedCaliberIndex = 0;
    private EditBox quantityField;

    private boolean typeDropdownOpen = false;
    private boolean caliberDropdownOpen = false;
    private int typeScrollOffset = 0;
    private int caliberScrollOffset = 0;

    private List<String> currentTypes = List.of();
    private List<String> currentCalibers = List.of();
    private AmmoRecipe currentRecipe;

    private final Map<Item, Integer> cachedInventory = new HashMap<>();
    private int lastScanTick = -1;

    public AmmoWorkbenchScreen(AmmoWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 230;
        this.imageHeight = 228;
        this.inventoryLabelX = INV_X;
        this.inventoryLabelY = 137;
    }

    @Override
    protected void init() {
        super.init();
        quantityField = new EditBox(font, leftPos + 38, topPos + 28, 36, 14,
                Component.literal(""));
        quantityField.setValue("1");
        quantityField.setMaxLength(3);
        quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        quantityField.setResponder(s -> updateRecipe());
        addRenderableWidget(quantityField);

        refreshDropdowns();
    }

    private void refreshDropdowns() {
        currentTypes = AmmoRecipeRegistry.getTypesForCategory(selectedCategory);
        selectedTypeIndex = 0;
        typeScrollOffset = 0;
        caliberScrollOffset = 0;
        refreshCalibers();
    }

    private void refreshCalibers() {
        if (currentTypes.isEmpty()) {
            currentCalibers = List.of();
        } else {
            String type = currentTypes.get(selectedTypeIndex);
            currentCalibers = AmmoRecipeRegistry.getCalibersForType(selectedCategory, type);
        }
        selectedCaliberIndex = 0;
        updateRecipe();
    }

    private void updateRecipe() {
        if (currentTypes.isEmpty() || currentCalibers.isEmpty()) {
            currentRecipe = null;
            return;
        }
        currentRecipe = AmmoRecipeRegistry.findRecipe(
                selectedCategory,
                currentTypes.get(selectedTypeIndex),
                currentCalibers.get(selectedCaliberIndex));
    }

    private int getQuantity() {
        try {
            int q = Integer.parseInt(quantityField.getValue());
            return Math.max(1, q);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private Map<Item, Integer> scanPlayerInventory() {
        if (minecraft == null || minecraft.player == null) return Map.of();
        int tick = minecraft.player.tickCount;
        if (tick == lastScanTick && !cachedInventory.isEmpty()) {
            return cachedInventory;
        }
        cachedInventory.clear();
        Inventory inv = minecraft.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                cachedInventory.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        lastScanTick = tick;
        return cachedInventory;
    }

    private boolean allMaterialsSufficient(Map<Item, Integer> available, int quantity) {
        if (currentRecipe == null) return false;
        for (AmmoRecipe.MaterialRequirement mat : currentRecipe.materials()) {
            int required = mat.getRequired(quantity);
            int has = available.getOrDefault(mat.item().get(), 0);
            if (has < required) return false;
        }
        return true;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        renderTabs(g, x, y, mouseX, mouseY);

        renderDropdownButton(g, x + 6, y + 7, 96, 14,
                currentTypes.isEmpty() ? "---" : currentTypes.get(selectedTypeIndex));
        renderDropdownButton(g, x + 106, y + 7, 96, 14,
                currentCalibers.isEmpty() ? "---" : currentCalibers.get(selectedCaliberIndex));

        g.drawString(font, Component.translatable("gui.piranport.ammo_workbench.quantity"),
                x + 6, y + 31, 0xFF404040, false);

        if (currentRecipe != null) {
            int qty = getQuantity();
            ItemStack preview = currentRecipe.getResultStack(qty);
            g.renderItem(preview, x + 110, y + 32);
            g.renderItemDecorations(font, preview, x + 110, y + 32);
            Component name = currentRecipe.getResultName();
            int nameW = font.width(name);
            g.drawString(font, name, x + (imageWidth - nameW) / 2, y + 55, 0xFF404040, false);
            String countText = "×" + (currentRecipe.outputCount() * qty);
            g.drawString(font, countText, x + 128, y + 37, 0xFF606060, false);
        }

        g.fill(x + 4, y + 70, x + imageWidth - 4, y + 71, 0xFF8B8B8B);

        renderMaterialBars(g, x, y);
        renderCraftButton(g, x, y, mouseX, mouseY);
        renderProgressBar(g, x, y);

        drawSlotBg(g, x + 206, y + 130);

        g.fill(x + 4, y + 144, x + imageWidth - 4, y + 145, 0xFF8B8B8B);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + INV_X - 1 + col * 18, y + 147 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + INV_X - 1 + col * 18, y + 205);
        }
    }

    private void renderTabs(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        AmmoCategory[] cats = AmmoCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = x - TAB_W;
            int ty = y + 4 + i * (TAB_H + 2);
            boolean selected = cats[i] == selectedCategory;

            if (selected) {
                g.fill(tx, ty, tx + TAB_W + 1, ty + TAB_H, 0xFFC6C6C6);
                g.fill(tx - 1, ty - 1, tx + TAB_W + 1, ty, 0xFF8B8B8B);
                g.fill(tx - 1, ty + TAB_H, tx + TAB_W + 1, ty + TAB_H + 1, 0xFF8B8B8B);
                g.fill(tx - 1, ty - 1, tx, ty + TAB_H + 1, 0xFF8B8B8B);
            } else {
                g.fill(tx + 2, ty, tx + TAB_W, ty + TAB_H, 0xFFA0A0A0);
                g.fill(tx + 1, ty - 1, tx + TAB_W, ty, 0xFF8B8B8B);
                g.fill(tx + 1, ty + TAB_H, tx + TAB_W, ty + TAB_H + 1, 0xFF8B8B8B);
                g.fill(tx + 1, ty - 1, tx + 2, ty + TAB_H + 1, 0xFF8B8B8B);
            }

            if (i < TAB_ICONS.size()) {
                ItemStack icon = new ItemStack(TAB_ICONS.get(i).get());
                g.renderItem(icon, tx + 5, ty + 4);
            }
        }
    }

    private void renderDropdownButton(GuiGraphics g, int x, int y, int w, int h, String text) {
        g.fill(x, y, x + w, y + h, 0xFF373737);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFFFFFFF);
        String display = font.width(text) > w - 14 ? font.plainSubstrByWidth(text, w - 14) + ".." : text;
        g.drawString(font, display, x + 3, y + 3, 0xFF000000, false);
        g.drawString(font, ARROW_DOWN, x + w - 10, y + 3, 0xFF606060, false);
    }

    private void renderMaterialBars(GuiGraphics g, int x, int y) {
        g.drawString(font, Component.translatable("gui.piranport.ammo_workbench.materials"),
                x + 6, y + 74, 0xFF404040, false);

        if (currentRecipe == null) {
            g.drawString(font, Component.translatable("gui.piranport.ammo_workbench.select_type"),
                    x + 6, y + 88, 0xFF808080, false);
            return;
        }

        Map<Item, Integer> available = scanPlayerInventory();
        int qty = getQuantity();
        List<AmmoRecipe.MaterialRequirement> mats = currentRecipe.materials();

        for (int i = 0; i < mats.size(); i++) {
            int barY = y + 88 + i * 12;
            AmmoRecipe.MaterialRequirement mat = mats.get(i);
            int required = mat.getRequired(qty);
            int has = available.getOrDefault(mat.item().get(), 0);
            boolean sufficient = has >= required;

            ItemStack icon = new ItemStack(mat.item().get());
            g.pose().pushPose();
            g.pose().translate(x + 6, barY - 2, 0);
            g.pose().scale(0.5f, 0.5f, 1.0f);
            g.renderItem(icon, 0, 0);
            g.pose().popPose();

            int barX = x + 60;
            int barW = 100;
            int barH = 8;
            g.fill(barX, barY, barX + barW, barY + barH, 0xFF373737);
            g.fill(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, 0xFF555555);

            float ratio = required > 0 ? Math.min((float) has / required, 1.0f) : 0;
            int fillW = (int) ((barW - 2) * ratio);
            int fillColor = sufficient ? 0xFF44CC44 : 0xFFCC4444;
            if (fillW > 0) {
                g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, fillColor);
            }

            String countText = has + "/" + required;
            int textColor = sufficient ? 0xFF208020 : 0xFFCC2020;
            g.drawString(font, countText, barX + barW + 4, barY, textColor, false);
        }
    }

    private void renderCraftButton(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        int bx = x + 6, by = y + 128, bw = 56, bh = 14;
        boolean crafting = menu.isCrafting();
        Map<Item, Integer> available = scanPlayerInventory();
        boolean canCraft = !crafting && currentRecipe != null
                && allMaterialsSufficient(available, getQuantity());

        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;

        int bgColor = canCraft ? (hovered ? 0xFF5588CC : 0xFF4477BB) : 0xFF888888;
        g.fill(bx, by, bx + bw, by + bh, 0xFF373737);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, bgColor);

        Component label = crafting
                ? Component.translatable("gui.piranport.ammo_workbench.crafting")
                : Component.translatable("gui.piranport.ammo_workbench.craft");
        int textColor = canCraft ? 0xFFFFFFFF : 0xFFCCCCCC;
        int labelW = font.width(label);
        g.drawString(font, label, bx + (bw - labelW) / 2, by + 3, textColor, false);
    }

    private void renderProgressBar(GuiGraphics g, int x, int y) {
        int progress = menu.getProgress();
        int total = menu.getTotalTime();
        if (total <= 0) return;

        int barX = x + 66, barY = y + 130, barW = 100, barH = 8;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF373737);
        g.fill(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, 0xFF555555);

        int fillW = (int) ((barW - 2f) * progress / total);
        g.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFF44AAFF);

        int pct = (int) (100f * progress / total);
        String pctText = pct + "%";
        g.drawString(font, pctText, barX + barW + 4, barY, 0xFF404040, false);
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF373737);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (typeDropdownOpen) {
            renderDropdownList(g, leftPos + 6, topPos + 21, 96, currentTypes, selectedTypeIndex,
                    typeScrollOffset, mouseX, mouseY);
        }
        if (caliberDropdownOpen) {
            renderDropdownList(g, leftPos + 106, topPos + 21, 96, currentCalibers,
                    selectedCaliberIndex, caliberScrollOffset, mouseX, mouseY);
        }

        AmmoCategory[] cats = AmmoCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = leftPos - TAB_W;
            int ty = topPos + 4 + i * (TAB_H + 2);
            if (mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H) {
                g.renderTooltip(font, Component.literal(cats[i].getDisplayName()), mouseX, mouseY);
            }
        }

        renderTooltip(g, mouseX, mouseY);
    }

    private void renderDropdownList(GuiGraphics g, int x, int y, int w, List<String> items,
                                     int selectedIndex, int scrollOffset, int mouseX, int mouseY) {
        int visible = Math.min(items.size() - scrollOffset, DROPDOWN_VISIBLE);
        if (visible <= 0) return;

        int listH = visible * 12 + 2;

        g.fill(x - 1, y - 1, x + w + 1, y + listH + 1, 0xFF373737);
        g.fill(x, y, x + w, y + listH, 0xFFFFFFFF);

        for (int i = 0; i < visible; i++) {
            int idx = i + scrollOffset;
            int iy = y + 1 + i * 12;
            boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= iy && mouseY < iy + 12;
            boolean selected = idx == selectedIndex;

            if (selected) {
                g.fill(x, iy, x + w, iy + 12, 0xFF4477BB);
            } else if (hovered) {
                g.fill(x, iy, x + w, iy + 12, 0xFFDDDDFF);
            }

            String text = items.get(idx);
            if (font.width(text) > w - 4) {
                text = font.plainSubstrByWidth(text, w - 8) + "..";
            }
            int textColor = selected ? 0xFFFFFFFF : 0xFF000000;
            g.drawString(font, text, x + 3, iy + 2, textColor, false);
        }

        if (scrollOffset > 0) {
            g.drawString(font, ARROW_UP, x + w - 10, y + 1, 0xFF808080, false);
        }
        if (scrollOffset + DROPDOWN_VISIBLE < items.size()) {
            g.drawString(font, ARROW_DOWN, x + w - 10, y + listH - 10, 0xFF808080, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, (imageWidth - font.width(title)) / 2, -10, 0xFF404040, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);

        int x = leftPos;
        int y = topPos;

        if (typeDropdownOpen) {
            if (handleDropdownClick(x + 6, y + 21, 96, currentTypes, typeScrollOffset,
                    (int) mx, (int) my, true)) {
                return true;
            }
            typeDropdownOpen = false;
            return true;
        }
        if (caliberDropdownOpen) {
            if (handleDropdownClick(x + 106, y + 21, 96, currentCalibers, caliberScrollOffset,
                    (int) mx, (int) my, false)) {
                return true;
            }
            caliberDropdownOpen = false;
            return true;
        }

        AmmoCategory[] cats = AmmoCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = x - TAB_W;
            int ty = y + 4 + i * (TAB_H + 2);
            if (mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H) {
                if (cats[i] != selectedCategory) {
                    selectedCategory = cats[i];
                    refreshDropdowns();
                }
                return true;
            }
        }

        if (mx >= x + 6 && mx < x + 102 && my >= y + 7 && my < y + 21) {
            typeDropdownOpen = true;
            caliberDropdownOpen = false;
            return true;
        }

        if (mx >= x + 106 && mx < x + 202 && my >= y + 7 && my < y + 21) {
            caliberDropdownOpen = true;
            typeDropdownOpen = false;
            return true;
        }

        int bx = x + 6, by = y + 128, bw = 56, bh = 14;
        if (mx >= bx && mx < bx + bw && my >= by && my < by + bh) {
            if (!menu.isCrafting() && currentRecipe != null) {
                int qty = getQuantity();
                Map<Item, Integer> available = scanPlayerInventory();
                if (allMaterialsSufficient(available, qty)) {
                    PacketDistributor.sendToServer(
                            new AmmoWorkbenchCraftPayload(menu.getBlockPos(),
                                    currentRecipe.id(), qty));
                }
            }
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    private boolean handleDropdownClick(int listX, int listY, int listW, List<String> items,
                                         int scrollOffset, int mx, int my, boolean isTypeDropdown) {
        int visible = Math.min(items.size() - scrollOffset, DROPDOWN_VISIBLE);
        for (int i = 0; i < visible; i++) {
            int iy = listY + 1 + i * 12;
            if (mx >= listX && mx < listX + listW && my >= iy && my < iy + 12) {
                int idx = i + scrollOffset;
                if (isTypeDropdown) {
                    selectedTypeIndex = idx;
                    typeDropdownOpen = false;
                    refreshCalibers();
                } else {
                    selectedCaliberIndex = idx;
                    caliberDropdownOpen = false;
                    updateRecipe();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (typeDropdownOpen) {
            int maxScroll = Math.max(0, currentTypes.size() - DROPDOWN_VISIBLE);
            typeScrollOffset = Math.max(0, Math.min(maxScroll,
                    typeScrollOffset - (int) scrollY));
            return true;
        }
        if (caliberDropdownOpen) {
            int maxScroll = Math.max(0, currentCalibers.size() - DROPDOWN_VISIBLE);
            caliberScrollOffset = Math.max(0, Math.min(maxScroll,
                    caliberScrollOffset - (int) scrollY));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (quantityField.isFocused()) {
            if (keyCode == 256) { // ESC — first clears focus, second closes (vanilla)
                quantityField.setFocused(false);
                return true;
            }
            return quantityField.keyPressed(keyCode, scanCode, modifiers)
                    || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
