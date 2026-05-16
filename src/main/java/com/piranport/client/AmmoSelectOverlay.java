package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.component.SelectedAmmoType;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModKeyMappings;
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
 * HUD 弹种选择器：手持火炮时按 Tab 键显示可用弹种。
 * 在热栏上方绘制弹种图标面板。
 */
@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class AmmoSelectOverlay {

    /** 松键后保持显示的 tick 数 */
    private static int showTicks = 0;

    /** 键位处理器调用此方法"刷新"显示计时器。 */
    public static void bumpShow() {
        showTicks = 40; // 2 秒（20 TPS）
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

        // 收集背包中可用的弹药类型
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

        // 确定当前选中的弹种
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

        // 在热栏上方绘制选择器面板
        int panelY = mc.getWindow().getGuiScaledHeight() - 50;
        int slotSize = 18;
        int gap = 2;
        int totalWidth = availableTypes.size() * (slotSize + gap) - gap;
        int startX = screenWidth / 2 - totalWidth / 2;

        // Background for the whole panel
        gui.fill(startX - 2, panelY - 2, startX + totalWidth + 2, panelY + slotSize + 12, 0xAA000000);

        for (int i = 0; i < availableTypes.size(); i++) {
            Item ammoItem = availableTypes.get(i);
            int bx = startX + i * (slotSize + gap);
            boolean isSelected = ammoItem == selectedItem;

            // 背包中该弹种的数量
            int count = 0;
            for (ItemStack s : player.getInventory().items) {
                if (s.getItem() == ammoItem) count += s.getCount();
            }
            ItemStack oh2 = player.getInventory().offhand.get(0);
            if (oh2.getItem() == ammoItem) count += oh2.getCount();

            // Slot background
            if (isSelected) {
                gui.fill(bx, panelY, bx + slotSize, panelY + slotSize, 0xFFFFFFFF);
            } else {
                gui.fill(bx, panelY, bx + slotSize, panelY + slotSize, 0x88666666);
            }

            // Item icon
            gui.renderFakeItem(new ItemStack(ammoItem, 1), bx, panelY);

            // Count text
            gui.drawString(mc.font, String.valueOf(count), bx + slotSize - mc.font.width(String.valueOf(count)), panelY + slotSize, 0xFFFFFF);

            // Name label below
            String name = ammoItem.getDescription().getString();
            int nameW = mc.font.width(name);
            int nameX = bx + (slotSize - nameW) / 2;
            gui.drawString(mc.font, name, nameX, panelY + slotSize + 2, isSelected ? 0xFFFFFF : 0x888888);
        }
    }
}
