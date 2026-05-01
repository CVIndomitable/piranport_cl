package com.piranport.network;

import com.piranport.combat.TransformationManager;
import com.piranport.entity.TorpedoEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: player presses 9/0 to steer wire-guided torpedoes left/right.
 * direction: -1 = left, +1 = right
 */
public class TorpedoSteerPayload {
    private final int direction;

    public TorpedoSteerPayload(int direction) {
        this.direction = direction;
    }

    public static void encode(TorpedoSteerPayload msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.direction);
    }

    public static TorpedoSteerPayload decode(FriendlyByteBuf buf) {
        return new TorpedoSteerPayload(buf.readInt());
    }

    public static void handle(TorpedoSteerPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Player player = ctx.get().getSender();
            if (player == null) return;
            if (!TransformationManager.isPlayerTransformed(player)) return;

            int dir = msg.direction;
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
        ctx.get().setPacketHandled(true);
    }
}
