package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.component.SelectedAmmoType;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S 包：玩家按 Tab 键（或滚轮）切换手持火炮的偏好弹种。
 * 循环切换到背包中下一个可用弹种。
 */
public record SwitchAmmoPayload() implements CustomPacketPayload {
    public static final Type<SwitchAmmoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "switch_ammo"));

    public static final StreamCodec<ByteBuf, SwitchAmmoPayload> STREAM_CODEC =
            StreamCodec.unit(new SwitchAmmoPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwitchAmmoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;

            ItemStack weapon = player.getMainHandItem();
            if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem
                    || weapon.getItem() instanceof com.piranport.item.CannonItem)) {
                return;
            }

            Inventory inv = player.getInventory();
            List<Item> availableTypes = new ArrayList<>();

            // 扫描背包中匹配口径的弹药
            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack ammo = inv.items.get(i);
                if (!ammo.isEmpty() && ShipCoreItem.matchesCaliber(ammo, weapon)) {
                    if (!availableTypes.contains(ammo.getItem())) {
                        availableTypes.add(ammo.getItem());
                    }
                }
            }
            ItemStack offhand = inv.offhand.get(0);
            if (!offhand.isEmpty() && ShipCoreItem.matchesCaliber(offhand, weapon)) {
                if (!availableTypes.contains(offhand.getItem())) {
                    availableTypes.add(offhand.getItem());
                }
            }

            if (availableTypes.isEmpty()) {
                player.displayClientMessage(Component.translatable("message.piranport.no_ammo"), true);
                return;
            }

            // 确定当前选中弹种并切换到下一个
            SelectedAmmoType current = weapon.getOrDefault(ModDataComponents.SELECTED_AMMO_TYPE.get(), SelectedAmmoType.EMPTY);
            int currentIdx = -1;
            if (current.hasSelection()) {
                ResourceLocation currentId = ResourceLocation.tryParse(current.ammoItemId());
                if (currentId != null) {
                    Item currentItem = BuiltInRegistries.ITEM.get(currentId);
                    currentIdx = availableTypes.indexOf(currentItem);
                }
            }

            int nextIdx = (currentIdx + 1) % availableTypes.size();
            Item nextType = availableTypes.get(nextIdx);
            String nextId = BuiltInRegistries.ITEM.getKey(nextType).toString();

            weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(), new SelectedAmmoType(nextId));

            // Phase 10: 弹种切换音效
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AMMO_SWITCH.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);

            player.displayClientMessage(
                    Component.translatable("message.piranport.ammo_switched", nextType.getDescription()), true);
        });
    }
}
