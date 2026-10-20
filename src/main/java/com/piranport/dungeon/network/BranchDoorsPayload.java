package com.piranport.dungeon.network;

import com.piranport.PiranPort;
import com.piranport.dungeon.event.BranchDoors;
import com.piranport.platform.ClientHooks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** S2C：本人在某分歧来源节点的门群快照，只发给该玩家，别人看不到他的亮灭。 */
public record BranchDoorsPayload(UUID instanceId, String fromNode, List<BranchDoors.Door> doors)
        implements CustomPacketPayload {

    public static final Type<BranchDoorsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "branch_doors"));

    private static final StreamCodec<ByteBuf, BranchDoors.State> STATE_CODEC =
            ByteBufCodecs.VAR_INT.map(i -> BranchDoors.State.values()[i], BranchDoors.State::ordinal);

    private static final StreamCodec<ByteBuf, BranchDoors.Door> DOOR_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BranchDoors.Door::target,
            STATE_CODEC, BranchDoors.Door::state,
            ByteBufCodecs.VAR_INT, BranchDoors.Door::unmetMask,
            ByteBufCodecs.VAR_INT, BranchDoors.Door::ruleIndex,
            BranchDoors.Door::new);

    public static final StreamCodec<ByteBuf, BranchDoorsPayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, BranchDoorsPayload::instanceId,
            ByteBufCodecs.STRING_UTF8, BranchDoorsPayload::fromNode,
            ByteBufCodecs.collection(ArrayList::new, DOOR_CODEC), BranchDoorsPayload::doors,
            BranchDoorsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(BranchDoorsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientHooks.setBranchDoors(payload.instanceId(), payload.fromNode(), payload.doors()));
    }
}
