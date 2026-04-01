package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.client.DungeonReviveScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: Notifies client that the player died in a dungeon. Opens the revive screen.
 */
public record PlayerDiedInDungeonPayload() implements CustomPacketPayload {

    public static final Type<PlayerDiedInDungeonPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "player_died_dungeon"));

    public static final StreamCodec<ByteBuf, PlayerDiedInDungeonPayload> STREAM_CODEC =
            StreamCodec.unit(new PlayerDiedInDungeonPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PlayerDiedInDungeonPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new DungeonReviveScreen());
        });
    }
}
