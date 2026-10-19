package com.piranport.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * 创造模式弹药库中"已勾选"的弹药/燃料条目。
 *
 * <p>存物品注册 ID 而不是布尔表：弹药种类会随数据包/版本变化，ID 列表天然可扩展，
 * 也不会因为新增弹药而让旧存档的组件结构失效。</p>
 */
public record CreativeAmmoSelection(List<String> itemIds) {

    public static final int MAX_ITEMS = 256;
    public static final int MAX_ID_LENGTH = 160;
    public static final CreativeAmmoSelection EMPTY = new CreativeAmmoSelection(List.of());

    public CreativeAmmoSelection {
        itemIds = List.copyOf(itemIds);
    }

    public boolean contains(String itemId) {
        return itemIds.contains(itemId);
    }

    public static final Codec<CreativeAmmoSelection> CODEC =
            Codec.STRING.listOf().xmap(CreativeAmmoSelection::new, CreativeAmmoSelection::itemIds);

    public static final StreamCodec<ByteBuf, CreativeAmmoSelection> STREAM_CODEC = StreamCodec.of(
            (buf, selection) -> {
                List<String> ids = selection.itemIds();
                ByteBufCodecs.VAR_INT.encode(buf, ids.size());
                for (String id : ids) {
                    ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).encode(buf, id);
                }
            },
            buf -> {
                int size = ByteBufCodecs.VAR_INT.decode(buf);
                if (size < 0 || size > MAX_ITEMS) size = 0;
                List<String> ids = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    ids.add(ByteBufCodecs.stringUtf8(MAX_ID_LENGTH).decode(buf));
                }
                return new CreativeAmmoSelection(ids);
            }
    );
}
