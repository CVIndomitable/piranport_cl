package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.client.DungeonHudLayer;
import com.piranport.dungeon.client.DungeonResultScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: Sends dungeon completion result to the client.
 */
public record DungeonResultPayload(String stageName, long timeMillis,
                                     boolean isFirstClear, List<String> rewardNames)
        implements CustomPacketPayload {

    public static final Type<DungeonResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "dungeon_result"));

    public static final StreamCodec<ByteBuf, DungeonResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageName());
                buf.writeLong(p.timeMillis());
                ByteBufCodecs.BOOL.encode(buf, p.isFirstClear());
                ByteBufCodecs.VAR_INT.encode(buf, p.rewardNames().size());
                for (String s : p.rewardNames()) {
                    ByteBufCodecs.STRING_UTF8.encode(buf, s);
                }
            },
            buf -> {
                String name = ByteBufCodecs.STRING_UTF8.decode(buf);
                long time = buf.readLong();
                boolean first = ByteBufCodecs.BOOL.decode(buf);
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                if (count < 0 || count > 64) count = 0;
                List<String> rewards = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    rewards.add(ByteBufCodecs.STRING_UTF8.decode(buf));
                }
                return new DungeonResultPayload(name, time, first, List.copyOf(rewards));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DungeonResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            DungeonHudLayer.clearDungeonState();
            Minecraft.getInstance().setScreen(new DungeonResultScreen(
                    payload.stageName(), payload.timeMillis(),
                    payload.isFirstClear(), payload.rewardNames()));
        });
    }
}
