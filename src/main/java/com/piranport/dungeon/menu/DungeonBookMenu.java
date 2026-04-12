package com.piranport.dungeon.menu;

import com.piranport.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for the dungeon book (lectern) GUI.
 * No item slots — all interaction is via C2S payloads.
 * Player inventory slots are hidden (off-screen).
 */
public class DungeonBookMenu extends AbstractContainerMenu {
    private final BlockPos lecternPos;
    private final int keySlot;

    public DungeonBookMenu(int containerId, Inventory playerInventory,
                            BlockPos lecternPos, int keySlot) {
        super(ModMenuTypes.DUNGEON_BOOK_MENU.get(), containerId);
        this.lecternPos = lecternPos;
        this.keySlot = keySlot;
    }

    public static DungeonBookMenu fromNetwork(int containerId,
                                                Inventory playerInventory,
                                                FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int keySlot = buf.readVarInt();
        return new DungeonBookMenu(containerId, playerInventory, pos, keySlot);
    }

    public BlockPos getLecternPos() { return lecternPos; }
    public int getKeySlot() { return keySlot; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(lecternPos.getX() + 0.5,
                lecternPos.getY() + 0.5,
                lecternPos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Leave lobby when closing the GUI
        if (!player.level().isClientSide()) {
            GlobalPos globalPos = GlobalPos.of(player.level().dimension(), lecternPos);
            com.piranport.dungeon.lobby.DungeonLobbyManager.INSTANCE
                    .leaveLobby(globalPos, player.getUUID());
        }
    }
}
