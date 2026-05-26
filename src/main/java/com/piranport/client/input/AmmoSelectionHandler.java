package com.piranport.client.input;

import com.piranport.client.AmmoSelectOverlay;
import com.piranport.network.SwitchAmmoPayload;
import com.piranport.registry.ModKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 弹药选择轮盘（Tab键）：按下打开轮盘，松开确认选择。
 *
 * <p><b>线程模型</b>: 客户端渲染线程（单线程），无需同步。
 */
public class AmmoSelectionHandler {

    private AmmoSelectionHandler() {}

    /** 处理弹药选择轮盘的逻辑。返回 true 如果轮盘当前处于打开状态。 */
    public static boolean handleAmmoWheel(Minecraft mc, Player player, boolean transformed, boolean inReconMode) {
        if (player == null) return false;

        // Tab 按下且轮盘未打开 → 打开轮盘
        if (ModKeyMappings.SWITCH_AMMO.isDown() && !AmmoSelectOverlay.isOpen()) {
            if (transformed && !inReconMode) {
                ItemStack hand = player.getMainHandItem();
                if (hand.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
                    AmmoSelectOverlay.open();
                }
            }
        }

        // Tab 松开且轮盘打开 → 确认选择
        if (!ModKeyMappings.SWITCH_AMMO.isDown() && AmmoSelectOverlay.isOpen()) {
            Item selectedAmmo = AmmoSelectOverlay.getHoveredAmmo();
            if (selectedAmmo != null) {
                String ammoId = BuiltInRegistries.ITEM.getKey(selectedAmmo).toString();
                PacketDistributor.sendToServer(new SwitchAmmoPayload(ammoId));
            }
            AmmoSelectOverlay.close();
        }

        return AmmoSelectOverlay.isOpen();
    }
}
