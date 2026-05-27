package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.component.SelectedAmmoType;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipCoreCombat;
import com.piranport.registry.ModDataComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;

/**
 * 圆形轮盘弹种选择器：手持火炮时按住 Tab 键显示轮盘，鼠标方向选择弹种。
 * 图标排列在屏幕中央的圆形轮盘上，鼠标悬停项黄色高亮，当前选中项白色边框。
 *
 * 打开轮盘时会释放鼠标控制（releaseMouse），允许光标移动；
 * 关闭轮盘时会重新抓取鼠标（grabMouse），恢复视角控制。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class AmmoSelectOverlay {

    /** 轮盘中心死区半径（像素）— 鼠标在此范围内视为未选择 */
    private static final double DEAD_ZONE_RADIUS = 10.0;
    /** 弹药图标排列的圆周半径（像素） */
    private static final double WHEEL_RADIUS = 40.0;
    /** 弹药图标大小（像素） */
    private static final int SLOT_SIZE = 18;

    private static boolean isOpen = false;
    private static Item hoveredAmmo = null;
    private static List<Item> availableAmmos = new ArrayList<>();

    public static boolean isOpen() {
        return isOpen;
    }

    public static void open() {
        isOpen = true;
        hoveredAmmo = null;
        updateAvailableAmmos();

        // 释放鼠标控制，允许光标在屏幕上移动选择弹药
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.releaseMouse();
    }

    public static void close() {
        if (!isOpen) {
            return;  // 已经关闭，避免重复操作
        }

        isOpen = false;
        hoveredAmmo = null;
        availableAmmos.clear();

        // P1修复: 无条件抓取鼠标，确保关闭轮盘后恢复视角控制
        Minecraft mc = Minecraft.getInstance();
        mc.mouseHandler.grabMouse();
    }

    public static Item getHoveredAmmo() {
        return hoveredAmmo;
    }

    private static void updateAvailableAmmos() {
        availableAmmos.clear();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) {
            return;
        }

        boolean isCreative = player.getAbilities().instabuild;

        if (isCreative) {
            // 创造模式：从注册表获取所有匹配口径的弹药
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack ammoStack = new ItemStack(item);
                if (ShipCoreCombat.matchesCaliber(ammoStack, weapon)) {
                    if (!availableAmmos.contains(item)) {
                        availableAmmos.add(item);
                    }
                }
            }
        } else {
            // 生存模式：扫描背包中匹配口径的弹药
            for (ItemStack s : player.getInventory().items) {
                if (!s.isEmpty() && ShipCoreCombat.matchesCaliber(s, weapon)) {
                    if (!availableAmmos.contains(s.getItem())) {
                        availableAmmos.add(s.getItem());
                    }
                }
            }
            ItemStack oh = player.getInventory().offhand.get(0);
            if (!oh.isEmpty() && ShipCoreCombat.matchesCaliber(oh, weapon)) {
                if (!availableAmmos.contains(oh.getItem())) {
                    availableAmmos.add(oh.getItem());
                }
            }
        }

        // 如果只有1种弹药，直接选中
        if (availableAmmos.size() == 1) {
            hoveredAmmo = availableAmmos.get(0);
        }
    }

    private static void updateHoveredAmmo(int mouseX, int mouseY, int cx, int cy) {
        if (availableAmmos.isEmpty()) return;

        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double distance = Math.sqrt(dx * dx + dy * dy);

        // 死区：鼠标距离中心小于 DEAD_ZONE_RADIUS 像素时不改变选择
        if (distance < DEAD_ZONE_RADIUS) {
            return;
        }

        // 鼠标方向角度（与渲染公式对齐：0=右，atan2 标准）
        double mouseAngle = Math.atan2(dy, dx);

        // 找角度最近的弹药项（与渲染角度公式一致）
        int n = availableAmmos.size();
        int bestIndex = 0;
        double bestDelta = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double itemAngle = (2.0 * Math.PI * i / n) - Math.PI / 2.0;
            double delta = mouseAngle - itemAngle;
            // 归一化到 [-π, π]
            delta = delta - 2.0 * Math.PI * Math.round(delta / (2.0 * Math.PI));
            double absDelta = Math.abs(delta);
            if (absDelta < bestDelta) {
                bestDelta = absDelta;
                bestIndex = i;
            }
        }

        hoveredAmmo = availableAmmos.get(bestIndex);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;
        if (!isOpen) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) {
            close();
            return;
        }

        if (availableAmmos.isEmpty()) {
            close();
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        // 获取鼠标位置并更新悬停项
        double mouseX = mc.mouseHandler.xpos() * screenWidth / mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * screenHeight / mc.getWindow().getScreenHeight();
        updateHoveredAmmo((int)mouseX, (int)mouseY, cx, cy);

        // 获取当前已选中的弹种（用于对比显示）
        SelectedAmmoType selected = weapon.getOrDefault(
                ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        Item currentlySelected = null;
        if (selected.hasSelection()) {
            ResourceLocation id = ResourceLocation.tryParse(selected.ammoItemId());
            if (id != null) {
                currentlySelected = BuiltInRegistries.ITEM.get(id);
            }
        }

        int n = availableAmmos.size();
        double radius = WHEEL_RADIUS;
        int slotSize = SLOT_SIZE;

        // 半透明背景遮罩
        RenderSystem.enableBlend();
        gui.fill(0, 0, screenWidth, screenHeight, 0x33000000);

        // 渲染轮盘
        for (int i = 0; i < n; i++) {
            double angle = (2.0 * Math.PI * i / n) - Math.PI / 2.0;
            int bx = (int) Math.round(cx + radius * Math.cos(angle) - slotSize / 2.0);
            int by = (int) Math.round(cy + radius * Math.sin(angle) - slotSize / 2.0);

            Item ammoItem = availableAmmos.get(i);
            boolean isHovered = ammoItem == hoveredAmmo;
            boolean isCurrentlySelected = ammoItem == currentlySelected;

            // 统计弹药数量
            int count = 0;
            boolean isCreative = player.getAbilities().instabuild;

            if (!isCreative) {
                // 生存模式：统计背包中的弹药数量
                for (ItemStack s : player.getInventory().items) {
                    if (s.getItem() == ammoItem) count += s.getCount();
                }
                ItemStack oh = player.getInventory().offhand.get(0);
                if (oh.getItem() == ammoItem) count += oh.getCount();
            }

            // 边框：悬停=黄色高亮，已选中=白色细框
            if (isHovered) {
                gui.fill(bx - 2, by - 2, bx + slotSize + 2, by + slotSize + 2, 0xFFFFFF00);
            } else if (isCurrentlySelected) {
                gui.fill(bx - 1, by - 1, bx + slotSize + 1, by + slotSize + 1, 0xFFFFFFFF);
            }

            // 背景槽
            gui.fill(bx, by, bx + slotSize, by + slotSize, 0xCC333333);
            gui.renderFakeItem(new ItemStack(ammoItem, 1), bx, by);

            // 数量显示
            String countStr = isCreative ? "∞" : String.valueOf(count);
            gui.drawString(mc.font, countStr,
                    bx + slotSize - mc.font.width(countStr),
                    by + slotSize - mc.font.lineHeight, 0xFFFFFF);

            // 名称标签
            String name = ammoItem.getDescription().getString();
            int nameW = mc.font.width(name);
            double labelR = radius + slotSize + 4;
            int lx = (int) Math.round(cx + labelR * Math.cos(angle) - nameW / 2.0);
            int ly = (int) Math.round(cy + labelR * Math.sin(angle) - mc.font.lineHeight / 2.0);

            // 悬停项白色，已选中项灰色，其他暗灰
            int textColor = isHovered ? 0xFFFFFF : (isCurrentlySelected ? 0xCCCCCC : 0x888888);
            gui.drawString(mc.font, name, lx, ly, textColor);
        }

        // 中央提示文字
        Component hint = Component.translatable("gui.piranport.ammo_select_hint");
        gui.drawCenteredString(mc.font, hint, cx, (int) Math.round(cy - radius - 20), 0xCCCCCC);
    }
}
