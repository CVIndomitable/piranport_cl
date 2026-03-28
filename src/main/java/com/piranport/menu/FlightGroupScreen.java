package com.piranport.menu;

import com.piranport.component.FlightGroupData;
import com.piranport.item.AircraftItem;
import com.piranport.network.FlightGroupUpdatePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlightGroupScreen extends AbstractContainerScreen<FlightGroupMenu> {

    private int selectedGroupIndex = 0;

    // Group row Y positions (relative to topPos)
    private static final int[] GROUP_Y = {14, 47, 80, 113};
    private static final int GROUP_HEIGHT = 30;
    private static final int GROUP_WIDTH = 210;

    // Slots in each group row — compact: start from left, only assigned slots shown
    private static final int AIRCRAFT_X = 26;   // x offset within group row (relative to leftPos)
    private static final int AIRCRAFT_Y = 7;    // y offset within group row
    private static final int SLOT_SIZE = 18;
    private static final int MAX_WEAPON_SLOTS = 6;

    // Mode button position (relative to leftPos); placed after compact aircraft area
    private static final int MODE_X = AIRCRAFT_X + MAX_WEAPON_SLOTS * SLOT_SIZE + 4; // 138
    private static final int MODE_W = 36;
    private static final int BTN_H = 16;

    // Available aircraft display (relative to topPos)
    private static final int AVAIL_LABEL_Y = 147;
    private static final int AVAIL_Y = 157;

    // Ammo types cycling order; aerial_bomb replaces small/medium distinction
    private static final String[] AMMO_TYPES = {
            "",
            "piranport:aviation_fuel",
            "piranport:aerial_bomb",
            "piranport:aerial_torpedo",
            "piranport:fighter_ammo"
    };
    private static final String[] AMMO_LABELS = {
            "---", "燃料", "航弹", "鱼雷", "子弹"
    };
    // Ammo indicator colors shown as a strip at the bottom of assigned slots
    private static final int[] AMMO_COLORS = {
            0xFF666666,   // --- : dark gray
            0xFFFFAA00,   // fuel: amber
            0xFFFF4444,   // aerial bomb: red
            0xFF4488FF,   // torpedo: blue
            0xFFCCCCCC,   // fighter ammo: light gray
    };

    public FlightGroupScreen(FlightGroupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 220;
        this.imageHeight = 178;
        this.inventoryLabelY = 10000; // suppress default inventory label
        this.titleLabelY = 5;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main background
        gfx.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6);
        // 3D border
        gfx.fill(x, y, x + imageWidth - 1, y + 1, 0xFFFFFFFF);
        gfx.fill(x, y, x + 1, y + imageHeight - 1, 0xFFFFFFFF);
        gfx.fill(x + imageWidth - 1, y, x + imageWidth, y + imageHeight, 0xFF555555);
        gfx.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF555555);

        FlightGroupData data = menu.getGroupData();
        List<FlightGroupData.FlightGroup> groups = data.groups();

        for (int gi = 0; gi < FlightGroupData.MAX_GROUPS; gi++) {
            int gy = y + GROUP_Y[gi];
            boolean selected = (gi == selectedGroupIndex);

            // Group row background
            int rowBg = selected ? 0xFFAAAAAA : 0xFFB8B8B8;
            gfx.fill(x + 3, gy, x + 3 + GROUP_WIDTH, gy + GROUP_HEIGHT, rowBg);
            // Selection highlight border
            int borderColor = selected ? 0xFF4488FF : 0xFF888888;
            gfx.fill(x + 3, gy, x + 3 + GROUP_WIDTH, gy + 1, borderColor);
            gfx.fill(x + 3, gy, x + 4, gy + GROUP_HEIGHT, borderColor);
            gfx.fill(x + 3, gy + GROUP_HEIGHT - 1, x + 3 + GROUP_WIDTH, gy + GROUP_HEIGHT, 0xFF444444);
            gfx.fill(x + 2 + GROUP_WIDTH, gy, x + 3 + GROUP_WIDTH, gy + GROUP_HEIGHT, 0xFF444444);

            FlightGroupData.FlightGroup group = gi < groups.size()
                    ? groups.get(gi) : FlightGroupData.FlightGroup.empty(gi + 1);

            // Compact display: show only assigned slots, packed from left
            List<Integer> sortedSlots = sortedAssignedSlots(group);
            List<ItemStack> weapons = menu.getWeaponItems();
            for (int ci = 0; ci < sortedSlots.size(); ci++) {
                int si = sortedSlots.get(ci);
                int sx = x + AIRCRAFT_X + ci * SLOT_SIZE;
                int sy = gy + AIRCRAFT_Y;
                drawSlotBg(gfx, sx, sy);
                if (si < weapons.size() && !weapons.get(si).isEmpty()) {
                    gfx.renderItem(weapons.get(si), sx + 1, sy + 1);
                }
                // Per-slot ammo indicator: 3px strip at bottom
                String slotAmmo = group.getSlotAmmo(si);
                int ammoColor = getAmmoColor(slotAmmo);
                gfx.fill(sx + 2, sy + 14, sx + 16, sy + 17, ammoColor);
            }

            // Attack mode button
            int modeX = x + MODE_X;
            int modeY = gy + AIRCRAFT_Y;
            FlightGroupData.AttackMode mode = group.attackMode();
            int modeBg = switch (mode) {
                case FOCUS   -> 0xFF335599;
                case SPREAD  -> 0xFF994422;
                case FOLLOW  -> 0xFF225533;
            };
            int modeFg = switch (mode) {
                case FOCUS   -> 0xFF4466BB;
                case SPREAD  -> 0xFFBB5533;
                case FOLLOW  -> 0xFF338844;
            };
            String modeLabel = switch (mode) {
                case FOCUS  -> "集火";
                case SPREAD -> "分散";
                case FOLLOW -> "跟随";
            };
            gfx.fill(modeX, modeY, modeX + MODE_W, modeY + BTN_H, modeBg);
            gfx.fill(modeX + 1, modeY + 1, modeX + MODE_W - 1, modeY + BTN_H - 1, modeFg);
            gfx.drawString(this.font, modeLabel, modeX + 6, modeY + 4, 0xFFFFFF, false);
        }

        // Divider above available aircraft section
        gfx.fill(x + 3, y + AVAIL_LABEL_Y - 3, x + 3 + GROUP_WIDTH, y + AVAIL_LABEL_Y - 2, 0xFF888888);

        // Available aircraft slot backgrounds and icons
        List<ItemStack> weapons = menu.getWeaponItems();
        for (int si = 0; si < menu.getWeaponSlotCount(); si++) {
            int sx = x + 5 + si * SLOT_SIZE;
            int sy = y + AVAIL_Y;
            drawSlotBg(gfx, sx, sy);
            if (si < weapons.size() && !weapons.get(si).isEmpty()) {
                gfx.renderItem(weapons.get(si), sx + 1, sy + 1);
                // Green overlay if assigned to selected group
                if (isInSelectedGroup(data, si)) {
                    gfx.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x8844EE44);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);

        for (int gi = 0; gi < FlightGroupData.MAX_GROUPS; gi++) {
            int gy = GROUP_Y[gi];
            boolean selected = (gi == selectedGroupIndex);
            int textColor = selected ? 0x2255FF : 0x404040;
            gfx.drawString(this.font, "G" + (gi + 1) + ":", 6, gy + 11, textColor, false);
        }

        gfx.drawString(this.font,
                Component.translatable("container.piranport.available_aircraft"),
                5, AVAIL_LABEL_Y, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int relX = (int) mouseX - leftPos;
        int relY = (int) mouseY - topPos;

        // Check group rows
        for (int gi = 0; gi < FlightGroupData.MAX_GROUPS; gi++) {
            int gy = GROUP_Y[gi];
            if (relX >= 3 && relX < 3 + GROUP_WIDTH && relY >= gy && relY < gy + GROUP_HEIGHT) {

                // Check compact aircraft slot indicators
                int slotHit = getAircraftSlotHit(relX, relY, gy, gi);
                if (slotHit >= 0) {
                    selectedGroupIndex = gi;
                    if (button == 0) {
                        toggleSlotInGroup(gi, slotHit);
                    } else if (button == 1) {
                        FlightGroupData data = menu.getGroupData();
                        List<FlightGroupData.FlightGroup> groups = data.groups();
                        if (gi < groups.size() && groups.get(gi).slotIndices().contains(slotHit)) {
                            cycleSlotAmmo(gi, slotHit);
                        }
                    }
                    return true;
                }

                // Check mode button
                int modeY = gy + AIRCRAFT_Y;
                if (relX >= MODE_X && relX < MODE_X + MODE_W
                        && relY >= modeY && relY < modeY + BTN_H) {
                    selectedGroupIndex = gi;
                    toggleAttackMode(gi);
                    return true;
                }

                // Click on group row → select group
                selectedGroupIndex = gi;
                return true;
            }
        }

        // Check available aircraft slots (left-click only)
        for (int si = 0; si < menu.getWeaponSlotCount(); si++) {
            int sx = 5 + si * SLOT_SIZE;
            if (relX >= sx && relX < sx + 16 && relY >= AVAIL_Y && relY < AVAIL_Y + 16) {
                List<ItemStack> weapons = menu.getWeaponItems();
                if (si < weapons.size() && weapons.get(si).getItem() instanceof AircraftItem) {
                    toggleSlotInGroup(selectedGroupIndex, si);
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Returns the actual slot index from a click on a compact group row, or -1 if not a slot. */
    private int getAircraftSlotHit(int relX, int relY, int groupRelY, int gi) {
        FlightGroupData data = menu.getGroupData();
        List<FlightGroupData.FlightGroup> groups = data.groups();
        FlightGroupData.FlightGroup group = gi < groups.size()
                ? groups.get(gi) : FlightGroupData.FlightGroup.empty(gi + 1);

        List<Integer> sortedSlots = sortedAssignedSlots(group);
        int ay = groupRelY + AIRCRAFT_Y;
        for (int ci = 0; ci < sortedSlots.size(); ci++) {
            int ax = AIRCRAFT_X + ci * SLOT_SIZE;
            if (relX >= ax && relX < ax + 16 && relY >= ay && relY < ay + 16) {
                return sortedSlots.get(ci);
            }
        }
        return -1;
    }

    private boolean isInSelectedGroup(FlightGroupData data, int slotIndex) {
        List<FlightGroupData.FlightGroup> groups = data.groups();
        if (selectedGroupIndex < groups.size()) {
            return groups.get(selectedGroupIndex).slotIndices().contains(slotIndex);
        }
        return false;
    }

    private void toggleSlotInGroup(int groupIndex, int slotIndex) {
        FlightGroupData data = menu.getGroupData();
        List<FlightGroupData.FlightGroup> groups = data.groups();
        if (groupIndex >= groups.size()) return;
        FlightGroupData.FlightGroup updated = groups.get(groupIndex).withSlotToggled(slotIndex);
        FlightGroupData newData = data.withGroup(groupIndex, updated);
        menu.setGroupData(newData);
        sendUpdate(newData);
    }

    private void cycleSlotAmmo(int groupIndex, int slotIndex) {
        FlightGroupData data = menu.getGroupData();
        List<FlightGroupData.FlightGroup> groups = data.groups();
        if (groupIndex >= groups.size()) return;
        String current = groups.get(groupIndex).getSlotAmmo(slotIndex);
        int idx = 0;
        for (int i = 0; i < AMMO_TYPES.length; i++) {
            if (AMMO_TYPES[i].equals(current)) { idx = i; break; }
        }
        String next = AMMO_TYPES[(idx + 1) % AMMO_TYPES.length];
        FlightGroupData newData = data.withGroup(groupIndex,
                groups.get(groupIndex).withSlotAmmo(slotIndex, next));
        menu.setGroupData(newData);
        sendUpdate(newData);
    }

    private void toggleAttackMode(int groupIndex) {
        FlightGroupData data = menu.getGroupData();
        List<FlightGroupData.FlightGroup> groups = data.groups();
        if (groupIndex >= groups.size()) return;
        FlightGroupData.FlightGroup group = groups.get(groupIndex);
        // Cycle: FOCUS → SPREAD → FOLLOW → FOCUS
        FlightGroupData.AttackMode newMode = switch (group.attackMode()) {
            case FOCUS   -> FlightGroupData.AttackMode.SPREAD;
            case SPREAD  -> FlightGroupData.AttackMode.FOLLOW;
            case FOLLOW  -> FlightGroupData.AttackMode.FOCUS;
        };
        FlightGroupData newData = data.withGroup(groupIndex, group.withAttackMode(newMode));
        menu.setGroupData(newData);
        sendUpdate(newData);
    }

    private void sendUpdate(FlightGroupData data) {
        PacketDistributor.sendToServer(new FlightGroupUpdatePayload(menu.getCoreSlot(), data));
    }

    private int getAmmoColor(String ammoType) {
        for (int i = 0; i < AMMO_TYPES.length; i++) {
            if (AMMO_TYPES[i].equals(ammoType)) return AMMO_COLORS[i];
        }
        return AMMO_COLORS[0];
    }

    private String getAmmoLabel(String ammoType) {
        for (int i = 0; i < AMMO_TYPES.length; i++) {
            if (AMMO_TYPES[i].equals(ammoType)) return AMMO_LABELS[i];
        }
        return "---";
    }

    /** Returns the assigned slot indices for a group, sorted ascending. */
    private static List<Integer> sortedAssignedSlots(FlightGroupData.FlightGroup group) {
        List<Integer> sorted = new ArrayList<>(group.slotIndices());
        Collections.sort(sorted);
        return sorted;
    }

    private void drawSlotBg(GuiGraphics gfx, int x, int y) {
        gfx.fill(x, y, x + 18, y + 1, 0xFF373737);
        gfx.fill(x, y, x + 1, y + 18, 0xFF373737);
        gfx.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
        gfx.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        gfx.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        renderTooltip(gfx, mouseX, mouseY);
        renderAircraftTooltip(gfx, mouseX, mouseY);
    }

    private void renderAircraftTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        int relX = mouseX - leftPos;
        int relY = mouseY - topPos;
        List<ItemStack> weapons = menu.getWeaponItems();

        // Check group row aircraft slots (compact layout)
        FlightGroupData data = menu.getGroupData();
        for (int gi = 0; gi < FlightGroupData.MAX_GROUPS; gi++) {
            int gy = GROUP_Y[gi];
            FlightGroupData.FlightGroup group = gi < data.groups().size()
                    ? data.groups().get(gi) : FlightGroupData.FlightGroup.empty(gi + 1);

            List<Integer> sortedSlots = sortedAssignedSlots(group);
            for (int ci = 0; ci < sortedSlots.size(); ci++) {
                int si = sortedSlots.get(ci);
                int sx = AIRCRAFT_X + ci * SLOT_SIZE;
                int sy = gy + AIRCRAFT_Y;
                if (relX >= sx && relX < sx + 16 && relY >= sy && relY < sy + 16) {
                    if (si < weapons.size() && !weapons.get(si).isEmpty()) {
                        String ammoLabel = getAmmoLabel(group.getSlotAmmo(si));
                        gfx.renderTooltip(this.font,
                                List.of(
                                        weapons.get(si).getHoverName(),
                                        Component.literal("弹种: " + ammoLabel + " (右键切换)")
                                                .withStyle(net.minecraft.ChatFormatting.GRAY)
                                ),
                                java.util.Optional.empty(), mouseX, mouseY);
                        return;
                    }
                }
            }
        }

        // Check available aircraft row
        for (int si = 0; si < menu.getWeaponSlotCount(); si++) {
            int sx = 5 + si * SLOT_SIZE;
            if (relX >= sx && relX < sx + 16 && relY >= AVAIL_Y && relY < AVAIL_Y + 16) {
                if (si < weapons.size() && !weapons.get(si).isEmpty()) {
                    gfx.renderTooltip(this.font, weapons.get(si), mouseX, mouseY);
                    return;
                }
            }
        }
    }
}
