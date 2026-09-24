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

        // renderBackground() may apply a strong blur. Draw a fully opaque panel
        // afterwards so the confirmation text remains readable on every client.
        gfx.fill(cx - 100, cy - 50, cx + 100, cy + 55, 0xFF1A1A1A);
        gfx.renderOutline(cx - 100, cy - 50, 200, 105, 0xFFFF4444);

        super.render(gfx, mouseX, mouseY, partialTick);

        // Draw all copy in the final GUI pass so the blur/post-processing layer
        // cannot soften it before the screen is presented.
        gfx.drawCenteredString(font, title, cx, cy - 43, 0xFFFFD700);
        gfx.drawCenteredString(font,
                Component.translatable("gui.piranport.dungeon_revive.died"),
                cx, cy - 29, 0xFFFF4444);
        if (hasTotem) {
            gfx.drawCenteredString(font,
                    Component.translatable("gui.piranport.dungeon_revive.cost"),
                    cx, cy - 9, 0xFFFFFFFF);
        } else {
            gfx.drawCenteredString(font,
                    Component.translatable("gui.piranport.dungeon_revive.no_totem"),
                    cx, cy - 9, 0xFFFF6666);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
