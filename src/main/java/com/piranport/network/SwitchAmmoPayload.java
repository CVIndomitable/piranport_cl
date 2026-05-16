package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.component.SelectedAmmoType;
import com.piranport.item.ShipCoreItem;
import com.piranport.registry.ModDataComponents;
import com.piranport.registry.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S 包：玩家松开 Tab 键时发送选中的弹种ID，设置手持火炮的偏好弹种。
 */
public record SwitchAmmoPayload(String ammoItemId) implements CustomPacketPayload {
    public static final Type<SwitchAmmoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "switch_ammo"));

    public static final StreamCodec<ByteBuf, SwitchAmmoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, SwitchAmmoPayload::ammoItemId,
                    SwitchAmmoPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwitchAmmoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player == null) return;

            ItemStack weapon = player.getMainHandItem();
            if (!(weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem)) {
                return;
            }

            // 验证弹种ID有效性
            ResourceLocation ammoId = ResourceLocation.tryParse(payload.ammoItemId());
            if (ammoId == null) return;

            Item ammoItem = BuiltInRegistries.ITEM.get(ammoId);
            if (ammoItem == null) return;

            // 验证背包中有该弹种且口径匹配
            Inventory inv = player.getInventory();
            boolean hasAmmo = false;
            for (ItemStack s : inv.items) {
                if (s.getItem() == ammoItem && ShipCoreItem.matchesCaliber(s, weapon)) {
                    hasAmmo = true;
                    break;
                }
            }
            if (!hasAmmo) {
                ItemStack offhand = inv.offhand.get(0);
                if (offhand.getItem() == ammoItem && ShipCoreItem.matchesCaliber(offhand, weapon)) {
                    hasAmmo = true;
                }
            }

            if (!hasAmmo) return;

            // 设置选中弹种
            weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(),
                    new SelectedAmmoType(payload.ammoItemId()));

            // 播放音效和显示消息
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AMMO_SWITCH.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.piranport.ammo_switched",
                            ammoItem.getDescription()), true);
        });
    }
}
