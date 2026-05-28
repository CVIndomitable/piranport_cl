package com.piranport.network;

import com.piranport.PiranPort;
import com.piranport.item.ShipCoreItem;
import com.piranport.item.ShipCoreCombat;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C2S：玩家从瞄准镜模式开火。mode 区分快速点击、弹道解算、最大射程三种模式。 */
public record ScopeFirePayload(FireMode mode, double targetX, double targetY, double targetZ) implements CustomPacketPayload {

    public enum FireMode {
        QUICK_FIRE,   // 正常方向开火（使用玩家视线）
        AIMED,        // 弹道解算瞄准
        MAX_RANGE     // 最大射程仰角开火
    }

    public static final Type<ScopeFirePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(PiranPort.MOD_ID, "scope_fire"));

    public static final StreamCodec<ByteBuf, ScopeFirePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.mode().ordinal());
                buf.writeDouble(p.targetX());
                buf.writeDouble(p.targetY());
                buf.writeDouble(p.targetZ());
            },
            buf -> {
                FireMode mode = FireMode.values()[buf.readByte()];
                double tx = buf.readDouble();
                double ty = buf.readDouble();
                double tz = buf.readDouble();
                return new ScopeFirePayload(mode, tx, ty, tz);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    /** 快捷工厂：快速点击（正常方向开火） */
    public static ScopeFirePayload quickFire() {
        return new ScopeFirePayload(FireMode.QUICK_FIRE, 0, 0, 0);
    }

    /** 快捷工厂：瞄准开火（弹道解算） */
    public static ScopeFirePayload aimedFire(double x, double y, double z) {
        return new ScopeFirePayload(FireMode.AIMED, x, y, z);
    }

    /** 快捷工厂：最大射程开火 */
    public static ScopeFirePayload maxRangeFire() {
        return new ScopeFirePayload(FireMode.MAX_RANGE, 0, 0, 0);
    }

    public static void handle(ScopeFirePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            ItemStack weapon = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (weapon.isEmpty()) return;

            // 验证主手持有可发射武器（防止伪造请求）
            boolean hasFireableWeapon = weapon.getItem() instanceof com.piranport.artillery.ArtilleryItem
                    || weapon.getItem() instanceof com.piranport.item.TorpedoLauncherItem
                    || weapon.getItem() instanceof com.piranport.item.MissileLauncherItem
                    || weapon.getItem() instanceof com.piranport.item.DepthChargeLauncherItem
                    || weapon.getItem() instanceof com.piranport.item.AircraftItem;
            if (!hasFireableWeapon) return;

            if (com.piranport.config.ModCommonConfig.isShipCoreGuiEnabled()) {
                return;
            }

            switch (payload.mode()) {
                case MAX_RANGE -> {
                    ShipCoreCombat.fireMaxRange(player, weapon);
                }
                case AIMED -> {
                    double dx = payload.targetX() - player.getX();
                    double dy = payload.targetY() - player.getY();
                    double dz = payload.targetZ() - player.getZ();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    // 使用服务器模拟距离限制，而非硬编码300格
                    int simDist = player.server.getPlayerList().getSimulationDistance();
                    double maxRange = Math.max(64.0, simDist * 16.0);
                    if (distSq > maxRange * maxRange) {
                        PiranPort.LOGGER.warn("ScopeFirePayload target too far ({}m), fallback to max-range", Math.sqrt(distSq));
                        ShipCoreCombat.fireMaxRange(player, weapon);
                    } else {
                        ShipCoreCombat.fireFromScope(player, weapon,
                                payload.targetX(), payload.targetY(), payload.targetZ());
                    }
                }
                case QUICK_FIRE -> {
                    ShipCoreCombat.tryFireFromInventory(player.level(), player, InteractionHand.MAIN_HAND);
                }
            }
        });
    }
}
