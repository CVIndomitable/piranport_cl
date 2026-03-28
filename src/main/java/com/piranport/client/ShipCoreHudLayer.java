package com.piranport.client;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.config.ModClientConfig;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.TorpedoLauncherItem;
import com.piranport.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = PiranPort.MOD_ID, value = Dist.CLIENT)
public class ShipCoreHudLayer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        // Legacy HUD bar — hidden by default, replaced by ReloadBarDecorator on the item icon.
        if (!ModClientConfig.SHOW_LEGACY_RELOAD_HUD.get()) return;
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof ShipCoreItem sci)) return;
        if (!TransformationManager.isTransformed(mainHand)) return;

        ShipCoreItem.ShipType type = sci.getShipType();
        ItemContainerContents contents = mainHand.getOrDefault(
                ModDataComponents.SHIP_CORE_CONTENTS.get(), ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(type.totalSlots(), ItemStack.EMPTY);
        contents.copyInto(items);

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Draw weapon cooldown bars centered above the hotbar
        int barWidth = 18;
        int barHeight = 3;
        int gap = 2;
        int totalWidth = type.weaponSlots * (barWidth + gap) - gap;
        int startX = screenWidth / 2 - totalWidth / 2;
        int barY = screenHeight - 44;

        float partialTick = 0.0f; // sub-tick interpolation not critical for cooldown bars

        for (int i = 0; i < type.weaponSlots; i++) {
            ItemStack weapon = items.get(i);
            int bx = startX + i * (barWidth + gap);

            // Background
            gui.fill(bx, barY, bx + barWidth, barY + barHeight, 0x88000000);

            if (weapon.isEmpty()) {
                // Empty slot: dim bar
                gui.fill(bx, barY, bx + barWidth, barY + barHeight, 0x44FFFFFF);
                continue;
            }

            float cooldownFraction = getCooldownPercent(player, mainHand, weapon, partialTick);

            if (cooldownFraction <= 0f) {
                // Ready: solid green
                gui.fill(bx, barY, bx + barWidth, barY + barHeight, 0xFF44CC44);
            } else {
                // Cooling down: red/orange bar shrinks left-to-right as it cools
                int fillWidth = Math.round(barWidth * (1f - cooldownFraction));
                // Gray = remaining cooldown
                gui.fill(bx, barY, bx + barWidth, barY + barHeight, 0xFF555555);
                // Color = progress toward ready
                int color = cooldownFraction > 0.5f ? 0xFFFF4444 : 0xFFFFAA00;
                if (fillWidth > 0) {
                    gui.fill(bx, barY, bx + fillWidth, barY + barHeight, color);
                }
            }
        }
    }

    private static float getCooldownPercent(LocalPlayer player, ItemStack coreStack,
                                             ItemStack weapon, float partialTick) {
        if (weapon.getItem() instanceof TorpedoLauncherItem) {
            return player.getCooldowns().getCooldownPercent(weapon.getItem(), partialTick);
        }
        // Cannon uses the ship core item's cooldown
        return player.getCooldowns().getCooldownPercent(coreStack.getItem(), partialTick);
    }
}
