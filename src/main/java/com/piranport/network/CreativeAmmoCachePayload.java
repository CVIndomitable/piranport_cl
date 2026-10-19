package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.ammo.CreativeAmmoCache;
import com.piranport.ammo.CreativeAmmoCatalog;
import com.piranport.component.CreativeAmmoSelection;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S：创造模式弹药库 GUI 的勾选变更。
 *
 * <p>服务端重新校验"手上确实是弹药库"和"条目确实属于弹药/燃料清单"，
 * 客户端只负责发意图，不能借这个包往组件里塞任意物品 ID。</p>
 */
public record CreativeAmmoCachePayload(int action, String itemId, boolean enabled, boolean offhand)
        implements CustomPacketPayload {

    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_SELECT_ALL = 1;
    public static final int ACTION_CLEAR = 2;

    public static final Type<CreativeAmmoCachePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "creative_ammo_cache"));

    public static final StreamCodec<ByteBuf, CreativeAmmoCachePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buf, payload.action());
                ByteBufCodecs.stringUtf8(CreativeAmmoSelection.MAX_ID_LENGTH).encode(buf, payload.itemId());
                ByteBufCodecs.BOOL.encode(buf, payload.enabled());
                ByteBufCodecs.BOOL.encode(buf, payload.offhand());
            },
            buf -> new CreativeAmmoCachePayload(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.stringUtf8(CreativeAmmoSelection.MAX_ID_LENGTH).decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeAmmoCachePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player == null) return;

            ItemStack cacheStack = findCacheStack(player, payload.offhand());
            if (cacheStack.isEmpty()) return;

            List<String> ids = new ArrayList<>(CreativeAmmoCache.selection(cacheStack).itemIds());
            switch (payload.action()) {
                case ACTION_SELECT_ALL -> {
                    ids.clear();
                    for (Item item : CreativeAmmoCatalog.allItems()) {
                        ids.add(CreativeAmmoCatalog.idOf(item));
                    }
                }
                case ACTION_CLEAR -> ids.clear();
                case ACTION_TOGGLE -> {
                    if (!CreativeAmmoCatalog.isValid(payload.itemId())) return;
                    if (payload.enabled()) {
                        if (!ids.contains(payload.itemId())) ids.add(payload.itemId());
                    } else {
                        ids.remove(payload.itemId());
                    }
                }
                default -> {
                    return;
                }
            }
            if (ids.size() > CreativeAmmoSelection.MAX_ITEMS) {
                ids = new ArrayList<>(ids.subList(0, CreativeAmmoSelection.MAX_ITEMS));
            }
            cacheStack.set(ModDataComponents.CREATIVE_AMMO_SELECTION.get(),
                    new CreativeAmmoSelection(ids));
            player.getInventory().setChanged();
        });
    }

    /** 先查声明的那只手，物品被移走时再兜底扫一遍背包。 */
    private static ItemStack findCacheStack(Player player, boolean offhand) {
        ItemStack held = player.getItemInHand(offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (CreativeAmmoCache.isCacheStack(held)) return held;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (CreativeAmmoCache.isCacheStack(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }
}
