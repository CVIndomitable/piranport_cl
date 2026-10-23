package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.combat.data.AmmoSelectionRules;
import com.piranport.combat.data.WeaponState;
import com.piranport.item.ShipCoreCombat;
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
 * C2S 包：玩家松开 Tab 键时发送选中的弹种ID，设置当前武器的下一轮弹种偏好。
 */
public record SwitchAmmoPayload(String ammoItemId) implements CustomPacketPayload {
    private static final int MAX_AMMO_ITEM_ID_LENGTH = 256;

    public static final Type<SwitchAmmoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "switch_ammo"));

    public static final StreamCodec<ByteBuf, SwitchAmmoPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(MAX_AMMO_ITEM_ID_LENGTH), SwitchAmmoPayload::ammoItemId,
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
            ResourceLocation ammoId = ResourceLocation.tryParse(payload.ammoItemId());
            if (ammoId == null) return;
            Item ammoItem = BuiltInRegistries.ITEM.get(ammoId);
            if (ammoItem == null || ammoItem == net.minecraft.world.item.Items.AIR) return;
            if (!AmmoSelectionRules.hasAvailable(player, weapon, ammoItem)) return;

            if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
                // Preserve the existing same-type cannon synchronization and in-progress reload behavior.
                com.piranport.combat.cannon.CannonReloading.selectAmmo(player, weapon, payload.ammoItemId());
            } else {
                new WeaponState(weapon).setSelectedAmmoType(payload.ammoItemId());
            }

            // 播放音效和显示消息
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AMMO_SWITCH.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.piranport.ammo_switched",
                            ammoItem.getDescription()), true);
        });
    }

}
