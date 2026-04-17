package com.piranport.menu;

import com.piranport.crafting.WeaponWorkbenchRecipe;
import com.piranport.crafting.WeaponWorkbenchRecipeRegistry;
import com.piranport.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 武器合成台 GUI。
 * 左侧5个标签页 | 中上下拉菜单+预览 | 右上蓝图 | 右中原料 | 右下合成/产物 | 底部背包
 */
public class WeaponWorkbenchScreen extends AbstractContainerScreen<WeaponWorkbenchMenu> {

    // ===== 布局常量 =====
    private static final int TAB_W = 30, TAB_H = 26, TAB_COUNT = 5;
    private static final int DD_X = 8, DD_Y = 20, DD_W = 118, DD_H = 16;
    private static final int DD_ITEM_H = 14, DD_MAX_VISIBLE = 8;
    private static final int CRAFT_X = 152, CRAFT_Y = 100, CRAFT_W = 40, CRAFT_H = 16;
    private static final int PREVIEW_X = 40, PREVIEW_Y = 48;
    private static final int PROGRESS_X = 152, PROGRESS_Y = 120, PROGRESS_W = 40, PROGRESS_H = 4;

    // ===== 下拉状态 =====
    private boolean dropdownOpen = false;
    private int dropdownScroll = 0;

    // ===== 标签页图标缓存 =====
    private ItemStack[] tabIcons;

    private static final Component[] TAB_TOOLTIPS = {
            Component.translatable("gui.piranport.workbench.tab.cannon"),
            Component.translatable("gui.piranport.workbench.tab.torpedo"),
            Component.translatable("gui.piranport.workbench.tab.missile"),
            Component.translatable("gui.piranport.workbench.tab.depth_charge"),
            Component.translatable("gui.piranport.workbench.tab.aircraft")
    };

    public WeaponWorkbenchScreen(WeaponWorkbenchMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 196;
        this.imageHeight = 240;
        this.inventoryLabelX = 17;
    }

    @Override
    protected void init() {
        super.init();
        tabIcons = new ItemStack[]{
                new ItemStack(ModItems.SMALL_GUN.get()),
                new ItemStack(ModItems.TWIN_TORPEDO_LAUNCHER.get()),
                new ItemStack(ModItems.TERRIER_LAUNCHER.get()),
                new ItemStack(ModItems.DEPTH_CHARGE_LAUNCHER.get()),
                new ItemStack(ModItems.FIGHTER_SQUADRON.get())
        };
    }

    // ===== 辅助查询 =====

    private List<WeaponWorkbenchRecipe> getCurrentTabRecipes() {
        return WeaponWorkbenchRecipeRegistry.getRecipesForTab(menu.getSelectedTab());
    }

    private WeaponWorkbenchRecipe getCurrentRecipe() {
        return WeaponWorkbenchRecipeRegistry.getRecipe(menu.getSelectedTab(), menu.getSelectedRecipe());
    }

    private boolean hasEnoughMaterial(ItemStack required) {
        int needed = required.getCount();
        for (int i = 1; i <= 6; i++) {
            ItemStack slotStack = menu.getSlot(i).getItem();
            if (slotStack.is(required.getItem())) {
                needed -= slotStack.getCount();
            }
        }
        return needed <= 0;
    }

    private boolean canCraftClient(WeaponWorkbenchRecipe recipe) {
        for (ItemStack required : recipe.materials()) {
            if (!hasEnoughMaterial(required)) return false;
        }
        ItemStack bp = menu.getSlot(0).getItem();
        if (bp.isEmpty()) return false;
        boolean isCreativeBp = bp.is(ModItems.CREATIVE_BLUEPRINT.get());
        if (!isCreativeBp) {
            if (recipe.requiredBlueprint() == null) return false;
            if (!bp.is(recipe.requiredBlueprint())) return false;
        }
        ItemStack output = menu.getSlot(7).getItem();
        if (!output.isEmpty()) {
            ItemStack result = recipe.getResultStack();
            if (!ItemStack.isSameItemSameComponents(output, result)) return false;
            if (output.getCount() + result.getCount() > output.getMaxStackSize()) return false;
        }
        return true;
    }

    // ===== 渲染 =====

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        // 主面板背景
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        // 标签页
        renderTabs(g, mouseX, mouseY);

        // 下拉按钮（闭合态）
        renderDropdownButton(g, x, y);

        // 预览
        renderPreview(g, x, y);

        // 所需原料列表
        renderRequirements(g, x, y);

        // 蓝图标签 + 槽位
        g.drawString(font, Component.translatable("gui.piranport.workbench.blueprint"),
                x + 152, y + 10, 0xFF404040, false);
        drawSlotBg(g, x + 171, y + 19);

        // 原料标签 + 槽位
        g.drawString(font, Component.translatable("gui.piranport.workbench.materials"),
                x + 148, y + 32, 0xFF404040, false);
        for (int row = 0; row < 3; row++) {
            drawSlotBg(g, x + 153, y + 41 + row * 18);
            drawSlotBg(g, x + 171, y + 41 + row * 18);
        }

        // 产物槽
        drawSlotBg(g, x + 162, y + 127);

        // 合成按钮 + 进度条
        renderCraftButton(g, x, y, mouseX, mouseY);
        renderProgressBar(g, x, y);

        // 合成中锁定遮罩
        if (menu.isCrafting()) {
            int overlay = 0x60404040;
            g.fill(x + 171, y + 19, x + 188, y + 36, overlay);
            for (int row = 0; row < 3; row++) {
                g.fill(x + 153, y + 41 + row * 18, x + 170, y + 58 + row * 18, overlay);
                g.fill(x + 171, y + 41 + row * 18, x + 188, y + 58 + row * 18, overlay);
            }
        }

        // 玩家背包槽
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBg(g, x + 16 + col * 18, y + 155 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBg(g, x + 16 + col * 18, y + 213);
        }
    }

    private void renderTabs(GuiGraphics g, int mx, int my) {
        int selectedTab = menu.getSelectedTab();
        for (int i = 0; i < TAB_COUNT; i++) {
            int tx = leftPos - TAB_W;
            int ty = topPos + 6 + i * 28;
            boolean selected = (i == selectedTab);
            boolean hovered = mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H;

            int bg = selected ? 0xFFC6C6C6 : (hovered ? 0xFFAAAAAA : 0xFF8B8B8B);
            g.fill(tx, ty, tx + TAB_W, ty + TAB_H, 0xFF373737);
            g.fill(tx + 1, ty + 1, tx + TAB_W - (selected ? 0 : 1), ty + TAB_H - 1, bg);

            if (tabIcons != null && i < tabIcons.length) {
                g.renderItem(tabIcons[i], tx + 7, ty + 5);
            }
        }
    }

    private void renderDropdownButton(GuiGraphics g, int x, int y) {
        int bx = x + DD_X, by = y + DD_Y;
        g.fill(bx, by, bx + DD_W, by + DD_H, 0xFF373737);
        g.fill(bx + 1, by + 1, bx + DD_W - 1, by + DD_H - 1, 0xFFE0E0E0);

        WeaponWorkbenchRecipe recipe = getCurrentRecipe();
        String text = recipe != null
                ? recipe.getResultStack().getHoverName().getString() : "---";
        if (font.width(text) > DD_W - 16) {
            text = font.plainSubstrByWidth(text, DD_W - 20) + "...";
        }
        g.drawString(font, text, bx + 4, by + 4, 0xFF404040, false);
        g.drawString(font, dropdownOpen ? "\u25B2" : "\u25BC",
                bx + DD_W - 12, by + 4, 0xFF404040, false);
    }

    private void renderPreview(GuiGraphics g, int x, int y) {
        WeaponWorkbenchRecipe recipe = getCurrentRecipe();
        if (recipe == null) return;

        ItemStack previewItem = recipe.getResultStack();
        g.pose().pushPose();
        g.pose().translate(x + PREVIEW_X, y + PREVIEW_Y, 100);
        g.pose().scale(2.0f, 2.0f, 1.0f);
        g.renderItem(previewItem, 0, 0);
        g.pose().popPose();

        g.drawString(font, previewItem.getHoverName(), x + 8, y + 86, 0xFF404040, false);
    }

    private void renderRequirements(GuiGraphics g, int x, int y) {
        WeaponWorkbenchRecipe recipe = getCurrentRecipe();
        if (recipe == null) return;

        int headerY = y + 100;
        g.drawString(font, Component.translatable("gui.piranport.workbench.requires"),
                x + 8, headerY, 0xFF404040, false);

        // 每列最多 ROWS_PER_COL 行，超过换列，避免第三行侵入背包栏位
        final int ROWS_PER_COL = 2;
        final int COL_W = 70;
        final int ROW_H = 16;
        final int LIST_Y0 = headerY + 12;
        int rowIdx = 0;

        for (ItemStack required : recipe.materials()) {
            int col = rowIdx / ROWS_PER_COL;
            int row = rowIdx % ROWS_PER_COL;
            int itemX = x + 8 + col * COL_W;
            int itemY = LIST_Y0 + row * ROW_H;
            g.renderItem(required, itemX, itemY - 1);
            boolean enough = hasEnoughMaterial(required);
            int color = enough ? 0xFF00AA00 : 0xFFAA0000;
            String txt = "x" + required.getCount() + " "
                    + required.getHoverName().getString();
            if (font.width(txt) > COL_W - 20) {
                txt = font.plainSubstrByWidth(txt, COL_W - 24) + "..";
            }
            g.drawString(font, txt, itemX + 18, itemY + 3, color, false);
            rowIdx++;
        }

        // 蓝图行：无论是否指定特定蓝图，都显示要求（未指定时只接受创造模式蓝图）
        ItemStack bpSlot = menu.getSlot(0).getItem();
        ItemStack bpStack;
        boolean hasBp;
        if (recipe.requiredBlueprint() != null) {
            bpStack = new ItemStack(recipe.requiredBlueprint());
            hasBp = !bpSlot.isEmpty()
                    && (bpSlot.is(recipe.requiredBlueprint())
                        || bpSlot.is(com.piranport.registry.ModItems.CREATIVE_BLUEPRINT.get()));
        } else {
            bpStack = new ItemStack(com.piranport.registry.ModItems.CREATIVE_BLUEPRINT.get());
            hasBp = !bpSlot.isEmpty()
                    && bpSlot.is(com.piranport.registry.ModItems.CREATIVE_BLUEPRINT.get());
        }
        int color = hasBp ? 0xFF00AA00 : 0xFFAA0000;
        int col = rowIdx / ROWS_PER_COL;
        int row = rowIdx % ROWS_PER_COL;
        int itemX = x + 8 + col * COL_W;
        int itemY = LIST_Y0 + row * ROW_H;
        g.renderItem(bpStack, itemX, itemY - 1);
        String bpName = bpStack.getHoverName().getString();
        if (font.width(bpName) > COL_W - 20) {
            bpName = font.plainSubstrByWidth(bpName, COL_W - 24) + "..";
        }
        g.drawString(font, bpName, itemX + 18, itemY + 3, color, false);
    }

    private void renderCraftButton(GuiGraphics g, int x, int y, int mx, int my) {
        int bx = x + CRAFT_X, by = y + CRAFT_Y;
        boolean hovering = mx >= bx && mx < bx + CRAFT_W && my >= by && my < by + CRAFT_H;
        boolean crafting = menu.isCrafting();

        int bg;
        Component text;
        if (crafting) {
            bg = 0xFFCC8800;
            text = Component.translatable("gui.piranport.workbench.crafting");
        } else {
            WeaponWorkbenchRecipe recipe = getCurrentRecipe();
            boolean canCraft = recipe != null && canCraftClient(recipe);
            bg = canCraft ? (hovering ? 0xFF55CC55 : 0xFF44AA44) : 0xFF888888;
            text = Component.translatable("gui.piranport.workbench.craft");
        }

        g.fill(bx, by, bx + CRAFT_W, by + CRAFT_H, 0xFF373737);
        g.fill(bx + 1, by + 1, bx + CRAFT_W - 1, by + CRAFT_H - 1, bg);

        int textX = bx + (CRAFT_W - font.width(text)) / 2;
        int textY = by + (CRAFT_H - 8) / 2;
        g.drawString(font, text, textX, textY, 0xFFFFFFFF, false);
    }

    private void renderProgressBar(GuiGraphics g, int x, int y) {
        if (!menu.isCrafting()) return;

        int bx = x + PROGRESS_X, by = y + PROGRESS_Y;
        g.fill(bx, by, bx + PROGRESS_W, by + PROGRESS_H, 0xFF373737);

        int progress = menu.getCraftingProgress();
        int total = menu.getCraftingTotalTime();
        if (total > 0) {
            int barWidth = (int) ((float) progress / total * (PROGRESS_W - 2));
            float fraction = (float) progress / total;
            int r, gr;
            if (fraction < 0.5f) {
                r = 255;
                gr = (int) (255 * fraction * 2);
            } else {
                r = (int) (255 * (1 - fraction) * 2);
                gr = 255;
            }
            int color = 0xFF000000 | (r << 16) | (gr << 8);
            g.fill(bx + 1, by + 1, bx + 1 + barWidth, by + PROGRESS_H - 1, color);
        }
    }

    private void renderDropdown(GuiGraphics g, int mx, int my) {
        List<WeaponWorkbenchRecipe> recipes = getCurrentTabRecipes();
        if (recipes.isEmpty()) return;

        int x = leftPos + DD_X;
        int y = topPos + DD_Y + DD_H;
        int visibleCount = Math.min(recipes.size() - dropdownScroll, DD_MAX_VISIBLE);
        int listHeight = visibleCount * DD_ITEM_H;

        // 下拉需置于最上层：抬高 Z 以盖住预览图标/槽位物品（物品渲染 Z≈100~200）
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // 背景
        g.fill(x - 1, y - 1, x + DD_W + 1, y + listHeight + 1, 0xFF373737);
        g.fill(x, y, x + DD_W, y + listHeight, 0xFFE8E8E8);

        for (int i = 0; i < visibleCount; i++) {
            int idx = i + dropdownScroll;
            WeaponWorkbenchRecipe recipe = recipes.get(idx);
            int iy = y + i * DD_ITEM_H;
            boolean hovered = mx >= x && mx < x + DD_W && my >= iy && my < iy + DD_ITEM_H;
            boolean selected = idx == menu.getSelectedRecipe();

            if (hovered) g.fill(x, iy, x + DD_W, iy + DD_ITEM_H, 0xFFBBDDFF);
            else if (selected) g.fill(x, iy, x + DD_W, iy + DD_ITEM_H, 0xFFDDDDDD);

            // 图标 + 名称
            ItemStack stack = recipe.getResultStack();
            g.renderItem(stack, x + 2, iy - 1);
            String name = stack.getHoverName().getString();
            if (font.width(name) > DD_W - 22) {
                name = font.plainSubstrByWidth(name, DD_W - 26) + "...";
            }
            g.drawString(font, name, x + 20, iy + 3, 0xFF404040, false);
        }

        // 滚动指示
        if (dropdownScroll > 0) {
            g.drawString(font, "\u25B2", x + DD_W - 10, y + 2, 0xFF808080, false);
        }
        if (dropdownScroll + DD_MAX_VISIBLE < recipes.size()) {
            g.drawString(font, "\u25BC", x + DD_W - 10, y + listHeight - 10, 0xFF808080, false);
        }

        g.pose().popPose();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        if (dropdownOpen) {
            renderDropdown(g, mouseX, mouseY);
        }

        renderTooltip(g, mouseX, mouseY);

        // 标签页悬停提示
        for (int i = 0; i < TAB_COUNT; i++) {
            int tx = leftPos - TAB_W;
            int ty = topPos + 6 + i * 28;
            if (mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H) {
                g.renderTooltip(font, TAB_TOOLTIPS[i], mouseX, mouseY);
            }
        }
    }

    // ===== 交互 =====

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            // 下拉菜单展开时优先处理
            if (dropdownOpen) {
                int item = getDropdownItemAt(mx, my);
                if (item >= 0) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 100 + item);
                    dropdownOpen = false;
                    return true;
                }
                dropdownOpen = false;
                return true;
            }

            // 标签页点击
            for (int i = 0; i < TAB_COUNT; i++) {
                int tx = leftPos - TAB_W;
                int ty = topPos + 6 + i * 28;
                if (mx >= tx && mx < tx + TAB_W && my >= ty && my < ty + TAB_H) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
                    dropdownScroll = 0;
                    return true;
                }
            }

            // 下拉按钮
            int ddBtnX = leftPos + DD_X, ddBtnY = topPos + DD_Y;
            if (mx >= ddBtnX && mx < ddBtnX + DD_W && my >= ddBtnY && my < ddBtnY + DD_H) {
                dropdownOpen = !dropdownOpen;
                dropdownScroll = 0;
                return true;
            }

            // 合成按钮
            int cbx = leftPos + CRAFT_X, cby = topPos + CRAFT_Y;
            if (mx >= cbx && mx < cbx + CRAFT_W && my >= cby && my < cby + CRAFT_H) {
                if (!menu.isCrafting()) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 200);
                }
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (dropdownOpen) {
            List<WeaponWorkbenchRecipe> recipes = getCurrentTabRecipes();
            int maxScroll = Math.max(0, recipes.size() - DD_MAX_VISIBLE);
            dropdownScroll = Math.max(0, Math.min(maxScroll,
                    dropdownScroll - (int) scrollY));
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private int getDropdownItemAt(double mx, double my) {
        List<WeaponWorkbenchRecipe> recipes = getCurrentTabRecipes();
        int x = leftPos + DD_X;
        int y = topPos + DD_Y + DD_H;
        int visibleCount = Math.min(recipes.size() - dropdownScroll, DD_MAX_VISIBLE);

        if (mx >= x && mx < x + DD_W && my >= y && my < y + visibleCount * DD_ITEM_H) {
            int index = (int) ((my - y) / DD_ITEM_H) + dropdownScroll;
            if (index >= 0 && index < recipes.size()) return index;
        }
        return -1;
    }

    private void drawSlotBg(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
        g.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
    }
}
