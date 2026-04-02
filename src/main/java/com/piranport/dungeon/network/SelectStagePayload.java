package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.data.DungeonRegistry;
import com.piranport.dungeon.key.DungeonKeyItem;
import com.piranport.dungeon.lobby.DungeonLobbyManager;
import com.piranport.registry.ModDataComponents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: Flagship selects a stage in the dungeon book.
 */
public record SelectStagePayload(BlockPos lecternPos, int keySlot, String stageId)
        implements CustomPacketPayload {

    public static final Type<SelectStagePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "select_stage"));

    public static final StreamCodec<ByteBuf, SelectStagePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeLong(p.lecternPos().asLong());
                ByteBufCodecs.VAR_INT.encode(buf, p.keySlot());
                ByteBufCodecs.STRING_UTF8.encode(buf, p.stageId());
            },
            buf -> new SelectStagePayload(
                    BlockPos.of(buf.readLong()),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(SelectStagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!DungeonRegistry.INSTANCE.hasStage(payload.stageId())) return;

            DungeonLobbyManager.Lobby lobby =
                    DungeonLobbyManager.INSTANCE.getLobby(payload.lecternPos());
            if (lobby != null && lobby.isFlagship(player.getUUID())) {
                lobby.setSelectedStageId(payload.stageId());

                // Write stage ID onto the key so SelectNodePayload can read it later
                ItemStack keyStack = player.getInventory().getItem(payload.keySlot());
                if (keyStack.getItem() instanceof DungeonKeyItem) {
                    keyStack.set(ModDataComponents.DUNGEON_STAGE_ID.get(), payload.stageId());
                }
            }
        });
    }
}
