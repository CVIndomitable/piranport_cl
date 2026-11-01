package com.piranport.rigging;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家舰装的挂点快照 —— 服务端算好，原样同步给客户端渲染。
 *
 * <p><b>为什么整包同步而不让客户端自己算</b>：远程玩家的背包不在客户端手里，
 * 旧实现只能靠主/副手猜装备，看不到别人真正装了什么。
 *
 * <p><b>不持久化</b>：里面是 ItemStack 展示副本，只用于视觉；服务端每 {@code SYNC_INTERVAL}
 * tick 会重建一次，存档里没有必要保留（AttachmentType 只 sync 不 serialize）。
 */
public record RiggingLoadout(
        List<RiggingMount> mounts,
        boolean hasAircraft,
        int hullProfile,
        int selectedSlot) {

    // hullProfile 的取值：服务端按皮肤 ID 判定，客户端只负责转成枚举。
    // 与旧 ShipRiggingLayer.resolveHullProfile 的映射保持一致。
    public static final int HULL_DEFAULT = 0;
    public static final int HULL_CARRIER = 1;
    public static final int HULL_DESTROYER = 2;
    public static final int HULL_CRUISER = 3;
    public static final int HULL_SUBMARINE = 4;

    /**
     * 未变身 / 尚未同步的占位快照。
     *
     * <p>{@code selectedSlot} 用 -1 当哨兵：真实快照的选中槽永远是 0~8，
     * 客户端据此区分「服务端说没变身」与「已变身但快捷栏里没武器」。
     */
    public static final RiggingLoadout EMPTY =
            new RiggingLoadout(List.of(), false, HULL_DEFAULT, -1);

    public RiggingLoadout {
        mounts = List.copyOf(mounts);
    }

    /** 是否为「没有挂点快照」，而不是「有快照但没装武器」。 */
    public boolean isEmpty() {
        return selectedSlot < 0;
    }

    /**
     * 廉价内容指纹，供服务端判断要不要重新发包。
     *
     * <p>只混入「挂点 + 物品注册 ID + 选中槽 + 舰种」，<b>不</b>用 {@code ItemStack.hashCode()}：
     * 后者包含组件数据，装填计时/燃料等组件每 tick 都在变，会把指纹打散成每 10 tick 都同步。
     * 展示副本上的组件允许滞后，Phase 1 只需要装备种类正确。
     *
     * <p><b>不能用 record 自带的 equals 做变化检测</b>：ItemStack 没有重写 equals，
     * record 的相等性会退化成引用比较，每次重建快照都「不等」。
     */
    public long fingerprint() {
        long hash = 17L;
        hash = hash * 31 + mounts.size();
        for (RiggingMount mount : mounts) {
            hash = hash * 31 + mount.socket().ordinal();
            hash = hash * 31 + mount.category().ordinal();
            hash = hash * 31 + BuiltInRegistries.ITEM.getId(mount.stack().getItem());
        }
        hash = hash * 31 + (hasAircraft ? 1 : 0);
        hash = hash * 31 + hullProfile;
        hash = hash * 31 + selectedSlot;
        return hash;
    }

    /** 挂点数量上限：解码时据此拒绝损坏/恶意的包。 */
    private static final int MAX_MOUNTS = RiggingSocket.count();

    public static final StreamCodec<RegistryFriendlyByteBuf, RiggingLoadout> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public RiggingLoadout decode(RegistryFriendlyByteBuf buf) {
                    int count = buf.readVarInt();
                    if (count < 0 || count > MAX_MOUNTS) {
                        throw new IllegalArgumentException("Invalid rigging mount count: " + count);
                    }
                    List<RiggingMount> mounts = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        RiggingSocket socket = RiggingSocket.byId(buf.readVarInt());
                        RiggingCategory category = RiggingCategory.byId(buf.readVarInt());
                        ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
                        mounts.add(new RiggingMount(socket, category, stack));
                    }
                    boolean hasAircraft = buf.readBoolean();
                    int hullProfile = buf.readVarInt();
                    int selectedSlot = buf.readVarInt() - 1;
                    return new RiggingLoadout(mounts, hasAircraft, hullProfile, selectedSlot);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, RiggingLoadout loadout) {
                    buf.writeVarInt(loadout.mounts().size());
                    for (RiggingMount mount : loadout.mounts()) {
                        buf.writeVarInt(mount.socket().ordinal());
                        buf.writeVarInt(mount.category().ordinal());
                        ItemStack.STREAM_CODEC.encode(buf, mount.stack());
                    }
                    buf.writeBoolean(loadout.hasAircraft());
                    buf.writeVarInt(loadout.hullProfile());
                    // selectedSlot 可能是 -1（哨兵），varint 不写负数，统一 +1 编码。
                    buf.writeVarInt(loadout.selectedSlot() + 1);
                }
            };
}
