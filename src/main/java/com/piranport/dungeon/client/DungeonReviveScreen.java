package com.piranport.dungeon.client;

import com.piranport.dungeon.network.ReviveRequestPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shown when the player dies in a dungeon. Offers revive (costs totem) or give up.
 */
public class DungeonReviveScreen extends Screen {
    private boolean hasTotem = false;

    public DungeonReviveScreen() {
        super(Component.translatable("gui.piranport.dungeon_revive.title"));
    }

    @Override
    protected void init() {
        super.init();

        // Check if player has a totem
        if (minecraft != null && minecraft.player != null) {
            for (int i = 0; i < minecraft.player.getInventory().getContainerSize(); i++) {
                ItemStack stack = minecraft.player.getInventory().getItem(i);
                if (stack.is(Items.TOTEM_OF_UNDYING)) {
                    hasTotem = true;
                    break;
                }
            }
        }

        int cx = width / 2;
        int cy = height / 2;

        // Revive button
        Button reviveBtn = Button.builder(
                Component.translatable("gui.piranport.dungeon_revive.revive"),
                btn -> {
                    PacketDistributor.sendToServer(new ReviveRequestPayload());
                    onClose();
                }
        ).bounds(cx - 55, cy + 20, 50, 20).build();
        reviveBtn.active = hasTotem;
        addRenderableWidget(reviveBtn);

        // Give up button
        addRenderableWidget(Button.builder(
                Component.translatable("gui.piranport.dungeon_revive.giveup"),
                btn -> onClose()
        ).bounds(cx + 5, cy + 20, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;

        gfx.fill(cx - 80, cy - 40, cx + 80, cy + 50, 0xCC1A1A1A);
        gfx.renderOutline(cx - 80, cy - 40, 160, 90, 0xFFFF4444);

        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_revive.died"),
                cx, cy - 30, 0xFFFF4444);

        if (hasTotem) {
            gfx.drawCenteredString(font,
                    Component.translatable("gui.piranport.dungeon_revive.cost"),
                    cx, cy - 5, 0xFFFFFFFF);
        } else {
            gfx.drawCenteredString(font,
                    Component.translatable("gui.piranport.dungeon_revive.no_totem"),
                    cx, cy - 5, 0xFFFF6666);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
