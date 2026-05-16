package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.server.ScopingManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S：通知服务端玩家进入瞄准镜模式，服务端阻止 ArtilleryItem.use() 中的立即开火。 */
public record ScopeEnterPayload() implements CustomPacketPayload {

    public static final Type<ScopeEnterPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "scope_enter"));

    public static final StreamCodec<ByteBuf, ScopeEnterPayload> STREAM_CODEC = StreamCodec.unit(new ScopeEnterPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(ScopeEnterPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() != null) {
                ScopingManager.setScoping(ctx.player(), true);
            }
        });
    }
}
