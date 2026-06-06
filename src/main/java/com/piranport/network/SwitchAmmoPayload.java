package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.component.LoadedAmmo;
import com.piranport.component.SelectedAmmoType;
import com.piranport.component.SlotCooldowns;
import com.piranport.component.WeaponCooldown;
import com.piranport.item.ShipCoreCombat;
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
            // BuiltInRegistries.ITEM.get() 在找不到时返回 Items.AIR 而非 null
            if (ammoItem == null || ammoItem == net.minecraft.world.item.Items.AIR) return;

            // 验证玩家实际持有武器（防止伪造请求）
            boolean hasWeapon = false;
            for (int i = 0; i < 9; i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (slot.getItem() instanceof com.piranport.artillery.ArtilleryItem) {
                    hasWeapon = true;
                    break;
                }
            }
            if (!hasWeapon) return;

            // 验证口径匹配（防止客户端伪造不匹配口径的弹药）
            ItemStack ammoStack = new ItemStack(ammoItem);
            if (!ShipCoreCombat.matchesCaliber(ammoStack, weapon, player.level())) {
                return;
            }

            // 创造模式：跳过背包弹药检查
            boolean isCreative = player.getAbilities().instabuild;

            if (!isCreative) {
                // 生存模式：验证背包中有该弹种
                Inventory inv = player.getInventory();
                boolean hasAmmo = false;
                for (ItemStack s : inv.items) {
                    if (s.getItem() == ammoItem) {
                        hasAmmo = true;
                        break;
                    }
                }
                if (!hasAmmo) {
                    ItemStack offhand = inv.offhand.get(0);
                    if (offhand.getItem() == ammoItem) {
                        hasAmmo = true;
                    }
                }

                if (!hasAmmo) return;
            }

            // 设置选中弹种
            weapon.set(ModDataComponents.SELECTED_AMMO_TYPE.get(),
                    new SelectedAmmoType(payload.ammoItemId()));

            // 同步弹种到所有同类型火炮
            ShipCoreCombat.syncAmmoToSiblingGuns(player);

            // 切换弹种时重新开始装填（所有同类型火炮一起重置）
            ItemStack coreStack = TransformationManager.findTransformedCore(player);
            if (coreStack.isEmpty()) return;

            // 获取武器的装填时间
            int reloadTicks = 0;
            if (weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem artilleryItem) {
                reloadTicks = artilleryItem.getEffectiveData(player.level()).reloadTime();
            }
            int boostedReloadTicks = TransformationManager.boostedCooldown(player, reloadTicks);
            long currentTick = player.level().getGameTime();

            // 遍历所有同类型火炮槽位，仅重置正在装填中的火炮；已装填完成的火炮不受影响。
            Inventory inv = player.getInventory();
            SlotCooldowns cooldowns = coreStack.getOrDefault(
                    ModDataComponents.SLOT_COOLDOWNS.get(), SlotCooldowns.EMPTY);
            SlotCooldowns updated = cooldowns;

            for (int i = 0; i < inv.items.size(); i++) {
                ItemStack slot = inv.items.get(i);
                if (slot.getItem() == weapon.getItem()
                        && isReloadingUnloadedCannon(slot, updated, i, currentTick, player.level())) {
                    updated = updated.withSlotCooldown(i, boostedReloadTicks, currentTick);
                    slot.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                            WeaponCooldown.of(currentTick, boostedReloadTicks));
                }
            }
            ItemStack offhand = inv.offhand.get(0);
            if (offhand.getItem() == weapon.getItem()
                    && isReloadingUnloadedCannon(offhand, updated, 40, currentTick, player.level())) {
                updated = updated.withSlotCooldown(40, boostedReloadTicks, currentTick);
                offhand.set(ModDataComponents.WEAPON_COOLDOWN.get(),
                        WeaponCooldown.of(currentTick, boostedReloadTicks));
            }

            coreStack.set(ModDataComponents.SLOT_COOLDOWNS.get(), updated);

            // 播放音效和显示消息
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    ModSounds.AMMO_SWITCH.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            player.displayClientMessage(
                    Component.translatable("message.piranport.ammo_switched",
                            ammoItem.getDescription()), true);
        });
    }

    private static boolean isReloadingUnloadedCannon(ItemStack stack, SlotCooldowns cooldowns,
            int slotIndex, long currentTick, net.minecraft.world.level.Level level) {
        if (!(stack.getItem() instanceof com.piranport.artillery.ArtilleryItem artilleryItem)) {
            return false;
        }
        int barrels = artilleryItem.getEffectiveData(level).barrels();
        LoadedAmmo loaded = stack.getOrDefault(ModDataComponents.LOADED_AMMO.get(), LoadedAmmo.EMPTY);
        if (loaded.hasAmmo() && loaded.count() >= barrels) {
            return false;
        }
        WeaponCooldown itemCooldown = stack.get(ModDataComponents.WEAPON_COOLDOWN.get());
        return cooldowns.isOnCooldown(slotIndex, currentTick)
                || (itemCooldown != null && itemCooldown.isOnCooldown(currentTick));
    }
}
