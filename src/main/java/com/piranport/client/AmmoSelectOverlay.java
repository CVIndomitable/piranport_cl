package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.component.SelectedAmmoType;
import com.piranport.item.ShipCoreItem;
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
 * 圆形轮盘弹种选择器：手持火炮时按 Tab 键在准星周围显示可选弹种。
 * 图标排列在屏幕中央的圆形轮盘上，当前选中项高亮。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class AmmoSelectOverlay {

    private static int showTicks = 0;

    public static void bumpShow() {
        showTicks = 40;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        if (showTicks > 0) {
            showTicks--;
        } else {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack weapon = player.getMainHandItem();
        if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem
                || weapon.getItem() instanceof com.piranport.item.CannonItem)) {
            return;
        }

        List<Item> availableTypes = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            if (!s.isEmpty() && ShipCoreItem.matchesCaliber(s, weapon)) {
                if (!availableTypes.contains(s.getItem())) {
                    availableTypes.add(s.getItem());
                }
            }
        }
        ItemStack oh = player.getInventory().offhand.get(0);
        if (!oh.isEmpty() && ShipCoreItem.matchesCaliber(oh, weapon)) {
            if (!availableTypes.contains(oh.getItem())) {
                availableTypes.add(oh.getItem());
            }
        }

        if (availableTypes.isEmpty()) return;

        SelectedAmmoType selected = weapon.getOrDefault(ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
        Item selectedItem = null;
        if (selected.hasSelection()) {
            ResourceLocation id = ResourceLocation.tryParse(selected.ammoItemId());
            if (id != null) {
                selectedItem = BuiltInRegistries.ITEM.get(id);
            }
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        int n = availableTypes.size();
        double radius = 40.0;
        int slotSize = 18;

        // 半透明背景遮罩
        RenderSystem.enableBlend();
        gui.fill(0, 0, screenWidth, screenHeight, 0x33000000);

        for (int i = 0; i < n; i++) {
            double angle = (2.0 * Math.PI * i / n) - Math.PI / 2.0;
            int bx = (int) Math.round(cx + radius * Math.cos(angle) - slotSize / 2.0);
            int by = (int) Math.round(cy + radius * Math.sin(angle) - slotSize / 2.0);

            Item ammoItem = availableTypes.get(i);
            boolean isSelected = ammoItem == selectedItem;

            int count = 0;
            for (ItemStack s : player.getInventory().items) {
                if (s.getItem() == ammoItem) count += s.getCount();
            }
            ItemStack oh2 = player.getInventory().offhand.get(0);
            if (oh2.getItem() == ammoItem) count += oh2.getCount();

            if (isSelected) {
                gui.fill(bx - 1, by - 1, bx + slotSize + 1, by + slotSize + 1, 0xFFFFFFFF);
            }
            gui.fill(bx, by, bx + slotSize, by + slotSize, 0xCC333333);
            gui.renderFakeItem(new ItemStack(ammoItem, 1), bx, by);

            // 数量
            String countStr = String.valueOf(count);
            gui.drawString(mc.font, countStr,
                    bx + slotSize - mc.font.width(countStr),
                    by + slotSize - mc.font.lineHeight, 0xFFFFFF);

            // 名称在轮盘外侧
            String name = ammoItem.getDescription().getString();
            int nameW = mc.font.width(name);
            double labelR = radius + slotSize + 4;
            int lx = (int) Math.round(cx + labelR * Math.cos(angle) - nameW / 2.0);
            int ly = (int) Math.round(cy + labelR * Math.sin(angle) - mc.font.lineHeight / 2.0);
            gui.drawString(mc.font, name, lx, ly, isSelected ? 0xFFFFFF : 0xAAAAAA);
        }

        // 中央提示文字
        Component hint = Component.translatable("key.piranport.switch_ammo");
        gui.drawCenteredString(mc.font, hint, cx, (int) Math.round(cy - radius - 20), 0xCCCCCC);
    }
}
