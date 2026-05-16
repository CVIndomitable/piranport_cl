package com.piranport.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 存储玩家为该火炮选定的偏好弹种（自动装填模式）。
 * 设置后，发射逻辑会优先消耗该弹种，背包中该弹种不足时回退到任意足量弹种。
 * 空字符串表示未选择（使用背包中第一个足量弹种）。
 */
public record SelectedAmmoType(String ammoItemId) {

    public static final SelectedAmmoType EMPTY = new SelectedAmmoType("");

    public boolean hasSelection() {
        return !ammoItemId.isEmpty();
    }

    public static final Codec<SelectedAmmoType> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.fieldOf("ammo_item_id").forGetter(SelectedAmmoType::ammoItemId)
    ).apply(inst, SelectedAmmoType::new));

    public static final StreamCodec<ByteBuf, SelectedAmmoType> STREAM_CODEC = StreamCodec.of(
            (buf, sat) -> ByteBufCodecs.STRING_UTF8.encode(buf, sat.ammoItemId()),
            buf -> {
                String id = ByteBufCodecs.STRING_UTF8.decode(buf);
                if (id.length() > 256) throw new io.netty.handler.codec.DecoderException("ammoItemId too long");
                return new SelectedAmmoType(id);
            }
    );
}
