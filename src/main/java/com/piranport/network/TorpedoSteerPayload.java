package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.combat.TransformationManager;
import com.piranport.entity.TorpedoEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: player presses 9/0 to steer wire-guided torpedoes left/right.
 * direction: -1 = left, +1 = right
 */
public record TorpedoSteerPayload(int direction) implements CustomPacketPayload {

    public static final Type<TorpedoSteerPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "torpedo_steer"));

    public static final StreamCodec<ByteBuf, TorpedoSteerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> ByteBufCodecs.INT.encode(buf, p.direction()),
            buf -> new TorpedoSteerPayload(ByteBufCodecs.INT.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TorpedoSteerPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player == null) return;
            if (!TransformationManager.isPlayerTransformed(player)) return;

            int dir = payload.direction();
            if (dir != -1 && dir != 1) return;

            // Find all wire-guided torpedoes owned by this player in a 32-block radius
            AABB searchBox = player.getBoundingBox().inflate(32.0);
            for (Entity entity : player.level().getEntities(player, searchBox,
                    e -> e instanceof TorpedoEntity)) {
                TorpedoEntity torpedo = (TorpedoEntity) entity;
                if (torpedo.isWireGuided() && torpedo.getOwner() == player) {
                    torpedo.steer(dir);
                }
            }
        });
    }
}
