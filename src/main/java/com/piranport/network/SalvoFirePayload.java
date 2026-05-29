package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.artillery.ArtilleryItem;
import com.piranport.item.ShipCoreCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S 齐射包：双击时触发，发射物品栏中所有同类型火炮。
 * 结构镜像 ScopeFirePayload，不传 itemTypeId —— 服务端直接从手持物品读取。
 */
public record SalvoFirePayload(byte mode, double targetX, double targetY, double targetZ)
        implements CustomPacketPayload {

    public static final Type<SalvoFirePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "salvo_fire"));

    public static final StreamCodec<ByteBuf, SalvoFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.mode());
                buf.writeDouble(p.targetX());
                buf.writeDouble(p.targetY());
                buf.writeDouble(p.targetZ());
            },
            buf -> {
                byte mode = buf.readByte();
                double tx = buf.readDouble();
                double ty = buf.readDouble();
                double tz = buf.readDouble();
                return new SalvoFirePayload(mode, tx, ty, tz);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static SalvoFirePayload quickFire() {
        return new SalvoFirePayload((byte) 0, 0, 0, 0);
    }

    public static SalvoFirePayload aimedFire(double x, double y, double z) {
        return new SalvoFirePayload((byte) 1, x, y, z);
    }

    public static SalvoFirePayload maxRangeFire() {
        return new SalvoFirePayload((byte) 2, 0, 0, 0);
    }

    public static void handle(SalvoFirePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (weapon.isEmpty()) return;
            if (!(weapon.getItem() instanceof ArtilleryItem)) return;

            if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) return;

            Item weaponType = weapon.getItem();

            switch (payload.mode()) {
                case 0 -> // QUICK_FIRE
                    ShipCoreCombat.beginSalvo(player, weaponType,
                            ShipCoreCombat.ARTILLERY_AIM_NONE, 0, 0, 0);

                case 1 -> { // AIMED
                    double dx = payload.targetX() - player.getX();
                    double dy = payload.targetY() - player.getY();
                    double dz = payload.targetZ() - player.getZ();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    int simDist = player.server.getPlayerList().getSimulationDistance();
                    double maxRange = Math.max(64.0, simDist * 16.0);
                    if (distSq > maxRange * maxRange) {
                        PiranPort.LOGGER.warn("SalvoFirePayload target too far ({}m), fallback to max-range",
                                Math.sqrt(distSq));
                        ShipCoreCombat.beginSalvo(player, weaponType,
                                ShipCoreCombat.ARTILLERY_AIM_MAX_RANGE, 0, 0, 0);
                    } else {
                        ShipCoreCombat.beginSalvo(player, weaponType,
                                ShipCoreCombat.ARTILLERY_AIM_TARGET,
                                payload.targetX(), payload.targetY(), payload.targetZ());
                    }
                }

                case 2 -> // MAX_RANGE
                    ShipCoreCombat.beginSalvo(player, weaponType,
                            ShipCoreCombat.ARTILLERY_AIM_MAX_RANGE, 0, 0, 0);
            }
        });
    }
}
